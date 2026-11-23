package com.tpjy.service;

import com.tpjy.common.Constants;
import com.tpjy.dto.Operator;
import com.tpjy.entity.AuditLog;
import com.tpjy.service.audit.AuditSink;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 留痕与审计日志（WBS 1.3.8，甲）。
 *
 * <p>覆盖五类操作 + 登录（SRS §3.6 可维护性）：
 * READ_CHANGE 识别值改动、ABANDON 记录作废、REVIEW_CONFIRM 复核确认、
 * BATCH_CONFIRM 批量确认、VIDEO_DELETE 视频删除、LOGIN 登录。
 *
 * <p>支持多落地目标：数据库（页面运行时）+ 文件（命令行离线测试时）。
 */
public class AuditService {

    private final List<AuditSink> sinks = new ArrayList<AuditSink>();

    public AuditService addSink(AuditSink sink) {
        if (sink != null) {
            this.sinks.add(sink);
        }
        return this;
    }

    /** 记录一条审计日志。 */
    public void log(Operator operator, String action, String targetType, Long targetId, String detail) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setCreatedAt(new Date());
        for (AuditSink sink : sinks) {
            try {
                sink.write(operator, log);
            } catch (Exception e) {
                System.err.println("审计落地失败（" + sink.getClass().getSimpleName() + "）：" + e.getMessage());
            }
        }
    }

    /** 识别值改动留痕（原识别值 → 确认值，仅改动项调用本方法）。 */
    public void logReadChange(Operator operator, Long recordId, int seq, String original, String confirmed) {
        log(operator, Constants.ACTION_READ_CHANGE, "READING", recordId,
                "第 " + seq + " 次示值：" + original + " → " + confirmed);
    }

    public void logAbandon(Operator operator, Long recordId, String reason) {
        log(operator, Constants.ACTION_ABANDON, "RECORD", recordId,
                "记录作废（原因不记入记录本身）：" + (reason == null ? "" : reason));
    }

    public void logReviewConfirm(Operator operator, Long recordId, String action) {
        log(operator, Constants.ACTION_REVIEW_CONFIRM, "RECORD", recordId, "复核" + action);
    }

    public void logBatchConfirm(Operator operator, int count, String recordNos) {
        log(operator, Constants.ACTION_BATCH_CONFIRM, "RECORD", null,
                "批量复核确认 " + count + " 条：" + recordNos);
    }

    public void logVideoDelete(Operator operator, Long videoId, String videoNo, String reason) {
        log(operator, Constants.ACTION_VIDEO_DELETE, "VIDEO", videoId,
                "删除视频 " + videoNo + "（" + reason + "）");
    }

    public void logLogin(Operator operator) {
        log(operator, Constants.ACTION_LOGIN, "USER", operator == null ? null : operator.getId(),
                "登录成功");
    }
}
