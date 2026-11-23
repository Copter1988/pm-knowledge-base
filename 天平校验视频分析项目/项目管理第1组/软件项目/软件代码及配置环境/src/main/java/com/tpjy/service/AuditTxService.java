package com.tpjy.service;

import com.tpjy.dto.Operator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 审计写入的独立事务包装（成员乙）。
 * 审计日志的语义是「留痕」，必须独立于业务事务提交：即使业务回滚，审计也应保留。
 * 用 REQUIRES_NEW 挂起业务事务、独立提交审计，避免审计随业务一起回滚。
 */
@Service
public class AuditTxService {

    private final AuditService auditService;

    public AuditTxService(AuditService auditService) {
        this.auditService = auditService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logReadChange(Operator operator, Long recordId, int seq, String original, String confirmed) {
        auditService.logReadChange(operator, recordId, seq, original, confirmed);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAbandon(Operator operator, Long recordId, String reason) {
        auditService.logAbandon(operator, recordId, reason);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logReviewConfirm(Operator operator, Long recordId, String action) {
        auditService.logReviewConfirm(operator, recordId, action);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logBatchConfirm(Operator operator, int count, String recordNos) {
        auditService.logBatchConfirm(operator, count, recordNos);
    }
}
