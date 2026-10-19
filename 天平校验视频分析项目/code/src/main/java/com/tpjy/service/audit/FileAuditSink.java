package com.tpjy.service.audit;

import com.tpjy.dto.Operator;
import com.tpjy.entity.AuditLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

/** 审计日志写入文本文件（命令行离线测试用，无需数据库）。 */
public class FileAuditSink implements AuditSink {

    private final File file;
    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public FileAuditSink(File file) {
        this.file = file;
    }

    @Override
    public void write(Operator operator, AuditLog log) {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            boolean needHeader = !file.exists() || file.length() == 0;
            Writer w = new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8");
            try {
                if (needHeader) {
                    w.write("# 审计日志（五类操作 + 登录）\n");
                    w.write("# 时间 | 操作者 | 动作 | 目标类型 | 目标ID | 详情\n");
                }
                w.write(fmt.format(log.getCreatedAt() == null ? new Date() : log.getCreatedAt()));
                w.write(" | ");
                w.write(operator == null || operator.getName() == null ? "-" : operator.getName());
                w.write(" | ");
                w.write(log.getAction());
                w.write(" | ");
                w.write(log.getTargetType() == null ? "-" : log.getTargetType());
                w.write(" | ");
                w.write(log.getTargetId() == null ? "-" : String.valueOf(log.getTargetId()));
                w.write(" | ");
                w.write(log.getDetail() == null ? "" : log.getDetail().replace('\n', ' '));
                w.write("\n");
            } finally {
                w.close();
            }
        } catch (Exception e) {
            System.err.println("写审计日志失败：" + e.getMessage());
        }
    }
}
