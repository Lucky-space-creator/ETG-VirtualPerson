package com.virtualwife.admin.module.statistics.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.virtualwife.admin.module.chat.entity.ChatRecord;
import com.virtualwife.admin.module.chat.mapper.ChatRecordMapper;
import com.virtualwife.admin.module.statistics.entity.StatisticsDaily;
import com.virtualwife.admin.module.statistics.mapper.StatisticsDailyMapper;
import com.virtualwife.admin.module.user.entity.User;
import com.virtualwife.admin.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsScheduledTask {

    private final UserMapper userMapper;
    private final ChatRecordMapper chatRecordMapper;
    private final StatisticsDailyMapper statisticsDailyMapper;

    /**
     * 每日凌晨1点自动统计前一天数据
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void dailyStatistics() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("开始每日统计, date={}", yesterday);
        saveDailyStats(yesterday);
    }

    /**
     * 补全指定天数的历史数据（启动时调用）
     */
    public void backfillHistory(int days) {
        log.info("补全近{}天历史统计数据...", days);
        for (int i = days; i >= 1; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            // 跳过已存在的日期
            StatisticsDaily existing = statisticsDailyMapper.selectOne(
                    new LambdaQueryWrapper<StatisticsDaily>().eq(StatisticsDaily::getStatDate, date));
            if (existing == null) {
                saveDailyStats(date);
            }
        }
        log.info("历史数据补全完成");
    }

    /**
     * 手动触发指定日期的统计
     */
    public boolean triggerDailyStats(LocalDate date) {
        log.info("手动触发统计, date={}", date);
        try {
            saveDailyStats(date);
            return true;
        } catch (Exception e) {
            log.error("手动统计失败", e);
            return false;
        }
    }

    /**
     * 统计并保存指定日期的日报数据
     */
    private void saveDailyStats(LocalDate date) {
        try {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

            StatisticsDaily daily = new StatisticsDaily();
            daily.setStatDate(date);

            // 用户统计
            Long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .lt(User::getCreateTime, endOfDay));
            daily.setTotalUsers(totalUsers != null ? totalUsers.intValue() : 0);

            Long newUsers = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .ge(User::getCreateTime, startOfDay)
                    .lt(User::getCreateTime, endOfDay));
            daily.setNewUsers(newUsers != null ? newUsers.intValue() : 0);

            Long activeUsers = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .ge(User::getLastLoginTime, startOfDay)
                    .lt(User::getLastLoginTime, endOfDay));
            daily.setActiveUsers(activeUsers != null ? activeUsers.intValue() : 0);

            // 消息统计
            Long totalMessages = chatRecordMapper.selectCount(new LambdaQueryWrapper<ChatRecord>()
                    .lt(ChatRecord::getCreateTime, endOfDay));
            daily.setTotalMessages(totalMessages != null ? totalMessages.intValue() : 0);

            Long aiMessages = chatRecordMapper.selectCount(new LambdaQueryWrapper<ChatRecord>()
                    .eq(ChatRecord::getMessageType, "ai")
                    .lt(ChatRecord::getCreateTime, endOfDay));
            daily.setAiMessages(aiMessages != null ? aiMessages.intValue() : 0);

            Long userMessages = chatRecordMapper.selectCount(new LambdaQueryWrapper<ChatRecord>()
                    .eq(ChatRecord::getMessageType, "user")
                    .lt(ChatRecord::getCreateTime, endOfDay));
            daily.setUserMessages(userMessages != null ? userMessages.intValue() : 0);

            daily.setAvgSessionCount(BigDecimal.ZERO);
            daily.setTotalTokens(0L);

            // 存在则更新，不存在则插入
            StatisticsDaily existing = statisticsDailyMapper.selectOne(
                    new LambdaQueryWrapper<StatisticsDaily>().eq(StatisticsDaily::getStatDate, date));
            if (existing != null) {
                daily.setId(existing.getId());
                statisticsDailyMapper.updateById(daily);
            } else {
                statisticsDailyMapper.insert(daily);
            }

            log.info("每日统计完成, date={}", date);
        } catch (Exception e) {
            log.error("每日统计失败, date={}", date, e);
        }
    }
}
