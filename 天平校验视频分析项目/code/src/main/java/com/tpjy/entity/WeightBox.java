package com.tpjy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

/** 砝码盒（按盒管理，一盒共用一号）。对应表 weight_box。 */
@TableName("weight_box")
public class WeightBox {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String boxNo;
    private BigDecimal weightL1;
    private BigDecimal weightL2;
    private BigDecimal weightL3;
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBoxNo() {
        return boxNo;
    }

    public void setBoxNo(String boxNo) {
        this.boxNo = boxNo;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
