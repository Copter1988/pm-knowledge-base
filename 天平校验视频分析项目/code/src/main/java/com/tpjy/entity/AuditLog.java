package com.tpjy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 审计日志（五类操作 + 登录）。 */
@Data
@TableName("audit_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long operatorId;
    private String operatorName;
    private String action;        // READ_CHANGE / ABANDON / REVIEW_CONFIRM / BATCH_CONFIRM / VIDEO_DELETE / LOGIN
    private String targetType;
    private Long targetId;
    private String detail;
    private LocalDateTime createdAt;
}
