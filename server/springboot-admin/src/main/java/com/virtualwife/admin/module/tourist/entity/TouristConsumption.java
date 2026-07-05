package com.virtualwife.admin.module.tourist.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 游客消费记录实体
 */
@Data
@TableName("tourist_consumption")
public class TouristConsumption {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long scenicSpotId;

    /**
     * 游客ID
     */
    private String touristId;

    /**
     * 用户昵称
     */
    private String userNickname;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 性别
     */
    private String gender;

    /**
     * 景点名称
     */
    private String attractionName;

    /**
     * 景点内容
     */
    private String attractionContent;

    /**
     * 景点类型
     */
    private String attractionType;

    /**
     * 访问日期
     */
    private LocalDate visitDate;

    /**
     * 停留时长（小时）
     */
    private BigDecimal stayDuration;

    /**
     * 门票费用
     */
    private BigDecimal ticketCost;

    /**
     * 餐饮费用
     */
    private BigDecimal foodCost;

    /**
     * 购物费用
     */
    private BigDecimal shoppingCost;

    /**
     * 交通费用
     */
    private BigDecimal transportCost;

    /**
     * 娱乐费用
     */
    private BigDecimal entertainmentCost;

    /**
     * 总费用
     */
    private BigDecimal totalCost;

    /**
     * 团队大小
     */
    private Integer groupSize;

    /**
     * 满意度（1-5）
     */
    private Integer satisfaction;

    /**
     * 景区标识
     */
    private String scenicSpot;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
