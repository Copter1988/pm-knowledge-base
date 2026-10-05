package com.tpjy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 9 次示值（子表，seq 1~9 顺序归组 L1/L2/L3）。 */
@Data
@TableName("reading")
public class Reading {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recordId;
    private Integer seq;            // 1~9
    private String weightLevel;     // L1 / L2 / L3
    private BigDecimal nominal;     // 标称值（冗余自 balance）
    private BigDecimal readingValue; // 示值
    private LocalDateTime measureTime; // 读数时间（分钟级，存库、不印 PDF）
    private Integer isQualified;    // 逐示值判定
}
