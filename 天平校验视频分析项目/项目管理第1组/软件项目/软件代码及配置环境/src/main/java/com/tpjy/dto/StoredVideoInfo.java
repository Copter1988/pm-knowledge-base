package com.tpjy.dto;

import java.io.File;
import java.util.Date;

/** 视频落盘结果，供记录管理模块写入 video 表。 */
public class StoredVideoInfo {

    private final File file;
    private final String relativePath;
    private final String originalName;
    private final Date storedAt;

    public StoredVideoInfo(File file, String relativePath, String originalName, Date storedAt) {
        this.file = file;
        this.relativePath = relativePath;
        this.originalName = originalName;
        this.storedAt = storedAt;
    }

    public File getFile() {
        return file;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getOriginalName() {
        return originalName;
    }

    public Date getStoredAt() {
        return storedAt;
    }
}
