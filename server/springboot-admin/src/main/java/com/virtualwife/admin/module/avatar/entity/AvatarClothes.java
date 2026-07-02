package com.virtualwife.admin.module.avatar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数字人衣服配置实体
 */
@Data
@TableName("avatar_clothes")
public class AvatarClothes {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 数字人形象ID
     */
    private Long avatarId;

    /**
     * 衣服名称
     */
    private String clothesName;

    /**
     * VRM模型路径
     */
    private String vrmModelUrl;

    /**
     * 缩略图路径
     */
    private String thumbnailUrl;

    /**
     * 衣服描述
     */
    private String description;

    /**
     * 是否默认
     */
    private Integer isDefault;

    /**
     * 排序
     */
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 缩略图显示URL（presigned，不存入数据库）
     */
    @TableField(exist = false)
    private String thumbnailDisplayUrl;

    /**
     * VRM模型显示URL（presigned，不存入数据库）
     */
    @TableField(exist = false)
    private String vrmDisplayUrl;
}
