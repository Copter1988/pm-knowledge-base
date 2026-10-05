package com.tpjy.service;

import org.springframework.stereotype.Service;

/**
 * 复核确认（WBS 1.3.4，成员乙）。
 * 逐条/批量确认（待复核→已复核）、打回（本条→已作废）；确认后记录锁定。
 *
 * TODO 迭代2实现，需联动 AuditService（REVIEW_CONFIRM / BATCH_CONFIRM）。
 */
@Service
public class ReviewService {
    // TODO
}
