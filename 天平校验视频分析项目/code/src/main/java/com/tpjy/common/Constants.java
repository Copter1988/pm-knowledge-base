package com.tpjy.common;

/** 全局常量。 */
public final class Constants {
    private Constants() {}

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
