package com.tpjy.controller;

import com.tpjy.dto.PageResult;
import com.tpjy.dto.RecordDetail;
import com.tpjy.dto.CurveData;
import com.tpjy.entity.CalibrationRecord;
import com.tpjy.mapper.BalanceMapper;
import com.tpjy.service.CurveRenderer;
import com.tpjy.service.PdfExportService;
import com.tpjy.service.QueryService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.file.Files;

/** 复核确认人员（REVIEWER）：甲的记录查询、曲线与 PDF 导出入口。 */
@Controller
@RequestMapping("/reviewer")
public class ReviewerController {

    @Resource
    private QueryService queryService;
    @Resource
    private PdfExportService pdfExportService;
    @Resource
    private CurveRenderer curveRenderer;
    @Resource
    private BalanceMapper balanceMapper;

    @GetMapping("/pending")
    public String pending() {
        return "reviewer/pending";
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
                CheckerController.parseDate(from, false), CheckerController.parseDate(to, true),
                status == null || status.trim().isEmpty() ? null : status.trim(),
                includeVoid, page, 20);
        model.addAttribute("pageResult", result);
        model.addAttribute("balances", balanceMapper.selectList(null));
        model.addAttribute("balanceId", balanceId);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("status", status);
        model.addAttribute("includeVoid", includeVoid);
        return "reviewer/records";
    }

    @GetMapping("/curve")
    public String curve(@RequestParam("recordId") Long recordId, Model model) {
        try {
            CurveData curve = queryService.curve(recordId);
            File folder = new File("upload/curve");
            File png = curveRenderer.render(curve, new File(folder, "reviewer-" + recordId + ".png"));
            model.addAttribute("curve", curve);
            model.addAttribute("curveImage", "/upload/curve/" + png.getName());
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "checker/curve";
    }

    @GetMapping("/export/pdf")
    public void exportPdf(@RequestParam("recordId") Long recordId,
                          HttpServletResponse response) throws Exception {
        RecordDetail detail = queryService.detail(recordId);
        File folder = new File("upload/export");
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("无法创建 PDF 导出目录");
        }
        File pdf = new File(folder, "record-" + recordId + ".pdf");
        try {
            pdfExportService.export(detail, pdf);
            response.setContentType(MediaType.APPLICATION_PDF_VALUE);
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + detail.getRecordNo() + ".pdf\"");
            response.setContentLengthLong(pdf.length());
            Files.copy(pdf.toPath(), response.getOutputStream());
            response.flushBuffer();
        } finally {
            if (pdf.exists()) {
                pdf.delete();
            }
        }
    }

    @GetMapping("/balance")
    public String balance() {
        return "reviewer/balance";
    }
}
