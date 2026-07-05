package com.virtualwife.admin.module.mobile.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.virtualwife.admin.common.util.MinioUtil;
import com.virtualwife.admin.module.avatar.entity.AvatarConfig;
import com.virtualwife.admin.module.avatar.service.AvatarConfigService;
import com.virtualwife.admin.module.chat.service.ChatService;
import com.virtualwife.admin.module.llm.entity.LlmConfig;
import com.virtualwife.admin.module.llm.service.LlmConfigService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Android兼容路由 - 映射Django路径到SpringBoot
 * 让Android App无需修改代码即可对接SpringBoot
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AndroidCompatController {

    private final ChatService chatService;
    private final LlmConfigService llmConfigService;
    private final AvatarConfigService avatarConfigService;
    private final MinioUtil minioUtil;

    /**
     * 聊天接口 - 兼容Android的 chatbot/chat 路径
     * 使用手动读取请求体避免UTF-8编码问题
     */
    @PostMapping("/chatbot/chat")
    public Map<String, Object> chat(HttpServletRequest httpRequest) {
        try {
            // 手动读取请求体
            byte[] bytes = httpRequest.getInputStream().readAllBytes();

            // 尝试UTF-8解码，如果出现乱码则使用GBK
            String body = new String(bytes, StandardCharsets.UTF_8);
            if (body.contains("�") || body.contains("?")) {
                // UTF-8解码失败，尝试GBK
                body = new String(bytes, java.nio.charset.Charset.forName("GBK"));
            }

            JSONObject request = JSONUtil.parseObj(body);

            String query = request.getStr("query", request.getStr("message", ""));
            String youName = request.getStr("you_name", request.getStr("username", "游客"));
            String sessionId = request.getStr("session_id", request.getStr("sessionId", ""));
            String scenicSpot = request.getStr("scenic_spot", request.getStr("scenicSpot", "default"));
            String userInterest = request.getStr("user_interest", request.getStr("userInterest", ""));
            Long avatarId = request.get("avatar_id") != null
                    ? Long.valueOf(request.get("avatar_id").toString())
                    : null;
            Long scenicSpotId = request.get("scenic_spot_id") != null
                    ? Long.valueOf(request.get("scenic_spot_id").toString())
                    : null;

            if (sessionId == null || sessionId.isBlank()) {
                sessionId = UUID.randomUUID().toString().replace("-", "");
            }

            Long userId = 0L;
            if (request.get("user_id") != null) {
                userId = Long.valueOf(request.get("user_id").toString());
            }

            String reply = chatService.chat(userId, sessionId, avatarId, query, scenicSpot, userInterest, scenicSpotId);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", reply);
            result.put("result", reply);
            result.put("status", "ok");
            result.put("session_id", sessionId);
            return result;
        } catch (Exception e) {
            log.error("chatbot/chat error: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("status", "error");
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * 系统配置 - 兼容Android的 chatbot/config/get 路径
     */
    @GetMapping("/chatbot/config/get")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        LlmConfig defaultLlm = llmConfigService.list().stream()
                .filter(c -> c.getIsDefault() != null
                        && c.getIsDefault() == 1)
                .findFirst().orElse(null);

        Map<String, Object> data = new HashMap<>();
        if (defaultLlm != null) {
            data.put("llm_provider", defaultLlm.getProvider());
            data.put("llm_model_name", defaultLlm.getModelName());
            data.put("llm_api_url", defaultLlm.getApiUrl());
        }
        data.put("tts_voice", "zh-CN-XiaoyiNeural");
        data.put("tts_type", "Edge");

        // 获取默认形象配置
        AvatarConfig defaultAvatar = avatarConfigService.getDefault();
        Map<String, Object> characterConfig = new HashMap<>();
        if (defaultAvatar != null) {
            characterConfig.put("character", defaultAvatar.getId());
            characterConfig.put("character_name",
                    defaultAvatar.getAvatarName());
            characterConfig.put("yourName", "游客");
            characterConfig.put("vrmModel",
                    defaultAvatar.getVrmModelUrl());
            characterConfig.put("vrmModelType", "system");
            // 背景URL
            String bgUrl = defaultAvatar.getBackgroundUrl();
            if (bgUrl != null && !bgUrl.isBlank()) {
                data.put("background_url",
                        minioUtil.getPresignedUrl(bgUrl));
            }
        } else {
            characterConfig.put("character", 1);
            characterConfig.put("character_name", "AI导游");
            characterConfig.put("yourName", "游客");
        }
        data.put("characterConfig", characterConfig);

        config.put("code", 200);
        config.put("data", data);
        config.put("response", data);
        return config;
    }

    /**
     * 保存配置 - 兼容Android的 chatbot/config/save 路径
     */
    @PostMapping("/chatbot/config/save")
    public Map<String, Object> saveConfig(
            @RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "Config saved");
        return result;
    }

    /**
     * 背景列表 - 兼容 chatbot/config/background/show
     */
    @GetMapping("/chatbot/config/background/show")
    public Map<String, Object> showBackgrounds() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<AvatarConfig> avatars = avatarConfigService.list();
            List<Map<String, Object>> bgList = new ArrayList<>();
            Set<String> addedUrls = new HashSet<>();
            for (AvatarConfig avatar : avatars) {
                String bgUrl = avatar.getBackgroundUrl();
                if (bgUrl != null && !bgUrl.isBlank()
                        && addedUrls.add(bgUrl)) {
                    Map<String, Object> bg = new HashMap<>();
                    bg.put("id", avatar.getId());
                    bg.put("original_name", avatar.getAvatarName()
                            + " - 背景");
                    bg.put("image", minioUtil.getPresignedUrl(bgUrl));
                    bgList.add(bg);
                }
            }
            result.put("code", "200");
            result.put("response", bgList);
        } catch (Exception e) {
            log.error("获取背景列表失败", e);
            result.put("code", "500");
            result.put("response", Collections.emptyList());
        }
        return result;
    }

    /**
     * 系统VRM模型列表 - 兼容 chatbot/config/vrm/system/show
     */
    @GetMapping("/chatbot/config/vrm/system/show")
    public Map<String, Object> showSystemVrmModels() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<AvatarConfig> avatars = avatarConfigService.list(
                    new com.baomidou.mybatisplus.core.conditions
                            .query.LambdaQueryWrapper<AvatarConfig>()
                            .eq(AvatarConfig::getIsSystem, 1));
            List<Map<String, Object>> vrmList = new ArrayList<>();
            for (AvatarConfig avatar : avatars) {
                Map<String, Object> vrm = new HashMap<>();
                vrm.put("id", avatar.getId());
                vrm.put("original_name", avatar.getAvatarName());
                vrm.put("vrm", avatar.getVrmModelUrl());
                vrm.put("type", "system");
                vrmList.add(vrm);
            }
            result.put("code", "200");
            result.put("response", vrmList);
        } catch (Exception e) {
            log.error("获取系统VRM模型列表失败", e);
            result.put("code", "500");
            result.put("response", Collections.emptyList());
        }
        return result;
    }

    /**
     * 用户VRM模型列表 - 兼容 chatbot/config/vrm/user/show
     */
    @GetMapping("/chatbot/config/vrm/user/show")
    public Map<String, Object> showUserVrmModels() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<AvatarConfig> avatars = avatarConfigService.list(
                    new com.baomidou.mybatisplus.core.conditions
                            .query.LambdaQueryWrapper<AvatarConfig>()
                            .eq(AvatarConfig::getIsSystem, 0));
            List<Map<String, Object>> vrmList = new ArrayList<>();
            for (AvatarConfig avatar : avatars) {
                Map<String, Object> vrm = new HashMap<>();
                vrm.put("id", avatar.getId());
                vrm.put("original_name", avatar.getAvatarName());
                vrm.put("vrm", avatar.getVrmModelUrl());
                vrm.put("type", "user");
                vrmList.add(vrm);
            }
            result.put("code", "200");
            result.put("response", vrmList);
        } catch (Exception e) {
            log.error("获取用户VRM模型列表失败", e);
            result.put("code", "500");
            result.put("response", Collections.emptyList());
        }
        return result;
    }

    /**
     * TTS语音列表 - 兼容 chatbot/config/tts/voices
     */
    @GetMapping("/chatbot/config/tts/voices")
    public Map<String, Object> showVoices(
            @RequestParam(defaultValue = "Edge") String ttsType) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> voices = new ArrayList<>();
            if ("Edge".equals(ttsType)) {
                voices.add(buildVoice("zh-CN-XiaoyiNeural", "晓伊"));
                voices.add(buildVoice("zh-CN-XiaoxiaoNeural", "晓晓"));
                voices.add(buildVoice("zh-CN-YunxiNeural", "云希"));
                voices.add(buildVoice("zh-CN-YunjianNeural", "云健"));
            }
            result.put("code", "200");
            result.put("response", voices);
        } catch (Exception e) {
            log.error("获取语音列表失败", e);
            result.put("code", "500");
            result.put("response", Collections.emptyList());
        }
        return result;
    }

    private Map<String, Object> buildVoice(String id, String name) {
        Map<String, Object> voice = new HashMap<>();
        voice.put("id", id);
        voice.put("name", name);
        return voice;
    }
}
