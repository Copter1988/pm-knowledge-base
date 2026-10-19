package com.tpjy.service.ocr;

import com.tpjy.dto.MassReading;

/**
 * 示值识别预留接口（SRS §3.2.3 灵活性：可替换实现而不影响业务层）。
 *
 * <p>本系统定位是「辅助录入」：识别不到返回 null，由检验人员人工补录兜底，
 * 因此默认实现 {@link NoopOcrService} 也可以让功能闭环。
 *
 * <p>具体 OCR 适配器必须区分待机零值与稳定称量值：待机时可能出现 0.000g 且在省电模式下变暗；
 * 读数应连续 1~2 秒不跳动才标记 stable。若天平以灰色/深色指示稳定状态，也应作为辅助判据。
 */
public interface OcrService {

    /**
     * 从某一帧中识别第 seq 次称量的示值。
     *
     * @param framePath 抽帧图片路径
     * @param seq       示值序号 1~9
     * @return 识别到的示值（含实际显示单位与 stable 标记）；未识别、待机零值或未稳定时不得冒充有效结果
     */
    MassReading readValue(String framePath, int seq);

    /** 实现名，便于审计与排查。 */
    String name();
}
