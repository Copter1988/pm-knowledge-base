package com.tpjy.service;

import com.tpjy.common.LoginUser;
import com.tpjy.entity.AuditLog;
import com.tpjy.mapper.AuditLogMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/** 留痕与审计（FR-021，五类操作）。 */
@Service
public class AuditService {

    @Resource
    private AuditLogMapper auditLogMapper;

    public void log(LoginUser operator, String action, String targetType, Long targetId, String detail) {
        AuditLog a = new AuditLog();
        a.setOperatorId(operator == null ? null : operator.getUserId());
        a.setOperatorName(operator == null ? null : operator.getRealName());
        a.setAction(action);
        a.setTargetType(targetType);
        a.setTargetId(targetId);
        a.setDetail(detail);
        a.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(a);
    }
}
