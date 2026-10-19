package com.tpjy.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 单次示值的判定明细（用于表格展示与 PDF 导出）。 */
public class ReadingJudge {

    private int seq;
    private String level;
    private BigDecimal nominal;
    private BigDecimal value;
    /** 天平实际显示数值文本与单位。 */
    private String rawValue;
    private String displayUnit = "g";
    /** 测量时间（yyyy-MM-dd HH:mm）。 */
    private String measureTime;
    private BigDecimal lower;
    private BigDecimal upper;
    /** 是否在限度内。 */
    private boolean qualified;

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

    /** 记录表展示用：实际显示值 + 实际单位。 */
    public String getDisplayText() {
        if (value == null) {
            return "未识别";
        }
        String shown = rawValue == null ? value.stripTrailingZeros().toPlainString() : rawValue;
        return shown + " " + displayUnit;
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

    /** 偏差百分比文本（保留 2 位），如 0.03%；无法计算时为 -。 */
    public String getDeviationText() {
        if (value == null || nominal == null || nominal.signum() == 0) {
            return "-";
        }
        BigDecimal dev = value.subtract(nominal).abs()
                .divide(nominal, 6, RoundingMode.HALF_UP)
                .movePointRight(2).setScale(2, RoundingMode.HALF_UP);
        return dev.toPlainString() + "%";
    }
}
