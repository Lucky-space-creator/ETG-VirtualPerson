package com.virtualwife.admin.module.tourist.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.virtualwife.admin.module.tourist.entity.TouristConsumption;
import com.virtualwife.admin.module.tourist.mapper.TouristConsumptionMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 游客消费记录服务
 */
@Slf4j
@Service
public class TouristConsumptionService extends ServiceImpl<TouristConsumptionMapper, TouristConsumption> {

    /**
     * 从Excel文件导入消费数据
     *
     * @param file      Excel文件
     * @param scenicSpot 景区标识
     * @return 导入结果
     */
    public Map<String, Object> importFromExcel(MultipartFile file, String scenicSpot) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                result.put("success", false);
                result.put("message", "Excel文件为空");
                return result;
            }

            // 获取表头
            Row headerRow = sheet.getRow(0);
            Map<String, Integer> columnMap = new HashMap<>();
            for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); i++) {
                String cellValue = getCellValueAsString(headerRow.getCell(i));
                if (cellValue != null && !cellValue.isBlank()) {
                    columnMap.put(cellValue.trim().toLowerCase(), i);
                }
            }

            // 解析数据行
            List<TouristConsumption> records = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    TouristConsumption record = new TouristConsumption();
                    record.setTouristId(getCellValue(row, columnMap, "tourist_id"));
                    record.setUserNickname(getCellValue(row, columnMap, "user_nickname"));
                    record.setAge(getCellValueAsInt(row, columnMap, "age"));
                    record.setGender(getCellValue(row, columnMap, "gender"));
                    record.setAttractionName(getCellValue(row, columnMap, "attraction_name"));
                    record.setAttractionContent(getCellValue(row, columnMap, "attraction_content"));
                    record.setAttractionType(getCellValue(row, columnMap, "attraction_type"));
                    record.setVisitDate(getCellValueAsDate(row, columnMap, "visit_date"));
                    record.setStayDuration(getCellValueAsBigDecimal(row, columnMap, "stay_duration"));
                    record.setTicketCost(getCellValueAsBigDecimal(row, columnMap, "ticket_cost"));
                    record.setFoodCost(getCellValueAsBigDecimal(row, columnMap, "food_cost"));
                    record.setShoppingCost(getCellValueAsBigDecimal(row, columnMap, "shopping_cost"));
                    record.setTransportCost(getCellValueAsBigDecimal(row, columnMap, "transport_cost"));
                    record.setEntertainmentCost(getCellValueAsBigDecimal(row, columnMap, "entertainment_cost"));
                    record.setTotalCost(getCellValueAsBigDecimal(row, columnMap, "total_cost"));
                    record.setGroupSize(getCellValueAsInt(row, columnMap, "group_size"));
                    record.setSatisfaction(getCellValueAsInt(row, columnMap, "satisfaction"));
                    record.setScenicSpot(scenicSpot);
                    record.setCreateTime(LocalDateTime.now());
                    record.setUpdateTime(LocalDateTime.now());

                    records.add(record);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    errors.add("第" + (i + 1) + "行: " + e.getMessage());
                    if (errors.size() > 10) {
                        errors.add("... 更多错误已省略");
                        break;
                    }
                }
            }

            // 批量插入
            if (!records.isEmpty()) {
                saveBatch(records, 1000);
            }

            result.put("success", true);
            result.put("totalRows", sheet.getLastRowNum());
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("errors", errors);

            log.info("消费数据导入完成: 成功={}, 失败={}, 景区={}", successCount, failCount, scenicSpot);

        } catch (Exception e) {
            log.error("导入Excel失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "导入失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取消费统计数据
     */
    public Map<String, Object> getConsumptionStats(String scenicSpot) {
        Map<String, Object> stats = new HashMap<>();

        // 查询条件
        var query = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TouristConsumption>();
        if (scenicSpot != null && !scenicSpot.isBlank() && !"all".equals(scenicSpot)) {
            query.eq(TouristConsumption::getScenicSpot, scenicSpot);
        }

        List<TouristConsumption> records = list(query);

        if (records.isEmpty()) {
            return stats;
        }

        // 基础统计
        int totalRecords = records.size();
        BigDecimal totalConsumption = records.stream()
                .map(TouristConsumption::getTotalCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgConsumption = totalConsumption.divide(BigDecimal.valueOf(totalRecords), 2, BigDecimal.ROUND_HALF_UP);

        stats.put("totalRecords", totalRecords);
        stats.put("totalConsumption", totalConsumption);
        stats.put("avgConsumption", avgConsumption);

        // 消费结构
        BigDecimal totalTicket = records.stream().map(TouristConsumption::getTicketCost).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFood = records.stream().map(TouristConsumption::getFoodCost).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalShopping = records.stream().map(TouristConsumption::getShoppingCost).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTransport = records.stream().map(TouristConsumption::getTransportCost).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEntertainment = records.stream().map(TouristConsumption::getEntertainmentCost).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> structure = new ArrayList<>();
        if (totalConsumption.compareTo(BigDecimal.ZERO) > 0) {
            structure.add(Map.of("name", "门票", "value", totalTicket.multiply(BigDecimal.valueOf(100)).divide(totalConsumption, 1, BigDecimal.ROUND_HALF_UP)));
            structure.add(Map.of("name", "餐饮", "value", totalFood.multiply(BigDecimal.valueOf(100)).divide(totalConsumption, 1, BigDecimal.ROUND_HALF_UP)));
            structure.add(Map.of("name", "购物", "value", totalShopping.multiply(BigDecimal.valueOf(100)).divide(totalConsumption, 1, BigDecimal.ROUND_HALF_UP)));
            structure.add(Map.of("name", "交通", "value", totalTransport.multiply(BigDecimal.valueOf(100)).divide(totalConsumption, 1, BigDecimal.ROUND_HALF_UP)));
            structure.add(Map.of("name", "娱乐", "value", totalEntertainment.multiply(BigDecimal.valueOf(100)).divide(totalConsumption, 1, BigDecimal.ROUND_HALF_UP)));
        }
        stats.put("structure", structure);

        // 年龄分布
        Map<String, Long> ageGroups = new LinkedHashMap<>();
        ageGroups.put("18-25岁", records.stream().filter(r -> r.getAge() != null && r.getAge() >= 18 && r.getAge() <= 25).count());
        ageGroups.put("26-35岁", records.stream().filter(r -> r.getAge() != null && r.getAge() >= 26 && r.getAge() <= 35).count());
        ageGroups.put("36-45岁", records.stream().filter(r -> r.getAge() != null && r.getAge() >= 36 && r.getAge() <= 45).count());
        ageGroups.put("46-55岁", records.stream().filter(r -> r.getAge() != null && r.getAge() >= 46 && r.getAge() <= 55).count());
        ageGroups.put("56岁以上", records.stream().filter(r -> r.getAge() != null && r.getAge() >= 56).count());

        List<Map<String, Object>> ageDistribution = new ArrayList<>();
        ageGroups.forEach((name, count) -> ageDistribution.add(Map.of("name", name, "value", count)));
        stats.put("ageDistribution", ageDistribution);

        // 满意度分布
        Map<String, Long> satisfactionGroups = new LinkedHashMap<>();
        satisfactionGroups.put("非常满意", records.stream().filter(r -> r.getSatisfaction() != null && r.getSatisfaction() >= 5).count());
        satisfactionGroups.put("满意", records.stream().filter(r -> r.getSatisfaction() != null && r.getSatisfaction() == 4).count());
        satisfactionGroups.put("一般", records.stream().filter(r -> r.getSatisfaction() != null && r.getSatisfaction() == 3).count());
        satisfactionGroups.put("不满意", records.stream().filter(r -> r.getSatisfaction() != null && r.getSatisfaction() <= 2).count());

        List<Map<String, Object>> satisfaction = new ArrayList<>();
        satisfactionGroups.forEach((name, count) -> satisfaction.add(Map.of("name", name, "value", count)));
        stats.put("satisfaction", satisfaction);

        // 景点排行
        Map<String, List<TouristConsumption>> byAttraction = new LinkedHashMap<>();
        records.stream()
                .filter(r -> r.getAttractionName() != null && !r.getAttractionName().isBlank())
                .forEach(r -> byAttraction.computeIfAbsent(r.getAttractionName(), k -> new ArrayList<>()).add(r));

        List<Map<String, Object>> spotRanking = new ArrayList<>();
        byAttraction.entrySet().stream()
                .sorted((a, b) -> b.getValue().size() - a.getValue().size())
                .limit(10)
                .forEach(entry -> {
                    List<TouristConsumption> spotRecords = entry.getValue();
                    BigDecimal spotTotal = spotRecords.stream().map(TouristConsumption::getTotalCost).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal spotAvg = spotRecords.isEmpty() ? BigDecimal.ZERO : spotTotal.divide(BigDecimal.valueOf(spotRecords.size()), 2, BigDecimal.ROUND_HALF_UP);
                    double avgSatisfaction = spotRecords.stream().filter(r -> r.getSatisfaction() != null).mapToInt(TouristConsumption::getSatisfaction).average().orElse(0);
                    double avgStay = spotRecords.stream().filter(r -> r.getStayDuration() != null).mapToDouble(r -> r.getStayDuration().doubleValue()).average().orElse(0);

                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", entry.getKey());
                    item.put("visitorCount", spotRecords.size());
                    item.put("avgConsumption", spotAvg.intValue());
                    item.put("totalConsumption", spotTotal.intValue());
                    item.put("avgSatisfaction", Math.round(avgSatisfaction * 10.0) / 10.0);
                    item.put("avgStayDuration", Math.round(avgStay * 10.0) / 10.0);
                    spotRanking.add(item);
                });
        stats.put("spotRanking", spotRanking);

        // 消费模式
        long highCost = records.stream().filter(r -> r.getTotalCost() != null && r.getTotalCost().compareTo(BigDecimal.valueOf(1000)) >= 0).count();
        long shoppingDominant = records.stream().filter(r -> r.getTotalCost() != null && r.getTotalCost().compareTo(BigDecimal.ZERO) > 0 && r.getShoppingCost() != null && r.getShoppingCost().multiply(BigDecimal.valueOf(100)).divide(r.getTotalCost(), 0, BigDecimal.ROUND_HALF_UP).compareTo(BigDecimal.valueOf(40)) >= 0).count();
        long foodDominant = records.stream().filter(r -> r.getTotalCost() != null && r.getTotalCost().compareTo(BigDecimal.ZERO) > 0 && r.getFoodCost() != null && r.getFoodCost().multiply(BigDecimal.valueOf(100)).divide(r.getTotalCost(), 0, BigDecimal.ROUND_HALF_UP).compareTo(BigDecimal.valueOf(35)) >= 0).count();
        long familyGroup = records.stream().filter(r -> r.getGroupSize() != null && r.getGroupSize() >= 3).count();

        List<Map<String, Object>> patterns = new ArrayList<>();
        patterns.add(Map.of("name", "高消费游客", "percent", (int)(highCost * 100 / totalRecords), "color", "#f56c6c", "desc", "总消费≥1000元"));
        patterns.add(Map.of("name", "购物主导型", "percent", (int)(shoppingDominant * 100 / totalRecords), "color", "#e6a23c", "desc", "购物消费占比>40%"));
        patterns.add(Map.of("name", "餐饮主导型", "percent", (int)(foodDominant * 100 / totalRecords), "color", "#67c23a", "desc", "餐饮消费占比>35%"));
        patterns.add(Map.of("name", "家庭出游型", "percent", (int)(familyGroup * 100 / totalRecords), "color", "#409eff", "desc", "3人及以上团体"));
        stats.put("patterns", patterns);

        return stats;
    }

    // ==================== 工具方法 ====================

    private String getCellValue(Row row, Map<String, Integer> columnMap, String columnName) {
        Integer colIndex = columnMap.get(columnName);
        if (colIndex == null) return null;
        return getCellValueAsString(row.getCell(colIndex));
    }

    private Integer getCellValueAsInt(Row row, Map<String, Integer> columnMap, String columnName) {
        String value = getCellValue(row, columnMap, columnName);
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim().split("\\.")[0]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal getCellValueAsBigDecimal(Row row, Map<String, Integer> columnMap, String columnName) {
        String value = getCellValue(row, columnMap, columnName);
        if (value == null || value.isBlank()) return null;
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate getCellValueAsDate(Row row, Map<String, Integer> columnMap, String columnName) {
        String value = getCellValue(row, columnMap, columnName);
        if (value == null || value.isBlank()) return null;
        try {
            // 尝试多种日期格式
            String[] patterns = {"yyyy-MM-dd", "yyyy/MM/dd", "yyyy年MM月dd日", "yyyyMMdd"};
            for (String pattern : patterns) {
                try {
                    return LocalDate.parse(value.trim(), DateTimeFormatter.ofPattern(pattern));
                } catch (Exception ignored) {
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double numValue = cell.getNumericCellValue();
                if (numValue == (int) numValue) {
                    return String.valueOf((int) numValue);
                }
                return String.valueOf(numValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (Exception e2) {
                        return null;
                    }
                }
            default:
                return null;
        }
    }
}
