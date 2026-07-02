package com.virtualwife.admin.common.config;

import com.virtualwife.admin.module.statistics.scheduled.StatisticsScheduledTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 应用启动监听器
 * 启动时自动补全近7天的统计数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupListener {

    private final StatisticsScheduledTask statisticsScheduledTask;

    @EventListener(ApplicationReadyEvent.class)
    @Order(2)
    public void onStartup() {
        try {
            log.info("启动时补全统计数据...");
            statisticsScheduledTask.backfillHistory(7);
        } catch (Exception e) {
            log.warn("统计数据补全失败（不影响启动）: {}", e.getMessage());
        }
    }
}
