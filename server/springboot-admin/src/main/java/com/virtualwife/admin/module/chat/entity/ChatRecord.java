package com.virtualwife.admin.module.chat.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_record")
public class ChatRecord {
    @TableId(type = IdType.AUTO)
    @ExcelProperty("ID")
    private Long id;

    @ExcelProperty("用户ID")
    private Long userId;

    @ExcelProperty("会话ID")
    private String sessionId;

    @ExcelProperty("数字人")
    private String avatarName;

    @ExcelProperty("消息类型")
    private String messageType;

    @ExcelProperty("内容")
    private String content;

    @ExcelProperty("情感")
    private String emotion;

    @ExcelProperty("Token数")
    private Integer tokenCount;

    @TableField(fill = FieldFill.INSERT)
    @ExcelProperty("创建时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @ExcelProperty("更新时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
