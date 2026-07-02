package com.virtualwife.admin.module.statistics.controller;

import com.virtualwife.admin.common.result.Result;
import com.virtualwife.admin.module.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.success(statisticsService.getDashboard());
    }

    @Cacheable(value = "dashboard", key = "'trend:' + #days")
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend(@RequestParam(defaultValue = "7") int days) {
        return Result.success(statisticsService.getTrend(days));
    }

    @GetMapping("/user-ranking")
    public Result<List<Map<String, Object>>> userRanking() {
        return Result.success(statisticsService.getUserRanking());
    }

    /**
     * 游客消费统计
     */
    @GetMapping("/consumption")
    public Result<Map<String, Object>> consumption() {
        return Result.success(statisticsService.getConsumptionStats());
    }

    /**
     * 实时数据
     */
    @GetMapping("/realtime")
    public Result<Map<String, Object>> realtime() {
        return Result.success(statisticsService.getRealtimeStats());
    }
}
