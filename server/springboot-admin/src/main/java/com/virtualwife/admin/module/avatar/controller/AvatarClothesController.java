package com.virtualwife.admin.module.avatar.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.virtualwife.admin.common.result.Result;
import com.virtualwife.admin.common.util.MinioUtil;
import com.virtualwife.admin.module.avatar.entity.AvatarClothes;
import com.virtualwife.admin.module.avatar.service.AvatarClothesService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数字人衣服管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/avatar/clothes")
@RequiredArgsConstructor
public class AvatarClothesController {

    private final AvatarClothesService clothesService;
    private final MinioUtil minioUtil;

    /**
     * 获取指定数字人的衣服列表
     */
    @GetMapping("/list/{avatarId}")
    public Result<List<AvatarClothes>> getClothes(@PathVariable Long avatarId) {
        return Result.success(clothesService.getClothesByAvatarId(avatarId));
    }

    /**
     * 获取默认衣服
     */
    @GetMapping("/default/{avatarId}")
    public Result<AvatarClothes> getDefaultClothes(@PathVariable Long avatarId) {
        AvatarClothes clothes = clothesService.getDefaultClothes(avatarId);
        if (clothes == null) {
            return Result.error("未配置衣服");
        }
        return Result.success(clothes);
    }

    /**
     * 获取衣服详情
     */
    @GetMapping("/{id}")
    public Result<AvatarClothes> getClothesById(@PathVariable Long id) {
        AvatarClothes clothes = clothesService.getById(id);
        if (clothes == null) {
            return Result.error("衣服不存在");
        }
        return Result.success(clothes);
    }

    /**
     * 新增衣服
     */
    @PostMapping
    public Result<?> create(HttpServletRequest httpRequest) {
        AvatarClothes clothes = parseClothesFromRequest(httpRequest);
        clothesService.save(clothes);
        return Result.success("新增成功");
    }

    /**
     * 更新衣服
     */
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, HttpServletRequest httpRequest) {
        AvatarClothes clothes = parseClothesFromRequest(httpRequest);
        clothes.setId(id);
        clothesService.updateById(clothes);
        return Result.success("更新成功");
    }

    /**
     * 删除衣服
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        clothesService.removeById(id);
        return Result.success("删除成功");
    }

    /**
     * 设置默认衣服
     */
    @PutMapping("/{id}/default")
    public Result<?> setDefault(@PathVariable Long id) {
        clothesService.setDefault(id);
        return Result.success("设置成功");
    }

    /**
     * 上传VRM模型
     */
    @PostMapping("/upload/vrm")
    public Result<Map<String, String>> uploadVrm(@RequestParam("file") MultipartFile file) throws Exception {
        String objectPath = minioUtil.uploadFile(file, "avatar/clothes/vrm");
        Map<String, String> data = new HashMap<>();
        data.put("path", objectPath);
        data.put("displayUrl", minioUtil.getPresignedUrl(objectPath));
        return Result.success("上传成功", data);
    }

    /**
     * 上传缩略图
     */
    @PostMapping("/upload/thumbnail")
    public Result<Map<String, String>> uploadThumbnail(@RequestParam("file") MultipartFile file) throws Exception {
        String objectPath = minioUtil.uploadFile(file, "avatar/clothes/thumbnail");
        Map<String, String> data = new HashMap<>();
        data.put("path", objectPath);
        data.put("displayUrl", minioUtil.getPresignedUrl(objectPath));
        return Result.success("上传成功", data);
    }

    /**
     * 从请求体解析AvatarClothes
     */
    private AvatarClothes parseClothesFromRequest(HttpServletRequest httpRequest) {
        try {
            byte[] bytes = httpRequest.getInputStream().readAllBytes();
            String body = new String(bytes, StandardCharsets.UTF_8);
            if (body.contains("�") || body.contains("?")) {
                body = new String(bytes, Charset.forName("GBK"));
            }
            JSONObject json = JSONUtil.parseObj(body);

            AvatarClothes clothes = new AvatarClothes();
            clothes.setAvatarId(json.getLong("avatarId"));
            clothes.setClothesName(json.getStr("clothesName", ""));
            clothes.setVrmModelUrl(json.getStr("vrmModelUrl", ""));
            clothes.setThumbnailUrl(json.getStr("thumbnailUrl", ""));
            clothes.setDescription(json.getStr("description", ""));
            clothes.setIsDefault(json.getInt("isDefault", 0));
            clothes.setSortOrder(json.getInt("sortOrder", 0));

            return clothes;
        } catch (Exception e) {
            log.error("解析AvatarClothes失败: {}", e.getMessage(), e);
            return new AvatarClothes();
        }
    }
}
