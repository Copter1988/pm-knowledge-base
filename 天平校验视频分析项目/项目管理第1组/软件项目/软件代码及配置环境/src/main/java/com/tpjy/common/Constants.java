package com.tpjy.common;

/** 全局常量。 */
public final class Constants {
    private Constants() {}

    // WBS 甲模块使用的平铺常量（保留嵌套常量以兼容现有页面骨架）。
    public static final String ROLE_ADMIN = Role.ADMIN;
    public static final String ROLE_CHECKER = Role.CHECKER;
    public static final String ROLE_REVIEWER = Role.REVIEWER;

    public static final String STATUS_DRAFT = "草稿";
    public static final String STATUS_PENDING = RecordStatus.PENDING;
    public static final String STATUS_REJECTED = RecordStatus.REJECTED;
    public static final String STATUS_REVIEWED = RecordStatus.REVIEWED;
    public static final String STATUS_VOID = RecordStatus.VOID;

    public static final String ACTION_READ_CHANGE = AuditAction.READ_CHANGE;
    public static final String ACTION_ABANDON = AuditAction.ABANDON;
    public static final String ACTION_REVIEW_CONFIRM = AuditAction.REVIEW_CONFIRM;
    public static final String ACTION_BATCH_CONFIRM = AuditAction.BATCH_CONFIRM;
    public static final String ACTION_VIDEO_DELETE = AuditAction.VIDEO_DELETE;
    public static final String ACTION_LOGIN = AuditAction.LOGIN;

    public static final String LEVEL_L1 = "L1";
    public static final String LEVEL_L2 = "L2";
    public static final String LEVEL_L3 = "L3";

    public static final String CALIB_YES = "是";
    public static final String CALIB_NO = "否";
    public static final String CALIB_NA = "NA";

    public static final String DEFAULT_REMARK = "无";
    public static final String QR_PREFIX_PERSON = "PERSON:";
    public static final String QR_PREFIX_BALANCE = "BALANCE:";
    public static final String RECORD_NO_PREFIX = "JY";
    public static final String VIDEO_NO_PREFIX = "V-";

    public static final int ENABLED = 1;
    public static final int DISABLED = 0;

    public static final class Role {
        public static final String ADMIN = "ADMIN";        // 管理员
        public static final String CHECKER = "CHECKER";    // 检验人员
        public static final String REVIEWER = "REVIEWER";  // 复核确认人员
    }

    public static final class RecordStatus {
        public static final String PENDING = "待复核";
        public static final String REJECTED = "已打回";
        public static final String REVIEWED = "已复核";
        public static final String VOID = "已作废";
    }

    public static final class AuditAction {
        public static final String LOGIN = "LOGIN";
        public static final String READ_CHANGE = "READ_CHANGE";       // 识别值改动
        public static final String ABANDON = "ABANDON";               // 记录作废
        public static final String REVIEW_CONFIRM = "REVIEW_CONFIRM"; // 复核确认
        public static final String BATCH_CONFIRM = "BATCH_CONFIRM";   // 批量确认
        public static final String VIDEO_DELETE = "VIDEO_DELETE";     // 视频删除
    }

    /** 合格限度比例 0.05%（限度 = 标称值 × (1 ± 0.05%)） */
    public static final String LIMIT_RATE = "0.0005";
    /** 较大误差阈值 1%（仅提示/预填，不参与合格判定） */
    public static final String BIG_DEVIATION_RATE = "0.01";

    /** 会话中登录用户信息的键 */
    public static final String SESSION_USER = "loginUser";
}
