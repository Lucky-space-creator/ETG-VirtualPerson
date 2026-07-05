package com.virtualwife.admin.module.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {

    private Long userId;

    @Size(max = 64, message = "sessionId长度不能超过64")
    private String sessionId;

    private Long avatarId;

    private Long scenicSpotId;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过2000字")
    private String message;

    @Size(max = 50, message = "景区标识长度不能超过50")
    private String scenicSpot;

    @Size(max = 200, message = "兴趣标签长度不能超过200")
    private String userInterest;
}
