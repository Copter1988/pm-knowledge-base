package com.tpjy.service.ocr;

import java.math.BigDecimal;

/**
 * 示值 OCR 预留接口（SRS §3.3.3：OCR 仅作预填、最终以人工核对为准）。
 * 实现类可替换（如本地 PaddleOCR HTTP 服务），不影响业务层。
 */
public interface OcrService {

    /**
     * 从视频帧识别一次示值数字。
     *
     * @param framePath 帧图片路径
     * @param seq       第几次示值（1~9）
     * @return 识别到的示值；识别不到返回 null（由人工补录兜底）
     */
    BigDecimal readValue(String framePath, int seq);
}
