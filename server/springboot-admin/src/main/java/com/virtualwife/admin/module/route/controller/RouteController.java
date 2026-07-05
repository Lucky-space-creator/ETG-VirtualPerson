package com.virtualwife.admin.module.route.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.virtualwife.admin.common.result.Result;
import com.virtualwife.admin.common.util.MinioUtil;
import com.virtualwife.admin.module.route.entity.Route;
import com.virtualwife.admin.module.route.entity.Spot;
import com.virtualwife.admin.module.route.service.RouteService;
import com.virtualwife.admin.module.route.service.SpotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/route")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;
    private final SpotService spotService;
    private final MinioUtil minioUtil;

    @GetMapping("/page")
    public Result<Page<Route>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long scenicSpotId) {
        return Result.success(routeService.pageRoutes(pageNum, pageSize, keyword, scenicSpotId));
    }

    @PostMapping
    public Result<?> create(@RequestBody Route route) {
        routeService.save(route);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Route route) {
        route.setId(id);
        routeService.updateById(route);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        routeService.removeById(id);
        return Result.success();
    }

    @GetMapping("/{routeId}/spot/page")
    public Result<Page<Spot>> pageSpots(
            @PathVariable Long routeId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(spotService.pageSpots(routeId, pageNum, pageSize));
    }

    @PostMapping("/{routeId}/spot")
    public Result<?> createSpot(@PathVariable Long routeId, @RequestBody Spot spot) {
        spot.setRouteId(routeId);
        spotService.save(spot);
        return Result.success();
    }

    @PutMapping("/{routeId}/spot/{id}")
    public Result<?> updateSpot(@PathVariable Long routeId, @PathVariable Long id, @RequestBody Spot spot) {
        spot.setId(id);
        spot.setRouteId(routeId);
        spotService.updateById(spot);
        return Result.success();
    }

    @DeleteMapping("/{routeId}/spot/{id}")
    public Result<?> deleteSpot(@PathVariable Long routeId, @PathVariable Long id) {
        spotService.removeById(id);
        return Result.success();
    }

    /**
     * 上传景点图片
     */
    @PostMapping("/spot/upload-image")
    public Result<Map<String, String>> uploadSpotImage(@RequestParam("file") MultipartFile file) throws Exception {
        String objectPath = minioUtil.uploadFile(file, "route/spot");
        Map<String, String> data = new HashMap<>();
        data.put("path", objectPath);
        data.put("displayUrl", minioUtil.getPresignedUrl(objectPath));
        return Result.success("上传成功", data);
    }
}
