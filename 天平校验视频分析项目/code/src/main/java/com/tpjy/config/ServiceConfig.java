package com.tpjy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpjy.mapper.AuditLogMapper;
import com.tpjy.service.AuditService;
import com.tpjy.service.CurveRenderer;
import com.tpjy.service.PdfExportService;
import com.tpjy.service.RecognitionService;
import com.tpjy.service.audit.DbAuditSink;
import com.tpjy.service.frame.FrameExtractor;
import com.tpjy.service.frame.FrameExtractors;
import com.tpjy.service.ocr.CardOcrService;
import com.tpjy.service.ocr.LocalCardOcrService;
import com.tpjy.service.ocr.NoopOcrService;
import com.tpjy.service.ocr.OcrService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 把「甲」的模块注册成 Spring Bean，供「乙」的页面直接 {@code @Autowired} 使用。
 * 命令行（AnalyzeCli）不走这里，自己 new 出来即可，互不影响。
 */
@Configuration
public class ServiceConfig {

    /** 抽帧实现由 application.yml 的 app.frame-extractor 决定（ffmpeg / javacv）。 */
    @Bean
    public FrameExtractor frameExtractor(AppProps props) {
        return FrameExtractors.create(props);
    }

    /** OCR 默认 Noop，识别不到由检验人员人工补录兜底。 */
    @Bean
    public OcrService ocrService() {
        return new NoopOcrService();
    }

    @Bean
    public RecognitionService recognitionService(FrameExtractor frameExtractor, OcrService ocrService,
                                                 AppProps props, CardOcrService cardOcrService) {
        return new RecognitionService(frameExtractor, ocrService, props.getFrameInterval(), cardOcrService);
    }

    @Bean
    public CardOcrService localCardOcrService(AppProps props, ObjectMapper objectMapper) {
        return new LocalCardOcrService(props, objectMapper);
    }

    @Bean
    public CurveRenderer curveRenderer() {
        return new CurveRenderer();
    }

    @Bean
    public PdfExportService pdfExportService(AppProps props) {
        return new PdfExportService(props.getPdfFontPath());
    }

    /** 页面运行时审计日志落数据库。 */
    @Bean
    public AuditService auditService(AuditLogMapper auditLogMapper) {
        return new AuditService().addSink(new DbAuditSink(auditLogMapper));
    }
}
