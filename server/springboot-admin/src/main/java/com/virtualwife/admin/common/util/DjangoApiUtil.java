package com.virtualwife.admin.common.util;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Django API工具类
 * 用于与Django后端通信：配置同步、LLM测试等
 */
@Slf4j
@Component
public class DjangoApiUtil {

    @Value("${virtualwife.django.api-url:}")
    private String djangoApiUrl;

    /**
     * 获取Django当前配置
     */
    public String getConfig() {
        if (djangoApiUrl == null || djangoApiUrl.isBlank()) return null;
        try {
            HttpResponse response = HttpRequest.get(djangoApiUrl + "/chatbot/config/get")
                    .timeout(10000)
                    .execute();
            if (response.isOk()) {
                return response.body();
            }
            log.warn("获取Django配置失败, status={}", response.getStatus());
            return null;
        } catch (Exception e) {
            log.error("调用Django配置接口失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 保存配置到Django
     */
    public boolean saveConfig(Map<String, Object> config) {
        if (djangoApiUrl == null || djangoApiUrl.isBlank()) return false;
        try {
            HttpResponse response = HttpRequest.post(djangoApiUrl + "/chatbot/config/save")
                    .body(JSONUtil.toJsonStr(config))
                    .timeout(10000)
                    .execute();
            boolean success = response.isOk();
            if (!success) {
                log.warn("保存Django配置失败, status={}, body={}", response.getStatus(), response.body());
            }
            return success;
        } catch (Exception e) {
            log.error("调用Django保存配置接口失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 测试LLM连通性
     */
    public boolean testLlmConnection(Map<String, Object> llmConfig) {
        if (djangoApiUrl == null || djangoApiUrl.isBlank()) return false;
        try {
            HttpResponse response = HttpRequest.post(djangoApiUrl + "/chatbot/llm/test")
                    .body(JSONUtil.toJsonStr(llmConfig))
                    .timeout(30000)
                    .execute();
            return response.isOk();
        } catch (Exception e) {
            log.error("LLM连通测试失败: {}", e.getMessage());
            return false;
        }
    }
}
