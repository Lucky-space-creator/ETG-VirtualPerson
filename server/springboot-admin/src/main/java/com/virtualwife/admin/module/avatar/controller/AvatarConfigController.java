package com.virtualwife.admin.module.avatar.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.virtualwife.admin.common.result.Result;
import com.virtualwife.admin.common.util.MinioUtil;
import com.virtualwife.admin.module.avatar.entity.AvatarConfig;
import com.virtualwife.admin.module.avatar.service.AvatarConfigService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/avatar")
@RequiredArgsConstructor
public class AvatarConfigController {

    private final AvatarConfigService avatarService;
    private final MinioUtil minioUtil;

    @GetMapping("/page")
    public Result<Page<AvatarConfig>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long scenicSpotId,
            @RequestParam(defaultValue = "false") boolean mobile) {
        Page<AvatarConfig> page = avatarService.pageAvatars(pageNum, pageSize, keyword, scenicSpotId);
        page.getRecords().forEach(a -> fillDisplayUrls(a, mobile));
        return Result.success(page);
    }

    /**
     * 获取默认形象（Android端调用）
     * 注意：此路径必须在 /{id} 之前，避免 "default" 被当作 id 参数
     */
    @GetMapping("/default")
    public Result<AvatarConfig> getDefault(
            @RequestParam(defaultValue = "false") boolean mobile) {
        AvatarConfig avatar = avatarService.getDefault();
        if (avatar == null) {
            return Result.error("未配置默认形象");
        }
        fillDisplayUrls(avatar, mobile);
        return Result.success(avatar);
    }

    @GetMapping("/{id}")
    public Result<AvatarConfig> getById(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean mobile) {
        AvatarConfig avatar = avatarService.getById(id);
        if (avatar == null) {
            return Result.error("形象不存在");
        }
        fillDisplayUrls(avatar, mobile);
        return Result.success(avatar);
    }

    @PostMapping
    public Result<?> create(HttpServletRequest httpRequest) {
        AvatarConfig avatar = parseAvatarFromRequest(httpRequest);
        avatarService.save(avatar);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, HttpServletRequest httpRequest) {
        AvatarConfig avatar = parseAvatarFromRequest(httpRequest);
        avatar.setId(id);
        avatarService.updateById(avatar);
        return Result.success();
    }

    /**
     * 从请求体解析AvatarConfig，处理UTF-8编码问题
     */
    private AvatarConfig parseAvatarFromRequest(HttpServletRequest httpRequest) {
        try {
            byte[] bytes = httpRequest.getInputStream().readAllBytes();
            String body = new String(bytes, StandardCharsets.UTF_8);
            if (body.contains("�") || body.contains("?")) {
                body = new String(bytes, Charset.forName("GBK"));
            }
            JSONObject json = JSONUtil.parseObj(body);

            AvatarConfig avatar = new AvatarConfig();
            avatar.setAvatarName(json.getStr("avatarName", ""));
            avatar.setPersona(json.getStr("persona", ""));
            avatar.setPersonality(json.getStr("personality", ""));
            avatar.setVoiceType(json.getStr("voiceType", ""));
            // emotionConfig是JSON字段，不能为空字符串
            String emotionConfig = json.getStr("emotionConfig", "");
            if (emotionConfig == null || emotionConfig.isBlank() || emotionConfig.equals("{}")) {
                emotionConfig = "{\"neutral\":\"neutral\"}";
            }
            avatar.setEmotionConfig(emotionConfig);
            avatar.setVrmModelUrl(json.getStr("vrmModelUrl", ""));
            avatar.setThumbnailUrl(json.getStr("thumbnailUrl", ""));
            avatar.setBackgroundUrl(json.getStr("backgroundUrl", ""));
            avatar.setSortOrder(json.getInt("sortOrder", 0));
            avatar.setIsSystem(json.getInt("isSystem", 0));
            avatar.setIsDefault(json.getInt("isDefault", 0));
            // 新增字段
            avatar.setClothesStyle(json.getStr("clothesStyle", "casual"));
            avatar.setClothesColor(json.getStr("clothesColor", "#4A90D9"));
            avatar.setHairStyle(json.getStr("hairStyle", "long"));
            avatar.setHairColor(json.getStr("hairColor", "#2C1810"));

            return avatar;
        } catch (Exception e) {
            log.error("解析AvatarConfig失败: {}", e.getMessage(), e);
            return new AvatarConfig();
        }
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        avatarService.removeById(id);
        return Result.success();
    }

    /**
     * 设为默认形象
     */
    @PutMapping("/{id}/default")
    public Result<?> setDefault(@PathVariable Long id) {
        avatarService.setDefault(id);
        return Result.success("已设为默认");
    }

    /**
     * 上传VRM模型 - 返回原始MinIO路径（不返回presigned URL）
     */
    @PostMapping("/upload/vrm")
    public Result<Map<String, String>> uploadVrm(@RequestParam("file") MultipartFile file) throws Exception {
        String objectPath = minioUtil.uploadFile(file, "avatar/vrm");
        Map<String, String> data = new HashMap<>();
        data.put("path", objectPath);
        data.put("displayUrl", minioUtil.getPresignedUrl(objectPath));
        return Result.success("上传成功", data);
    }

    /**
     * 上传缩略图 - 返回原始MinIO路径（不返回presigned URL）
     */
    @PostMapping("/upload/thumbnail")
    public Result<Map<String, String>> uploadThumbnail(@RequestParam("file") MultipartFile file) throws Exception {
        String objectPath = minioUtil.uploadFile(file, "avatar/thumbnail");
        Map<String, String> data = new HashMap<>();
        data.put("path", objectPath);
        data.put("displayUrl", minioUtil.getPresignedUrl(objectPath));
        return Result.success("上传成功", data);
    }

    /**
     * 上传背景图片 - 返回原始MinIO路径
     */
    @PostMapping("/upload/background")
    public Result<Map<String, String>> uploadBackground(@RequestParam("file") MultipartFile file) throws Exception {
        String objectPath = minioUtil.uploadFile(file, "avatar/background");
        Map<String, String> data = new HashMap<>();
        data.put("path", objectPath);
        data.put("displayUrl", minioUtil.getPresignedUrl(objectPath));
        return Result.success("上传成功", data);
    }

    private void fillDisplayUrls(AvatarConfig avatar, boolean mobile) {
        if (avatar == null) return;
        avatar.setThumbnailDisplayUrl(minioUtil.getPresignedUrl(avatar.getThumbnailUrl(), mobile));
        avatar.setVrmDisplayUrl(minioUtil.getPresignedUrl(avatar.getVrmModelUrl(), mobile));
        avatar.setBackgroundDisplayUrl(minioUtil.getPresignedUrl(avatar.getBackgroundUrl(), mobile));
    }

}
