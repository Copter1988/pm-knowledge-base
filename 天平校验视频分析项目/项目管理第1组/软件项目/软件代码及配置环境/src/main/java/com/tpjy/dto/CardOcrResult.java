package com.tpjy.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 身份卡文字 OCR 结果。 */
public class CardOcrResult {

    private String balanceCode;
    private String personName;
    private String checkerCardCode;
    private double checkerCardConfidence;
    private Integer checkerCardSecond;
    private String nameplateRange1Precision;
    private BigDecimal nameplateRange1Capacity;
    private String nameplateRange2Precision;
    private BigDecimal nameplateRange2Capacity;
    private double nameplateConfidence;
    private Integer nameplateSecond;
    private String rawText;
    private String method;
    private boolean available;
    private List<AutoReading> autoReadings = new ArrayList<AutoReading>();

    public static CardOcrResult unavailable() {
        CardOcrResult r = new CardOcrResult();
        r.available = false;
        return r;
    }

    public String getBalanceCode() {
        return balanceCode;
    }

    public void setBalanceCode(String balanceCode) {
        this.balanceCode = balanceCode;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public String getCheckerCardCode() {
        return checkerCardCode;
    }

    public void setCheckerCardCode(String checkerCardCode) {
        this.checkerCardCode = checkerCardCode;
    }

    public double getCheckerCardConfidence() {
        return checkerCardConfidence;
    }

    public void setCheckerCardConfidence(double checkerCardConfidence) {
        this.checkerCardConfidence = checkerCardConfidence;
    }

    public Integer getCheckerCardSecond() {
        return checkerCardSecond;
    }

    public void setCheckerCardSecond(Integer checkerCardSecond) {
        this.checkerCardSecond = checkerCardSecond;
    }

    public String getNameplateRange1Precision() {
        return nameplateRange1Precision;
    }

    public void setNameplateRange1Precision(String nameplateRange1Precision) {
        this.nameplateRange1Precision = nameplateRange1Precision;
    }

    public BigDecimal getNameplateRange1Capacity() {
        return nameplateRange1Capacity;
    }

    public void setNameplateRange1Capacity(BigDecimal nameplateRange1Capacity) {
        this.nameplateRange1Capacity = nameplateRange1Capacity;
    }

    public String getNameplateRange2Precision() {
        return nameplateRange2Precision;
    }

    public void setNameplateRange2Precision(String nameplateRange2Precision) {
        this.nameplateRange2Precision = nameplateRange2Precision;
    }

    public BigDecimal getNameplateRange2Capacity() {
        return nameplateRange2Capacity;
    }

    public void setNameplateRange2Capacity(BigDecimal nameplateRange2Capacity) {
        this.nameplateRange2Capacity = nameplateRange2Capacity;
    }

    public double getNameplateConfidence() {
        return nameplateConfidence;
    }

    public void setNameplateConfidence(double nameplateConfidence) {
        this.nameplateConfidence = nameplateConfidence;
    }

    public Integer getNameplateSecond() {
        return nameplateSecond;
    }

    public void setNameplateSecond(Integer nameplateSecond) {
        this.nameplateSecond = nameplateSecond;
    }

    public boolean hasCompleteNameplateRanges() {
        return nameplateRange1Precision != null && nameplateRange1Capacity != null
                && nameplateRange2Precision != null && nameplateRange2Capacity != null;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public List<AutoReading> getAutoReadings() {
        return autoReadings;
    }

    public void setAutoReadings(List<AutoReading> autoReadings) {
        this.autoReadings = autoReadings;
    }
}
