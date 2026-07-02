package com.virtualwife.admin.common.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置 - 使用JVM内存缓存（不依赖Redis）
 * 生产环境可切换为RedisCacheManager
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "dashboard",
                "llmConfig",
                "avatarConfig",
                "route",
                "spot",
                "knowledge",
                "knowledgeItem",
                "ragDocument",
                "userInsight",
                "hotQuestions",
                "chatRecord"
        );
    }
}
