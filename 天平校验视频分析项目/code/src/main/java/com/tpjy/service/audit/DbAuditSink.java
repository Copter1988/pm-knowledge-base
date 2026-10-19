package com.tpjy.service.audit;

import com.tpjy.dto.Operator;
import com.tpjy.entity.AuditLog;
import com.tpjy.mapper.AuditLogMapper;

/** 审计日志写入数据库 audit_log 表。 */
public class DbAuditSink implements AuditSink {

    private final AuditLogMapper auditLogMapper;

    public DbAuditSink(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public void write(Operator operator, AuditLog log) {
        if (operator != null) {
            log.setOperatorId(operator.getId());
            log.setOperatorName(operator.getName());
        }
        auditLogMapper.insert(log);
    }
}
