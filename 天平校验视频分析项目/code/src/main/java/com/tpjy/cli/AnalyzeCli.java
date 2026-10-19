package com.tpjy.cli;

import com.tpjy.common.Constants;
import com.tpjy.config.AppProps;
import com.tpjy.dto.CurveData;
import com.tpjy.dto.JudgeResult;
import com.tpjy.dto.Operator;
import com.tpjy.dto.RecognitionResult;
import com.tpjy.dto.RecordDetail;
import com.tpjy.dto.RecognizeOptions;
import com.tpjy.dto.RecognizedReading;
import com.tpjy.dto.ReadingJudge;
import com.tpjy.service.AuditService;
import com.tpjy.service.CurveBuilder;
import com.tpjy.service.CurveRenderer;
import com.tpjy.service.JudgeService;
import com.tpjy.service.PdfExportService;
import com.tpjy.service.RecognitionService;
import com.tpjy.service.audit.FileAuditSink;
import com.tpjy.service.frame.FrameExtractor;
import com.tpjy.service.frame.FrameExtractors;
import com.tpjy.service.ocr.NoopOcrService;
import com.tpjy.service.ocr.OcrService;
import com.tpjy.util.Json;
import com.tpjy.util.TpjyUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 无界面命令行测试入口（甲的模块：视频进 → 结果出）。
 *
 * <p>不经过 Spring、不需要数据库，只要本机有 ffmpeg 就能跑。
 * 在 IDEA 中右键本类的 main 方法 → Run，然后在 Run Configuration 的
 * Program arguments 里粘贴下面这行即可（示例）：
 *
 * <pre>
 * --video=videos/v1_ok.mp4 --person=李一 --balance=F001 --weights=200,50,10
 * --precision=0.1mg --known-balances=F001,F002 --seq=11 --out=out
 * </pre>
 *
 * <p>输出（默认 工程根/out）：
 * recognize.json 识别结果、judge.json/judge.txt 判定明细、
 * record.json 记录草案、audit.log 审计留痕。
 */
public final class AnalyzeCli {

    private AnalyzeCli() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> a = parse(args);
        if (a.containsKey("help")) {
            printUsage();
            return;
        }

        String videoArg = a.get("video");
        if (videoArg == null || videoArg.trim().isEmpty()) {
            System.err.println("缺少参数 --video=<视频文件路径>。用 --help 查看用法。");
            System.exit(2);
            return;
        }
        File video = new File(videoArg.trim());
        if (!video.isFile()) {
            System.err.println("视频文件不存在：" + video.getAbsolutePath());
            System.exit(2);
            return;
        }

