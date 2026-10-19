package com.tpjy.dto;

import java.math.BigDecimal;

/** 趋势图中的一个测量点（L1/L2/L3 各 3 点）。 */
public class CurvePoint {

    private int seq;
    private String level;
    private BigDecimal nominal;
    private BigDecimal value;
    private BigDecimal lower;
    private BigDecimal upper;
    private boolean qualified;
    /** 天平实际显示值和单位，如 20000.2 mg。 */
    private String displayText;
    /** 测量时间（yyyy-MM-dd HH:mm）。 */
    private String measureTime;
    /** 该点对应的视频时间点，用于回放定位。 */
    private String clock;

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

    public BigDecimal getLower() {
        return lower;
    }

    public void setLower(BigDecimal lower) {
        this.lower = lower;
    }

    public BigDecimal getUpper() {
        return upper;
    }

    public void setUpper(BigDecimal upper) {
        this.upper = upper;
    }

    public boolean isQualified() {
        return qualified;
    }

    public void setQualified(boolean qualified) {
        this.qualified = qualified;
    }

    public String getClock() {
        return clock;
    }

    public void setClock(String clock) {
        this.clock = clock;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public String getMeasureTime() {
        return measureTime;
    }

    public void setMeasureTime(String measureTime) {
        this.measureTime = measureTime;
    }
}
