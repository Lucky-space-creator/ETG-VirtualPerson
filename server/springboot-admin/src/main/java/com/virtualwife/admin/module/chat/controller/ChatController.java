package com.virtualwife.admin.module.chat.controller;

import com.virtualwife.admin.common.result.Result;
import com.virtualwife.admin.module.chat.dto.ChatRequest;
import com.virtualwife.admin.module.chat.service.ChatService;
import com.virtualwife.admin.module.avatar.entity.AvatarConfig;
import com.virtualwife.admin.module.avatar.service.AvatarConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 聊天API - 供Android端和Web前端调用
 * 不需要ADMIN角色认证，游客可直接使用
 */
@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final AvatarConfigService avatarService;

    /**
     * 发送消息并获取AI回复
     * POST /api/admin/chat/send
     */
    @PostMapping("/send")
    public Result<Map<String, Object>> sendMessage(@Valid @RequestBody ChatRequest request) {
        try {
            // 生成会话ID（如果未提供）
            String sessionId = request.getSessionId();
            if (sessionId == null || sessionId.isBlank()) {
                sessionId = UUID.randomUUID().toString().replace("-", "");
            }

            Long userId = request.getUserId() != null ? request.getUserId() : 0L;
            Long avatarId = request.getAvatarId();
            String scenicSpot = request.getScenicSpot() != null ? request.getScenicSpot() : "default";
            String userInterest = request.getUserInterest() != null ? request.getUserInterest() : "";

            String reply = chatService.chat(userId, sessionId, avatarId, request.getMessage(), scenicSpot, userInterest);

            Map<String, Object> data = new HashMap<>();
            data.put("sessionId", sessionId);
            data.put("reply", reply);
            data.put("avatarId", avatarId);

            return Result.success(data);
        } catch (Exception e) {
            log.error("聊天处理失败: {}", e.getMessage(), e);
            return Result.error("聊天处理失败: " + e.getMessage());
        }
    }

    /**
     * 获取可用的数字人列表
     * GET /api/admin/chat/avatars
     */
    @GetMapping("/avatars")
    public Result<List<AvatarConfig>> getAvatars() {
        List<AvatarConfig> avatars = avatarService.list();
        return Result.success(avatars);
    }

    /**
     * 健康检查
     * GET /api/admin/chat/health
     */
    @GetMapping("/health")
    public Result<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "ok");
        status.put("service", "chat");
        return Result.success(status);
    }

}
