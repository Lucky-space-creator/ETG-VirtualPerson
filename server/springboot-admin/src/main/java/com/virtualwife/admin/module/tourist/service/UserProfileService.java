package com.virtualwife.admin.module.tourist.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.virtualwife.admin.module.chat.entity.ChatRecord;
import com.virtualwife.admin.module.chat.mapper.ChatRecordMapper;
import com.virtualwife.admin.module.tourist.entity.TouristConsumption;
import com.virtualwife.admin.module.tourist.mapper.TouristConsumptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户画像服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final TouristConsumptionMapper touristConsumptionMapper;
    private final ChatRecordMapper chatRecordMapper;

    /**
     * 生成用户消费画像
     */
    public Map<String, Object> generateUserProfile() {
        Map<String, Object> profile = new HashMap<>();

        // 获取所有消费记录
        var query = new LambdaQueryWrapper<TouristConsumption>().last("LIMIT 10000");
        List<TouristConsumption> records = touristConsumptionMapper.selectList(query);

        if (records.isEmpty()) {
            profile.put("status", "no_data");
            profile.put("message", "暂无消费数据");
            return profile;
        }

        // 1. 消费水平分析
        profile.put("consumptionLevel", analyzeConsumptionLevel(records));

        // 2. 消费偏好分析
        profile.put("consumptionPreference", analyzeConsumptionPreference(records));

        // 3. 游客类型分析
        profile.put("touristTypes", analyzeTouristTypes(records));

        // 4. 满意度分析
        profile.put("satisfactionAnalysis", analyzeSatisfaction(records));

        // 5. 时间偏好分析
        profile.put("timePreference", analyzeTimePreference(records));

        // 6. 生成个性化建议
        profile.put("recommendations", generateRecommendations(profile));

        return profile;
    }

    /**
     * 分析消费水平
     */
    private Map<String, Object> analyzeConsumptionLevel(List<TouristConsumption> records) {
        Map<String, Object> result = new HashMap<>();

        BigDecimal avgCost = records.stream()
                .map(TouristConsumption::getTotalCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(records.size()), 2, BigDecimal.ROUND_HALF_UP);

        long highCost = records.stream()
                .filter(r -> r.getTotalCost() != null && r.getTotalCost().compareTo(BigDecimal.valueOf(1000)) >= 0)
                .count();
        long midCost = records.stream()
                .filter(r -> r.getTotalCost() != null &&
                        r.getTotalCost().compareTo(BigDecimal.valueOf(500)) >= 0 &&
                        r.getTotalCost().compareTo(BigDecimal.valueOf(1000)) < 0)
                .count();
        long lowCost = records.stream()
                .filter(r -> r.getTotalCost() != null && r.getTotalCost().compareTo(BigDecimal.valueOf(500)) < 0)
                .count();

        result.put("averageCost", avgCost.intValue());
        result.put("highCostPercentage", (int)(highCost * 100 / records.size()));
        result.put("midCostPercentage", (int)(midCost * 100 / records.size()));
        result.put("lowCostPercentage", (int)(lowCost * 100 / records.size()));

        String level;
        if (avgCost.compareTo(BigDecimal.valueOf(1000)) >= 0) {
            level = "高消费";
        } else if (avgCost.compareTo(BigDecimal.valueOf(500)) >= 0) {
            level = "中等消费";
        } else {
            level = "经济型";
        }
        result.put("level", level);

        return result;
    }

    /**
     * 分析消费偏好
     */
    private Map<String, Object> analyzeConsumptionPreference(List<TouristConsumption> records) {
        Map<String, Object> result = new HashMap<>();

        BigDecimal totalTicket = records.stream().map(TouristConsumption::getTicketCost).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFood = records.stream().map(TouristConsumption::getFoodCost).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalShopping = records.stream().map(TouristConsumption::getShoppingCost).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTransport = records.stream().map(TouristConsumption::getTransportCost).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEntertainment = records.stream().map(TouristConsumption::getEntertainmentCost).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = totalTicket.add(totalFood).add(totalShopping).add(totalTransport).add(totalEntertainment);

        if (total.compareTo(BigDecimal.ZERO) > 0) {
            result.put("ticketRatio", totalTicket.multiply(BigDecimal.valueOf(100)).divide(total, 1, BigDecimal.ROUND_HALF_UP));
            result.put("foodRatio", totalFood.multiply(BigDecimal.valueOf(100)).divide(total, 1, BigDecimal.ROUND_HALF_UP));
            result.put("shoppingRatio", totalShopping.multiply(BigDecimal.valueOf(100)).divide(total, 1, BigDecimal.ROUND_HALF_UP));
            result.put("transportRatio", totalTransport.multiply(BigDecimal.valueOf(100)).divide(total, 1, BigDecimal.ROUND_HALF_UP));
            result.put("entertainmentRatio", totalEntertainment.multiply(BigDecimal.valueOf(100)).divide(total, 1, BigDecimal.ROUND_HALF_UP));
        }

        // 找出最大消费类别
        Map<String, BigDecimal> ratios = new LinkedHashMap<>();
        ratios.put("门票", totalTicket);
        ratios.put("餐饮", totalFood);
        ratios.put("购物", totalShopping);
        ratios.put("交通", totalTransport);
        ratios.put("娱乐", totalEntertainment);

        String maxCategory = ratios.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("未知");
        result.put("primaryPreference", maxCategory);

        return result;
    }

    /**
     * 分析游客类型
     */
    private Map<String, Object> analyzeTouristTypes(List<TouristConsumption> records) {
        Map<String, Object> result = new HashMap<>();

        long familyGroup = records.stream().filter(r -> r.getGroupSize() != null && r.getGroupSize() >= 3).count();
        long coupleGroup = records.stream().filter(r -> r.getGroupSize() != null && r.getGroupSize() == 2).count();
        long soloTraveler = records.stream().filter(r -> r.getGroupSize() != null && r.getGroupSize() == 1).count();

        result.put("familyPercentage", (int)(familyGroup * 100 / records.size()));
        result.put("couplePercentage", (int)(coupleGroup * 100 / records.size()));
        result.put("soloPercentage", (int)(soloTraveler * 100 / records.size()));

        // 年龄分布
        long young = records.stream().filter(r -> r.getAge() != null && r.getAge() < 30).count();
        long middle = records.stream().filter(r -> r.getAge() != null && r.getAge() >= 30 && r.getAge() < 50).count();
        long senior = records.stream().filter(r -> r.getAge() != null && r.getAge() >= 50).count();

        result.put("youngPercentage", (int)(young * 100 / records.size()));
        result.put("middlePercentage", (int)(middle * 100 / records.size()));
        result.put("seniorPercentage", (int)(senior * 100 / records.size()));

        return result;
    }

    /**
     * 分析满意度
     */
    private Map<String, Object> analyzeSatisfaction(List<TouristConsumption> records) {
        Map<String, Object> result = new HashMap<>();

        double avgSatisfaction = records.stream()
                .filter(r -> r.getSatisfaction() != null)
                .mapToInt(TouristConsumption::getSatisfaction)
                .average().orElse(0);

        long verySatisfied = records.stream().filter(r -> r.getSatisfaction() != null && r.getSatisfaction() >= 5).count();
        long satisfied = records.stream().filter(r -> r.getSatisfaction() != null && r.getSatisfaction() == 4).count();
        long neutral = records.stream().filter(r -> r.getSatisfaction() != null && r.getSatisfaction() == 3).count();
        long unsatisfied = records.stream().filter(r -> r.getSatisfaction() != null && r.getSatisfaction() <= 2).count();

        result.put("averageSatisfaction", Math.round(avgSatisfaction * 10.0) / 10.0);
        result.put("verySatisfiedPercentage", (int)(verySatisfied * 100 / records.size()));
        result.put("satisfiedPercentage", (int)(satisfied * 100 / records.size()));
        result.put("neutralPercentage", (int)(neutral * 100 / records.size()));
        result.put("unsatisfiedPercentage", (int)(unsatisfied * 100 / records.size()));

        return result;
    }

    /**
     * 分析时间偏好
     */
    private Map<String, Object> analyzeTimePreference(List<TouristConsumption> records) {
        Map<String, Object> result = new HashMap<>();

        double avgStay = records.stream()
                .filter(r -> r.getStayDuration() != null)
                .mapToDouble(r -> r.getStayDuration().doubleValue())
                .average().orElse(0);

        result.put("averageStayHours", Math.round(avgStay * 10.0) / 10.0);

        // 停留时间分布
        long shortStay = records.stream().filter(r -> r.getStayDuration() != null && r.getStayDuration().doubleValue() < 2).count();
        long mediumStay = records.stream().filter(r -> r.getStayDuration() != null && r.getStayDuration().doubleValue() >= 2 && r.getStayDuration().doubleValue() < 5).count();
        long longStay = records.stream().filter(r -> r.getStayDuration() != null && r.getStayDuration().doubleValue() >= 5).count();

        result.put("shortStayPercentage", (int)(shortStay * 100 / records.size()));
        result.put("mediumStayPercentage", (int)(mediumStay * 100 / records.size()));
        result.put("longStayPercentage", (int)(longStay * 100 / records.size()));

        return result;
    }

    /**
     * 生成个性化建议
     */
    private List<String> generateRecommendations(Map<String, Object> profile) {
        List<String> recommendations = new ArrayList<>();

        Map<String, Object> consumptionLevel = (Map<String, Object>) profile.get("consumptionLevel");
        Map<String, Object> consumptionPreference = (Map<String, Object>) profile.get("consumptionPreference");
        Map<String, Object> touristTypes = (Map<String, Object>) profile.get("touristTypes");

        if (consumptionLevel != null) {
            String level = (String) consumptionLevel.get("level");
            if ("高消费".equals(level)) {
                recommendations.add("推出高端定制旅游产品，如VIP导览、私人订制路线");
                recommendations.add("增加高端餐饮和精品购物选择");
            } else if ("经济型".equals(level)) {
                recommendations.add("提供更多优惠套餐和折扣活动");
                recommendations.add("增加性价比高的餐饮选择");
            }
        }

        if (consumptionPreference != null) {
            String primary = (String) consumptionPreference.get("primaryPreference");
            if ("餐饮".equals(primary)) {
                recommendations.add("增加特色餐饮种类，引入地方美食");
                recommendations.add("设置美食体验活动");
            } else if ("购物".equals(primary)) {
                recommendations.add("开发特色文创产品");
                recommendations.add("增加纪念品商店");
            }
        }

        if (touristTypes != null) {
            Long familyPercentage = (Long) touristTypes.get("familyPercentage");
            if (familyPercentage != null && familyPercentage > 30) {
                recommendations.add("增加亲子互动项目和儿童设施");
                recommendations.add("推出家庭套餐优惠");
            }
        }

        recommendations.add("建立会员体系，提高游客复购率");
        recommendations.add("优化景区动线，提升游览体验");

        return recommendations;
    }
}
