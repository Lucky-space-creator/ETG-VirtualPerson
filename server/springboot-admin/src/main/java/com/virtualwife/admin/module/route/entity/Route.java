package com.virtualwife.admin.module.route.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("route")
public class Route {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long kbId;
    private String routeName;
    private String interestTags;
    private Integer timeBudget;
    private Integer energyLevel;
    private String description;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
