package com.tpjy.service.frame;

import java.io.File;
import java.util.List;

/**
 * 视频抽帧接口（NFR-006 可维护性：抽帧实现可替换）。
 *
 * <p>默认实现 {@link FfmpegFrameExtractor}（需本机可执行 ffmpeg）；
 * 备选实现 JavaCV（无外部依赖，但 jar 体积大）—— 见 docs/接口说明.md 的接入步骤。
 * 通过 application.yml 的 app.frame-extractor 切换。
 */
public interface FrameExtractor {

    /**
     * 按固定间隔抽帧到 outputDir。
     *
     * @param video     视频文件
     * @param outputDir 图片输出目录（不存在则创建）
     * @return 按时间升序的帧列表
     * @throws Exception 抽帧失败（ffmpeg 缺失、视频损坏等）
     */
    List<Frame> extract(File video, File outputDir) throws Exception;

    /** 实现名，用于日志与自检提示。 */
    String name();
}
