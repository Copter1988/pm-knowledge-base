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
    /** 是否启用文字卡本地 OCR（RapidOCR）。 */
    private boolean cardOcrEnabled = true;
    /** Python 解释器路径；默认使用项目内 OCR 虚拟环境。 */
    private String ocrPythonPath = "./.ocr-venv/Scripts/python.exe";
    /** 本地 OCR bridge 脚本。 */
    private String ocrWorkerPath = "./scripts/local_ocr_worker.py";
    /** 单次视频最多 OCR 的帧数，控制总耗时。 */
    private int cardOcrMaxFrames = 18;
    /** 优先扫描视频前多少秒内的身份卡。 */
    private int cardOcrWindowSeconds = 75;
    /** 本地 OCR 子进程超时秒数。 */
    private int cardOcrTimeoutSeconds = 90;

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

    public boolean isCardOcrEnabled() {
        return cardOcrEnabled;
    }

    public void setCardOcrEnabled(boolean cardOcrEnabled) {
        this.cardOcrEnabled = cardOcrEnabled;
    }

    public String getOcrPythonPath() {
        return ocrPythonPath;
    }

    public void setOcrPythonPath(String ocrPythonPath) {
        this.ocrPythonPath = ocrPythonPath;
    }

    public String getOcrWorkerPath() {
        return ocrWorkerPath;
    }

    public void setOcrWorkerPath(String ocrWorkerPath) {
        this.ocrWorkerPath = ocrWorkerPath;
    }

    public int getCardOcrMaxFrames() {
        return cardOcrMaxFrames;
    }

    public void setCardOcrMaxFrames(int cardOcrMaxFrames) {
        this.cardOcrMaxFrames = cardOcrMaxFrames;
    }

    public int getCardOcrWindowSeconds() {
        return cardOcrWindowSeconds;
    }

    public void setCardOcrWindowSeconds(int cardOcrWindowSeconds) {
        this.cardOcrWindowSeconds = cardOcrWindowSeconds;
    }

    public int getCardOcrTimeoutSeconds() {
        return cardOcrTimeoutSeconds;
    }

    public void setCardOcrTimeoutSeconds(int cardOcrTimeoutSeconds) {
        this.cardOcrTimeoutSeconds = cardOcrTimeoutSeconds;
    }
}
