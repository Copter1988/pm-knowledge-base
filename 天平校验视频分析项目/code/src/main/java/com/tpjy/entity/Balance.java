package com.tpjy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 天平主数据（对应「天平清单」）。 */
@Data
@TableName("balance")
public class Balance {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String balanceCode;      // 天平编号 F001
    private String dept;             // 科室
    private String uniqueNo;         // 天平唯一号 ≤20 位
    private Integer rangeCount;      // 1 单量程 / 2 双量程
    private String range1;           // 第一量程 220g
    private String range1Precision;  // 0.1mg
    private String range2;           // 第二量程（双量程才有）
    private String range2Precision;
    private String internalCalib;    // 自动触发 / 手动触发 / NA
    private String checkFreq;        // 校验频次（仅参考）
    private Long weightBoxId;
    private BigDecimal weightL1;     // 砝码标称值 L1（从大到小）
    private BigDecimal weightL2;
    private BigDecimal weightL3;
    private LocalDateTime createdAt;
}
