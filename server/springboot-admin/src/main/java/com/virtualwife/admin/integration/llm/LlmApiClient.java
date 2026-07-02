package com.virtualwife.admin.integration.llm;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.virtualwife.admin.module.llm.entity.LlmConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM API客户端
 * 直接调用 OpenAI 兼容 API（支持 Ollama、DeepSeek、通义千问等）
 */
@Slf4j
@Component
public class LlmApiClient {

    /**
     * 调用LLM生成回复
     *
     * @param config    LLM配置
     * @param systemMsg 系统提示词（数字人人设）
     * @param history   历史消息列表 [{role:"user"/"assistant", content:"..."}]
     * @param userMsg   当前用户消息
     * @return AI回复文本
     */
    public String chat(LlmConfig config, String systemMsg, List<Map<String, String>> history, String userMsg) {
        String apiUrl = config.getApiUrl(); apiUrl = apiUrl.trim();
        String apiKey = config.getApiKey(); apiKey = apiKey.trim();
        String modelName = config.getModelName(); modelName = modelName.trim();

        // 构建messages数组
        JSONArray messages = new JSONArray();

        // 系统提示
        if (systemMsg != null && !systemMsg.isBlank()) {
            JSONObject sys = new JSONObject();
            sys.set("role", "system");
            sys.set("content", systemMsg);
            messages.add(sys);
        }

        // 历史消息
        if (history != null) {
            for (Map<String, String> msg : history) {
                JSONObject m = new JSONObject();
                m.set("role", msg.get("role"));
                m.set("content", msg.get("content"));
                messages.add(m);
            }
        }

        // 当前用户消息
        JSONObject user = new JSONObject();
        user.set("role", "user");
        user.set("content", userMsg);
        messages.add(user);

        // 构建请求体（优化响应速度）
        JSONObject body = new JSONObject();
        body.set("model", modelName);
        body.set("messages", messages);
        body.set("temperature", config.getTemperature() != null ? config.getTemperature() : 0.7);
        // 减少max_tokens以加快响应
        int maxTokens = config.getMaxTokens() != null ? config.getMaxTokens() : 1024;
        body.set("max_tokens", Math.min(maxTokens, 500));  // 限制为500 tokens
        body.set("stream", false);

        // 构建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        if (apiKey != null && !apiKey.isBlank()) {
            headers.put("Authorization", "Bearer " + apiKey);
        }

        // 根据provider适配不同API格式
        String provider = config.getProvider() != null ? config.getProvider().toLowerCase() : "";
        String requestUrl = buildApiUrl(apiUrl, provider);

        log.info("调用LLM: provider={}, model={}, url={}", provider, modelName, requestUrl);

        try {
            // 增加超时时间到60秒，LLM响应通常较慢
            HttpRequest request = HttpRequest.post(requestUrl)
                    .addHeaders(headers)
                    .body(body.toString())
                    .timeout(60000);

            HttpResponse response = request.execute();

            if (!response.isOk()) {
                log.error("LLM调用失败: status={}, body={}", response.getStatus(), response.body());
                throw new RuntimeException("LLM调用失败: HTTP " + response.getStatus());
            }

            String responseBody = response.body();
            JSONObject result = JSONUtil.parseObj(responseBody);

            // 解析OpenAI格式响应
            JSONArray choices = result.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");
                if (message != null) {
                    // 优先取content
                    String content = message.getStr("content", "");
                    // 兼容思考模型（mimo等）：content为空时取reasoning_content
                    if (content == null || content.isBlank()) {
                        String reasoning = message.getStr("reasoning_content", "");
                        if (reasoning != null && !reasoning.isBlank()) {
                            log.info("LLM返回思考模型，使用reasoning_content，长度={}", reasoning.length());
                            // 截取前500字作为回复（reasoning通常很长）
                            return reasoning.length() > 500 ? reasoning.substring(0, 500) : reasoning;
                        }
                    }
                    return content;
                }
            }

            // Ollama格式兼容
            String ollamaResponse = result.getStr("response");
            if (ollamaResponse != null) {
                return ollamaResponse;
            }

            log.warn("LLM响应格式未知: {}", responseBody);
            return "抱歉，AI暂时无法回复，请稍后再试。";

        } catch (Exception e) {
            log.error("LLM调用异常: {}", e.getMessage(), e);
            throw new RuntimeException("LLM调用失败: " + e.getMessage());
        }
    }

    /**
     * 根据provider构建API URL
     */
    private String buildApiUrl(String apiUrl, String provider) {
        if (apiUrl == null || apiUrl.isBlank()) {
            throw new RuntimeException("LLM API URL未配置");
        }

        // 去掉尾部斜杠
        String url = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;

        // Ollama: /api/chat
        if (provider.contains("ollama")) {
            if (!url.contains("/api/chat")) {
                url = url + "/api/chat";
            }
            return url;
        }

        // 如果URL已经包含完整路径（/chat/completions），直接使用
        if (url.contains("/chat/completions")) {
            return url;
        }

        // OpenAI兼容API: /v1/chat/completions (适用于DeepSeek、通义千问、Moonshot等)
        if (url.contains("/v1")) {
            url = url + "/chat/completions";
        } else {
            url = url + "/v1/chat/completions";
        }
        return url;
    }

    /**
     * 测试LLM连通性
     */
    public boolean testConnection(LlmConfig config) {
        try {
            String reply = chat(config, "你好", null, "请回复OK");
            return reply != null && !reply.isBlank();
        } catch (Exception e) {
            log.error("LLM连通测试失败: {}", e.getMessage());
            return false;
        }
    }
}
