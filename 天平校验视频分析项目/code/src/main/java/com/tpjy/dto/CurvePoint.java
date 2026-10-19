package com.tpjy.dto;

import java.math.BigDecimal;

/** 曲线上的一个点（横轴 = 示值序号 1~9）。 */
public class CurvePoint {

    private int seq;
    private String level;
    private BigDecimal nominal;
    private BigDecimal value;
    private BigDecimal lower;
    private BigDecimal upper;
    private boolean qualified;
    /** 该点对应的视频时间点（查询页显示；导出 PDF 不打印）。 */
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
}
