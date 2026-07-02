package com.virtualwife.admin.module.tourist.controller;

import com.virtualwife.admin.common.result.Result;
import com.virtualwife.admin.module.tourist.service.TouristConsumptionService;
import com.virtualwife.admin.module.tourist.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 游客消费数据Controller
 */
@Slf4j
@RestController
@RequestMapping("/tourist")
@RequiredArgsConstructor
public class TouristConsumptionController {

    private final TouristConsumptionService touristConsumptionService;
    private final UserProfileService userProfileService;

    /**
     * 导入消费数据Excel
     * POST /api/admin/tourist/import
     */
    @PostMapping("/import")
    public Result<Map<String, Object>> importData(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "scenicSpot", defaultValue = "default") String scenicSpot) {

        if (file == null || file.isEmpty()) {
            return Result.badRequest("请选择要导入的文件");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            return Result.badRequest("仅支持 .xlsx 或 .xls 格式的Excel文件");
        }

        Map<String, Object> result = touristConsumptionService.importFromExcel(file, scenicSpot);

        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success("导入成功", result);
        } else {
            return Result.error((String) result.get("message"));
        }
    }

    /**
     * 获取消费统计
     * GET /api/admin/tourist/stats
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(
            @RequestParam(value = "scenicSpot", defaultValue = "all") String scenicSpot) {
        return Result.success(touristConsumptionService.getConsumptionStats(scenicSpot));
    }

    /**
     * 获取消费记录列表
     * GET /api/admin/tourist/page
     */
    @GetMapping("/page")
    public Result<?> getPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String scenicSpot) {

        var query = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.virtualwife.admin.module.tourist.entity.TouristConsumption>();

        if (scenicSpot != null && !scenicSpot.isBlank() && !"all".equals(scenicSpot)) {
            query.eq(com.virtualwife.admin.module.tourist.entity.TouristConsumption::getScenicSpot, scenicSpot);
        }

        query.orderByDesc(com.virtualwife.admin.module.tourist.entity.TouristConsumption::getCreateTime);

        var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.virtualwife.admin.module.tourist.entity.TouristConsumption>(pageNum, pageSize);
        touristConsumptionService.page(page, query);

        return Result.success(page);
    }

    /**
     * 删除消费记录
     * DELETE /api/admin/tourist/{id}
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        touristConsumptionService.removeById(id);
        return Result.success("删除成功");
    }

    /**
     * 清空指定景区数据
     * DELETE /api/admin/tourist/clear/{scenicSpot}
     */
    @DeleteMapping("/clear/{scenicSpot}")
    public Result<?> clearByScenicSpot(@PathVariable String scenicSpot) {
        var query = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.virtualwife.admin.module.tourist.entity.TouristConsumption>();
        query.eq(com.virtualwife.admin.module.tourist.entity.TouristConsumption::getScenicSpot, scenicSpot);
        touristConsumptionService.remove(query);
        return Result.success("清空成功");
    }

    /**
     * 获取用户消费画像
     * GET /api/admin/tourist/profile
     */
    @GetMapping("/profile")
    public Result<Map<String, Object>> getUserProfile() {
        return Result.success(userProfileService.generateUserProfile());
    }
}