        AppProps props = AppProps.defaults();
        if (a.containsKey("ffmpeg")) {
            props.setFfmpegPath(a.get("ffmpeg"));
        }
        if (a.containsKey("interval")) {
            props.setFrameInterval(Integer.parseInt(a.get("interval")));
        }
        if (a.containsKey("out")) {
            props.setCliOutputDir(a.get("out"));
        }
        File outDir = new File(props.getCliOutputDir());
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new IllegalStateException("无法创建输出目录：" + outDir.getAbsolutePath());
        }

        // 校验日期：优先命令行，其次视频文件时间（= 视频开始拍摄时间，分钟级）
        Date checkDate = parseDate(a.get("check-date"));
        if (checkDate == null) {
            checkDate = new Date(video.lastModified() > 0 ? video.lastModified() : System.currentTimeMillis());
        }
        int dailySeq = parseInt(a.get("seq"), 1);
        int videoSeq = parseInt(a.get("video-seq"), 1);
        String recordNo = TpjyUtils.recordNo(checkDate, dailySeq);
        String videoNo = TpjyUtils.videoNo(checkDate, videoSeq);

        List<BigDecimal> weights = parseWeights(a.get("weights"));
        int expectedDigits = -1;
        if (a.containsKey("digits")) {
            expectedDigits = Integer.parseInt(a.get("digits"));
        } else if (a.containsKey("precision")) {
            expectedDigits = TpjyUtils.digitsOfPrecision(a.get("precision"));
        }

        RecognizeOptions opt = new RecognizeOptions();
        opt.setPersonName(a.get("person"));
        opt.setBalanceCode(a.get("balance"));
        opt.setCheckDate(checkDate);
        opt.setWeights(weights);
        opt.setRange1CapacityGrams(parseCapacity(a.get("range1-capacity")));
        opt.setRange2CapacityGrams(parseCapacity(a.get("range2-capacity")));
        opt.setExpectedDigits(expectedDigits);
        opt.setPrecisionText(a.get("precision"));
        if (!a.containsKey("digits") && (a.containsKey("precision") || a.containsKey("precision2"))) {
            opt.applyRangePrecisions(a.get("precision"), a.get("precision2"));
        }
        opt.setManualValues(parseList(a.get("values")));
        opt.setVideoNo(videoNo);
        opt.setFrameOutputDir(new File(props.getFrameDir(), videoNo));
        if (a.containsKey("known-balances")) {
            opt.setKnownBalanceCodes(parseList(a.get("known-balances")));
        }

        // 组装各部（全部离线，无需 Spring / 数据库）
        FrameExtractor extractor = FrameExtractors.create(props);
        OcrService ocr = new NoopOcrService();
        RecognitionService recognitionService = new RecognitionService(extractor, ocr, props.getFrameInterval());
        JudgeService judgeService = new JudgeService();
        AuditService auditService = new AuditService()
                .addSink(new FileAuditSink(new File(outDir, "audit.log")));
        Operator operator = new Operator(null, a.get("person") == null ? "命令行" : a.get("person"), Constants.ROLE_CHECKER);

        System.out.println("=== 天平校验视频分析 · 命令行识别 ===");
        System.out.println("视频      : " + video.getAbsolutePath());
        System.out.println("抽帧实现  : " + extractor.name() + "（间隔 " + props.getFrameInterval() + " 秒）");
        System.out.println("OCR 实现  : " + ocr.name());
        System.out.println("记录号    : " + recordNo + "   视频号: " + videoNo);
        System.out.println("校验日期  : " + TpjyUtils.formatDate(checkDate));
        System.out.println();

        // 1. 识别
        RecognitionResult rr = recognitionService.recognize(video, opt);
        rr.setCheckDate(TpjyUtils.formatDate(checkDate));
        Json.write(new File(outDir, "recognize.json"), rr);

        System.out.println("--- 一、视觉识别（WBS 1.3.2）---");
        System.out.println("抽帧总帧数: " + rr.getFrameCount());
        System.out.println("人员卡    : " + (rr.getPersonFromQr() == null ? "未识别到（按指定人员记录）" : rr.getPersonFromQr()));
        System.out.println("天平卡    : " + (rr.getBalanceFromQr() == null ? "未识别到（按指定天平记录）" : rr.getBalanceFromQr()));
        System.out.println("识别耗时  : " + rr.getElapsedMillis() + " ms"
                + (rr.getElapsedMillis() <= 120000 ? "（满足 ≤2 分钟）" : "（超出 2 分钟，需优化）"));
        if (!rr.getAlerts().isEmpty()) {
            System.out.println("自动提示  :");
            for (int i = 0; i < rr.getAlerts().size(); i++) {
                com.tpjy.dto.AlertItem al = rr.getAlerts().get(i);
                System.out.println("  [" + al.getLevel() + "] " + al.getType() + " " + al.getMessage()
                        + (al.getClock() == null ? "" : "   ← 定位到视频 " + al.getClock()));
            }
        } else {
            System.out.println("自动提示  : 无");
        }

        // 2. 判定
        BigDecimal w1 = weights.size() >= 3 ? weights.get(0) : null;
        BigDecimal w2 = weights.size() >= 3 ? weights.get(1) : null;
        BigDecimal w3 = weights.size() >= 3 ? weights.get(2) : null;
        boolean cleanOk = parseBool(a.get("clean"), true);
        boolean levelOk = parseBool(a.get("level"), true);
        String calib = a.containsKey("calib") ? a.get("calib") : Constants.CALIB_YES;
        String recordStatus = a.containsKey("status") ? a.get("status") : Constants.STATUS_DRAFT;

        JudgeResult jr = judgeService.judgeRecord(rr.getReadings(), w1, w2, w3, cleanOk, levelOk, calib);
        Json.write(new File(outDir, "judge.json"), jr);

        System.out.println();
        System.out.println("--- 二、校验判定引擎（WBS 1.3.5）---");
        System.out.println("前置条件  : 清洁=" + (cleanOk ? "是" : "否") + "，水平=" + (levelOk ? "是" : "否")
                + "，内部校正=" + calib);
        System.out.println();
        System.out.printf("%-6s %-4s %-10s %-22s %-18s %-12s %-19s %-8s%n",
                "次别", "组", "标称值(g)", "限度(g)", "实际显示", "换算(g)", "测量时间", "判定");
        for (ReadingJudge d : jr.getDetails()) {
            String limit = (d.getLower() == null ? "-" : d.getLower().toPlainString()) + " ~ "
                    + (d.getUpper() == null ? "-" : d.getUpper().toPlainString());
            System.out.printf("%-6s %-4s %-10s %-22s %-18s %-12s %-19s %-8s%n",
                    d.getLevel() + "-" + ((d.getSeq() - 1) % 3 + 1),
                    d.getLevel(),
                    d.getNominal() == null ? "-" : TpjyUtils.plain(d.getNominal()),
                    limit,
                    d.getDisplayText(),
                    d.getValue() == null ? "-" : d.getValue().toPlainString(),
                    d.getMeasureTime() == null ? "-" : d.getMeasureTime(),
                    d.getValue() == null ? "待补录" : (d.isQualified() ? "合格" : "不合格"));
        }
        System.out.println();
        System.out.println("本次校验判定: " + jr.getResultText()
                + (jr.isVetoedByPrecondition() ? "（前置条件一票否决）" : ""));
        for (String reason : jr.getFailReasons()) {
            System.out.println("  原因: " + reason);
        }
        System.out.println("备注预填    : " + jr.getRemarkPrefill());
        System.out.println("判定规则    : 任一示值超出（1±0.05%）L，或前置条件任一项不通过 → 不通过");

        // 3. 记录草案 + 判定文本
        Map<String, Object> draft = new LinkedHashMap<String, Object>();
        draft.put("recordNo", recordNo);
        draft.put("videoNo", videoNo);
        draft.put("checkDate", TpjyUtils.formatDate(checkDate));
        draft.put("person", rr.getPersonFromQr() == null ? opt.getPersonName() : rr.getPersonFromQr());
        draft.put("balanceCode", rr.getBalanceFromQr() == null ? opt.getBalanceCode() : rr.getBalanceFromQr());
        draft.put("precisionText", opt.getPrecisionText());
        draft.put("expectedDigits", expectedDigits);
        draft.put("cleanOk", cleanOk);
        draft.put("levelOk", levelOk);
        draft.put("internalCalibOk", calib);
        draft.put("status", recordStatus);
        draft.put("result", jr.isQualified() ? 1 : 0);
        draft.put("resultText", jr.getResultText());
        draft.put("remark", jr.getRemarkPrefill());
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (ReadingJudge d : jr.getDetails()) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("seq", d.getSeq());
            row.put("level", d.getLevel());
            row.put("nominal", d.getNominal());
            row.put("lower", d.getLower());
            row.put("upper", d.getUpper());
            row.put("value", d.getValue());
            row.put("displayValue", d.getRawValue());
            row.put("displayUnit", d.getDisplayUnit());
            row.put("measureTime", d.getMeasureTime());
            row.put("displayText", d.getDisplayText());
            row.put("qualified", d.isQualified());
            rows.add(row);
        }
        draft.put("readings", rows);
        Json.write(new File(outDir, "record.json"), draft);

        writeText(new File(outDir, "judge.txt"), buildJudgeText(recordNo, videoNo, opt, rr, jr, cleanOk, levelOk, calib));

        // 3.5 查询 / 曲线 / PDF 导出（WBS 1.3.6）
        String effectiveBalanceCode = rr.getBalanceFromQr() == null ? opt.getBalanceCode() : rr.getBalanceFromQr();
        String effectivePerson = rr.getPersonFromQr() == null ? opt.getPersonName() : rr.getPersonFromQr();

        CurveBuilder curveBuilder = new CurveBuilder();
        CurveData curve = curveBuilder.build(recordNo, effectiveBalanceCode,
                TpjyUtils.formatDate(checkDate), jr);
        curveBuilder.fillClock(curve, rr.getReadings());
        Json.write(new File(outDir, "curve.json"), curve);
        new CurveRenderer().render(curve, new File(outDir, "curve.png"));

        RecordDetail detail = new RecordDetail();
        detail.setRecordNo(recordNo);
        detail.setVideoNo(videoNo);
        detail.setPerson(effectivePerson);
        detail.setBalanceCode(effectiveBalanceCode);
        detail.setCheckDate(TpjyUtils.formatDate(checkDate));
        detail.setPrecisionText(opt.getPrecisionText());
        detail.setRangeText(opt.getPrecisionText() == null ? null : ("d " + opt.getPrecisionText()));
        detail.setStatus(recordStatus);
        detail.setRemark(jr.getRemarkPrefill());
        detail.setCleanOk(cleanOk ? 1 : 0);
        detail.setLevelOk(levelOk ? 1 : 0);
        detail.setInternalCalibOk(calib);
        detail.setJudge(jr);
        detail.setCurve(curve);

        boolean pdfOk = false;
        String pdfErr = null;
        if (!a.containsKey("no-pdf")) {
            try {
                new PdfExportService(a.get("font") != null ? a.get("font") : props.getPdfFontPath())
                        .export(detail, new File(outDir, "record.pdf"));
                pdfOk = true;
            } catch (Exception e) {
                pdfErr = e.getMessage();
            }
        }

        System.out.println();
        System.out.println("--- 三、查询 / 曲线 / 导出（WBS 1.3.6）---");
        System.out.println("趋势数据   : L1/L2/L3 三组，各 3 个点；连接每组最大值/最小值");
        System.out.println("趋势图片   : curve.png（纵轴默认 ±0.2%）");
        if (a.containsKey("no-pdf")) {
            System.out.println("PDF 导出   : 已跳过（--no-pdf）");
        } else if (pdfOk) {
            System.out.println("PDF 导出   : record.pdf");
        } else {
            System.out.println("PDF 导出   : 失败 —— " + pdfErr);
        }

        // 4. 审计留痕（M8）
        auditService.logLogin(operator);
        auditService.log(operator, "RECOGNIZE", "VIDEO", null,
                "识别视频 " + videoNo + "：人员=" + rr.getPersonFromQr() + "，天平=" + rr.getBalanceFromQr()
                        + "，耗时 " + rr.getElapsedMillis() + "ms");
        for (com.tpjy.dto.AlertItem al : rr.getAlerts()) {
            if ("PERSON_MISMATCH".equals(al.getType()) || "BALANCE_NOT_FOUND".equals(al.getType())) {
                auditService.log(operator, "ALERT", "VIDEO", null, al.getType() + "：" + al.getMessage());
            }
        }
        System.out.println();
        System.out.println("--- 输出 ---");
        System.out.println("out/recognize.json  识别结果（含四类提示）");
        System.out.println("out/judge.json      判定明细");
        System.out.println("out/judge.txt       判定文本（可直接贴进记录/报告）");
        System.out.println("out/record.json     记录草案（记录号/前置条件/9 示值/结论）");
        System.out.println("out/curve.json      曲线数据（给 ECharts）");
        System.out.println("out/curve.png       曲线图（Java2D 直接出图）");
        System.out.println("out/record.pdf      导出 PDF（中文字体内嵌，按记录表显示测量时间和原屏单位）");
        System.out.println("out/audit.log       审计留痕");
        System.out.println("抽帧图片: " + opt.getFrameOutputDir().getAbsolutePath());
    }

    private static String buildJudgeText(String recordNo, String videoNo, RecognizeOptions opt,
                                         RecognitionResult rr, JudgeResult jr,
                                         boolean cleanOk, boolean levelOk, String calib) {
        StringBuilder sb = new StringBuilder();
        sb.append("天平校验记录（判定明细）\n");
        sb.append("记录号：").append(recordNo).append("　　视频号：").append(videoNo).append('\n');
        sb.append("校验日期：").append(rr.getCheckDate() == null ? "-" : rr.getCheckDate()).append('\n');
        sb.append("检验人员：").append(rr.getPersonFromQr() == null ? opt.getPersonName() : rr.getPersonFromQr()).append('\n');
        sb.append("天平编号：").append(rr.getBalanceFromQr() == null ? opt.getBalanceCode() : rr.getBalanceFromQr()).append('\n');
        sb.append("量程精度：").append(opt.getPrecisionText() == null ? "-" : opt.getPrecisionText()).append('\n');
        sb.append("显示口径：测量示值保留天平实际显示值/单位；限度与判定统一使用 g\n");
        sb.append("前置条件：清洁=").append(cleanOk ? "是" : "否")
                .append("，水平=").append(levelOk ? "是" : "否")
                .append("，内部校正=").append(calib).append('\n');
        sb.append('\n');
        sb.append("次别  组  标称值(g)  限度(g)                  天平实际显示      换算(g)     测量时间          判定\n");
        for (ReadingJudge d : jr.getDetails()) {
            sb.append(pad(d.getLevel() + "-" + ((d.getSeq() - 1) % 3 + 1), 6))
                    .append(pad(d.getLevel(), 4))
                    .append(pad(d.getNominal() == null ? "-" : TpjyUtils.plain(d.getNominal()), 10))
                    .append(pad((d.getLower() == null ? "-" : d.getLower().toPlainString()) + " ~ "
                            + (d.getUpper() == null ? "-" : d.getUpper().toPlainString()), 24))
                    .append(pad(d.getDisplayText(), 18))
                    .append(pad(d.getValue() == null ? "-" : d.getValue().toPlainString(), 12))
                    .append(pad(d.getMeasureTime() == null ? "-" : d.getMeasureTime(), 19))
                    .append(d.getValue() == null ? "待补录" : (d.isQualified() ? "合格" : "不合格"))
                    .append('\n');
        }
        sb.append('\n');
        sb.append("校验判定：").append(jr.getResultText());
        if (jr.isVetoedByPrecondition()) {
            sb.append("（前置条件一票否决）");
        }
        sb.append('\n');
        for (String reason : jr.getFailReasons()) {
            sb.append("　原因：").append(reason).append('\n');
        }
        sb.append("备注：").append(jr.getRemarkPrefill()).append('\n');
        sb.append("判定规则：任一示值超出（1±0.05%）L，或前置条件任一项不通过 → 不通过\n");
        return sb.toString();
    }

    private static String pad(String s, int len) {
        StringBuilder sb = new StringBuilder(s == null ? "" : s);
        while (sb.length() < len) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static void writeText(File file, String text) throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        Writer w = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
        try {
            w.write(text);
        } finally {
            w.close();
        }
    }

    private static Map<String, String> parse(String[] args) {
        Map<String, String> map = new HashMap<String, String>();
        if (args == null) {
            return map;
        }
        for (String arg : args) {
            if (arg == null) {
                continue;
            }
            String s = arg.trim();
            if (s.isEmpty()) {
                continue;
            }
            while (s.startsWith("-")) {
                s = s.substring(1);
            }
            if (s.isEmpty()) {
                continue;
            }
            int eq = s.indexOf('=');
            if (eq < 0) {
                map.put(s.toLowerCase(), "true");
            } else {
                String key = s.substring(0, eq).toLowerCase();
                String value = s.substring(eq + 1);
                // 容忍写成 --check-date="2026-06-29 10:13" 这种带引号的写法
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                } else if (value.length() >= 2 && value.startsWith("'") && value.endsWith("'")) {
                    value = value.substring(1, value.length() - 1);
                }
                map.put(key, value);
            }
        }
        return map;
    }

    private static int parseInt(String s, int def) {
        if (s == null || s.trim().isEmpty()) {
            return def;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static boolean parseBool(String s, boolean def) {
        if (s == null || s.trim().isEmpty()) {
            return def;
        }
        String v = s.trim().toLowerCase();
        return "1".equals(v) || "true".equals(v) || "yes".equals(v) || "是".equals(v);
    }

    private static Date parseDate(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        String[] patterns = new String[]{"yyyy-MM-dd HH:mm", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "yyyy/MM/dd HH:mm"};
        for (String p : patterns) {
            try {
                return new SimpleDateFormat(p).parse(s.trim());
            } catch (Exception ignore) {
                // 尝试下一个格式
            }
        }
        return null;
    }

    private static List<String> parseList(String s) {
        List<String> list = new ArrayList<String>();
        if (s == null || s.trim().isEmpty()) {
            return list;
        }
        String[] parts = s.split(",");
        for (String p : parts) {
            list.add(p.trim());
        }
        return list;
    }

    private static List<BigDecimal> parseWeights(String s) {
        List<BigDecimal> list = new ArrayList<BigDecimal>();
        for (String p : parseList(s)) {
            BigDecimal v = TpjyUtils.parseDecimal(p);
            if (v != null) {
                list.add(v);
            }
        }
        return list;
    }

    private static BigDecimal parseCapacity(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return com.tpjy.dto.MassReading.parse(s).getGrams();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void printUsage() {
        List<String> lines = Arrays.asList(
                "天平校验视频分析系统 · 命令行识别与判定（甲的模块，无需数据库）",
                "",
                "必需参数：",
                "  --video=<路径>            校验视频文件",
                "",
                "常用参数：",
                "  --person=李一             当前检验人员姓名（用于「拍摄人员与登录账号一致」查对）",
                "  --balance=F001            选定的天平编号（视频无天平卡时按此记录）",
                "  --weights=200,50,10       天平 L1/L2/L3 砝码标称值（g），判定与限度计算用",
                "  --precision=0.1mg         第一量程精度，换算「应显示位数」做位数校验",
                "  --precision2=0.01mg       双量程第二量程精度（L2/L3 使用；单量程可省略）",
                "  --range1-capacity=220g    第一量程容量，用于超量程检查",
                "  --range2-capacity=40g     双量程第二量程容量，用于选择细精度档",
                "  --digits=4                直接指定应显示位数（优先于 --precision）",
                "  --known-balances=F001,F002 已登记天平清单，用于「天平未登记」提示",
                "  --values=200.0006,199.9998,... 人工补录/模拟 9 个示值（noop OCR 时用它验证判定链）",
                "  --clean=1 --level=1 --calib=是   前置条件（calib 取 是/否/NA）",
                "  --status=已复核           导出 PDF 时使用复核状态（CLI 默认 草稿）",
                "  --check-date=\"2026-06-29 10:13\"  校验日期（默认取视频文件时间）",
                "  --seq=11 --video-seq=43   记录号/视频号流水（默认 1）",
                "  --interval=1              抽帧间隔秒（默认 1）",
                "  --ffmpeg=<路径>           ffmpeg.exe 路径（默认取 PATH 中的 ffmpeg）",
                "  --out=out                 输出目录",
                "  --no-pdf                  不导出 PDF（只出曲线与 JSON）",
                "  --font=<ttf路径>          中文字体（默认取 app.pdf-font-path）",
                "",
                "示例：",
                "  --video=videos/v1_ok.mp4 --person=李一 --balance=F001 --weights=200,50,10 \\",
                "  --precision=0.1mg --known-balances=F001,F002 --values=200.0006,199.9998,200.0011,\\",
                "  50.0003,49.9997,50.0002,10.0001,10.0000,9.9999 --seq=11 --out=out"
        );
        for (String l : lines) {
            System.out.println(l);
        }
    }
}
