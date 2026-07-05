package com.virtualwife.admin.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ==================== 完全公开（无需认证）====================
                        // 登录/注册
                        .requestMatchers("/auth/**").permitAll()
                        // 用户头像
                        .requestMatchers("/user/avatar/**").permitAll()
                        // 聊天API（Android/Web游客端）
                        .requestMatchers("/chat/send", "/chat/avatars", "/chat/health").permitAll()
                        // Android兼容路由（原Django路径）
                        .requestMatchers("/chatbot/**").permitAll()
                        // TTS语音合成
                        .requestMatchers("/speech/**").permitAll()
                        // 移动端API（保留兼容）
                        .requestMatchers("/mobile/**").permitAll()
                        // 游客端页面
                        .requestMatchers("/tourist", "/index-tourist.html", "/static/**").permitAll()
                        // Swagger/Knife4j
                        .requestMatchers("/doc.html", "/webjars/**", "/v3/api-docs/**",
                                "/swagger-resources/**", "/swagger-ui/**", "/favicon.ico").permitAll()

                        // ==================== GET读操作公开（Android游客端）====================
                        // 景区列表 - GET公开，写操作需认证
                        .requestMatchers(HttpMethod.GET, "/scenic/**").permitAll()
                        .requestMatchers("/scenic/**").authenticated()
                        // 数字人形象 - 只读
                        .requestMatchers(HttpMethod.GET, "/avatar/**").permitAll()
                        // 路线推荐 - 只读
                        .requestMatchers(HttpMethod.GET, "/route/**").permitAll()
                        // 知识库 - 只读
                        .requestMatchers(HttpMethod.GET, "/kb/**").permitAll()
                        // RAG文档 - 只读
                        .requestMatchers(HttpMethod.GET, "/rag/**").permitAll()
                        // LLM配置 - 只读
                        .requestMatchers(HttpMethod.GET, "/llm/**").permitAll()
                        // 统计数据 - 只读
                        .requestMatchers(HttpMethod.GET, "/statistics/**").permitAll()
                        // 用户分析报告 - 只读
                        .requestMatchers(HttpMethod.GET, "/report/**").permitAll()
                        // 聊天记录 - 只读
                        .requestMatchers(HttpMethod.GET, "/chat/**").permitAll()
                        // 用户管理 - 只读
                        .requestMatchers(HttpMethod.GET, "/user/**").permitAll()
                        // RBAC权限管理 - 只读
                        .requestMatchers(HttpMethod.GET, "/rbac/**").permitAll()
                        // 游客消费 - 只读
                        .requestMatchers(HttpMethod.GET, "/tourist/**").permitAll()

                        // ==================== OPTIONS预检 ====================
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ==================== 其他需要认证 ====================
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
