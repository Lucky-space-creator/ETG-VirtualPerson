package com.virtualwife.admin.module.llm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.virtualwife.admin.common.util.DjangoApiUtil;
import com.virtualwife.admin.integration.llm.LlmApiClient;
import com.virtualwife.admin.module.llm.entity.LlmConfig;
import com.virtualwife.admin.module.llm.mapper.LlmConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LlmConfigService extends ServiceImpl<LlmConfigMapper, LlmConfig> {

    private final DjangoApiUtil djangoApiUtil;
    private final LlmApiClient llmApiClient;

    public Page<LlmConfig> pageConfigs(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<LlmConfig> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(LlmConfig::getConfigName, keyword).or().like(LlmConfig::getProvider, keyword);
        }
        wrapper.orderByDesc(LlmConfig::getCreateTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @CacheEvict(value = "llmConfig", allEntries = true)
    @Transactional
    @Override
    public boolean save(LlmConfig entity) {
        return super.save(entity);
    }

    @CacheEvict(value = "llmConfig", allEntries = true)
    @Transactional
    @Override
    public boolean updateById(LlmConfig entity) {
        return super.updateById(entity);
    }

    @CacheEvict(value = "llmConfig", allEntries = true)
    @Transactional
    @Override
    public boolean removeById(java.io.Serializable id) {
        return super.removeById(id);
    }

    /**
     * 设为默认配置
     */
    @CacheEvict(value = "llmConfig", allEntries = true)
    @Transactional
    public void setDefault(Long id) {
        LambdaUpdateWrapper<LlmConfig> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(LlmConfig::getIsDefault, 0);
        this.update(updateWrapper);
        LlmConfig config = new LlmConfig();
        config.setId(id);
        config.setIsDefault(1);
        this.updateById(config);
    }

    @Cacheable(value = "llmConfig", key = "#id")
    @Override
    public LlmConfig getById(java.io.Serializable id) {
        return super.getById(id);
    }

    /**
     * 获取默认LLM配置（带缓存）
     */
    @Cacheable(value = "llmConfig", key = "'default'")
    public LlmConfig getDefaultConfig() {
        LlmConfig config = this.getOne(
                new LambdaQueryWrapper<LlmConfig>()
                        .eq(LlmConfig::getIsDefault, 1)
                        .last("LIMIT 1")
        );
        if (config == null) {
            config = this.getOne(
                    new LambdaQueryWrapper<LlmConfig>()
                            .orderByDesc(LlmConfig::getCreateTime)
                            .last("LIMIT 1")
            );
        }
        return config;
    }

    /**
     * 连通性测试
     */
    public Map<String, Object> testConnection(Long id) {
        LlmConfig config = this.getById(id);
        if (config == null) {
            throw new RuntimeException("配置不存在");
        }

        long start = System.currentTimeMillis();
        boolean success = llmApiClient.testConnection(config);
        long elapsed = System.currentTimeMillis() - start;

        config.setConnectStatus(success ? 1 : 2);
        this.updateById(config);

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("elapsed", elapsed);
        result.put("message", success ? "连接成功" : "连接失败");
        return result;
    }

    /**
     * 同步到Django
     */
    public boolean syncToDjango(Long id) {
        LlmConfig config = this.getById(id);
        if (config == null) return false;

        Map<String, Object> djangoConfig = new HashMap<>();
        djangoConfig.put("llm_provider", config.getProvider());
        djangoConfig.put("llm_api_url", config.getApiUrl());
        djangoConfig.put("llm_api_key", config.getApiKey());
        djangoConfig.put("llm_model_name", config.getModelName());
        djangoConfig.put("llm_temperature", config.getTemperature());
        djangoConfig.put("llm_max_tokens", config.getMaxTokens());

        return djangoApiUtil.saveConfig(djangoConfig);
    }
}
