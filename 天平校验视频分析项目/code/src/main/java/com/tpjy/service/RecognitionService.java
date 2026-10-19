package com.tpjy.service;

import com.tpjy.common.Constants;
import com.tpjy.dto.AlertItem;
import com.tpjy.dto.MassReading;
import com.tpjy.dto.RecognitionResult;
import com.tpjy.dto.RecognizeOptions;
import com.tpjy.dto.RecognizedReading;
import com.tpjy.service.frame.Frame;
import com.tpjy.service.frame.FrameExtractor;
import com.tpjy.service.ocr.OcrService;
import com.tpjy.service.qr.QrDecoder;
import com.tpjy.util.TpjyUtils;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 视频导入与视觉识别（WBS 1.3.2，甲）。
 *
 * <p>处理链：抽帧 → ZXing 解人员卡/天平卡 → 自动查对 → OCR 预填 9 次示值 → 帧时间戳回填读数时间 → 四类提示。
 * 纯逻辑、无数据库依赖，可被命令行（AnalyzeCli）或页面（乙）直接调用。
 *
 * <p>性能权衡：一张帧图做 5 种变体解码较慢，故二维码扫描采用「抽样 + 找到即停」策略，
 * 保证整链 ≤ 2 分钟（NFR-001）。
 */
public class RecognitionService {

    /** 二维码最大扫描帧数（抽样上限，控制耗时）。 */
    private static final int QR_SCAN_BUDGET = 80;

    private final FrameExtractor frameExtractor;
    private final OcrService ocrService;
    private final QrDecoder qrDecoder = new QrDecoder();
    private final int intervalSeconds;

    public RecognitionService(FrameExtractor frameExtractor, OcrService ocrService, int intervalSeconds) {
        this.frameExtractor = frameExtractor;
        this.ocrService = ocrService;
        this.intervalSeconds = intervalSeconds <= 0 ? 1 : intervalSeconds;
    }

