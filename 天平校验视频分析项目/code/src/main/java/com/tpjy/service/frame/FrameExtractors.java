package com.tpjy.service.frame;

import com.tpjy.config.AppProps;

/**
 * 抽帧实现工厂：按配置返回实现。
 * 新增实现（如 JavaCV）只需在此处加一个分支，业务层不动。
 */
public final class FrameExtractors {

    private FrameExtractors() {
    }

    public static FrameExtractor create(AppProps props) {
        String type = props.getFrameExtractor() == null ? "ffmpeg" : props.getFrameExtractor().trim().toLowerCase();
        if ("javacv".equals(type)) {
            throw new IllegalStateException(
                    "app.frame-extractor=javacv，但当前工程未引入 JavaCV 依赖。"
                            + "请按 docs/接口说明.md「JavaCV 备选」两步接入（加依赖 + 加一个实现类），"
                            + "或把配置改回 ffmpeg。");
        }
        return new FfmpegFrameExtractor(props.getFfmpegPath(), props.getFrameInterval(), props.getRecognizeTimeoutSeconds());
    }
}
