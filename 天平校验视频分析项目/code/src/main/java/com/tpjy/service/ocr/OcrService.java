package com.tpjy.service.ocr;

import java.math.BigDecimal;

/**
 * 示值识别预留接口（SRS §3.2.3 灵活性：可替换实现而不影响业务层）。
 *
 * <p>本系统定位是「辅助录入」：识别不到返回 null，由检验人员人工补录兜底，
 * 因此默认实现 {@link NoopOcrService} 也可以让功能闭环。
 */
public interface OcrService {

    /**
     * 从某一帧中识别第 seq 次称量的示值。
     *
     * @param framePath 抽帧图片路径
     * @param seq       示值序号 1~9
     * @return 识别到的示值；识别不到返回 null（调用方走人工补录）
     */
    BigDecimal readValue(String framePath, int seq);

    /** 实现名，便于审计与排查。 */
    String name();
}
