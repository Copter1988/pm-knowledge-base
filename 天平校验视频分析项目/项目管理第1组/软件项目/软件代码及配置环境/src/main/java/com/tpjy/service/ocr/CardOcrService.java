package com.tpjy.service.ocr;

import com.tpjy.dto.CardOcrResult;
import com.tpjy.dto.RecognizeOptions;
import com.tpjy.service.frame.Frame;

import java.util.List;

/** 从视频帧中识别印刷文字身份卡，作为二维码识别的降级方案。 */
public interface CardOcrService {

    CardOcrResult recognize(List<Frame> frames, List<String> knownBalanceCodes,
                            String expectedPersonName, RecognizeOptions options);

    String name();
}
