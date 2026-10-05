# 天平校验视频分析系统 · 代码框架

课程项目（第 1 组）。技术栈：Spring Boot 2.7 + Thymeleaf + MyBatis-Plus + MySQL 8 + ZXing（二维码）+ OCR 预留接口。

## 环境要求

- JDK 8 及以上
- Maven 3.6+
- MySQL 8.0（本地或内网）

## 快速开始

1. **改数据库账号密码**：编辑 `src/main/resources/application.yml` 中 `spring.datasource.username/password`（默认 `root` / `123456`）。
2. **启动**（首次会自动建库 `tpjy` 并执行建表脚本、初始化演示数据）：

   ```bash
   mvn spring-boot:run
   ```

3. **访问**：浏览器打开 `http://localhost:8080/login`

## 演示账号（密码均为 123456）

| 账号 | 姓名 | 角色 |
|---|---|---|
| admin | 管理员 | ADMIN |
| liyi | 李一 | CHECKER（检验人员） |
| lier | 李二 | CHECKER（检验人员） |
| qiyi | 齐一 | REVIEWER（复核确认人员） |

## 目录结构

```
code/
├── pom.xml
├── sql 已并入 src/main/resources/sql/schema.sql（启动自动执行）
└── src/main/
    ├── java/com/tpjy/
    │   ├── TpjyApplication.java       # 启动类（@MapperScan）
    │   ├── common/                    # Result / 异常 / 常量 / 登录态
    │   ├── config/                    # 拦截器 / 配置 / 数据初始化
    │   ├── entity/                    # 8 张表实体
    │   ├── mapper/                    # MyBatis-Plus Mapper
    │   ├── service/                   # 业务（判定引擎已实现）
    │   │   └── ocr/                   # OCR 预留接口 + Noop 实现
    │   └── controller/                # 登录 / 首页 / 三角色控制器
    └── resources/
        ├── application.yml
        ├── sql/schema.sql
        ├── static/css/app.css
        └── templates/                 # 页面（登录已可用，其余为占位）
```

## 关键约定（编码前先定稿，见《代码落地实施方案》）

- **二维码内容**：人员卡 `PERSON:姓名`、天平卡 `BALANCE:天平编号`
- **记录号**：`JY + yyyyMMdd + "-" + 天平编号 + "-" + 当日3位流水`
- **限度**：标称值 ± 0.05%，用 BigDecimal 实时计算，绝不引用写死值
- **OCR**：`OcrService` 接口 + `NoopOcrService` 默认实现（返回 null → 人工补录兜底）

## 当前进度与下一步（迭代计划）

- ✅ 迭代 0：可运行骨架（登录 + 权限拦截 + 建表 + 演示数据）
- ⬜ 迭代 1：视频导入（二维码）+ 校验记录管理 + 判定引擎接入
- ⬜ 迭代 2：复核确认 + 查询/曲线/PDF + 天平模板维护 + 留痕审计
- ⬜ 迭代 3：三视频系统测试 + 部署 + 使用说明

## 打包部署

```bash
mvn clean package
java -jar target/tpjy-1.0.0.jar
```
