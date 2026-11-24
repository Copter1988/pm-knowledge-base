package com.tpjy.dto;

/** 记录模板中一个已启用字段的展示结果。 */
public class TemplateFieldView {

    private String fieldKey;
    private String fieldLabel;
    private String value;
    private boolean required;
    private Integer sortNo;

    public TemplateFieldView() {
    }

    public TemplateFieldView(String fieldKey, String fieldLabel, String value,
                             boolean required, Integer sortNo) {
        this.fieldKey = fieldKey;
        this.fieldLabel = fieldLabel;
        this.value = value;
        this.required = required;
        this.sortNo = sortNo;
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }
}
