package com.tpjy.dto;

import java.math.BigDecimal;

/** 一次示值的识别/录入结果（seq 1~9）。 */
public class RecognizedReading {

    private int seq;
    /** 归属砝码分组：L1 / L2 / L3。 */
    private String level;
    /** 该组砝码标称值（g），未提供则为 null。 */
    private BigDecimal nominal;
    /** 示值数值；未识别到为 null。 */
    private BigDecimal value;
    /** 原始文本（人工补录时保留用户输入，便于追溯）。 */
    private String rawValue;
    /** 天平实际显示单位：g / mg。 */
    private String displayUnit = "g";
    /** 测量时间（yyyy-MM-dd HH:mm），与视频时间点 clock 分开保存。 */
    private String measureTime;
    /** 该示值对应的视频时间点，形如 10:15。 */
    private String clock;
    private Integer second;
    /** 是否被人工改动过（来自 read_trace）。 */
    private boolean changed;
    /** 是否由本地数字 OCR 自动预填。 */
    private boolean autoRecognized;
    private double ocrConfidence;

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public BigDecimal getNominal() {
        return nominal;
    }

    public void setNominal(BigDecimal nominal) {
        this.nominal = nominal;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getRawValue() {
        return rawValue;
    }

    public void setRawValue(String rawValue) {
        this.rawValue = rawValue;
    }

    public String getDisplayUnit() {
        return displayUnit;
    }

    public void setDisplayUnit(String displayUnit) {
        this.displayUnit = displayUnit == null || displayUnit.trim().isEmpty() ? "g" : displayUnit;
    }

    public String getMeasureTime() {
        return measureTime;
    }

    public void setMeasureTime(String measureTime) {
        this.measureTime = measureTime;
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

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public boolean isAutoRecognized() {
        return autoRecognized;
    }

    public void setAutoRecognized(boolean autoRecognized) {
        this.autoRecognized = autoRecognized;
    }

    public double getOcrConfidence() {
        return ocrConfidence;
    }

    public void setOcrConfidence(double ocrConfidence) {
        this.ocrConfidence = ocrConfidence;
    }
}
