package com.tpjy.service.ocr;

import com.tpjy.dto.MassReading;

/**
 * 默认 OCR 实现：始终返回 null，表示「未识别到」。
 *
 * <p>这是设计上刻意保留的降风险点（见《代码落地实施方案》§4.2）：
 * 不集成任何 OCR 也能完成「导入 → 核对（人工补录）→ 提交」全流程。
 * 将来接 PaddleOCR / 其他服务时，新写一个实现类替换本类即可，业务层不动。
 */
public class NoopOcrService implements OcrService {

    @Override
    public MassReading readValue(String framePath, int seq) {
        return null;
    }

    @Override
    public String name() {
        return "noop(人工补录兜底)";
    }
}
