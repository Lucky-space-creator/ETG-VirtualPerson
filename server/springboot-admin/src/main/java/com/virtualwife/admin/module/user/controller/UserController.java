package com.virtualwife.admin.module.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.virtualwife.admin.common.result.Result;
import com.virtualwife.admin.module.user.entity.User;
import com.virtualwife.admin.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/page")
    public Result<Page<User>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(userService.pageUsers(pageNum, pageSize, keyword));
    }

    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }

    @PutMapping("/{id}/status")
    public Result<?> toggleStatus(@PathVariable Long id) {
        userService.toggleStatus(id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success();
    }
}
