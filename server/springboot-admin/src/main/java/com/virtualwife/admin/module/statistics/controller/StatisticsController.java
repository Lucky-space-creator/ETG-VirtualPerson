package com.virtualwife.admin.module.statistics.controller;

import com.virtualwife.admin.common.result.Result;
import com.virtualwife.admin.module.statistics.scheduled.StatisticsScheduledTask;
import com.virtualwife.admin.module.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final StatisticsScheduledTask statisticsScheduledTask;

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

    /**
     * 手动触发每日统计（管理员使用）
     * POST /statistics/trigger?date=2024-01-15
     * 不传date则统计昨天
     */
    @PostMapping("/trigger")
    public Result<Map<String, Object>> trigger(@RequestParam(required = false) String date) {
        LocalDate targetDate = (date != null && !date.isBlank())
                ? LocalDate.parse(date)
                : LocalDate.now().minusDays(1);
        boolean success = statisticsScheduledTask.triggerDailyStats(targetDate);
        Map<String, Object> data = new HashMap<>();
        data.put("date", targetDate.toString());
        data.put("success", success);
        return success ? Result.success("统计完成", data) : Result.error("统计失败");
    }

    /**
     * 补全历史数据（管理员使用）
     * POST /statistics/backfill?days=30
     */
    @PostMapping("/backfill")
    public Result<Map<String, Object>> backfill(@RequestParam(defaultValue = "30") int days) {
        statisticsScheduledTask.backfillHistory(days);
        Map<String, Object> data = new HashMap<>();
        data.put("days", days);
        data.put("success", true);
        return Result.success("历史数据补全完成", data);
    }
}
