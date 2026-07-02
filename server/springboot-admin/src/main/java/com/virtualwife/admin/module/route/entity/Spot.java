package com.virtualwife.admin.module.route.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("spot")
public class Spot {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long routeId;
    private String spotName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer geoRadius;
    private String narrateText;
    private String imageUrl;
    private Integer spotOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
