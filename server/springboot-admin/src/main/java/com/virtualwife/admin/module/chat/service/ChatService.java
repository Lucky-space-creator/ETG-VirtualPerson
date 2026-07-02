package com.virtualwife.admin.module.chat.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.virtualwife.admin.integration.llm.LlmApiClient;
import com.virtualwife.admin.integration.rag.RagApiClient;
import com.virtualwife.admin.module.avatar.entity.AvatarConfig;
import com.virtualwife.admin.module.avatar.service.AvatarConfigService;
import com.virtualwife.admin.module.chat.entity.ChatRecord;
import com.virtualwife.admin.module.chat.mapper.ChatRecordMapper;
import com.virtualwife.admin.module.llm.entity.LlmConfig;
import com.virtualwife.admin.module.llm.mapper.LlmConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRecordMapper chatRecordMapper;
    private final LlmConfigMapper llmConfigMapper;
    private final AvatarConfigService avatarService;
    private final LlmApiClient llmApiClient;
    private final RagApiClient ragApiClient;

    @Value("${virtualwife.rag.api-url:http://localhost:5001}")
    private String ragApiUrl;

    /**
     * 处理用户消息并返回AI回复
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @param avatarId  数字人形象ID（可选，默认使用第一个）
     * @param userMsg      用户消息文本
     * @param scenicSpot   景区标识（可选）
     * @param userInterest 用户兴趣偏好（可选）
     * @return AI回复文本
     */
    public String chat(Long userId, String sessionId, Long avatarId, String userMsg, String scenicSpot, String userInterest) {
        // 1. 保存用户消息
        ChatRecord userRecord = new ChatRecord();
        userRecord.setUserId(userId);
        userRecord.setSessionId(sessionId);
        userRecord.setMessageType("text");
        userRecord.setContent(userMsg);
        userRecord.setEmotion("neutral");

        // 获取数字人名称
        String avatarName = "默认导游";
        AvatarConfig avatar = null;
        try {
            if (avatarId != null) {
                avatar = avatarService.getById(avatarId);
            }
            if (avatar == null) {
                // 使用第一个可用的数字人
                List<AvatarConfig> avatars = avatarService.list();
                if (avatars != null && !avatars.isEmpty()) {
                    avatar = avatars.get(0);
                }
            }
            if (avatar != null) {
                avatarName = avatar.getAvatarName();
                userRecord.setAvatarName(avatarName);
            }
        } catch (Exception e) {
            log.warn("获取数字人信息失败，使用默认: {}", e.getMessage());
        }
        chatRecordMapper.insert(userRecord);

        // 2. RAG知识检索（支持多景区）
        String ragContext = "";
        try {
            ragContext = retrieveKnowledge(userMsg, scenicSpot);
        } catch (Exception e) {
            log.warn("RAG检索失败，将直接调用LLM: {}", e.getMessage());
        }

        // 3. 获取历史消息（最近10条）
        List<Map<String, String>> history = getRecentHistory(sessionId, 10);

        // 4. 构建系统提示词
        String systemPrompt = buildSystemPrompt(avatar, ragContext, userInterest);

        // 5. 获取默认LLM配置
        LlmConfig llmConfig = getDefaultLlmConfig();
        if (llmConfig == null) {
            log.error("未找到默认LLM配置");
            String fallback = "抱歉，AI服务暂时不可用，请联系管理员配置LLM。";
            saveAiReply(userId, sessionId, avatarName, fallback);
            return fallback;
        }

        // 6. 调用LLM
        String aiReply;
        try {
            aiReply = llmApiClient.chat(llmConfig, systemPrompt, history, userMsg);
        } catch (Exception e) {
            log.error("LLM调用失败: {}", e.getMessage());
            aiReply = "抱歉，AI暂时无法回复，请稍后再试。";
        }

        // 7. 保存AI回复
        saveAiReply(userId, sessionId, avatarName, aiReply);

        return aiReply;
    }

    /**
     * RAG知识检索（支持多景区）
     */
    private String retrieveKnowledge(String query, String scenicSpot) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("query", query);
            body.put("top_k", 3);
            body.put("scenic_spot", scenicSpot != null ? scenicSpot : "default");

            HttpResponse resp = HttpRequest.post(ragApiUrl + "/api/rag/search/hybrid")
                    .header("Content-Type", "application/json")
                    .body(JSONUtil.toJsonStr(body))
                    .timeout(15000)
                    .execute();

            if (resp.isOk()) {
                JSONObject result = JSONUtil.parseObj(resp.body());
                JSONArray chunks = result.getJSONArray("chunks");
                if (chunks != null && !chunks.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < chunks.size(); i++) {
                        Object chunk = chunks.get(i);
                        if (chunk instanceof String) {
                            sb.append(chunk);
                        } else if (chunk instanceof JSONObject) {
                            sb.append(((JSONObject) chunk).getStr("content", ""));
                        } else {
                            sb.append(chunk.toString());
                        }
                        if (i < chunks.size() - 1) sb.append("\n---\n");
                    }
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            log.warn("RAG检索异常: {}", e.getMessage());
        }
        return "";
    }

    /**
     * 获取最近历史消息
     */
    private List<Map<String, String>> getRecentHistory(String sessionId, int limit) {
        if (sessionId == null || sessionId.isBlank()) return Collections.emptyList();

        List<ChatRecord> records = chatRecordMapper.selectList(
                new LambdaQueryWrapper<ChatRecord>()
                        .eq(ChatRecord::getSessionId, sessionId)
                        .orderByDesc(ChatRecord::getCreateTime)
                        .last("LIMIT " + (limit * 2))  // 用户+AI各取limit条
        );

        // 反转为时间正序
        Collections.reverse(records);

        return records.stream()
                .limit(limit * 2)
                .map(r -> {
                    Map<String, String> msg = new HashMap<>();
                    // 用户消息 vs AI消息判断
                    boolean isAi = r.getAvatarName() != null && !r.getAvatarName().isBlank()
                            && !"text".equals(r.getMessageType());
                    msg.put("role", isAi ? "assistant" : "user");
                    msg.put("content", r.getContent());
                    return msg;
                })
                .collect(Collectors.toList());
    }

    /**
     * 构建系统提示词（支持个性化推荐，优化响应速度）
     */
    private String buildSystemPrompt(AvatarConfig avatar, String ragContext, String userInterest) {
        StringBuilder sb = new StringBuilder();

        // 角色设定（精简）
        if (avatar != null && avatar.getPersona() != null && !avatar.getPersona().isBlank()) {
            sb.append(avatar.getPersona());
        } else {
            sb.append("你是灵山胜境AI导游，热情友好、知识渊博。");
        }

        if (avatar != null && avatar.getPersonality() != null && !avatar.getPersonality().isBlank()) {
            sb.append("性格：").append(avatar.getPersonality()).append("。");
        }

        // 回复要求（精简）
        sb.append("用简洁口语化回答，控制在100字内。");

        // 个性化推荐（精简）
        if (userInterest != null && !userInterest.isBlank()) {
            sb.append("游客兴趣：").append(userInterest).append("。");
        }

        // RAG知识上下文（精简）
        if (ragContext != null && !ragContext.isBlank()) {
            sb.append("\n知识：").append(ragContext);
        }

        return sb.toString();
    }

    /**
     * 获取默认LLM配置
     */
    private LlmConfig getDefaultLlmConfig() {
        LlmConfig config = llmConfigMapper.selectOne(
                new LambdaQueryWrapper<LlmConfig>().eq(LlmConfig::getIsDefault, 1).last("LIMIT 1")
        );
        if (config == null) {
            // 如果没有默认配置，取第一个
            config = llmConfigMapper.selectOne(
                    new LambdaQueryWrapper<LlmConfig>().last("LIMIT 1")
            );
        }
        return config;
    }

    /**
     * 保存AI回复
     */
    private void saveAiReply(Long userId, String sessionId, String avatarName, String content) {
        ChatRecord aiRecord = new ChatRecord();
        aiRecord.setUserId(userId);
        aiRecord.setSessionId(sessionId);
        aiRecord.setAvatarName(avatarName);
        aiRecord.setMessageType("ai_reply");
        aiRecord.setContent(content);
        aiRecord.setEmotion("neutral");
        chatRecordMapper.insert(aiRecord);
    }
}
