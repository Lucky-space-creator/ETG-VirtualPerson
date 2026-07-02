package com.virtualwife.admin.integration.django;

import com.virtualwife.admin.common.util.DjangoApiUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Django配置同步服务
 * 将SpringBoot的LLM/角色配置同步到Django后端
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DjangoConfigSyncService {

    private final DjangoApiUtil djangoApiUtil;

    /**
     * 同步LLM配置到Django
     */
    public boolean syncLlmConfig(String provider, String apiUrl, String apiKey,
                                  String modelName, double temperature, int maxTokens) {
        Map<String, Object> config = new HashMap<>();
        config.put("llm_provider", provider);
        config.put("llm_api_url", apiUrl);
        config.put("llm_api_key", apiKey);
        config.put("llm_model_name", modelName);
        config.put("llm_temperature", temperature);
        config.put("llm_max_tokens", maxTokens);
        return djangoApiUtil.saveConfig(config);
    }
}
