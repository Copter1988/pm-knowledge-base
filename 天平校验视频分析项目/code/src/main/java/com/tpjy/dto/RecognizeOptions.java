package com.tpjy.dto;

import com.tpjy.util.TpjyUtils;

import java.util.ArrayList;
import java.util.List;

/** 识别参数（视频之外的上下文：由页面或命令行提供）。 */
public class RecognizeOptions {

    /** 当前校验人员姓名（= 登录账号姓名），用于「拍摄人员与登录账号一致」查对。 */
    private String personName;
    /** 当前选定的天平编号（视频里没有天平卡时人工指定）。 */
    private String balanceCode;
    /** 已登记的天平编号清单，用于「天平未登记」提示；为空表示不做该校验。 */
    private List<String> knownBalanceCodes;
    /** 天平 L1/L2/L3 砝码标称值（g）。 */
    private List<java.math.BigDecimal> weights = new ArrayList<java.math.BigDecimal>();
    /** 应显示的小数位数（由量程精度换算；-1 表示不校验位数）。 */
    private int expectedDigits = -1;
    /** 量程精度原文，如 0.1mg（用于提示文案）。 */
    private String precisionText;
    /**
     * 双量程天平的逐组位数。默认 -1，表示回退到 {@link #expectedDigits}。
     * 示例：F002 第一量程 0.1mg → L1=4 位；第二量程 0.01mg → L2/L3=5 位。
     */
    private int expectedDigitsL1 = -1;
    private int expectedDigitsL2 = -1;
    private int expectedDigitsL3 = -1;
    private String precisionTextL1;
    private String precisionTextL2;
    private String precisionTextL3;
    /** 9 个示值的人工补录/测试输入（按 seq 1~9 顺序；空表示完全依赖识别）。 */
    private List<String> manualValues = new ArrayList<String>();
    /** 抽帧图片输出目录（含视频号子目录）。 */
    private java.io.File frameOutputDir;
    /** 视频号，如 V-20260629-0043。 */
    private String videoNo;

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public String getBalanceCode() {
        return balanceCode;
    }

    public void setBalanceCode(String balanceCode) {
        this.balanceCode = balanceCode;
    }

    public List<String> getKnownBalanceCodes() {
        return knownBalanceCodes;
    }

    public void setKnownBalanceCodes(List<String> knownBalanceCodes) {
        this.knownBalanceCodes = knownBalanceCodes;
    }

    public List<java.math.BigDecimal> getWeights() {
        return weights;
    }

    public void setWeights(List<java.math.BigDecimal> weights) {
        this.weights = weights;
    }

    public int getExpectedDigits() {
        return expectedDigits;
    }

    public void setExpectedDigits(int expectedDigits) {
        this.expectedDigits = expectedDigits;
    }

    public String getPrecisionText() {
        return precisionText;
    }

    public void setPrecisionText(String precisionText) {
        this.precisionText = precisionText;
    }

    /**
     * 按天平量程配置预填逐组精度。单量程时三组使用第一量程；
     * 双量程按 SRS 示例，L1 使用第一量程、L2/L3 使用第二量程。
     */
    public void applyRangePrecisions(String range1Precision, String range2Precision) {
        expectedDigitsL1 = TpjyUtils.digitsOfPrecision(range1Precision);
        precisionTextL1 = range1Precision;
        String lowerRange = range2Precision == null || range2Precision.trim().isEmpty()
                ? range1Precision : range2Precision;
        expectedDigitsL2 = TpjyUtils.digitsOfPrecision(lowerRange);
        expectedDigitsL3 = expectedDigitsL2;
        precisionTextL2 = lowerRange;
        precisionTextL3 = lowerRange;
    }

    public int expectedDigitsForLevel(String level) {
        if ("L1".equals(level) && expectedDigitsL1 >= 0) {
            return expectedDigitsL1;
        }
        if ("L2".equals(level) && expectedDigitsL2 >= 0) {
            return expectedDigitsL2;
        }
        if ("L3".equals(level) && expectedDigitsL3 >= 0) {
            return expectedDigitsL3;
        }
        return expectedDigits;
    }

    public String precisionTextForLevel(String level) {
        if ("L1".equals(level) && precisionTextL1 != null) {
            return precisionTextL1;
        }
        if ("L2".equals(level) && precisionTextL2 != null) {
            return precisionTextL2;
        }
        if ("L3".equals(level) && precisionTextL3 != null) {
            return precisionTextL3;
        }
        return precisionText;
    }

    public int getExpectedDigitsL1() {
        return expectedDigitsL1;
    }

    public void setExpectedDigitsL1(int expectedDigitsL1) {
        this.expectedDigitsL1 = expectedDigitsL1;
    }

    public int getExpectedDigitsL2() {
        return expectedDigitsL2;
    }

    public void setExpectedDigitsL2(int expectedDigitsL2) {
        this.expectedDigitsL2 = expectedDigitsL2;
    }

    public int getExpectedDigitsL3() {
        return expectedDigitsL3;
    }

    public void setExpectedDigitsL3(int expectedDigitsL3) {
        this.expectedDigitsL3 = expectedDigitsL3;
    }

    public void setPrecisionTextL1(String precisionTextL1) {
        this.precisionTextL1 = precisionTextL1;
    }

    public void setPrecisionTextL2(String precisionTextL2) {
        this.precisionTextL2 = precisionTextL2;
    }

    public void setPrecisionTextL3(String precisionTextL3) {
        this.precisionTextL3 = precisionTextL3;
    }

    public List<String> getManualValues() {
        return manualValues;
    }

    public void setManualValues(List<String> manualValues) {
        this.manualValues = manualValues;
    }

    public java.io.File getFrameOutputDir() {
        return frameOutputDir;
    }

    public void setFrameOutputDir(java.io.File frameOutputDir) {
        this.frameOutputDir = frameOutputDir;
    }

    public String getVideoNo() {
        return videoNo;
    }

    public void setVideoNo(String videoNo) {
        this.videoNo = videoNo;
    }
}
