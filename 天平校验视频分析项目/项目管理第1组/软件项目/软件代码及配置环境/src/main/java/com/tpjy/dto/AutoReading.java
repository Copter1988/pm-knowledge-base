package com.tpjy.dto;

import java.math.BigDecimal;

/** 本地数字 OCR 从稳定帧中提取的一次示值。 */
public class AutoReading {

    private int seq;
    private String level;
    private String raw;
    private String unit;
    private BigDecimal grams;
    private Integer second;
    private double confidence;
    private int stableSamples;
    private int stableSeconds;

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

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getGrams() {
        return grams;
    }

    public void setGrams(BigDecimal grams) {
        this.grams = grams;
    }

    public Integer getSecond() {
        return second;
    }

    public void setSecond(Integer second) {
        this.second = second;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public int getStableSamples() {
        return stableSamples;
    }

    public void setStableSamples(int stableSamples) {
        this.stableSamples = stableSamples;
    }

    public int getStableSeconds() {
        return stableSeconds;
    }

    public void setStableSeconds(int stableSeconds) {
        this.stableSeconds = stableSeconds;
    }
}
