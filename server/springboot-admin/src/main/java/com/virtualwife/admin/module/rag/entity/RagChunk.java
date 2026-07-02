package com.virtualwife.admin.module.rag.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_chunk")
public class RagChunk {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long docId;
    private Long kbId;
    private Integer chunkIndex;
    private String content;
    private String enhancedContent;
    private String summary;
    private String entities;
    private String relations;
    private String sectionPath;
    private String contentHash;
    private Integer tokenCount;
    private String vectorId;
    private Integer vectorStatus;
    private Integer pageNum;
    private String metadataJson;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
