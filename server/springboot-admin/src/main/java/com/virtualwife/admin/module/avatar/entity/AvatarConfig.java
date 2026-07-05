package com.virtualwife.admin.module.avatar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("avatar_config")
public class AvatarConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long scenicSpotId;
    private String avatarName;
    private String vrmModelUrl;
    private String thumbnailUrl;
    private String persona;
    private String personality;
    private String voiceType;
    private String emotionConfig;
    private String backgroundUrl;
    private Integer isSystem;
    private Integer isDefault;
    private Integer sortOrder;
    // 衣服和发型配置
    private String clothesStyle;
    private String clothesColor;
    private String hairStyle;
    private String hairColor;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 缩略图显示URL（presigned，不存入数据库） */
    @TableField(exist = false)
    private String thumbnailDisplayUrl;

    /** VRM模型显示URL（presigned，不存入数据库） */
    @TableField(exist = false)
    private String vrmDisplayUrl;

    /** 背景图片显示URL（presigned，不存入数据库） */
    @TableField(exist = false)
    private String backgroundDisplayUrl;
}
