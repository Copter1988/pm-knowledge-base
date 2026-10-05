package com.tpjy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 校验记录（主表）。状态机：待复核 / 已打回 / 已复核 / 已作废。 */
@Data
@TableName("calibration_record")
public class CalibrationRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String recordNo;        // 业务记录号 = JY+日期+天平编号+当日流水
    private Long balanceId;
    private LocalDateTime checkDate; // 校验日期 = 视频开始拍摄时间（分钟级）
    private Integer cleanOk;         // 清洁 1是/0否
    private Integer levelOk;         // 水平
    private String internalCalibOk;  // 是 / 否 / NA
    private Integer result;          // 外部校验是否通过 1是/0否
    private String remark;           // 默认「无」
    private Long checkerId;          // 校验人员
    private Long reviewerId;         // 复核人员
    private String status;
    private Integer confirmFlag;     // 量程/精度/位数不符时「已确认」勾选
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