    /**
     * 识别一个校验视频。
     *
     * @param video 视频文件
     * @param opt   视频之外的上下文（人员、天平、砝码标称值、人工补录值等）
     */
    public RecognitionResult recognize(File video, RecognizeOptions opt) throws Exception {
        if (opt == null) {
            opt = new RecognizeOptions();
        }
        long start = System.currentTimeMillis();
        RecognitionResult result = new RecognitionResult();
        result.setVideoPath(video == null ? null : video.getAbsolutePath());
        result.setVideoNo(opt.getVideoNo());
        result.setOcrImpl(ocrService == null ? "未配置" : ocrService.name());
        result.setFrameInterval(intervalSeconds);
        Date checkDate = opt.getCheckDate() != null
                ? opt.getCheckDate()
                : new Date(video == null ? System.currentTimeMillis() : video.lastModified());
        result.setCheckDate(TpjyUtils.formatDate(checkDate));

        File frameDir = opt.getFrameOutputDir() != null ? opt.getFrameOutputDir() : new File("./upload/frame/" + System.currentTimeMillis());

        // 1. 抽帧
        List<Frame> frames = frameExtractor.extract(video, frameDir);
        result.setFrameCount(frames.size());

        // 2. 二维码：抽样扫描，人员卡与天平卡都找到即停
        String personFromQr = null;
        String balanceFromQr = null;
        Integer personSecond = null;
        Integer balanceSecond = null;
        int stride = Math.max(1, frames.size() / QR_SCAN_BUDGET);
        for (int i = 0; i < frames.size(); i += stride) {
            if (personFromQr != null && balanceFromQr != null) {
                break;
            }
            Frame frame = frames.get(i);
            List<String> texts = qrDecoder.decode(frame.getFile());
            if (texts.isEmpty()) {
                continue;
            }
            if (personFromQr == null) {
                String p = qrDecoder.pickByPrefix(texts, Constants.QR_PREFIX_PERSON);
                if (p != null && !p.isEmpty()) {
                    personFromQr = p;
                    personSecond = frame.getSecond();
                }
            }
            if (balanceFromQr == null) {
                String b = qrDecoder.pickByPrefix(texts, Constants.QR_PREFIX_BALANCE);
                if (b != null && !b.isEmpty()) {
                    balanceFromQr = b;
                    balanceSecond = frame.getSecond();
                }
            }
        }
        result.setPersonFromQr(personFromQr);
        result.setBalanceFromQr(balanceFromQr);
        String effectivePerson = personFromQr != null ? personFromQr : opt.getPersonName();
        String effectiveBalance = balanceFromQr != null ? balanceFromQr : opt.getBalanceCode();
        result.setEffectivePersonName(effectivePerson);
        result.setEffectiveBalanceCode(effectiveBalance);

        // 3. 自动查对：人员一致性 + 天平是否登记
        if (personFromQr != null && opt.getPersonName() != null && !opt.getPersonName().trim().isEmpty()
                && !personFromQr.trim().equals(opt.getPersonName().trim())) {
            result.addAlert("PERSON_MISMATCH", "WARN",
                    "拍摄人员（" + personFromQr + "）与登录账号（" + opt.getPersonName() + "）不一致，不予记录，请核对人员卡或改用本人账号",
                    personSecond);
        }
        if (personFromQr == null) {
            result.addAlert("QR_MISSING", "INFO",
                    "未识别到人员二维码卡片，已按当前登录/指定人员（" + (opt.getPersonName() == null ? "未指定" : opt.getPersonName()) + "）记录",
                    null);
        }
        if (balanceFromQr != null && opt.getKnownBalanceCodes() != null
                && !opt.getKnownBalanceCodes().isEmpty()
                && !opt.getKnownBalanceCodes().contains(balanceFromQr.trim())) {
            result.addAlert("BALANCE_NOT_FOUND", "WARN",
                    "天平 " + balanceFromQr + " 未在系统中登记，请回看视频确认天平编号或先登记该天平",
                    balanceSecond);
        }
        if (balanceFromQr == null) {
            result.addAlert("QR_MISSING", "INFO",
                    "未识别到天平二维码卡片，已按指定天平（" + (opt.getBalanceCode() == null ? "未指定" : opt.getBalanceCode()) + "）记录",
                    null);
        }
        if (effectiveBalance != null && opt.getKnownBalanceCodes() != null
                && !opt.getKnownBalanceCodes().isEmpty()
                && !opt.getKnownBalanceCodes().contains(effectiveBalance.trim())) {
            result.addAlert("BALANCE_NOT_FOUND", "WARN",
                    "选定天平 " + effectiveBalance + " 未在系统中登记，请先登记后再提交",
                    null);
        }

        // 4. 9 次示值：OCR 预填（默认 Noop 返回 null → 人工补录），manualValues 优先
        List<RecognizedReading> readings = new ArrayList<RecognizedReading>();
        int manualIdx = 0;
        for (int seq = 1; seq <= 9; seq++) {
            RecognizedReading r = new RecognizedReading();
            r.setSeq(seq);
            r.setLevel(new JudgeService().groupLevel(seq));
            if (opt.getManualValues() != null && manualIdx < opt.getManualValues().size()) {
                String raw = opt.getManualValues().get(manualIdx);
                manualIdx++;
                if (raw != null && !raw.trim().isEmpty()) {
                    try {
                        MassReading mass = MassReading.parse(raw);
                        if (!mass.isUsableMeasurement()) {
                            result.addAlert(mass.isStandbyZero() ? "STANDBY_ZERO" : "VALUE_INVALID",
                                    "WARN", "第 " + seq + " 次输入值 " + mass.getDisplayText()
                                            + " 不是有效称量值", null);
                        } else {
                            r.setRawValue(mass.getDisplayValue().toPlainString());
                            r.setDisplayUnit(mass.getUnit());
                            r.setValue(mass.getGrams());
                        }
                    } catch (IllegalArgumentException e) {
                        result.addAlert("VALUE_INVALID", "WARN",
                                "第 " + seq + " 次人工示值格式错误：" + e.getMessage(),
                                null);
                    }
                    r.setSecond(frames.isEmpty() ? null : frames.get(Math.min(frames.size() - 1,
                            (seq - 1) * frames.size() / 9)).getSecond());
                    r.setClock(r.getSecond() == null ? null : AlertItem.formatClock(r.getSecond()));
                    r.setMeasureTime(measureTime(checkDate, r.getSecond()));
                }
            }
            if (r.getValue() == null && ocrService != null && !frames.isEmpty()) {
                Frame frame = frames.get(Math.min(frames.size() - 1, (seq - 1) * frames.size() / 9));
                MassReading mass = ocrService.readValue(frame.getFile().getAbsolutePath(), seq);
                if (mass != null && mass.isUsableMeasurement()) {
                    r.setValue(mass.getGrams());
                    r.setRawValue(mass.getDisplayValue().toPlainString());
                    r.setDisplayUnit(mass.getUnit());
                    r.setSecond(frame.getSecond());
                    r.setClock(frame.getClock());
                    r.setMeasureTime(measureTime(checkDate, frame.getSecond()));
                } else if (mass != null) {
                    String type = mass.isStandbyZero() ? "STANDBY_ZERO" : "UNSTABLE_READING";
                    String message = mass.isStandbyZero()
                            ? "第 " + seq + " 次识别到待机零值 " + mass.getDisplayText() + "，不作为称量结果"
                            : "第 " + seq + " 次读数尚未稳定（" + mass.getDisplayText() + "），请等待示值稳定 1~2 秒";
                    result.addAlert(type, "INFO", message, frame.getSecond());
                }
            }
            // 砝码标称值：按分组取
            if (opt.getWeights() != null && opt.getWeights().size() >= 3) {
                BigDecimal nominal = new JudgeService().nominalOf(r.getLevel(),
                        opt.getWeights().get(0), opt.getWeights().get(1), opt.getWeights().get(2));
                r.setNominal(nominal);
                if (nominal != null && opt.getRange1CapacityGrams() != null
                        && nominal.compareTo(opt.getRange1CapacityGrams()) > 0) {
                    result.addAlert("RANGE_MISMATCH", "WARN",
                            r.getLevel() + " 砝码标称值 " + nominal.stripTrailingZeros().toPlainString()
                                    + " g 超过天平第一量程上限 "
                                    + opt.getRange1CapacityGrams().stripTrailingZeros().toPlainString() + " g",
                            r.getSecond());
                }
            }
            readings.add(r);
        }
        result.setReadings(readings);

        // 5. 位数校验（规则 5：量程精度只约束读数位数，不参与 ±0.05% 判定）
        boolean digitsConfigured = opt.getExpectedDigits() >= 0
                || opt.getExpectedDigitsL1() >= 0
                || opt.getExpectedDigitsL2() >= 0
                || opt.getExpectedDigitsL3() >= 0;
        if (digitsConfigured) {
            int levelCount = 0;
            for (RecognizedReading r : readings) {
                int expectedDigits = opt.expectedDigitsForLevel(r.getLevel(), r.getDisplayUnit());
                if (r.getRawValue() == null) {
                    continue;
                }
                if (expectedDigits < 0) {
                    continue;
                }
                levelCount++;
                int digits = TpjyUtils.digitsOfText(r.getRawValue());
                if (digits >= 0 && digits != expectedDigits) {
                    String precision = opt.precisionTextForLevel(r.getLevel());
                    result.addAlert("DIGIT_MISMATCH", "WARN",
                            "第 " + r.getSeq() + " 次示值识别为 " + r.getRawValue() + "，精度 d = "
                                    + (precision == null ? "?" : precision)
                                    + " 在 " + r.getDisplayUnit() + " 显示下应为 " + expectedDigits
                                    + " 位小数，可能存在量程 / 精度设置错误",
                            r.getSecond());
                }
            }
            if (levelCount == 0) {
                result.addAlert("RANGE_MISMATCH", "INFO",
                        "尚未拿到任何示值，无法校验读数位数（精度 d = "
                                + (opt.getPrecisionText() == null ? "量程配置" : opt.getPrecisionText()) + "）",
                        null);
            }
        }

        // 6. 未识别到示值的兜底提示
        int missing = 0;
        for (RecognizedReading r : readings) {
            if (r.getValue() == null) {
                missing++;
            }
        }
        if (missing > 0) {
            result.addAlert("OCR_MISSED", "INFO",
                    "有 " + missing + " 次示值未识别到，请人工补录（SRS：识别不到由检验人员补录兜底）",
                    null);
        }

        result.setElapsedMillis(System.currentTimeMillis() - start);
        return result;
    }

    private static String measureTime(Date checkDate, Integer second) {
        if (checkDate == null || second == null) {
            return null;
        }
        return TpjyUtils.formatDate(new Date(checkDate.getTime() + second * 1000L));
    }
}
