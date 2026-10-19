package com.tpjy.dto;

/**
 * 自动提示项（SRS §3.1.4 自动提示：四类问题）。
 * <p>type 取值：
 * PERSON_MISMATCH 拍摄人员与登录账号不一致、
 * BALANCE_NOT_FOUND 天平未登记或选错天平、
 * RANGE_MISMATCH 量程或精度对不上、
 * DIGIT_MISMATCH 读数位数不对、
 * BALANCE_CARD_OCR / PERSON_CARD_OCR 文字身份卡 OCR 命中、
 * STANDBY_ZERO 待机零点、
 * UNSTABLE_READING 示值尚未稳定、
 * VALUE_INVALID 人工输入格式错误、
 * OCR_MISSED 未识别到示值（走人工补录，非异常）。
 */
public class AlertItem {

    private String type;
    /** WARN 需处理 / INFO 提示。 */
    private String level;
    private String message;
    /** 对应视频时间点，形如 02:58，供「一键定位到视频」使用。 */
    private String clock;
    private Integer second;

    public AlertItem() {
    }

    public AlertItem(String type, String level, String message, Integer second) {
        this.type = type;
        this.level = level;
        this.message = message;
        this.second = second;
        this.clock = second == null ? null : formatClock(second);
    }

    public static String formatClock(int second) {
        return String.format("%02d:%02d", second / 60, second % 60);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getClock() {
        return clock;
    }

    public void setClock(String clock) {
        this.clock = clock;
    }

    public Integer getSecond() {
        return second;
    }

    public void setSecond(Integer second) {
        this.second = second;
    }
}
