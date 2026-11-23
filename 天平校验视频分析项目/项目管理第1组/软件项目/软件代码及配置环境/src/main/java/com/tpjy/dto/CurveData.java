package com.tpjy.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 称重值曲线数据（FR-016）。
 * 页面用 ECharts 消费（series = 示值、限度上下限参考线），命令行用 Java2D 直接画成 PNG。
 */
public class CurveData {

    private String recordNo;
    private String balanceCode;
    private String checkDate;
    private String resultText;
    private List<CurvePoint> points = new ArrayList<CurvePoint>();

    /** 便于 ECharts 直接取用的分类轴（1~9）。 */
    public List<Integer> getCategories() {
        List<Integer> list = new ArrayList<Integer>();
        for (CurvePoint p : points) {
            list.add(p.getSeq());
        }
        return list;
    }

    public String getRecordNo() {
        return recordNo;
    }

    public void setRecordNo(String recordNo) {
        this.recordNo = recordNo;
    }

    public String getBalanceCode() {
        return balanceCode;
    }

    public void setBalanceCode(String balanceCode) {
        this.balanceCode = balanceCode;
    }

    public String getCheckDate() {
        return checkDate;
    }

    public void setCheckDate(String checkDate) {
        this.checkDate = checkDate;
    }

    public String getResultText() {
        return resultText;
    }

    public void setResultText(String resultText) {
        this.resultText = resultText;
    }

    public List<CurvePoint> getPoints() {
        return points;
    }

    public void setPoints(List<CurvePoint> points) {
        this.points = points;
    }
}
