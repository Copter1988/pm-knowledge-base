package com.tpjy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/** 校验记录主表。对应表 calibration_record。 */
@TableName("calibration_record")
public class CalibrationRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 业务记录号，形如 JY20260629-011，加唯一索引，仅作显示/查询。 */
    private String recordNo;
    private Long balanceId;
    /** 校验日期 = 视频开始拍摄时间（分钟级）。 */
    private Date checkDate;
    /** 清洁是否符合要求：1 是 / 0 否。 */
    private Integer cleanOk;
    /** 水平调节是否到位：1 是 / 0 否。 */
    private Integer levelOk;
    /** 内部校正是否通过：是 / 否 / NA。 */
    private String internalCalibOk;
    /** 外部校验结论：1 合格 / 0 不合格。 */
    private Integer result;
    private String remark;
    private Long checkerId;
    private Long reviewerId;
    /** 草稿 / 待复核 / 已打回 / 已复核 / 已作废。 */
    private String status;
    /** 量程精度或读数位数不符时，人工回看视频确认后置 1。 */
    private Integer confirmFlag;
    private Date submittedAt;
    private Date reviewedAt;
    private Date createdAt;

    // ---- 非数据库字段：便于展示与导出的冗余信息 ----
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String balanceCode;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String dept;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String uniqueNo;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String checkerName;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String reviewerName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecordNo() {
        return recordNo;
    }

    public void setRecordNo(String recordNo) {
        this.recordNo = recordNo;
    }

    public Long getBalanceId() {
        return balanceId;
    }

    public void setBalanceId(Long balanceId) {
        this.balanceId = balanceId;
    }

    public Date getCheckDate() {
        return checkDate;
    }

    public void setCheckDate(Date checkDate) {
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

    public Integer getResult() {
        return result;
    }

    public void setResult(Integer result) {
        this.result = result;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getCheckerId() {
        return checkerId;
    }

    public void setCheckerId(Long checkerId) {
        this.checkerId = checkerId;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getConfirmFlag() {
        return confirmFlag;
    }

    public void setConfirmFlag(Integer confirmFlag) {
        this.confirmFlag = confirmFlag;
    }

    public Date getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Date submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Date getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Date reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
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
}
