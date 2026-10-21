package com.tpjy.controller;

import com.tpjy.common.LoginUser;
import com.tpjy.config.AppProps;
import com.tpjy.dto.CurveData;
import com.tpjy.dto.MassReading;
import com.tpjy.dto.PageResult;
import com.tpjy.dto.RecognitionResult;
import com.tpjy.dto.RecognizeOptions;
import com.tpjy.dto.StoredVideoInfo;
import com.tpjy.entity.Balance;
import com.tpjy.entity.CalibrationRecord;
import com.tpjy.mapper.BalanceMapper;
import com.tpjy.service.CurveRenderer;
import com.tpjy.service.QueryService;
import com.tpjy.service.RecognitionService;
import com.tpjy.service.video.VideoStorageService;
import com.tpjy.util.TpjyUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** 检验人员（CHECKER）：甲的导入识别、查询、曲线入口；记录落库/提交仍由乙实现。 */
@Controller
@RequestMapping("/checker")
public class CheckerController {

    @Resource
    private BalanceMapper balanceMapper;
    @Resource
    private VideoStorageService videoStorageService;
    @Resource
    private RecognitionService recognitionService;
    @Resource
    private QueryService queryService;
    @Resource
    private CurveRenderer curveRenderer;
    @Resource
    private AppProps props;

    @GetMapping("/workbench")
    public String workbench(Model model) {
        model.addAttribute("balances", balanceMapper.selectList(null));
        return "checker/index";
    }

    @GetMapping("/new")
    public String newCheck(Model model) {
        model.addAttribute("balances", balanceMapper.selectList(null));
        return "checker/new";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("video") MultipartFile video,
                         @RequestParam("balanceId") Long balanceId,
                         @RequestParam(value = "manualValues", required = false) String manualValues,
                         @RequestParam(value = "videoNo", required = false) String videoNo,
                         HttpSession session,
                         Model model) {
        model.addAttribute("balances", balanceMapper.selectList(null));
        try {
            Balance balance = balanceMapper.selectById(balanceId);
            if (balance == null) {
                throw new IllegalArgumentException("所选天平不存在");
            }
            LoginUser user = (LoginUser) session.getAttribute(com.tpjy.common.Constants.SESSION_USER);
            StoredVideoInfo stored = videoStorageService.store(video);

            RecognizeOptions opt = new RecognizeOptions();
            opt.setPersonName(user == null ? null : user.getRealName());
            opt.setExpectedCheckerCardCode(user == null ? null : user.getCardCode());
            opt.setBalanceCode(balance.getBalanceCode());
            opt.setCheckDate(stored.getStoredAt());
            opt.setKnownBalanceCodes(balanceCodes());
            opt.setWeights(weightsOf(balance));
            BigDecimal range1Capacity = capacityGrams(balance.getRange1());
            BigDecimal range2Capacity = capacityGrams(balance.getRange2());
            opt.setRange1CapacityGrams(range1Capacity);
            opt.setRange2CapacityGrams(range2Capacity);
            opt.setDatabaseRange1Precision(balance.getRange1Precision());
            opt.setDatabaseRange2Precision(balance.getRange2Precision());
            opt.setDatabaseRange1CapacityGrams(range1Capacity);
            opt.setDatabaseRange2CapacityGrams(range2Capacity);
            opt.applyRangePrecisions(balance.getRange1Precision(), balance.getRange2Precision());
            opt.setPrecisionText(balance.getRange1Precision());
            opt.setManualValues(parseManualValues(manualValues));
            opt.setVideoNo(blankToNull(videoNo));
            opt.setFrameOutputDir(new File(props.getFrameDir(), "view-" + System.currentTimeMillis()));

            RecognitionResult result = recognitionService.recognize(stored.getFile(), opt);
            result.setCheckDate(TpjyUtils.formatDate(stored.getStoredAt()));
            model.addAttribute("result", result);
            model.addAttribute("storedVideo", stored);
            String relative = stored.getRelativePath().replace('\\', '/');
            int uploadIndex = relative.indexOf("/upload/");
            if (uploadIndex >= 0) {
                model.addAttribute("videoUrl", relative.substring(uploadIndex));
            }
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "checker/new";
        }
        return "checker/verify";
    }

    @GetMapping("/verify")
    public String verify() {
        return "checker/verify";
    }

    @GetMapping("/records")
    public String records(@RequestParam(value = "balanceId", required = false) Long balanceId,
                          @RequestParam(value = "from", required = false) String from,
                          @RequestParam(value = "to", required = false) String to,
                          @RequestParam(value = "status", required = false) String status,
                          @RequestParam(value = "includeVoid", required = false, defaultValue = "false") boolean includeVoid,
                          @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                          Model model) {
        PageResult<CalibrationRecord> result = queryService.page(balanceId,
                parseDate(from, false), parseDate(to, true), blankToNull(status), includeVoid, page, 20);
        model.addAttribute("pageResult", result);
        model.addAttribute("balances", balanceMapper.selectList(null));
        model.addAttribute("balanceId", balanceId);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("status", status);
        model.addAttribute("includeVoid", includeVoid);
        return "checker/records";
    }

    @GetMapping("/curve")
    public String curve(@RequestParam(value = "recordId", required = false) Long recordId,
                        Model model) {
        model.addAttribute("balances", balanceMapper.selectList(null));
        if (recordId != null) {
            try {
                CurveData curve = queryService.curve(recordId);
                File folder = new File("upload/curve");
                File png = curveRenderer.render(curve, new File(folder, recordId + ".png"));
                model.addAttribute("curve", curve);
                model.addAttribute("curveImage", "/upload/curve/" + png.getName());
            } catch (Exception e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "checker/curve";
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

    static List<String> parseManualValues(String text) {
        List<String> values = new ArrayList<String>();
        if (text == null || text.trim().isEmpty()) {
            return values;
        }
        String[] parts = text.trim().split("[,，;；\\s]+");
        for (String part : parts) {
            if (!part.isEmpty()) {
                values.add(part);
            }
        }
        return values;
    }

    static Date parseDate(String value, boolean endOfDay) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
            fmt.setLenient(false);
            Date date = fmt.parse(value.trim());
            if (endOfDay) {
                return new Date(date.getTime() + 24L * 60L * 60L * 1000L - 1L);
            }
            return date;
        } catch (Exception e) {
            throw new IllegalArgumentException("日期格式应为 yyyy-MM-dd：" + value);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
