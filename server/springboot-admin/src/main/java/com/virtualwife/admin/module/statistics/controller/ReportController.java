package com.virtualwife.admin.module.statistics.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.virtualwife.admin.common.result.Result;
import com.virtualwife.admin.integration.llm.LlmApiClient;
import com.virtualwife.admin.module.chat.entity.ChatRecord;
import com.virtualwife.admin.module.chat.mapper.ChatRecordMapper;
import com.virtualwife.admin.module.llm.entity.LlmConfig;
import com.virtualwife.admin.module.llm.mapper.LlmConfigMapper;
import com.virtualwife.admin.module.statistics.entity.ReportCache;
import com.virtualwife.admin.module.statistics.mapper.ReportCacheMapper;
import com.virtualwife.admin.module.user.entity.User;
import com.virtualwife.admin.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ChatRecordMapper chatRecordMapper;
    private final UserMapper userMapper;
    private final LlmApiClient llmApiClient;
    private final LlmConfigMapper llmConfigMapper;
    private final ReportCacheMapper reportCacheMapper;

    private volatile boolean analyzing = false;

    /**
     * 情感分析：基于关键词快速分析用户消息情感倾向
     *
     * 评判标准：
     * - positive（积极）: 感谢、满意、开心、赞美
     * - negative（消极）: 不满、失望、抱怨
     * - neutral（中性）: 普通提问、陈述事实
     */
    @GetMapping("/sentiment")
    public Result<List<Map<String, Object>>> sentiment() {
        List<ChatRecord> records = chatRecordMapper.selectList(
                new LambdaQueryWrapper<ChatRecord>()
                        .eq(ChatRecord::getMessageType, "text")
                        .isNotNull(ChatRecord::getContent)
                        .orderByDesc(ChatRecord::getCreateTime)
                        .last("LIMIT 500"));

        if (records.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        Map<String, Long> sentimentCount = new HashMap<>();
        for (ChatRecord r : records) {
            String emotion = classifySentiment(r.getContent());
            sentimentCount.merge(emotion, 1L, Long::sum);
        }

        long total = sentimentCount.values().stream()
                .mapToLong(Long::longValue).sum();
        if (total == 0) {
            return Result.success(Collections.emptyList());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> e : sentimentCount.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("emotion", e.getKey());
            item.put("count", e.getValue());
            item.put("ratio", Math.round(
                    e.getValue() * 10000.0 / total) / 100.0);
            result.add(item);
        }
        result.sort((a, b) -> Long.compare(
                (Long) b.get("count"), (Long) a.get("count")));
        return Result.success(result);
    }

    /**
     * 情感趋势：按天统计情感变化
     */
    @GetMapping("/sentiment-trend")
    public Result<List<Map<String, Object>>> sentimentTrend(
            @RequestParam(defaultValue = "30") int days) {
        LocalDateTime since = LocalDate.now().minusDays(days).atStartOfDay();
        List<ChatRecord> records = chatRecordMapper.selectList(
                new LambdaQueryWrapper<ChatRecord>()
                        .eq(ChatRecord::getMessageType, "text")
                        .isNotNull(ChatRecord::getContent)
                        .ge(ChatRecord::getCreateTime, since)
                        .orderByAsc(ChatRecord::getCreateTime));

        if (records.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        Map<String, Map<String, Long>> dailyEmotions = new LinkedHashMap<>();
        for (ChatRecord r : records) {
            String date = r.getCreateTime().toLocalDate().toString();
            String emotion = classifySentiment(r.getContent());
            dailyEmotions
                    .computeIfAbsent(date, k -> new HashMap<>())
                    .merge(emotion, 1L, Long::sum);
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        for (Map.Entry<String, Map<String, Long>> entry :
                dailyEmotions.entrySet()) {
            Map<String, Object> point = new HashMap<>();
            point.put("date", entry.getKey());
            point.putAll(entry.getValue());
            trend.add(point);
        }
        return Result.success(trend);
    }

    /**
     * 基于关键词的快速情感分类
     */
    private String classifySentiment(String content) {
        if (content == null || content.isBlank()) return "neutral";
        String text = content.toLowerCase();

        String[] positiveWords = {
                "谢谢", "感谢", "好的", "太棒", "不错", "满意",
                "开心", "喜欢", "赞", "厉害", "漂亮", "完美",
                "很好", "真好", "好啊", "可以", "赞赞"
        };
        String[] negativeWords = {
                "不好", "失望", "差", "糟糕", "生气", "烦",
                "投诉", "退款", "骗", "垃圾", "太慢", "等很久",
                "无聊", "没意思", "不想", "算了"
        };

        int positiveScore = 0;
        int negativeScore = 0;
        for (String w : positiveWords) {
            if (text.contains(w)) positiveScore++;
        }
        for (String w : negativeWords) {
            if (text.contains(w)) negativeScore++;
        }

        if (positiveScore > negativeScore) return "positive";
        if (negativeScore > positiveScore) return "negative";
        return "neutral";
    }

    /**
     * 热门问题：统计高频用户消息
     */
    @GetMapping("/hot-questions")
    public Result<List<Map<String, Object>>> hotQuestions(
            @RequestParam(defaultValue = "10") int topN) {
        List<ChatRecord> records = chatRecordMapper.selectList(
                new LambdaQueryWrapper<ChatRecord>()
                        .eq(ChatRecord::getMessageType, "text")
                        .isNotNull(ChatRecord::getContent)
                        .orderByDesc(ChatRecord::getCreateTime)
                        .last("LIMIT 2000"));

        Map<String, Long> freq = new HashMap<>();
        for (ChatRecord r : records) {
            String content = r.getContent();
            if (content == null || content.isBlank()) continue;
            String trimmed = content.trim();
            if (trimmed.length() < 2) continue;
            freq.merge(trimmed, 1L, Long::sum);
        }

        List<Map<String, Object>> result = freq.entrySet().stream()
                .filter(e -> e.getValue() >= 2)
                .sorted((a, b) -> Long.compare(
                        b.getValue(), a.getValue()))
                .limit(topN)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("question", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 游客画像：返回基础统计 + 数据库中缓存的LLM分析结果
     */
    @GetMapping("/user-profile")
    public Result<Map<String, Object>> userProfile() {
        Map<String, Object> profile = new HashMap<>();

        long totalUsers = userMapper.selectCount(null);
        profile.put("totalUsers", totalUsers);

        long todayActive = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .ge(User::getLastLoginTime,
                                LocalDate.now().atStartOfDay()));
        profile.put("todayActive", todayActive);

        long totalMessages = chatRecordMapper.selectCount(
                new LambdaQueryWrapper<ChatRecord>()
                        .eq(ChatRecord::getMessageType, "text"));
        profile.put("totalMessages", totalMessages);

        List<ChatRecord> sessionRecords = chatRecordMapper.selectList(
                new LambdaQueryWrapper<ChatRecord>()
                        .select(ChatRecord::getSessionId)
                        .isNotNull(ChatRecord::getSessionId));
        long totalSessions = sessionRecords.stream()
                .map(ChatRecord::getSessionId)
                .distinct()
                .count();
        profile.put("totalSessions", totalSessions);

        double avgMessagesPerUser = totalUsers > 0
                ? Math.round(totalMessages * 100.0 / totalUsers) / 100.0
                : 0;
        profile.put("avgMessagesPerUser", avgMessagesPerUser);

        profile.put("topUsers", getTopActiveUsers(10));
        profile.put("avatarStats", getAvatarUsageStats());

        // 从数据库读取缓存的LLM分析结果
        Map<String, Object> llmInsight = loadCachedInsight("user_profile");
        profile.put("llmInsight", llmInsight);

        return Result.success(profile);
    }

    /**
     * 从数据库加载缓存的分析结果
     */
    private Map<String, Object> loadCachedInsight(String reportType) {
        ReportCache cache = reportCacheMapper.selectOne(
                new LambdaQueryWrapper<ReportCache>()
                        .eq(ReportCache::getReportType, reportType)
                        .last("LIMIT 1"));
        if (cache != null && cache.getContent() != null
                && !cache.getContent().isBlank()) {
            try {
                return JSONUtil.toBean(cache.getContent(),
                        new cn.hutool.json.JSONObject().getClass(),
                        false);
            } catch (Exception e) {
                log.warn("解析缓存报告失败: {}", e.getMessage());
            }
        }
        Map<String, Object> placeholder = new HashMap<>();
        placeholder.put("summary",
                "请点击\"生成报告\"触发LLM智能分析");
        return placeholder;
    }

    /**
     * 保存分析结果到数据库
     */
    private void saveCachedInsight(String reportType,
                                   Map<String, Object> data) {
        String json = JSONUtil.toJsonStr(data);
        ReportCache existing = reportCacheMapper.selectOne(
                new LambdaQueryWrapper<ReportCache>()
                        .eq(ReportCache::getReportType, reportType)
                        .last("LIMIT 1"));
        if (existing != null) {
            existing.setContent(json);
            reportCacheMapper.updateById(existing);
        } else {
            ReportCache cache = new ReportCache();
            cache.setReportType(reportType);
            cache.setContent(json);
            reportCacheMapper.insert(cache);
        }
    }

    /**
     * 消费分析
     */
    @GetMapping("/spending-analysis")
    public Result<Map<String, Object>> spendingAnalysis() {
        Map<String, Object> analysis = new HashMap<>();

        List<ChatRecord> records = chatRecordMapper.selectList(
                new LambdaQueryWrapper<ChatRecord>()
                        .eq(ChatRecord::getMessageType, "text")
                        .isNotNull(ChatRecord::getContent)
                        .orderByDesc(ChatRecord::getCreateTime)
                        .last("LIMIT 100"));

        if (records.isEmpty()) {
            analysis.put("summary", "暂无聊天数据");
            return Result.success(analysis);
        }

        String[] spendingKeywords = {
                "门票", "价格", "多少钱", "费用", "消费", "买",
                "订", "预订", "团购", "优惠", "打折", "免费",
                "收费", "划算", "便宜", "贵", "预算", "花钱",
                "吃饭", "餐", "住宿", "酒店", "停车", "交通"
        };

        List<String> spendingMessages = new ArrayList<>();
        for (ChatRecord r : records) {
            String content = r.getContent();
            if (content == null) continue;
            for (String keyword : spendingKeywords) {
                if (content.contains(keyword)) {
                    spendingMessages.add(content);
                    break;
                }
            }
        }

        analysis.put("spendingRelatedCount", spendingMessages.size());
        analysis.put("totalMessages", records.size());
        double ratio = records.isEmpty() ? 0
                : Math.round(spendingMessages.size() * 10000.0
                / records.size()) / 100.0;
        analysis.put("spendingRatio", ratio);

        if (!spendingMessages.isEmpty()) {
            Map<String, Object> basicAnalysis = new HashMap<>();
            basicAnalysis.put("summary",
                    "检测到 " + spendingMessages.size()
                            + " 条消费相关消息");
            analysis.put("llmAnalysis", basicAnalysis);
        }

        Map<String, Long> keywordFreq = new HashMap<>();
        for (String msg : spendingMessages) {
            for (String keyword : spendingKeywords) {
                if (msg.contains(keyword)) {
                    keywordFreq.merge(keyword, 1L, Long::sum);
                }
            }
        }
        List<Map<String, Object>> topKeywords = keywordFreq.entrySet()
                .stream()
                .sorted((a, b) -> Long.compare(
                        b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("keyword", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
        analysis.put("topKeywords", topKeywords);

        return Result.success(analysis);
    }

    /**
     * 生成报告：分析结果存入数据库
     */
    @PostMapping("/generate")
    public Result<?> generateReport() {
        if (analyzing) {
            return Result.success("报告正在生成中，请稍候...");
        }
        new Thread(() -> {
            analyzing = true;
            try {
                Map<String, Object> result = generateAnalysisReport();
                saveCachedInsight("user_profile", result);
                log.info("报告生成完成，已保存到数据库");
            } catch (Exception e) {
                log.error("报告生成失败", e);
            } finally {
                analyzing = false;
            }
        }).start();
        return Result.success("报告生成任务已触发，请稍候刷新查看");
    }

    /**
     * 生成分析报告：优先LLM，不可用时用关键词分析
     */
    private Map<String, Object> generateAnalysisReport() {
        LlmConfig llmConfig = getDefaultLlmConfig();

        // 快速测试LLM连通性（3秒超时）
        if (llmConfig != null && testLlmQuick(llmConfig)) {
            return analyzeUserProfileWithLLM();
        }

        // LLM不可用，使用关键词分析
        log.info("LLM不可用，使用关键词分析生成报告");
        return generateKeywordReport();
    }

    /**
     * 快速测试LLM连通性
     */
    private boolean testLlmQuick(LlmConfig config) {
        try {
            cn.hutool.http.HttpResponse resp =
                    cn.hutool.http.HttpRequest.post(
                                    config.getApiUrl())
                            .header("Content-Type", "application/json")
                            .header("Authorization",
                                    "Bearer " + config.getApiKey())
                            .body("{\"model\":\"" + config.getModelName()
                                    + "\",\"messages\":[{\"role\":\"user\","
                                    + "\"content\":\"ok\"}],\"max_tokens\":1}")
                            .timeout(3000)
                            .execute();
            return resp.isOk();
        } catch (Exception e) {
            log.warn("LLM连通测试失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 基于关键词的分析报告
     */
    private Map<String, Object> generateKeywordReport() {
        Map<String, Object> insight = new HashMap<>();

        List<ChatRecord> userMessages = chatRecordMapper.selectList(
                new LambdaQueryWrapper<ChatRecord>()
                        .eq(ChatRecord::getMessageType, "text")
                        .isNotNull(ChatRecord::getContent)
                        .orderByDesc(ChatRecord::getCreateTime)
                        .last("LIMIT 500"));

        if (userMessages.isEmpty()) {
            insight.put("summary", "暂无聊天数据");
            return insight;
        }

        // 路线关键词统计
        String[] routeKeywords = {
                "灵山", "大佛", "梵宫", "五印坛城", "九龙灌浴",
                "佛手", "祥符禅寺", "拈花湾", "亲子", "精华游"
        };
        Map<String, Long> routeFreq = new HashMap<>();
        for (ChatRecord r : userMessages) {
            String content = r.getContent();
            if (content == null) continue;
            for (String kw : routeKeywords) {
                if (content.contains(kw)) {
                    routeFreq.merge(kw, 1L, Long::sum);
                }
            }
        }
        String topRoute = routeFreq.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("暂无明确偏好");
        insight.put("routePreference",
                "游客最常提及: " + topRoute
                        + "，相关话题 " + routeFreq.values().stream()
                        .mapToLong(Long::longValue).sum() + " 次");

        // 兴趣特征
        String[] interestKeywords = {
                "拍照", "美食", "历史", "文化", "风景", "祈福",
                "亲子", "孩子", "老人", "门票", "停车"
        };
        Map<String, Long> interestFreq = new HashMap<>();
        for (ChatRecord r : userMessages) {
            String content = r.getContent();
            if (content == null) continue;
            for (String kw : interestKeywords) {
                if (content.contains(kw)) {
                    interestFreq.merge(kw, 1L, Long::sum);
                }
            }
        }
        String topInterests = interestFreq.entrySet().stream()
                .sorted((a, b) -> Long.compare(
                        b.getValue(), a.getValue()))
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining("、"));
        insight.put("interestFeatures",
                topInterests.isEmpty() ? "暂无明确特征"
                        : "游客关注: " + topInterests);

        // 游客类型
        long familyCount = userMessages.stream()
                .filter(r -> r.getContent() != null
                        && (r.getContent().contains("亲子")
                        || r.getContent().contains("孩子")
                        || r.getContent().contains("宝宝")))
                .count();
        long elderCount = userMessages.stream()
                .filter(r -> r.getContent() != null
                        && (r.getContent().contains("老人")
                        || r.getContent().contains("长辈")))
                .count();
        String touristType = "普通游客";
        if (familyCount > elderCount && familyCount > 2) {
            touristType = "亲子游客为主";
        } else if (elderCount > 2) {
            touristType = "家庭/老年游客为主";
        }
        insight.put("touristType", touristType);

        // 消费倾向
        long spendCount = userMessages.stream()
                .filter(r -> r.getContent() != null
                        && (r.getContent().contains("门票")
                        || r.getContent().contains("价格")
                        || r.getContent().contains("多少钱")))
                .count();
        String spendLevel = spendCount > 5 ? "价格敏感型"
                : spendCount > 2 ? "关注性价比"
                : "消费意愿正常";
        insight.put("spendingLevel", spendLevel);

        // 满意度
        long positive = userMessages.stream()
                .filter(r -> "positive".equals(
                        classifySentiment(r.getContent())))
                .count();
        long negative = userMessages.stream()
                .filter(r -> "negative".equals(
                        classifySentiment(r.getContent())))
                .count();
        String satisfaction = positive > negative ? "整体满意"
                : negative > positive ? "有待提升"
                : "评价中性";
        insight.put("satisfaction", satisfaction
                + "（积极" + positive + "/ 消极" + negative + "）");

        insight.put("summary",
                "基于 " + userMessages.size() + " 条聊天记录的关键词分析。"
                        + "路线偏好: " + topRoute + "；"
                        + "兴趣: " + (topInterests.isEmpty() ? "无明确特征" : topInterests) + "；"
                        + "满意度: " + satisfaction);
        insight.put("dataCount", userMessages.size());
        insight.put("analyzedAt", LocalDateTime.now().toString());
        insight.put("analysisMethod", "关键词分析（LLM不可用）");

        return insight;
    }

    /**
     * 使用LLM分析游客画像
     */
    private Map<String, Object> analyzeUserProfileWithLLM() {
        Map<String, Object> insight = new HashMap<>();
        LlmConfig llmConfig = getDefaultLlmConfig();

        if (llmConfig == null) {
            insight.put("summary", "未配置LLM，无法进行智能分析");
            return insight;
        }

        List<ChatRecord> userMessages = chatRecordMapper.selectList(
                new LambdaQueryWrapper<ChatRecord>()
                        .eq(ChatRecord::getMessageType, "text")
                        .isNotNull(ChatRecord::getContent)
                        .orderByDesc(ChatRecord::getCreateTime)
                        .last("LIMIT 30"));

        if (userMessages.isEmpty()) {
            insight.put("summary", "暂无足够的聊天数据进行分析");
            return insight;
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("分析以下景区游客聊天记录，生成用户画像。\n\n");
        prompt.append("分析维度：路线偏好、兴趣特征、消费倾向、");
        prompt.append("游客类型、服务满意度\n\n");
        prompt.append("聊天记录：\n");

        for (int i = 0; i < userMessages.size(); i++) {
            ChatRecord r = userMessages.get(i);
            String content = r.getContent();
            if (content != null && content.length() > 100) {
                content = content.substring(0, 100) + "...";
            }
            prompt.append(i + 1).append(". ")
                    .append(content).append("\n");
        }

        prompt.append("\n请返回JSON格式：\n");
        prompt.append("{\n");
        prompt.append("  \"routePreference\": \"路线偏好分析\",\n");
        prompt.append("  \"interestFeatures\": \"兴趣特征分析\",\n");
        prompt.append("  \"spendingLevel\": \"消费倾向分析\",\n");
        prompt.append("  \"touristType\": \"主要游客类型\",\n");
        prompt.append("  \"satisfaction\": \"服务满意度分析\",\n");
        prompt.append("  \"summary\": \"综合总结\"\n");
        prompt.append("}");

        try {
            String reply = llmApiClient.chat(
                    llmConfig,
                    "你是专业的旅游数据分析助手。请基于聊天记录分析游客画像，"
                            + "返回JSON格式。不要返回其他内容。",
                    null,
                    prompt.toString());

            insight = parseInsightJSON(reply);
            if (insight.isEmpty()) {
                insight.put("rawAnalysis", reply);
            }
            insight.put("dataCount", userMessages.size());
            insight.put("analyzedAt",
                    LocalDateTime.now().toString());
        } catch (Exception e) {
            log.error("LLM游客画像分析失败: {}", e.getMessage());
            insight.put("error", "分析失败: " + e.getMessage());
        }

        return insight;
    }

    /**
     * 解析LLM返回的分析JSON
     */
    private Map<String, Object> parseInsightJSON(String json) {
        Map<String, Object> result = new HashMap<>();
        try {
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start >= 0 && end > start) {
                String jsonStr = json.substring(start, end + 1);
                cn.hutool.json.JSONObject obj =
                        JSONUtil.parseObj(jsonStr);
                for (String key : obj.keySet()) {
                    result.put(key, obj.getStr(key));
                }
            }
        } catch (Exception e) {
            log.warn("解析分析JSON失败: {}", e.getMessage());
        }
        return result;
    }

    private List<Map<String, Object>> getTopActiveUsers(int topN) {
        List<ChatRecord> records = chatRecordMapper.selectList(
                new LambdaQueryWrapper<ChatRecord>()
                        .select(ChatRecord::getUserId)
                        .eq(ChatRecord::getMessageType, "text"));

        Map<Long, Long> userMsgCount = new HashMap<>();
        for (ChatRecord r : records) {
            if (r.getUserId() != null && r.getUserId() > 0) {
                userMsgCount.merge(r.getUserId(), 1L, Long::sum);
            }
        }

        return userMsgCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(
                        b.getValue(), a.getValue()))
                .limit(topN)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    User user = userMapper.selectById(e.getKey());
                    m.put("userId", e.getKey());
                    m.put("username", user != null
                            ? user.getUsername() : "未知");
                    m.put("messageCount", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getAvatarUsageStats() {
        List<ChatRecord> records = chatRecordMapper.selectList(
                new LambdaQueryWrapper<ChatRecord>()
                        .select(ChatRecord::getAvatarName)
                        .eq(ChatRecord::getMessageType, "ai_reply")
                        .isNotNull(ChatRecord::getAvatarName));

        Map<String, Long> avatarCount = new HashMap<>();
        for (ChatRecord r : records) {
            avatarCount.merge(r.getAvatarName(), 1L, Long::sum);
        }

        return avatarCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(
                        b.getValue(), a.getValue()))
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("avatarName", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }

    private LlmConfig getDefaultLlmConfig() {
        LlmConfig config = llmConfigMapper.selectOne(
                new LambdaQueryWrapper<LlmConfig>()
                        .eq(LlmConfig::getIsDefault, 1)
                        .last("LIMIT 1"));
        if (config == null) {
            config = llmConfigMapper.selectOne(
                    new LambdaQueryWrapper<LlmConfig>()
                            .last("LIMIT 1"));
        }
        return config;
    }

    /**
     * 综合报告数据（Vue前端调用）
     */
    @GetMapping("/data")
    public Result<?> reportData() {
        Map<String, Object> data = new HashMap<>();
        try {
            data.put("sentiment", sentiment().getData());
        } catch (Exception e) {
            log.error("获取情感数据失败", e);
            data.put("sentiment", Collections.emptyList());
        }
        try {
            data.put("sentimentTrend", sentimentTrend(30).getData());
        } catch (Exception e) {
            log.error("获取情感趋势失败", e);
            data.put("sentimentTrend", Collections.emptyList());
        }
        try {
            data.put("hotQuestions", hotQuestions(10).getData());
        } catch (Exception e) {
            log.error("获取热门问题失败", e);
            data.put("hotQuestions", Collections.emptyList());
        }
        try {
            data.put("userProfile", userProfile().getData());
        } catch (Exception e) {
            log.error("获取用户画像失败", e);
            data.put("userProfile", Collections.emptyMap());
        }
        try {
            data.put("spendingAnalysis",
                    spendingAnalysis().getData());
        } catch (Exception e) {
            log.error("获取消费分析失败", e);
            data.put("spendingAnalysis", Collections.emptyMap());
        }
        return Result.success(data);
    }
}
