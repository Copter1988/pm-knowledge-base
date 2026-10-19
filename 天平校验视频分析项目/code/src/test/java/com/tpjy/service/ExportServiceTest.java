package com.tpjy.service;

import com.tpjy.common.Constants;
import com.tpjy.dto.CurveData;
import com.tpjy.dto.CurvePoint;
import com.tpjy.dto.JudgeResult;
import com.tpjy.dto.RecordDetail;
import com.tpjy.dto.RecognizedReading;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 查询/曲线/PDF 导出测试（对应 AC-06）。
 * 不依赖数据库与浏览器：直接构造一条记录，验证曲线数据、PNG 出图与 PDF 文件真实生成。
 */
class ExportServiceTest {

    private final JudgeService judgeService = new JudgeService();
    private final CurveBuilder curveBuilder = new CurveBuilder();

    @Test
    @DisplayName("曲线数据：横轴 1~9，含限度上下限与分组")
    void curveData() {
        CurveData curve = curveBuilder.build("JY20260629-011", "F001", "2026-06-29 10:13", judge(mixedValues()));

        assertEquals(9, curve.getPoints().size());
        assertEquals(9, curve.getCategories().size());
        assertEquals(1, curve.getCategories().get(0).intValue());
        assertEquals(9, curve.getCategories().get(8).intValue());
        // L1 第一次：200 g，限度 199.9000 ~ 200.1000（实时算出来的，不是写死值）
        assertEquals(0, new BigDecimal("199.9000").compareTo(curve.getPoints().get(0).getLower()));
        assertEquals(0, new BigDecimal("200.1000").compareTo(curve.getPoints().get(0).getUpper()));
        assertEquals(Constants.LEVEL_L1, curve.getPoints().get(0).getLevel());
        assertEquals(Constants.LEVEL_L3, curve.getPoints().get(8).getLevel());
    }

    @Test
    @DisplayName("曲线图真实出 PNG 文件")
    void curvePng(@TempDir Path tmp) throws Exception {
        CurveData curve = curveBuilder.build("JY20260629-011", "F001", "2026-06-29 10:13", judge(mixedValues()));
        File png = new CurveRenderer().render(curve, new File(tmp.toFile(), "curve.png"));

        assertTrue(png.isFile(), "应生成 curve.png");
        assertTrue(png.length() > 5000, "PNG 应包含实际内容，实际 " + png.length() + " 字节");
        Files.copy(png.toPath(), new File("target/sample-curve.png").toPath(),
                StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    @DisplayName("导出 PDF 真实生成文件，且文件头为 %PDF")
    void exportPdf(@TempDir Path tmp) throws Exception {
        RecordDetail detail = sampleDetail();
        File pdf = new File(tmp.toFile(), "record.pdf");
        new PdfExportService("/fonts/simhei.ttf").export(detail, pdf);

        assertTrue(pdf.isFile(), "应生成 record.pdf");
        assertTrue(pdf.length() > 1000, "PDF 应有实际内容，实际 " + pdf.length() + " 字节");
        byte[] head = new byte[4];
        FileInputStream in = new FileInputStream(pdf);
        try {
            in.read(head);
        } finally {
            in.close();
        }
        assertEquals("%PDF", new String(head), "文件头应为 %PDF");
        Files.copy(pdf.toPath(), new File("target/sample-record.pdf").toPath(),
                StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    @DisplayName("导出件不得打印测量时间（SRS §3.3.2）")
    void noMeasureTimeInExport() {
        RecordDetail detail = sampleDetail();
        // 曲线点上带测量时间 10:15 / 10:16
        detail.getCurve().getPoints().get(0).setClock("10:15");
        detail.getCurve().getPoints().get(1).setClock("10:16");

        String html = new PdfExportService("/fonts/simhei.ttf").buildHtml(detail);

        assertFalse(html.contains("10:15"), "导出 HTML 不应出现测量时间");
        assertFalse(html.contains("10:16"), "导出 HTML 不应出现测量时间");
        assertFalse(html.toLowerCase().contains("measuretime"), "不应引用测量时间字段");
        // 但应有结论与示值
        assertTrue(html.contains("不合格"));
        assertTrue(html.contains("10.006"));
    }

    @Test
    @DisplayName("未完成判定时不允许导出（提示先完成识别与判定）")
    void refuseWhenNoJudge() {
        try {
            new PdfExportService("/fonts/simhei.ttf").export(new RecordDetail(), new File("target/x.pdf"));
            assertTrue(false, "应当抛出异常");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("判定"));
        }
    }

    @Test
    @DisplayName("未复核记录不允许导出 PDF")
    void refuseWhenNotReviewed() {
        RecordDetail detail = sampleDetail();
        detail.setStatus(Constants.STATUS_PENDING);
        try {
            new PdfExportService("/fonts/simhei.ttf").export(detail, new File("target/x.pdf"));
            assertTrue(false, "应当抛出异常");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("已复核"));
        }
    }

    // ---------------- 辅助 ----------------

    private RecordDetail sampleDetail() {
        JudgeResult jr = judge(mixedValues());
        CurveData curve = curveBuilder.build("JY20260629-011", "F001", "2026-06-29 10:13", jr);

        RecordDetail d = new RecordDetail();
        d.setRecordNo("JY20260629-011");
        d.setVideoNo("V-20260629-0043");
        d.setPerson("李一");
        d.setBalanceCode("F001");
        d.setDept("化学室");
        d.setUniqueNo("SJS2605002");
        d.setRangeText("Max 220 g · d 0.1 mg");
        d.setPrecisionText("0.1mg");
        d.setCheckDate("2026-06-29 10:13");
        d.setStatus(Constants.STATUS_REVIEWED);
        d.setCleanOk(1);
        d.setLevelOk(1);
        d.setInternalCalibOk(Constants.CALIB_YES);
        d.setRemark(jr.getRemarkPrefill());
        d.setJudge(jr);
        d.setCurve(curve);
        d.getTraces().add("10.0060 → 10.0001");
        return d;
    }

    private JudgeResult judge(List<String> values) {
        List<RecognizedReading> list = new ArrayList<RecognizedReading>();
        for (int i = 0; i < values.size(); i++) {
            RecognizedReading r = new RecognizedReading();
            r.setSeq(i + 1);
            r.setLevel(judgeService.groupLevel(i + 1));
            r.setRawValue(values.get(i));
            r.setValue(new BigDecimal(values.get(i)));
            list.add(r);
        }
        return judgeService.judgeRecord(list, new BigDecimal("200"), new BigDecimal("50"),
                new BigDecimal("10"), true, true, Constants.CALIB_YES);
    }

    /** 9 次示值：第 7 次刻意超限（L3 限度 9.9950~10.0050），用于验证"不合格"展示。 */
    private List<String> mixedValues() {
        return Arrays.asList(
                "200.0006", "199.9998", "200.0011",
                "50.0003", "49.9997", "50.0002",
                "10.0060", "10.0000", "9.9999");
    }
}
