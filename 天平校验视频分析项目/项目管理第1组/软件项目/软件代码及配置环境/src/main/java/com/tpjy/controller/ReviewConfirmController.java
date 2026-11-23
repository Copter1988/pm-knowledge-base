package com.tpjy.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tpjy.common.Constants;
import com.tpjy.common.LoginUser;
import com.tpjy.entity.Balance;
import com.tpjy.entity.CalibrationRecord;
import com.tpjy.entity.SysUser;
import com.tpjy.mapper.BalanceMapper;
import com.tpjy.mapper.CalibrationRecordMapper;
import com.tpjy.mapper.SysUserMapper;
import com.tpjy.service.ReviewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 复核确认（WBS 1.3.4，成员乙）——待复核列表 + 确认/打回。
 * 甲已在 ReviewerController 提供查询/曲线/PDF 入口；本控制器不改甲代码，用
 * {@code /reviewer/review/**} 子路径承载复核动作。
 */
@Controller
@RequestMapping("/reviewer/review")
public class ReviewConfirmController {

    @Resource
    private ReviewService reviewService;
    @Resource
    private CalibrationRecordMapper recordMapper;
    @Resource
    private BalanceMapper balanceMapper;
    @Resource
    private SysUserMapper userMapper;

    @GetMapping("/pending")
    public String pending(Model model) {
        List<CalibrationRecord> records = recordMapper.selectList(
                new QueryWrapper<CalibrationRecord>()
                        .eq("status", Constants.STATUS_PENDING)
                        .orderByDesc("check_date"));
        for (CalibrationRecord r : records) {
            fillDisplay(r);
        }
        model.addAttribute("records", records);
        return "reviewer/review-pending";
    }

    @PostMapping("/confirm")
    public String confirm(@RequestParam("recordId") Long recordId, HttpSession session) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        reviewService.confirm(recordId, user);
        return "redirect:/reviewer/review/pending";
    }

    @PostMapping("/reject")
    public String reject(@RequestParam("recordId") Long recordId,
                         @RequestParam(value = "reason", required = false) String reason,
                         HttpSession session) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        reviewService.reject(recordId, user, reason);
        return "redirect:/reviewer/review/pending";
    }

    @PostMapping("/batch-confirm")
    public String batchConfirm(@RequestParam(value = "recordIds", required = false) List<Long> recordIds,
                               HttpSession session) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        if (recordIds != null && !recordIds.isEmpty()) {
            reviewService.batchConfirm(recordIds, user);
        }
        return "redirect:/reviewer/review/pending";
    }

    private void fillDisplay(CalibrationRecord r) {
        if (r.getBalanceId() != null) {
            Balance b = balanceMapper.selectById(r.getBalanceId());
            if (b != null) {
                r.setBalanceCode(b.getBalanceCode());
            }
        }
        if (r.getCheckerId() != null) {
            SysUser u = userMapper.selectById(r.getCheckerId());
            if (u != null) {
                r.setCheckerName(u.getRealName());
            }
        }
    }
}
