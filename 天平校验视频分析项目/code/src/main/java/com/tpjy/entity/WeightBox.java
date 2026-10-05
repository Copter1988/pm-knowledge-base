package com.tpjy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 砝码盒（按盒管理，一盒共用一号）。 */
@Data
@TableName("weight_box")
public class WeightBox {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String boxNo;
    private String remark;
}
