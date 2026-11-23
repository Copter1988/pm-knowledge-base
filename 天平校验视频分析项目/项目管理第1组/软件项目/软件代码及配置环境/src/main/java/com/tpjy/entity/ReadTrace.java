package com.tpjy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/** 识别值改动留痕（原识别值 → 确认值，仅改动项留痕）。对应表 read_trace。 */
@TableName("read_trace")
public class ReadTrace {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recordId;
    private Long readingId;
    private String originalValue;
    private String confirmedValue;
    private Long changedBy;
    private Date changedAt;

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

    public Long getReadingId() {
        return readingId;
    }

    public void setReadingId(Long readingId) {
        this.readingId = readingId;
    }

    public String getOriginalValue() {
        return originalValue;
    }

    public void setOriginalValue(String originalValue) {
        this.originalValue = originalValue;
    }

    public String getConfirmedValue() {
        return confirmedValue;
    }

    public void setConfirmedValue(String confirmedValue) {
        this.confirmedValue = confirmedValue;
    }

    public Long getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(Long changedBy) {
        this.changedBy = changedBy;
    }

    public Date getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Date changedAt) {
        this.changedAt = changedAt;
    }
}
