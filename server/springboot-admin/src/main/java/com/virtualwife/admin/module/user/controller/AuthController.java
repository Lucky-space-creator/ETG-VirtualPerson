package com.virtualwife.admin.module.user.controller;

import com.virtualwife.admin.common.result.Result;
import com.virtualwife.admin.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginBody) {
        String username = loginBody.get("username");
        String password = loginBody.get("password");
        try {
            return Result.success("登录成功", userService.login(username, password));
        } catch (Exception e) {
            log.error("登录失败", e);
            return Result.error(401, e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh(@RequestBody Map<String, String> body) {
        try {
            String token = body.get("token");
            return Result.success(userService.refreshToken(token));
        } catch (Exception e) {
            log.error("刷新Token失败", e);
            return Result.error(401, e.getMessage());
        }
    }
}
