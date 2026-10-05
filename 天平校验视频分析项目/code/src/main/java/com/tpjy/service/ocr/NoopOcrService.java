package com.tpjy.service.ocr;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 默认 OCR 实现：不集成任何识别模型，始终返回 null。
 * 效果：示值全部由检验人员人工补录（功能不依赖 OCR 也能闭环）。
 */
@Service("noopOcrService")
public class NoopOcrService implements OcrService {
    @Override
    public BigDecimal readValue(String framePath, int seq) {
        return null;
    }
}
