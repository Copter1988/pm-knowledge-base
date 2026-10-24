package com.tpjy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 记录呈现模板（FR-019）：控制记录详情/导出时字段的显隐、显示名、必填与顺序。对应表 record_template。 */
@TableName("record_template")
public class RecordTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 字段键（固定，对应记录表字段，如 record_no / balance_code / check_date）。 */
    private String fieldKey;
    /** 显示名。 */
    private String fieldLabel;
    /** 是否显示：1 显示 / 0 隐藏。 */
    private Integer visible;
    /** 是否必填：1 必填 / 0 非必填。 */
    private Integer required;
    /** 排序号。 */
    private Integer sortNo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFieldKey() {
        return fieldKey;
    }

    public void setFieldKey(String fieldKey) {
        this.fieldKey = fieldKey;
    }

    public String getFieldLabel() {
        return fieldLabel;
    }

    public void setFieldLabel(String fieldLabel) {
        this.fieldLabel = fieldLabel;
    }

    public Integer getVisible() {
        return visible;
    }

    public void setVisible(Integer visible) {
        this.visible = visible;
    }

    public Integer getRequired() {
        return required;
    }

    public void setRequired(Integer required) {
        this.required = required;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }
}
