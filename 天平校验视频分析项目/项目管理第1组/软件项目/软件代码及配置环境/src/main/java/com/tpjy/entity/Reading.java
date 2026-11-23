package com.tpjy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.util.Date;

/** 单次示值（子表，seq 1~9 顺序归组 L1/L2/L3）。对应表 reading。 */
@TableName("reading")
public class Reading {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recordId;
    /** 示值序号 1~9。 */
    private Integer seq;
    /** 归属砝码分组：L1（1~3）/ L2（4~6）/ L3（7~9）。 */
    private String weightLevel;
    /** 该组砝码标称值（冗余，便于判定与曲线；限度绝不冗余）。 */
    private BigDecimal nominal;
    /** 统一换算后的实测示值（克），用于判定与曲线。 */
    private BigDecimal readingValue;
    /** 天平上实际显示的数值文本，例如 200.0008 或 20000.8。 */
    private String displayValue;
    /** 实际显示单位：g / mg。 */
    private String displayUnit;
    /** 测量时间（分钟级，导出记录表按测量时间字段展示）。 */
    private Date measureTime;
    /** 本次示值是否在限度内：1 是 / 0 否。 */
    @TableField("is_qualified")
    private Integer isQualified;

    /** 非数据库字段：是否为人工改动后的确认值（来自 read_trace）。 */
    @TableField(exist = false)
    private Boolean changed;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public Integer getSeq() {
        return seq;
    }

    public void setSeq(Integer seq) {
        this.seq = seq;
    }

    public String getWeightLevel() {
        return weightLevel;
    }

    public void setWeightLevel(String weightLevel) {
        this.weightLevel = weightLevel;
    }

    public BigDecimal getNominal() {
        return nominal;
    }

    public void setNominal(BigDecimal nominal) {
        this.nominal = nominal;
    }

    public BigDecimal getReadingValue() {
        return readingValue;
    }

    public void setReadingValue(BigDecimal readingValue) {
        this.readingValue = readingValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayUnit() {
        return displayUnit;
    }

    public void setDisplayUnit(String displayUnit) {
        this.displayUnit = displayUnit;
    }

    public Date getMeasureTime() {
        return measureTime;
    }

    public void setMeasureTime(Date measureTime) {
        this.measureTime = measureTime;
    }

    public Integer getIsQualified() {
        return isQualified;
    }

    public void setIsQualified(Integer isQualified) {
        this.isQualified = isQualified;
    }

    public Boolean getChanged() {
        return changed;
    }

    public void setChanged(Boolean changed) {
        this.changed = changed;
    }
}
