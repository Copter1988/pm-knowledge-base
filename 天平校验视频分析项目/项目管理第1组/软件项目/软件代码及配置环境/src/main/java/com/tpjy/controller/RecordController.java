package com.tpjy.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tpjy.common.BizException;
import com.tpjy.common.Constants;
import com.tpjy.common.LoginUser;
import com.tpjy.config.AppProps;
import com.tpjy.dto.MassReading;
import com.tpjy.dto.RecognitionResult;
import com.tpjy.dto.RecognizeOptions;
import com.tpjy.dto.RecognizedReading;
import com.tpjy.dto.StoredVideoInfo;
import com.tpjy.entity.Balance;
import com.tpjy.entity.CalibrationRecord;
import com.tpjy.mapper.BalanceMapper;
import com.tpjy.mapper.CalibrationRecordMapper;
import com.tpjy.service.JudgeService;
import com.tpjy.service.RecognitionService;
import com.tpjy.service.RecordService;
import com.tpjy.service.video.VideoStorageService;
import com.tpjy.util.TpjyUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 校验记录管理（WBS 1.3.3，成员乙）——记录落库 / 提交 / 作废的页面入口。
 *
 * <p>甲已在 {@link CheckerController} 提供识别链路；本控制器不改甲代码，改用
 * {@code /checker/record/**} 子路径，复用甲注册的 RecognitionService / VideoStorageService
 * 等 Bean，完成「导入 → 核对 → 提交」闭环。
 */
@Controller
@RequestMapping("/checker/record")
public class RecordController {

    private static final String S_RESULT = "recordResult";
    private static final String S_VIDEO = "recordVideo";
    private static final String S_BALANCE_ID = "recordBalanceId";

    @Resource
    private BalanceMapper balanceMapper;
    @Resource
    private VideoStorageService videoStorageService;
    @Resource
    private RecognitionService recognitionService;
    @Resource
    private RecordService recordService;
    @Resource
    private JudgeService judgeService;
    @Resource
    private CalibrationRecordMapper recordMapper;
    @Resource
    private AppProps props;

    @GetMapping("/import")
    public String importPage(Model model) {
        model.addAttribute("balances", balanceMapper.selectList(null));
        return "checker/record-import";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("video") MultipartFile video,
                         @RequestParam("balanceId") Long balanceId,
                         @RequestParam(value = "videoNo", required = false) String videoNo,
                         HttpSession session, Model model) {
        model.addAttribute("balances", balanceMapper.selectList(null));
        try {
            Balance balance = balanceMapper.selectById(balanceId);
            if (balance == null) {
                throw new BizException("所选天平不存在");
            }
            LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
            StoredVideoInfo stored = videoStorageService.store(video);

            RecognizeOptions opt = new RecognizeOptions();
            opt.setPersonName(user == null ? null : user.getRealName());
            opt.setExpectedCheckerCardCode(user == null ? null : user.getCardCode());
            opt.setBalanceCode(balance.getBalanceCode());
            opt.setCheckDate(stored.getStoredAt());
            opt.setKnownBalanceCodes(balanceCodes());
            opt.setWeights(weightsOf(balance));
            BigDecimal range1 = capacityGrams(balance.getRange1());
            BigDecimal range2 = capacityGrams(balance.getRange2());
            opt.setRange1CapacityGrams(range1);
            opt.setRange2CapacityGrams(range2);
            opt.setDatabaseRange1Precision(balance.getRange1Precision());
            opt.setDatabaseRange2Precision(balance.getRange2Precision());
            opt.setDatabaseRange1CapacityGrams(range1);
            opt.setDatabaseRange2CapacityGrams(range2);
            opt.applyRangePrecisions(balance.getRange1Precision(), balance.getRange2Precision());
            opt.setPrecisionText(balance.getRange1Precision());
            opt.setVideoNo(blankToNull(videoNo));
            opt.setFrameOutputDir(new File(props.getFrameDir(), "record-" + System.currentTimeMillis()));

            RecognitionResult result = recognitionService.recognize(stored.getFile(), opt);
            result.setCheckDate(TpjyUtils.formatDate(stored.getStoredAt()));
            result.setReadings(prepareReadings(result.getReadings(), balance));

            session.setAttribute(S_RESULT, result);
            session.setAttribute(S_VIDEO, stored);
            session.setAttribute(S_BALANCE_ID, balanceId);

            model.addAttribute("result", result);
            model.addAttribute("videoUrl", videoUrl(stored));
            return "checker/record-verify";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "checker/record-import";
        }
    }

    @PostMapping("/submit")
    public String submit(@RequestParam(value = "v", required = false) String[] values,
                         @RequestParam(value = "mt", required = false) String[] measureTimes,
                         @RequestParam(value = "cleanOk", required = false) Integer cleanOk,
                         @RequestParam(value = "levelOk", required = false) Integer levelOk,
                         @RequestParam(value = "calibOk", required = false) String calibOk,
                         @RequestParam(value = "remark", required = false) String remark,
                         HttpSession session, Model model) {
        RecognitionResult result = (RecognitionResult) session.getAttribute(S_RESULT);
        StoredVideoInfo video = (StoredVideoInfo) session.getAttribute(S_VIDEO);
        Long balanceId = (Long) session.getAttribute(S_BALANCE_ID);
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        try {
            if (result == null || video == null || balanceId == null) {
                throw new BizException("核对信息已失效，请重新导入视频");
            }
            List<RecognizedReading> readings = buildReadings(values, measureTimes, result);
            recordService.createRecord(result, video, balanceId, user,
                    cleanOk != null && cleanOk == 1,
                    levelOk != null && levelOk == 1,
                    calibOk, remark, readings);
            clearSession(session);
            return "redirect:/checker/records";
        } catch (Exception e) {
            model.addAttribute("balances", balanceMapper.selectList(null));
            model.addAttribute("result", result);
            if (video != null) {
                model.addAttribute("videoUrl", videoUrl(video));
            }
            model.addAttribute("error", e.getMessage());
            return "checker/record-verify";
        }
    }

    @GetMapping("/list")
    public String list(HttpSession session, Model model) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        List<CalibrationRecord> records = recordMapper.selectList(
                new QueryWrapper<CalibrationRecord>()
                        .eq("checker_id", user == null ? null : user.getUserId())
                        .in("status", Constants.STATUS_DRAFT, Constants.STATUS_PENDING, Constants.STATUS_REJECTED)
                        .orderByDesc("check_date"));
        for (CalibrationRecord r : records) {
            fillBalance(r);
        }
        model.addAttribute("records", records);
        return "checker/record-list";
    }

    @PostMapping("/abandon")
    public String abandon(@RequestParam("recordId") Long recordId,
                          @RequestParam(value = "reason", required = false) String reason,
                          HttpSession session) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        recordService.abandon(recordId, user, reason);
        return "redirect:/checker/record/list";
    }

    private void fillBalance(CalibrationRecord r) {
        if (r.getBalanceId() != null) {
            Balance b = balanceMapper.selectById(r.getBalanceId());
            if (b != null) {
                r.setBalanceCode(b.getBalanceCode());
            }
        }
    }

    /** 由表单 9 行（seq 1~9）组装最终示值：优先人工提交值，缺省回退识别值。 */
    private List<RecognizedReading> buildReadings(String[] values, String[] measureTimes,
                                                  RecognitionResult result) {
        List<RecognizedReading> out = new ArrayList<RecognizedReading>();
        for (int seq = 1; seq <= 9; seq++) {
            RecognizedReading original = originalBySeq(result.getReadings(), seq);
            RecognizedReading r = new RecognizedReading();
            r.setSeq(seq);
            r.setLevel(judgeService.groupLevel(seq));

            String text = valueAt(values, seq - 1);
            if (text != null && !text.trim().isEmpty()) {
                MassReading mass = MassReading.parse(text);
                r.setValue(mass.getGrams());
                r.setRawValue(mass.getDisplayValue().stripTrailingZeros().toPlainString());
                r.setDisplayUnit(mass.getUnit());
            } else if (original != null) {
                r.setValue(original.getValue());
                r.setRawValue(original.getRawValue());
                r.setDisplayUnit(original.getDisplayUnit());
            }

            String mt = valueAt(measureTimes, seq - 1);
            if (mt != null && !mt.trim().isEmpty()) {
                r.setMeasureTime(mt);
            } else if (original != null) {
                r.setMeasureTime(original.getMeasureTime());
            }
            out.add(r);
        }
        return out;
    }

    private RecognizedReading originalBySeq(List<RecognizedReading> readings, int seq) {
        if (readings == null) {
            return null;
        }
        for (RecognizedReading r : readings) {
            if (r != null && r.getSeq() == seq) {
                return r;
            }
        }
        return null;
    }

    private String valueAt(String[] arr, int index) {
        if (arr == null || index < 0 || index >= arr.length) {
            return null;
        }
        return arr[index];
    }

    private String videoUrl(StoredVideoInfo stored) {
        String relative = stored.getRelativePath().replace('\\', '/');
        int idx = relative.indexOf("/upload/");
        return idx >= 0 ? relative.substring(idx) : relative;
    }

    private void clearSession(HttpSession session) {
        session.removeAttribute(S_RESULT);
        session.removeAttribute(S_VIDEO);
        session.removeAttribute(S_BALANCE_ID);
    }

    private List<String> balanceCodes() {
        List<String> codes = new ArrayList<String>();
        for (Balance b : balanceMapper.selectList(null)) {
            codes.add(b.getBalanceCode());
        }
        return codes;
    }

    private static List<BigDecimal> weightsOf(Balance b) {
        List<BigDecimal> weights = new ArrayList<BigDecimal>();
        weights.add(b.getWeightL1());
        weights.add(b.getWeightL2());
        weights.add(b.getWeightL3());
        return weights;
    }

    private static BigDecimal capacityGrams(String rangeText) {
        if (rangeText == null || rangeText.trim().isEmpty() || "/".equals(rangeText.trim())) {
            return null;
        }
        try {
            return MassReading.parse(rangeText).getGrams();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    /** 把识别 readings 归一化为 9 条（seq 1~9），缺的补空占位，并回填标称值供核对页展示。 */
    private List<RecognizedReading> prepareReadings(List<RecognizedReading> readings, Balance balance) {
        Map<Integer, RecognizedReading> bySeq = new HashMap<Integer, RecognizedReading>();
        if (readings != null) {
            for (RecognizedReading r : readings) {
                if (r != null && r.getSeq() >= 1 && r.getSeq() <= 9) {
                    bySeq.put(r.getSeq(), r);
                }
            }
        }
        List<RecognizedReading> out = new ArrayList<RecognizedReading>();
        for (int seq = 1; seq <= 9; seq++) {
            RecognizedReading r = bySeq.get(seq);
            if (r == null) {
                r = new RecognizedReading();
                r.setSeq(seq);
                r.setLevel(judgeService.groupLevel(seq));
            }
            if (r.getNominal() == null) {
                r.setNominal(nominalOf(r.getLevel(), balance));
            }
            out.add(r);
        }
        return out;
    }

    private BigDecimal nominalOf(String level, Balance balance) {
        if ("L1".equals(level)) {
            return balance.getWeightL1();
        }
        if ("L2".equals(level)) {
            return balance.getWeightL2();
        }
        return balance.getWeightL3();
    }
}
