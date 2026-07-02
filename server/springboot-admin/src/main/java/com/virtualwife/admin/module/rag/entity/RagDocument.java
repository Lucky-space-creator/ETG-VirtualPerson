package com.virtualwife.admin.module.rag.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_document")
public class RagDocument {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long kbId;
    private String docName;
    private String docType;
    private String filePath;
    private Long fileSize;
    private String sha256;
    private Integer pageCount;
    private Integer chunkCount;
    private String chunkStrategy;
    private Integer processStatus;
    private Integer progressPercent;
    private String parsedText;
    private Integer chunkSize;
    private Integer chunkOverlap;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
