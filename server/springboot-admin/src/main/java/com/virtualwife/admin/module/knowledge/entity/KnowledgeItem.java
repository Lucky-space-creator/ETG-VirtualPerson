package com.virtualwife.admin.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_item")
public class KnowledgeItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long kbId;
    private String title;
    private String content;
    private String vectorId;
    private Integer vectorStatus;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
