# 天平校验视频分析系统 · 代码工程

课程项目（第 1 组）。当前分支已合并成员甲的 4 个编码模块：
视频导入与视觉识别、校验判定引擎、记录查询/曲线/PDF、留痕与审计。
技术栈：Spring Boot 2.7 + Thymeleaf + MyBatis-Plus + MySQL 8 + ZXing + ffmpeg + Flying Saucer + RapidOCR（本地文字卡 OCR）。

## 环境要求

- JDK 8 及以上
- Maven 3.6+
- MySQL 8.0（本地或内网）
- ffmpeg（本项目默认路径 `E:/ffmpeg/bin/ffmpeg.exe`，可在 `application.yml` 修改）
- Python 3（可选，用于文字身份卡 OCR）

## 快速开始

1. **提供数据库账号密码**：设置环境变量 `TPJY_DB_USER`、`TPJY_DB_PASSWORD`（未设置时回退为 `root` / `123456`）。
2. **启动**（首次会自动建库 `tpjy` 并执行建表脚本、初始化演示数据）：

   ```bash
   mvn -s mvn-settings.xml spring-boot:run
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
    │   ├── entity/                    # 8 张现有实体（schema 共 9 张表）
    │   ├── mapper/                    # MyBatis-Plus Mapper
    │   ├── service/                   # 判定、识别、查询、曲线、PDF、审计
    │   │   ├── frame/                 # ffmpeg 抽帧
    │   │   ├── qr/                    # ZXing 二维码识别
    │   │   ├── ocr/                   # OCR 预留接口 + Noop 实现
    │   │   ├── audit/                 # 数据库/文件审计落地
    │   │   └── video/                 # 视频落盘与 7 天保留
    │   ├── cli/                       # 无界面识别与演示视频入口
    │   └── controller/                # 登录、检验人员、复核人员控制器
    └── resources/
        ├── application.yml
        ├── sql/schema.sql
        ├── static/css/app.css
        └── templates/                 # 登录、导入/核对、查询、曲线、PDF 模板
```

## 关键约定（编码前先定稿，见《代码落地实施方案》）

- **二维码内容**：人员卡 `PERSON:姓名`、天平卡 `BALANCE:天平编号`
- **记录号**：`JY + yyyyMMdd + "-" + 天平编号 + "-" + 当日3位流水`
- **限度**：标称值 ± 0.05%，用 BigDecimal 实时计算，绝不引用写死值
- **OCR**：`OcrService` 接口 + `NoopOcrService` 默认实现（返回 null → 人工补录兜底）

## 当前进度

- ✅ 迭代 0：可运行骨架（登录 + 权限拦截 + 建表 + 演示数据）
- ✅ 甲 1.3.2：视频上传/落盘、ffmpeg 抽帧、ZXing 二维码 + RapidOCR 文字身份卡识别、自动查对、9 次示值人工补录、g/mg 原屏单位保留与换算、提示定位
- ✅ 甲 1.3.5：限度实时计算、9 次逐项判定、前置条件一票否决、1% 较大误差预填、单/双量程归组
- ✅ 甲 1.3.6：按天平/日期查询、L1/L2/L3 三张趋势图、复核记录记录表式 PDF 导出
- ✅ 甲 1.3.8：五类审计 + 登录日志、审计双落地、视频 7 天自动清理与删除留痕
- 🟡 乙 1.3.3 / 1.3.4：记录提交、复核确认尚待实现；当前查询页只读取数据库中已有记录
- 🟡 乙 1.3.1 / 1.3.7：登录骨架可用，账号增删、主数据/模板维护仍待实现
- 🟡 迭代 3：3 段实拍视频已完成抽帧/判定/趋势图/PDF 手动示值验证；自动 OCR、并发测试、部署说明仍待补

`OcrService` 按 SRS 保持可替换接口；默认 `NoopOcrService` 不读数字，导入页可人工补录 9 个示值。
人工输入支持 `200.0008g` 或 `20000.8mg`：系统保留原屏数值/单位，换算为 g 后计算限度和判定。
PDF 按记录表格式显示每次测量时间；趋势图为 L1/L2/L3 三个独立面板，纵轴默认放大到标称值 ±0.2%。
当前 3 段实拍视频中的身份卡是印刷文字 `F002/F001`。系统会先尝试二维码，再用本地 RapidOCR 读取文字卡；实测 video1 自动识别为 F002，整段处理约 62 秒。人员卡仍未出现在视频中，人员按登录账号/人工指定。

首次使用文字卡 OCR：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-local-ocr.ps1
```

OCR 依赖位于被 Git 忽略的 `.ocr-venv/`，模型和识别都在本机运行，不上传视频。

## 自动化验证

```bash
mvn -s mvn-settings.xml test
```

2026-10-19 验证结果：**41 tests, 0 failures, 0 errors**。
测试使用 H2 内存数据库启动完整 Spring 上下文，并实际调用上传接口、ffmpeg、QR 识别、查询、曲线和 PDF 下载；不依赖本机 MySQL 密码。

详细交接、接口边界和验收注意点见 `docs/甲模块交付说明.md`。
成员乙继续开发请先看 `docs/给乙的继续开发清单.md`。

数据库每日备份脚本：`scripts/backup-tpjy.ps1`（通过 `TPJY_DB_USER` / `TPJY_DB_PASSWORD` 提供凭据）。

## 打包部署

```bash
mvn -s mvn-settings.xml clean package
java -jar target/tpjy-1.0.0.jar
```
