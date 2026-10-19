package com.tpjy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.util.Date;

/** 天平主数据（对应业务「天平清单」）。对应表 balance。 */
@TableName("balance")
public class Balance {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 天平编号，如 F001（二维码 BALANCE:F001 即此值）。 */
    private String balanceCode;
    /** 科室，如「化学室」。 */
    private String dept;
    /** 天平唯一号，设备自带，≤20 位，如 SJS2605002。 */
    private String uniqueNo;
    /** 量程数量：1 单量程 / 2 双量程。 */
    private Integer rangeCount;
    /** 第一量程，如 220g。 */
    private String range1;
    /** 第一量程精度（分度值），如 0.1mg。 */
    private String range1Precision;
    private String range2;
    private String range2Precision;
    /** 内部校正方式：自动触发 / 手动触发 / NA。 */
    private String internalCalib;
    /** 校验频次，如「每天」（FR-020，仅作参考不主动提醒）。 */
    private String checkFreq;
    /** 砝码盒 id（按盒管理，一盒一号）。 */
    private Long weightBoxId;
    /** 测量砝码 L1/L2/L3 标称值（g），判定与曲线用。 */
    @TableField("weight_L1")
    private BigDecimal weightL1;
    @TableField("weight_L2")
    private BigDecimal weightL2;
    @TableField("weight_L3")
    private BigDecimal weightL3;
    private Date createdAt;

    /** 非数据库字段：砝码盒编号，便于页面/导出直接展示。 */
    @TableField(exist = false)
    private String weightBoxNo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getRangeCount() {
        return rangeCount;
    }

    public void setRangeCount(Integer rangeCount) {
        this.rangeCount = rangeCount;
    }

    public String getRange1() {
        return range1;
    }

    public void setRange1(String range1) {
        this.range1 = range1;
    }

    public String getRange1Precision() {
        return range1Precision;
    }

    public void setRange1Precision(String range1Precision) {
        this.range1Precision = range1Precision;
    }

    public String getRange2() {
        return range2;
    }

    public void setRange2(String range2) {
        this.range2 = range2;
    }

    public String getRange2Precision() {
        return range2Precision;
    }

    public void setRange2Precision(String range2Precision) {
        this.range2Precision = range2Precision;
    }

    public String getInternalCalib() {
        return internalCalib;
    }

    public void setInternalCalib(String internalCalib) {
        this.internalCalib = internalCalib;
    }

    public String getCheckFreq() {
        return checkFreq;
    }

    public void setCheckFreq(String checkFreq) {
        this.checkFreq = checkFreq;
    }

    public Long getWeightBoxId() {
        return weightBoxId;
    }

    public void setWeightBoxId(Long weightBoxId) {
        this.weightBoxId = weightBoxId;
    }

    public BigDecimal getWeightL1() {
        return weightL1;
    }

    public void setWeightL1(BigDecimal weightL1) {
        this.weightL1 = weightL1;
    }

    public BigDecimal getWeightL2() {
        return weightL2;
    }

    public void setWeightL2(BigDecimal weightL2) {
        this.weightL2 = weightL2;
    }

    public BigDecimal getWeightL3() {
        return weightL3;
    }

    public void setWeightL3(BigDecimal weightL3) {
        this.weightL3 = weightL3;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getWeightBoxNo() {
        return weightBoxNo;
    }

    public void setWeightBoxNo(String weightBoxNo) {
        this.weightBoxNo = weightBoxNo;
    }
}
