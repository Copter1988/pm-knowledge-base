package com.tpjy.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 一条校验记录的完整详情（页面详情页 / PDF 导出 / 命令行输出共用同一份模型）。
 *
 * <p>注意：按 SRS §3.3.2，测量时间「存库、不印在导出 PDF」，故 PDF 模板不引用 clock 字段。
 */
public class RecordDetail {

    private Long recordId;
    private String recordNo;
    private String videoNo;
    private String person;
    private String balanceCode;
    private String dept;
    private String uniqueNo;
    /** 形如 Max 220 g · d 0.1 mg。 */
    private String rangeText;
    private String precisionText;
    private String checkDate;
    /** 1 是 / 0 否。 */
    private Integer cleanOk;
    private Integer levelOk;
    /** 是 / 否 / NA。 */
    private String internalCalibOk;
    /** 草稿 / 待复核 / 已打回 / 已复核 / 已作废。 */
    private String status;
    private String remark;
    private String checkerName;
    private String reviewerName;

    private JudgeResult judge;
    private CurveData curve;
    /** 识别值改动留痕（原值 → 确认值），用于详情页留痕区。 */
    private List<String> traces = new ArrayList<String>();

    public String getCleanText() {
        return yesNo(cleanOk);
    }

    public String getLevelText() {
        return yesNo(levelOk);
    }

    private String yesNo(Integer v) {
        if (v == null) {
            return "-";
        }
        return v == 1 ? "是" : "否";
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getRecordNo() {
        return recordNo;
    }

    public void setRecordNo(String recordNo) {
        this.recordNo = recordNo;
    }

    public String getVideoNo() {
        return videoNo;
    }

    public void setVideoNo(String videoNo) {
        this.videoNo = videoNo;
    }

    public String getPerson() {
        return person;
    }

    public void setPerson(String person) {
        this.person = person;
    }

    public String getBalanceCode() {
        return balanceCode;
    }

    public void setBalanceCode(String balanceCode) {
        this.balanceCode = balanceCode;
    }

    public String getDept() {
        return dept;
    }

    public void setDept(String dept) {
        this.dept = dept;
    }

    public String getUniqueNo() {
        return uniqueNo;
    }

    public void setUniqueNo(String uniqueNo) {
        this.uniqueNo = uniqueNo;
    }

    public String getRangeText() {
        return rangeText;
    }

    public void setRangeText(String rangeText) {
        this.rangeText = rangeText;
    }

    public String getPrecisionText() {
        return precisionText;
    }

    public void setPrecisionText(String precisionText) {
        this.precisionText = precisionText;
    }

    public String getCheckDate() {
        return checkDate;
    }

    public void setCheckDate(String checkDate) {
        this.checkDate = checkDate;
    }

    public Integer getCleanOk() {
        return cleanOk;
    }

    public void setCleanOk(Integer cleanOk) {
        this.cleanOk = cleanOk;
    }

    public Integer getLevelOk() {
        return levelOk;
    }

    public void setLevelOk(Integer levelOk) {
        this.levelOk = levelOk;
    }

    public String getInternalCalibOk() {
        return internalCalibOk;
    }

    public void setInternalCalibOk(String internalCalibOk) {
        this.internalCalibOk = internalCalibOk;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getCheckerName() {
        return checkerName;
    }

    public void setCheckerName(String checkerName) {
        this.checkerName = checkerName;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public JudgeResult getJudge() {
        return judge;
    }

    public void setJudge(JudgeResult judge) {
        this.judge = judge;
    }

    public CurveData getCurve() {
        return curve;
    }

    public void setCurve(CurveData curve) {
        this.curve = curve;
    }

    public List<String> getTraces() {
        return traces;
    }

    public void setTraces(List<String> traces) {
        this.traces = traces;
    }
}
