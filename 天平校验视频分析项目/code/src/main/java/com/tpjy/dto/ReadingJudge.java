package com.tpjy.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 单次示值的判定明细（用于表格展示与 PDF 导出）。 */
public class ReadingJudge {

    private int seq;
    private String level;
    private BigDecimal nominal;
    private BigDecimal value;
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
