package com.virtualwife.admin.common.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.virtualwife.admin.common.util.MinioUtil;
import com.virtualwife.admin.module.llm.entity.LlmConfig;
import com.virtualwife.admin.module.llm.mapper.LlmConfigMapper;
import com.virtualwife.admin.module.user.entity.User;
import com.virtualwife.admin.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 系统数据初始化
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final LlmConfigMapper llmConfigMapper;
    private final PasswordEncoder passwordEncoder;
    private final MinioUtil minioUtil;

    @Override
    public void run(String... args) {
        initAdminUser();
        initDefaultLlmConfig();
        initMinioBucket();
    }

    private void initAdminUser() {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "admin"));
        if (count == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setNickname("系统管理员");
            admin.setRole("ADMIN");
            admin.setStatus(1);
            userMapper.insert(admin);
            log.info("默认管理员账号已创建: admin / admin123");
        }
    }

    private void initDefaultLlmConfig() {
        Long count = llmConfigMapper.selectCount(null);
        if (count == 0) {
            LlmConfig openai = new LlmConfig();
            openai.setConfigName("默认OpenAI配置");
            openai.setProvider("OpenAI");
            openai.setApiUrl("https://api.openai.com/v1");
            openai.setModelName("gpt-3.5-turbo");
            openai.setTemperature(new BigDecimal("0.70"));
            openai.setMaxTokens(2048);
            openai.setIsDefault(1);
            openai.setConnectStatus(0);
            llmConfigMapper.insert(openai);

            LlmConfig ollama = new LlmConfig();
            ollama.setConfigName("默认Ollama配置");
            ollama.setProvider("Ollama");
            ollama.setApiUrl("http://localhost:11434");
            ollama.setModelName("qwen:7b");
            ollama.setTemperature(new BigDecimal("0.70"));
            ollama.setMaxTokens(2048);
            ollama.setIsDefault(0);
            ollama.setConnectStatus(0);
            llmConfigMapper.insert(ollama);

            log.info("默认LLM配置已创建");
        }
    }

    private void initMinioBucket() {
        try {
            minioUtil.setBucketPublicRead();
        } catch (Exception e) {
            log.warn("MinIO bucket公开策略设置失败: {}", e.getMessage());
        }
    }
}
