package com.virtualwife.admin.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.virtualwife.admin.common.util.JwtUtil;
import com.virtualwife.admin.module.user.entity.User;
import com.virtualwife.admin.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 管理员登录
     */
    public Map<String, Object> login(String username, String password) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getStatus, 1));

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        this.updateById(user);

        // 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        return result;
    }

    /**
     * 刷新Token
     */
    public Map<String, Object> refreshToken(String token) {
        Long userId = jwtUtil.getUserId(token);
        User user = this.getById(userId);
        if (user == null || user.getStatus() == 0) {
            throw new RuntimeException("用户不存在或已禁用");
        }

        String newToken = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", newToken);
        result.put("user", user);
        return result;
    }

    /**
     * 分页查询用户
     */
    public Page<User> pageUsers(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(User::getUsername, keyword).or().like(User::getNickname, keyword);
        }
        wrapper.orderByDesc(User::getCreateTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 切换用户状态
     */
    public void toggleStatus(Long id) {
        User user = this.getById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setStatus(user.getStatus() == 1 ? 0 : 1);
        this.updateById(user);
    }
}
