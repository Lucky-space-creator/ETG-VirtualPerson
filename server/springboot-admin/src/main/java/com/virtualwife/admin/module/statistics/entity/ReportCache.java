package com.virtualwife.admin.module.statistics.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("report_cache")
public class ReportCache {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String reportType;
    @TableField("content")
    private String content;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
