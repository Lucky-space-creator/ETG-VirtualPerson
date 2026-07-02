package com.virtualwife.admin.module.rag.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_qa_pair")
public class RagQaPair {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long kbId;
    private String question;
    private String expectedDocIds;
    private String expectedChunkIds;
    private Integer difficulty;
    private String category;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
