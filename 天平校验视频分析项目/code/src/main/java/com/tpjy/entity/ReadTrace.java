package com.tpjy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 识别值改动留痕（原识别值 → 确认值，仅改动项留痕）。 */
@Data
@TableName("read_trace")
public class ReadTrace {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recordId;
    private Long readingId;
    private String originalValue;
    private String confirmedValue;
    private Long changedBy;
    private LocalDateTime changedAt;
}
