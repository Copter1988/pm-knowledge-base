package com.tpjy.service;

import org.springframework.stereotype.Service;

/**
 * 视频导入与视觉识别（WBS 1.3.2，成员甲）。
 * 流程：上传视频 → ffmpeg 抽帧 → ZXing 识别人员/天平二维码 → OCR 预填示值 → 自动查对。
 *
 * 二维码内容约定：人员卡 PERSON:姓名、天平卡 BALANCE:天平编号。
 *
 * TODO 迭代1实现：
 *   1) 保存视频到 upload/video/YYYYMMDD/
 *   2) ffmpeg 每秒抽 1 帧到 upload/frame/{recordId}/
 *   3) ZXing 逐帧解码二维码 → 人员/天平
 *   4) 自动查对（人员 vs 登录账号；天平 vs 已登记资料）
 *   5) OcrService.readValue() 预填 9 次示值 + 帧时间戳 → 读数时间
 */
@Service
public class RecognitionService {
    // TODO
}
