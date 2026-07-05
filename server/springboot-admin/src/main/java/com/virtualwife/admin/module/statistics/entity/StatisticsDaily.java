package com.virtualwife.admin.module.statistics.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("statistics_daily")
public class StatisticsDaily {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long scenicSpotId;
    private LocalDate statDate;
    private Integer totalUsers;
    private Integer newUsers;
    private Integer activeUsers;
    private Integer totalMessages;
    private Integer aiMessages;
    private Integer userMessages;
    private BigDecimal avgSessionCount;
    private Long totalTokens;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
