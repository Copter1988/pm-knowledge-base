package com.tpjy.service.audit;

import com.tpjy.dto.Operator;
import com.tpjy.entity.AuditLog;

/**
 * 审计日志落地目标（NFR-006 可维护性：日志去处可替换）。
 * 数据库落地用 {@link DbAuditSink}，命令行离线测试用 {@link FileAuditSink}。
 */
public interface AuditSink {

    void write(Operator operator, AuditLog log);
}
