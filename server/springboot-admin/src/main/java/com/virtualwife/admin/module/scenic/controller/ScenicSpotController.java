package com.virtualwife.admin.module.scenic.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.virtualwife.admin.common.result.Result;
import com.virtualwife.admin.module.scenic.entity.ScenicSpot;
import com.virtualwife.admin.module.scenic.service.ScenicSpotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/scenic")
@RequiredArgsConstructor
public class ScenicSpotController {

    private final ScenicSpotService scenicSpotService;

    /** 启用的景区列表（游客端） */
    @GetMapping("/list")
    public Result<List<ScenicSpot>> list() {
        return Result.success(scenicSpotService.listEnabled());
    }

    /** 分页查询（管理端） */
    @GetMapping("/page")
    public Result<Page<ScenicSpot>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<ScenicSpot> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(ScenicSpot::getSpotName, keyword);
        }
        wrapper.orderByAsc(ScenicSpot::getId);
        return Result.success(scenicSpotService.page(new Page<>(pageNum, pageSize), wrapper));
    }

    /** 新增景区 */
    @PostMapping
    public Result<Boolean> save(@RequestBody ScenicSpot spot) {
        return Result.success(scenicSpotService.save(spot));
    }

    /** 修改景区 */
    @PutMapping
    public Result<Boolean> update(@RequestBody ScenicSpot spot) {
        return Result.success(scenicSpotService.updateById(spot));
    }

    /** 删除景区 */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(scenicSpotService.removeById(id));
    }
}
