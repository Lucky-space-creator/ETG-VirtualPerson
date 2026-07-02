package com.virtualwife.admin.module.user.controller;

import com.virtualwife.admin.common.result.Result;
import com.virtualwife.admin.common.util.MinioUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户头像管理
 */
@Slf4j
@RestController
@RequestMapping("/user/avatar")
@RequiredArgsConstructor
public class UserAvatarController {

    private final MinioUtil minioUtil;

    /**
     * 上传用户头像
     */
    @PostMapping("/upload")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) throws Exception {
        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("请上传图片文件");
        }
        // 校验文件大小 (最大2MB)
        if (file.getSize() > 2 * 1024 * 1024) {
            return Result.error("图片大小不能超过2MB");
        }

        String objectPath = minioUtil.uploadFile(file, "user/avatar");
        String displayUrl = minioUtil.getPresignedUrl(objectPath);

        Map<String, String> data = new HashMap<>();
        data.put("path", objectPath);
        data.put("displayUrl", displayUrl);
        return Result.success("上传成功", data);
    }

    /**
     * 获取头像URL
     */
    @GetMapping("/url")
    public Result<Map<String, String>> getAvatarUrl(@RequestParam("path") String path) {
        String displayUrl = minioUtil.getPresignedUrl(path);
        Map<String, String> data = new HashMap<>();
        data.put("path", path);
        data.put("displayUrl", displayUrl);
        return Result.success(data);
    }
}
