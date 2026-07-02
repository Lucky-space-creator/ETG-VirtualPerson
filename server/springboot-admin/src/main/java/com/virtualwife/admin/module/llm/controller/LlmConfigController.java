package com.virtualwife.admin.module.llm.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.virtualwife.admin.common.result.Result;
import com.virtualwife.admin.module.llm.entity.LlmConfig;
import com.virtualwife.admin.module.llm.service.LlmConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/llm")
@RequiredArgsConstructor
public class LlmConfigController {

    private final LlmConfigService llmConfigService;

    @GetMapping("/page")
    public Result<Page<LlmConfig>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(llmConfigService.pageConfigs(pageNum, pageSize, keyword));
    }

    @GetMapping("/{id}")
    public Result<LlmConfig> getById(@PathVariable Long id) {
        return Result.success(llmConfigService.getById(id));
    }

    @PostMapping
    public Result<?> create(@RequestBody LlmConfig config) {
        llmConfigService.save(config);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody LlmConfig config) {
        config.setId(id);
        llmConfigService.updateById(config);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        llmConfigService.removeById(id);
        return Result.success();
    }

    @PutMapping("/{id}/default")
    public Result<?> setDefault(@PathVariable Long id) {
        llmConfigService.setDefault(id);
        return Result.success("已设为默认");
    }

    @PostMapping("/{id}/test")
    public Result<Map<String, Object>> testConnection(@PathVariable Long id) {
        return Result.success(llmConfigService.testConnection(id));
    }

    @PostMapping("/sync-django")
    public Result<?> syncDjango(@RequestBody Map<String, Long> body) {
        Long id = body.get("id");
        boolean success = llmConfigService.syncToDjango(id);
        return success ? Result.success("同步成功") : Result.error("同步失败");
    }
}
