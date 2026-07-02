package com.virtualwife.admin.module.rag.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rag_evaluation")
public class RagEvaluation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long kbId;
    private String evalType;
    private BigDecimal recallAt5;
    private BigDecimal recallAt10;
    private BigDecimal mrr;
    private BigDecimal ndcgAt5;
    private BigDecimal hitAt5;
    private BigDecimal avgFaithfulness;
    private BigDecimal avgRelevance;
    private String configSnapshot;
    private String detailJson;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
