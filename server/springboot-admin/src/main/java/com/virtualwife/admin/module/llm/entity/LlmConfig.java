package com.virtualwife.admin.module.llm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("llm_config")
public class LlmConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String configName;
    private String provider;
    private String apiUrl;
    private String apiKey;
    private String modelName;
    private BigDecimal temperature;
    private Integer maxTokens;
    private Integer isDefault;
    private Integer connectStatus;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
