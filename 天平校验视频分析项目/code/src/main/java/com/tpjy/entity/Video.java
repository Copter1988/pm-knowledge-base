package com.tpjy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 校验视频（7 天保留，软删除留痕）。 */
@Data
@TableName("video")
public class Video {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recordId;
    private String filePath;
    private String originalName;
    private LocalDateTime uploadedAt;
    private LocalDateTime deletedAt;
}
