package com.tpjy.service;

import com.tpjy.common.Constants;
import com.tpjy.dto.AlertItem;
import com.tpjy.dto.AutoReading;
import com.tpjy.dto.CardOcrResult;
import com.tpjy.dto.MassReading;
import com.tpjy.dto.RecognitionResult;
import com.tpjy.dto.RecognizeOptions;
import com.tpjy.dto.RecognizedReading;
import com.tpjy.service.frame.Frame;
import com.tpjy.service.frame.FrameExtractor;
import com.tpjy.service.ocr.CardOcrService;
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
    private final CardOcrService cardOcrService;
    private final QrDecoder qrDecoder = new QrDecoder();
    private final int intervalSeconds;

    public RecognitionService(FrameExtractor frameExtractor, OcrService ocrService, int intervalSeconds) {
        this(frameExtractor, ocrService, intervalSeconds, null);
    }

    public RecognitionService(FrameExtractor frameExtractor, OcrService ocrService,
                              int intervalSeconds, CardOcrService cardOcrService) {
        this.frameExtractor = frameExtractor;
        this.ocrService = ocrService;
        this.cardOcrService = cardOcrService;
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

        // 2. 身份优先：先读开头身份卡，校验员不匹配时不继续跑完整视频示值识别。
        CardOcrResult cardOcr = null;
        boolean cardOcrUsed = false;
        if (cardOcrService != null && !frames.isEmpty()) {
            cardOcr = cardOcrService.recognize(frames, opt.getKnownBalanceCodes(),
                    opt.getPersonName(), opt);
            if (cardOcr != null && cardOcr.isAvailable()) {
                result.setCardOcrText(cardOcr.getRawText());
                result.setExpectedCheckerCardCode(opt.getExpectedCheckerCardCode());
                result.setCheckerCardCode(cardOcr.getCheckerCardCode());
                checkCheckerCard(cardOcr, opt, result);
                if (cardOcr.getCheckerCardCode() != null
                        || cardOcr.getBalanceCode() != null
                        || cardOcr.getPersonName() != null) {
                    cardOcrUsed = true;
                }
                if (result.hasAlert("CHECKER_MISMATCH")
                        || result.hasAlert("CHECKER_CARD_UNCONFIGURED")) {
                    String cardBalance = cardOcr.getBalanceCode() != null
                            ? cardOcr.getBalanceCode() : opt.getBalanceCode();
                    String cardPerson = cardOcr.getPersonName() != null
                            ? cardOcr.getPersonName() : opt.getPersonName();
                    result.setPersonFromQr(cardOcr.getPersonName());
                    result.setBalanceFromQr(cardOcr.getBalanceCode());
                    result.setEffectivePersonName(cardPerson);
                    result.setEffectiveBalanceCode(cardBalance);
                    result.setIdentityMethod("文字卡 OCR");
                    if (cardOcr.getBalanceCode() != null) {
                        result.addAlert("BALANCE_CARD_OCR", "INFO",
                                "文字身份卡 OCR 已识别天平：" + cardOcr.getBalanceCode(), null);
                    }
                    result.setBlockingMessage(result.hasAlert("CHECKER_MISMATCH")
                            ? "视频检验员身份和登录身份不匹配，不予校验"
                            : "当前登录校验员未配置身份卡，无法完成身份匹配，不予校验");
                    result.setElapsedMillis(System.currentTimeMillis() - start);
                    return result;
                }
            } else {
                result.setExpectedCheckerCardCode(opt.getExpectedCheckerCardCode());
                result.addAlert("CHECKER_CARD_MISSING", "WARN",
                        "本地 OCR 未返回校验员卡结果，无法自动匹配当前登录校验员", null);
            }
        }

        // 3. 身份卡匹配后再做二维码抽样扫描。
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
        boolean qrPersonFound = personFromQr != null;
        boolean qrBalanceFound = balanceFromQr != null;

        if (cardOcr != null && cardOcr.isAvailable()) {
            applyNameplateRanges(cardOcr, opt, result);
            if (personFromQr == null && cardOcr.getPersonName() != null) {
                personFromQr = cardOcr.getPersonName();
                cardOcrUsed = true;
            }
            if (balanceFromQr == null && cardOcr.getBalanceCode() != null) {
                balanceFromQr = cardOcr.getBalanceCode();
                cardOcrUsed = true;
            }
            if (cardOcr.hasCompleteNameplateRanges()) {
                cardOcrUsed = true;
            }
        }
        result.setPersonFromQr(personFromQr);
        result.setBalanceFromQr(balanceFromQr);
        if (qrPersonFound || qrBalanceFound) {
            result.setIdentityMethod(cardOcrUsed ? "二维码 + 文字卡 OCR" : "二维码");
        } else if (cardOcrUsed) {
            result.setIdentityMethod("文字卡 OCR");
        } else {
            result.setIdentityMethod("人工指定");
        }
        if (cardOcrUsed && balanceFromQr != null && !qrBalanceFound) {
            result.addAlert("BALANCE_CARD_OCR", "INFO",
                    "文字身份卡 OCR 已识别天平：" + balanceFromQr, null);
        }
        if (cardOcrUsed && personFromQr != null && !qrPersonFound) {
            result.addAlert("PERSON_CARD_OCR", "INFO",
                    "文字身份卡 OCR 已识别人员：" + personFromQr, null);
        }
        String effectivePerson = personFromQr != null ? personFromQr : opt.getPersonName();
        String effectiveBalance = balanceFromQr != null ? balanceFromQr : opt.getBalanceCode();
        result.setEffectivePersonName(effectivePerson);
        result.setEffectiveBalanceCode(effectiveBalance);

        // 4. 自动查对：人员一致性 + 天平是否登记
        if (personFromQr != null && opt.getPersonName() != null && !opt.getPersonName().trim().isEmpty()
                && !personFromQr.trim().equals(opt.getPersonName().trim())) {
            result.addAlert("PERSON_MISMATCH", "WARN",
                    "拍摄人员（" + personFromQr + "）与登录账号（" + opt.getPersonName() + "）不一致，不予记录，请核对人员卡或改用本人账号",
                    personSecond);
        }
        if (personFromQr == null && (cardOcr == null || cardOcr.getCheckerCardCode() == null)) {
            result.addAlert("QR_MISSING", "INFO",
                    "未识别到人员二维码或校验员实体卡，已按当前登录/指定人员（"
                            + (opt.getPersonName() == null ? "未指定" : opt.getPersonName()) + "）记录",
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
                    "未识别到天平身份卡，已按指定天平（" + (opt.getBalanceCode() == null ? "未指定" : opt.getBalanceCode()) + "）记录",
                    null);
        }
        if (effectiveBalance != null && opt.getKnownBalanceCodes() != null
                && !opt.getKnownBalanceCodes().isEmpty()
                && !opt.getKnownBalanceCodes().contains(effectiveBalance.trim())) {
            result.addAlert("BALANCE_NOT_FOUND", "WARN",
                    "选定天平 " + effectiveBalance + " 未在系统中登记，请先登记后再提交",
                    null);
        }

        // 5. 9 次示值：OCR 预填（默认 Noop 返回 null → 人工补录），manualValues 优先
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
            if (r.getValue() == null && cardOcr != null) {
                for (AutoReading auto : cardOcr.getAutoReadings()) {
                    if (auto.getSeq() != seq || auto.getGrams() == null) {
                        continue;
                    }
                    r.setRawValue(auto.getRaw());
                    r.setDisplayUnit(auto.getUnit());
                    r.setValue(auto.getGrams());
                    r.setSecond(auto.getSecond());
                    r.setClock(auto.getSecond() == null ? null : AlertItem.formatClock(auto.getSecond()));
                    r.setMeasureTime(measureTime(checkDate, auto.getSecond()));
                    r.setAutoRecognized(true);
                    r.setOcrConfidence(auto.getConfidence());
                    break;
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

        int autoDetected = 0;
        for (RecognizedReading r : readings) {
            if (r.isAutoRecognized() && r.getValue() != null) {
                autoDetected++;
            }
        }
        if (autoDetected > 0) {
            result.setOcrImpl("RapidOCR-local（数字示值）");
            result.addAlert("AUTO_READING_OCR", "INFO",
                    "本地数字 OCR 已自动预填 " + autoDetected + "/9 次稳定示值", null);
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

    private static void checkCheckerCard(CardOcrResult cardOcr, RecognizeOptions opt,
                                         RecognitionResult result) {
        String actual = normalizeCardCode(cardOcr.getCheckerCardCode());
        String expected = normalizeCardCode(opt.getExpectedCheckerCardCode());
        result.setCheckerCardCode(actual);
        result.setExpectedCheckerCardCode(expected);
        if (actual == null) {
            result.setCheckerCardMatched(null);
            result.addAlert("CHECKER_CARD_MISSING", "WARN",
                    "未识别到校验员实体卡（如 JY001），无法自动确认当前操作人员", null);
            return;
        }

        result.addAlert("CHECKER_CARD_OCR", "INFO",
                "本地 OCR 已识别校验员卡：" + actual,
                cardOcr.getCheckerCardSecond());
        if (expected == null) {
            result.setCheckerCardMatched(false);
            result.addAlert("CHECKER_CARD_UNCONFIGURED", "WARN",
                    "当前登录校验员未配置实体卡号，无法与视频中的 " + actual + " 自动匹配",
                    cardOcr.getCheckerCardSecond());
            return;
        }
        boolean matched = actual.equals(expected);
        result.setCheckerCardMatched(matched);
        if (matched) {
            result.addAlert("CHECKER_CARD_MATCH", "INFO",
                    "校验员卡 " + actual + " 与当前登录校验员一致",
                    cardOcr.getCheckerCardSecond());
        } else {
            result.addAlert("CHECKER_MISMATCH", "WARN",
                    "视频中的校验员卡为 " + actual + "，当前登录校验员绑定的卡为 " + expected
                            + "，人员不一致，不予记录，请核对校验员卡或改用本人账号",
                    cardOcr.getCheckerCardSecond());
        }
    }

    private static void applyNameplateRanges(CardOcrResult cardOcr, RecognizeOptions opt,
                                             RecognitionResult result) {
        if (!cardOcr.hasCompleteNameplateRanges()) {
            result.setPrecisionSource("数据库主数据");
            result.addAlert("BALANCE_NAMEPLATE_NOT_FOUND", "INFO",
                    "未完整识别到天平铭牌的双量程精度，按数据库主数据执行位数校验",
                    cardOcr.getNameplateSecond());
            return;
        }

        String range1Precision = cardOcr.getNameplateRange1Precision();
        String range2Precision = cardOcr.getNameplateRange2Precision();
        BigDecimal range1Capacity = cardOcr.getNameplateRange1Capacity();
        BigDecimal range2Capacity = cardOcr.getNameplateRange2Capacity();
        result.setNameplateRangeText(rangeSummary(range1Capacity, range1Precision,
                range2Capacity, range2Precision));
        result.setPrecisionSource("天平铭牌 OCR");

        boolean compared = opt.getDatabaseRange1Precision() != null
                || opt.getDatabaseRange2Precision() != null;
        if (compared) {
            boolean same = TpjyUtils.samePrecision(range1Precision,
                    opt.getDatabaseRange1Precision())
                    && TpjyUtils.samePrecision(range2Precision,
                    opt.getDatabaseRange2Precision())
                    && sameCapacity(range1Capacity, opt.getDatabaseRange1CapacityGrams())
                    && sameCapacity(range2Capacity, opt.getDatabaseRange2CapacityGrams());
            if (same) {
                result.addAlert("BALANCE_NAMEPLATE_MATCH", "INFO",
                        "天平铭牌量程与数据库配置一致（" + result.getNameplateRangeText() + "）",
                        cardOcr.getNameplateSecond());
            } else {
                result.addAlert("BALANCE_NAMEPLATE_MISMATCH", "WARN",
                        "天平铭牌量程与数据库配置不一致：铭牌=" + result.getNameplateRangeText()
                                + "；数据库=" + rangeSummary(opt.getDatabaseRange1CapacityGrams(),
                                opt.getDatabaseRange1Precision(), opt.getDatabaseRange2CapacityGrams(),
                                opt.getDatabaseRange2Precision()),
                        cardOcr.getNameplateSecond());
            }
        } else {
            result.addAlert("BALANCE_NAMEPLATE_OCR", "INFO",
                    "已从天平铭牌识别量程：" + result.getNameplateRangeText(),
                    cardOcr.getNameplateSecond());
        }

        // 铭牌可读时以铭牌为准；不可读时前面已保留数据库配置。
        opt.setRange1CapacityGrams(range1Capacity);
        opt.setRange2CapacityGrams(range2Capacity);
        opt.applyRangePrecisions(range1Precision, range2Precision);
        opt.setPrecisionText(range1Precision);
    }

    private static boolean sameCapacity(BigDecimal left, BigDecimal right) {
        return left == null || right == null || left.compareTo(right) == 0;
    }

    private static String rangeSummary(BigDecimal capacity1, String precision1,
                                       BigDecimal capacity2, String precision2) {
        StringBuilder text = new StringBuilder();
        if (capacity1 != null || precision1 != null) {
            text.append("第一量程 ");
            text.append(capacity1 == null ? "?" : capacity1.stripTrailingZeros().toPlainString());
            text.append("g / d=").append(precision1 == null ? "? " : precision1);
        }
        if (capacity2 != null || precision2 != null) {
            if (text.length() > 0) {
                text.append("；");
            }
            text.append("第二量程 ");
            text.append(capacity2 == null ? "?" : capacity2.stripTrailingZeros().toPlainString());
            text.append("g / d=").append(precision2 == null ? "?" : precision2);
        }
        return text.toString();
    }

    private static String normalizeCardCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        return code.toUpperCase().replace(" ", "").trim();
    }

    private static String measureTime(Date checkDate, Integer second) {
        if (checkDate == null || second == null) {
            return null;
        }
        return TpjyUtils.formatDate(new Date(checkDate.getTime() + second * 1000L));
    }
}
