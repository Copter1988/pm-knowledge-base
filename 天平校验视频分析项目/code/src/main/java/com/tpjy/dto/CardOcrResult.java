package com.tpjy.dto;

import java.util.ArrayList;
import java.util.List;

/** 身份卡文字 OCR 结果。 */
public class CardOcrResult {

    private String balanceCode;
    private String personName;
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
