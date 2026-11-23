-- 天平校验视频分析系统 · 数据库结构（幂等，可重复执行）
-- 主键统一自增 id；记录号只作业务显示/查询（见 SRS §3.4）

-- 1. 用户（三类角色：ADMIN 管理员 / CHECKER 检验人员 / REVIEWER 复核确认人员）
CREATE TABLE IF NOT EXISTS sys_user (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  username    VARCHAR(50)  NOT NULL,
  password    VARCHAR(100) NOT NULL,
  real_name   VARCHAR(50)  NOT NULL,
  card_code   VARCHAR(20),
  role        VARCHAR(20)  NOT NULL,
  enabled     TINYINT      NOT NULL DEFAULT 1,
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 砝码盒（按盒管理，一盒共用一号）
CREATE TABLE IF NOT EXISTS weight_box (
  id      BIGINT AUTO_INCREMENT PRIMARY KEY,
  box_no  VARCHAR(10) NOT NULL,
  remark  VARCHAR(200),
  UNIQUE KEY uk_box_no (box_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 天平主数据（对应「天平清单」）
CREATE TABLE IF NOT EXISTS balance (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  balance_code     VARCHAR(20)  NOT NULL,
  dept             VARCHAR(50),
  unique_no        VARCHAR(20)  NOT NULL,
  range_count      TINYINT      NOT NULL,
  range1           VARCHAR(20),
  range1_precision VARCHAR(20),
  range2           VARCHAR(20),
  range2_precision VARCHAR(20),
  internal_calib   VARCHAR(20),
  check_freq       VARCHAR(20),
  weight_box_id    BIGINT,
  weight_L1        DECIMAL(12,4),
  weight_L2        DECIMAL(12,4),
  weight_L3        DECIMAL(12,4),
  created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_balance_code (balance_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 视频（7 天保留，软删除留痕）
CREATE TABLE IF NOT EXISTS video (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  record_id     BIGINT,
  file_path     VARCHAR(255),
  original_name VARCHAR(255),
  uploaded_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted_at    DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. 校验记录（主表；物理主键 id，记录号加唯一索引）
CREATE TABLE IF NOT EXISTS calibration_record (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  record_no        VARCHAR(64)  NOT NULL,
  balance_id       BIGINT       NOT NULL,
  check_date       DATETIME     NOT NULL,
  clean_ok         TINYINT,
  level_ok         TINYINT,
  internal_calib_ok VARCHAR(10),
  result           TINYINT,
  remark           VARCHAR(500),
  checker_id       BIGINT,
  reviewer_id      BIGINT,
  status           VARCHAR(20)  NOT NULL,
  confirm_flag     TINYINT DEFAULT 0,
  submitted_at     DATETIME,
  reviewed_at      DATETIME,
  created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_record_no (record_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. 9 次示值（子表，seq 1~9 顺序归组 L1/L2/L3）
CREATE TABLE IF NOT EXISTS reading (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  record_id     BIGINT       NOT NULL,
  seq           TINYINT      NOT NULL,
  weight_level  CHAR(2)      NOT NULL,
  nominal       DECIMAL(12,4),
  reading_value DECIMAL(14,6),
    display_value VARCHAR(50),
    display_unit  VARCHAR(10)   DEFAULT 'g',
  measure_time  DATETIME,
  is_qualified  TINYINT,
  UNIQUE KEY uk_record_seq (record_id, seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. 识别值改动留痕（原识别值 → 确认值）
CREATE TABLE IF NOT EXISTS read_trace (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  record_id       BIGINT NOT NULL,
  reading_id      BIGINT,
  original_value  VARCHAR(50),
  confirmed_value VARCHAR(50),
  changed_by      BIGINT,
  changed_at      DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. 审计日志（五类操作 + 登录）
CREATE TABLE IF NOT EXISTS audit_log (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  operator_id   BIGINT,
  operator_name VARCHAR(50),
  action        VARCHAR(30) NOT NULL,
  target_type   VARCHAR(30),
  target_id     BIGINT,
  detail        VARCHAR(1000),
  created_at    DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. 记录呈现模板（FR-019，MVP：字段显隐/显示名/必填/顺序；字段本身不可增删）
CREATE TABLE IF NOT EXISTS record_template (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  field_key   VARCHAR(50) NOT NULL,
  field_label VARCHAR(50) NOT NULL,
  visible     TINYINT DEFAULT 1,
  required    TINYINT DEFAULT 0,
  sort_no     INT     DEFAULT 0,
  UNIQUE KEY uk_field_key (field_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
