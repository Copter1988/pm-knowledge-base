package com.tpjy.service;

import com.tpjy.common.BizException;
import com.tpjy.common.Constants;
import com.tpjy.common.LoginUser;
import com.tpjy.dto.Operator;
import com.tpjy.entity.CalibrationRecord;
import com.tpjy.mapper.CalibrationRecordMapper;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 复核确认（WBS 1.3.4，成员乙）。
 * 逐条确认（待复核→已复核）、打回（待复核→已打回）；确认后记录锁定。
 * 联动 AuditService（REVIEW_CONFIRM；批量 BATCH_CONFIRM 见切片 6）。
 */
@Service
public class ReviewService {

    private final CalibrationRecordMapper recordMapper;
    private final AuditService auditService;

    public ReviewService(CalibrationRecordMapper recordMapper, AuditService auditService) {
        this.recordMapper = recordMapper;
        this.auditService = auditService;
    }

    /** 确认：待复核→已复核，写复核人与时间，记审计。 */
    public CalibrationRecord confirm(Long recordId, LoginUser reviewer) {
        CalibrationRecord record = requirePending(recordId);
        record.setStatus(Constants.STATUS_REVIEWED);
        record.setReviewerId(reviewer == null ? null : reviewer.getUserId());
        record.setReviewedAt(new Date());
        recordMapper.updateById(record);
        auditService.logReviewConfirm(operatorOf(reviewer), recordId, "确认");
        return record;
    }

    /** 打回：待复核→已打回。 */
    public CalibrationRecord reject(Long recordId, LoginUser reviewer, String reason) {
        CalibrationRecord record = requirePending(recordId);
        record.setStatus(Constants.STATUS_REJECTED);
        record.setReviewerId(reviewer == null ? null : reviewer.getUserId());
        record.setReviewedAt(new Date());
        recordMapper.updateById(record);
        auditService.logReviewConfirm(operatorOf(reviewer), recordId, "打回");
        return record;
    }

    /** 批量确认：只处理待复核记录，跳过其它状态；返回实际确认条数。 */
    public int batchConfirm(List<Long> ids, LoginUser reviewer) {
        int count = 0;
        StringBuilder recordNos = new StringBuilder();
        for (Long id : ids) {
            CalibrationRecord record = recordMapper.selectById(id);
            if (record == null || !Constants.STATUS_PENDING.equals(record.getStatus())) {
                continue;
            }
            record.setStatus(Constants.STATUS_REVIEWED);
            record.setReviewerId(reviewer == null ? null : reviewer.getUserId());
            record.setReviewedAt(new Date());
            recordMapper.updateById(record);
            if (recordNos.length() > 0) {
                recordNos.append(",");
            }
            recordNos.append(record.getRecordNo());
            count++;
        }
        if (count > 0) {
            auditService.logBatchConfirm(operatorOf(reviewer), count, recordNos.toString());
        }
        return count;
    }

    private CalibrationRecord requirePending(Long recordId) {
        CalibrationRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BizException("记录不存在：" + recordId);
        }
        if (!Constants.STATUS_PENDING.equals(record.getStatus())) {
            throw new BizException("仅待复核记录可确认或打回，当前状态：" + record.getStatus());
        }
        return record;
    }

    private Operator operatorOf(LoginUser user) {
        if (user == null) {
            return Operator.system();
        }
        return new Operator(user.getUserId(), user.getRealName(), user.getRole());
    }
}
