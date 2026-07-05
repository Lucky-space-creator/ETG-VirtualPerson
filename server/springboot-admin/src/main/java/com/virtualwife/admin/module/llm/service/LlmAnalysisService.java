package com.virtualwife.admin.module.llm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.virtualwife.admin.integration.llm.LlmApiClient;
import com.virtualwife.admin.module.llm.entity.LlmConfig;
import com.virtualwife.admin.module.llm.mapper.LlmConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * LLM 分析服务 - 供内部调用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmAnalysisService {

    private final LlmConfigMapper llmConfigMapper;
    private final LlmApiClient llmApiClient;

    /**
     * 使用默认 LLM 配置分析文本
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return LLM 分析结果
     */
    public String analyze(String systemPrompt, String userPrompt) {
        LlmConfig config = getDefaultConfig();
        if (config == null) {
            throw new RuntimeException("未找到默认 LLM 配置，请先在系统设置中配置大模型");
        }

        log.info("使用 LLM 分析: provider={}, model={}", config.getProvider(), config.getModelName());
        return llmApiClient.chat(config, systemPrompt, Collections.emptyList(), userPrompt);
    }

    /**
     * 获取默认 LLM 配置
     */
    private LlmConfig getDefaultConfig() {
        // 优先查找默认配置
        LambdaQueryWrapper<LlmConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LlmConfig::getIsDefault, 1);
        LlmConfig config = llmConfigMapper.selectOne(wrapper);

        if (config != null) {
            return config;
        }

        // 没有默认配置，返回第一个
        wrapper = new LambdaQueryWrapper<>();
        wrapper.last("LIMIT 1");
        return llmConfigMapper.selectOne(wrapper);
    }
}
