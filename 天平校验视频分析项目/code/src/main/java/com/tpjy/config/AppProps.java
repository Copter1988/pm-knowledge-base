package com.tpjy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 项目自定义配置（对应 application.yml 的 app.*）。
 * 同时提供 {@link #defaults()}：命令行离线测试不启动 Spring，用它拿默认值。
 */
@Component
@ConfigurationProperties(prefix = "app")
public class AppProps {

    /** 视频落地目录。 */
    private String videoDir = "./upload/video";
    /** 抽帧图片目录。 */
    private String frameDir = "./upload/frame";
    /** 抽帧实现：ffmpeg | javacv。 */
    private String frameExtractor = "ffmpeg";
    /** ffmpeg 可执行文件路径（可写绝对路径，免配环境变量）。 */
    private String ffmpegPath = "ffmpeg";
    /** 抽帧间隔（秒）。 */
    private int frameInterval = 1;
    /** 单视频识别上限（秒），对应 NFR-001。 */
    private int recognizeTimeoutSeconds = 120;
    /** 视频保留天数（FR-022）。 */
    private int videoKeepDays = 7;
    /** 限度比例，0.05% = 0.0005。 */
    private String limitRate = "0.0005";
    /** 较大误差阈值，1% = 0.01。 */
    private String bigDeviationRate = "0.01";
    /** PDF 中文字体 classpath 路径。 */
    private String pdfFontPath = "/fonts/simhei.ttf";
    /** 命令行测试输出目录。 */
    private String cliOutputDir = "./out";

    /** 不依赖 Spring 的默认配置。 */
    public static AppProps defaults() {
        return new AppProps();
    }

    public String getVideoDir() {
        return videoDir;
    }

    public void setVideoDir(String videoDir) {
        this.videoDir = videoDir;
    }

    public String getFrameDir() {
        return frameDir;
    }

    public void setFrameDir(String frameDir) {
        this.frameDir = frameDir;
    }

    public String getFrameExtractor() {
        return frameExtractor;
    }

    public void setFrameExtractor(String frameExtractor) {
        this.frameExtractor = frameExtractor;
    }

    public String getFfmpegPath() {
        return ffmpegPath;
    }

    public void setFfmpegPath(String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    public int getFrameInterval() {
        return frameInterval;
    }

    public void setFrameInterval(int frameInterval) {
        this.frameInterval = frameInterval;
    }

    public int getRecognizeTimeoutSeconds() {
        return recognizeTimeoutSeconds;
    }

    public void setRecognizeTimeoutSeconds(int recognizeTimeoutSeconds) {
        this.recognizeTimeoutSeconds = recognizeTimeoutSeconds;
    }

    public int getVideoKeepDays() {
        return videoKeepDays;
    }

    public void setVideoKeepDays(int videoKeepDays) {
        this.videoKeepDays = videoKeepDays;
    }

    public String getLimitRate() {
        return limitRate;
    }

    public void setLimitRate(String limitRate) {
        this.limitRate = limitRate;
    }

    public String getBigDeviationRate() {
        return bigDeviationRate;
    }

    public void setBigDeviationRate(String bigDeviationRate) {
        this.bigDeviationRate = bigDeviationRate;
    }

    public String getPdfFontPath() {
        return pdfFontPath;
    }

    public void setPdfFontPath(String pdfFontPath) {
        this.pdfFontPath = pdfFontPath;
    }

    public String getCliOutputDir() {
        return cliOutputDir;
    }

    public void setCliOutputDir(String cliOutputDir) {
        this.cliOutputDir = cliOutputDir;
    }
}
