package com.tpjy.dto;

import java.util.ArrayList;
import java.util.List;

/** 一次视频识别的完整结果（M2 的输出契约，乙/页面/导出都消费它）。 */
public class RecognitionResult {

    private String videoPath;
    /** 视频号，形如 V-20260629-0043。 */
    private String videoNo;
    /** 校验日期（= 视频开始拍摄时间，分钟级），yyyy-MM-dd HH:mm。 */
    private String checkDate;
    /** 视频里识别到的人员（人员卡 PERSON:姓名）。 */
    private String personFromQr;
    /** 视频里识别到的天平编号（天平卡 BALANCE:编号）。 */
    private String balanceFromQr;
    /** 最终用于建记录的人员：二维码优先，其次页面/命令行指定。 */
    private String effectivePersonName;
    /** 最终用于建记录的天平：二维码优先，其次页面/命令行指定。 */
    private String effectiveBalanceCode;
    /** 抽帧总帧数。 */
    private int frameCount = 0;
    /** 抽帧间隔（秒）。 */
    private int frameInterval = 1;
    /** 实际使用的 OCR 实现名。 */
    private String ocrImpl;
    /** 识别耗时（毫秒），用于核对 NFR-001「单视频 ≤ 2 分钟」。 */
    private long elapsedMillis;
    /** 四类自动提示。 */
    private List<AlertItem> alerts = new ArrayList<AlertItem>();
    /** 9 次示值（识别到才有值，未识别到为 null → 人工补录）。 */
    private List<RecognizedReading> readings = new ArrayList<RecognizedReading>();

    public void addAlert(String type, String level, String message, Integer second) {
        this.alerts.add(new AlertItem(type, level, message, second));
    }

    public boolean hasAlert(String type) {
        for (AlertItem a : alerts) {
            if (type.equals(a.getType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否允许创建/保存校验记录。
     * 人员不一致按 SRS 必须阻断；人员或天平无法确定时同样不能建记录。
     * 天平未登记等提示仍需页面由检验人员确认，但不在此处直接阻断。
     */
    public boolean canCreateRecord() {
        return !hasAlert("PERSON_MISMATCH")
                && effectivePersonName != null && !effectivePersonName.trim().isEmpty()
                && effectiveBalanceCode != null && !effectiveBalanceCode.trim().isEmpty();
    }

    /** 9 次示值是否全部拿到（含人工补录）。 */
    public boolean allReadingsPresent() {
        if (readings.size() < 9) {
            return false;
        }
        for (RecognizedReading r : readings) {
            if (r.getValue() == null) {
                return false;
            }
        }
        return true;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public String getVideoNo() {
        return videoNo;
    }

    public void setVideoNo(String videoNo) {
        this.videoNo = videoNo;
    }

    public String getCheckDate() {
        return checkDate;
    }

    public void setCheckDate(String checkDate) {
        this.checkDate = checkDate;
    }

    public String getPersonFromQr() {
        return personFromQr;
    }

    public void setPersonFromQr(String personFromQr) {
        this.personFromQr = personFromQr;
    }

    public String getBalanceFromQr() {
        return balanceFromQr;
    }

    public void setBalanceFromQr(String balanceFromQr) {
        this.balanceFromQr = balanceFromQr;
    }

    public String getEffectivePersonName() {
        return effectivePersonName;
    }

    public void setEffectivePersonName(String effectivePersonName) {
        this.effectivePersonName = effectivePersonName;
    }

    public String getEffectiveBalanceCode() {
        return effectiveBalanceCode;
    }

    public void setEffectiveBalanceCode(String effectiveBalanceCode) {
        this.effectiveBalanceCode = effectiveBalanceCode;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public void setFrameCount(int frameCount) {
        this.frameCount = frameCount;
    }

    public int getFrameInterval() {
        return frameInterval;
    }

    public void setFrameInterval(int frameInterval) {
        this.frameInterval = frameInterval;
    }

    public String getOcrImpl() {
        return ocrImpl;
    }

    public void setOcrImpl(String ocrImpl) {
        this.ocrImpl = ocrImpl;
    }

    public long getElapsedMillis() {
        return elapsedMillis;
    }

    public void setElapsedMillis(long elapsedMillis) {
        this.elapsedMillis = elapsedMillis;
    }

    public List<AlertItem> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<AlertItem> alerts) {
        this.alerts = alerts;
    }

    public List<RecognizedReading> getReadings() {
        return readings;
    }

    public void setReadings(List<RecognizedReading> readings) {
        this.readings = readings;
    }
}
