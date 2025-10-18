package com.puxinheihei.backend.service;

import com.puxinheihei.backend.mapper.BudgetRecordMapper;
import com.puxinheihei.backend.mapper.ItineraryDayMapper;
import com.puxinheihei.backend.mapper.ItineraryMapper;
import com.puxinheihei.backend.model.BudgetRecord;
import com.puxinheihei.backend.entity.ItineraryEntity;
import com.puxinheihei.backend.entity.ItineraryDayEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class BudgetService {
    private static final Logger log = LoggerFactory.getLogger(BudgetService.class);
    private final BudgetRecordMapper mapper;
    private final ItineraryMapper itineraryMapper;
    private final ItineraryDayMapper dayMapper;
    // 新增：LLM 服务用于智能预算分配
    private final BailianLLMService llmService;

    // 修改构造函数，注入 llmService
    public BudgetService(BudgetRecordMapper mapper, ItineraryMapper itineraryMapper, ItineraryDayMapper dayMapper, BailianLLMService llmService) {
        this.mapper = mapper;
        this.itineraryMapper = itineraryMapper;
        this.dayMapper = dayMapper;
        this.llmService = llmService;
    }

    public BudgetRecord addRecord(BudgetRecord record) {
        if (record.getId() == null || record.getId().isBlank()) {
            record.setId(UUID.randomUUID().toString());
        }
        if (record.getDate() == null) {
            record.setDate(LocalDate.now());
        }
        mapper.insert(record);
        return record;
    }

    public boolean deleteRecord(String id) {
        if (id == null || id.isBlank()) return false;
        return mapper.deleteById(id) > 0;
    }

    public List<BudgetRecord> listByItinerary(String itineraryId) {
        LambdaQueryWrapper<BudgetRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(BudgetRecord::getItineraryId, itineraryId);
        return mapper.selectList(qw);
    }

    public List<BudgetRecord> listAll() {
        return mapper.selectList(null);
    }

    public Map<String, Object> analyze() {
        List<BudgetRecord> list = mapper.selectList(null);
        BigDecimal total = list.stream()
                .map(BudgetRecord::getAmount)
                .filter(Objects::nonNull)
                .filter(a -> a.compareTo(BigDecimal.ZERO) < 0)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, BigDecimal> byCategory = list.stream()
                .filter(r -> r.getAmount() != null && r.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .collect(Collectors.groupingBy(
                        BudgetRecord::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, r -> r.getAmount().abs(), BigDecimal::add)
                ));
        java.util.Set<LocalDate> dates = list.stream()
                .filter(r -> r.getAmount() != null && r.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .map(BudgetRecord::getDate)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        int dayCount = dates.size();
        BigDecimal dailyAvg = (dayCount > 0) ? total.divide(BigDecimal.valueOf(dayCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        java.util.List<String> suggestions = heuristicSuggestions(null, java.util.Collections.emptyList(), new java.util.HashMap<>(), byCategory);
        return Map.of("total", total, "dailyAvg", dailyAvg, "byCategory", byCategory, "suggestions", suggestions);
    }

    public Map<String, Object> analyzeByItinerary(String itineraryId) {
        LambdaQueryWrapper<BudgetRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(BudgetRecord::getItineraryId, itineraryId);
        List<BudgetRecord> list = mapper.selectList(qw);

        Map<String, BigDecimal> byCategory = list.stream()
                .filter(r -> r.getAmount() != null && r.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .collect(Collectors.groupingBy(
                        BudgetRecord::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, r -> r.getAmount().abs(), BigDecimal::add)
                ));

        ItineraryEntity it = itineraryMapper.selectById(itineraryId);
        LambdaQueryWrapper<ItineraryDayEntity> dqw = new LambdaQueryWrapper<>();
        dqw.eq(ItineraryDayEntity::getItineraryId, itineraryId).orderByAsc(ItineraryDayEntity::getDayIndex);
        List<ItineraryDayEntity> days = dayMapper.selectList(dqw);
        int n = days.size();

        BigDecimal totalSpent = list.stream().map(BudgetRecord::getAmount).filter(Objects::nonNull)
                .filter(a -> a.compareTo(BigDecimal.ZERO) < 0)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal dailyAvg = (n > 0) ? totalSpent.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        LocalDate start = it != null ? it.getStartDate() : null;
        List<BigDecimal> spentPerDay = new ArrayList<>();
        List<BigDecimal> dailyBudgets = new ArrayList<>();
        for (ItineraryDayEntity d : days) {
            LocalDate date = (start != null && d.getDayIndex() != null) ? start.plusDays(d.getDayIndex() - 1) : null;
            BigDecimal s = BigDecimal.ZERO;
            if (date != null) {
                LambdaQueryWrapper<BudgetRecord> rw = new LambdaQueryWrapper<>();
                rw.eq(BudgetRecord::getItineraryId, itineraryId).eq(BudgetRecord::getDate, date);
                List<BudgetRecord> recs = mapper.selectList(rw);
                s = recs.stream().map(BudgetRecord::getAmount)
                        .filter(Objects::nonNull)
                        .filter(a -> a.compareTo(BigDecimal.ZERO) < 0)
                        .map(BigDecimal::abs)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            spentPerDay.add(s);
            dailyBudgets.add(Optional.ofNullable(d.getDailyBudget()).orElse(BigDecimal.ZERO));
        }
        BigDecimal totalBudget = Optional.ofNullable(it != null ? it.getBudget() : null).orElse(BigDecimal.ZERO);

        List<String> suggestions = llmService.analyzeBudget(totalBudget, dailyBudgets, spentPerDay, byCategory);
        if (suggestions == null || suggestions.isEmpty()) {
            Map<Integer, BigDecimal> spentMap = new HashMap<>();
            for (int i = 0; i < spentPerDay.size(); i++) {
                spentMap.put(i + 1, spentPerDay.get(i));
            }
            suggestions = heuristicSuggestions(totalBudget, dailyBudgets, spentMap, byCategory);
        }

        return Map.of("total", totalSpent, "dailyAvg", dailyAvg, "byCategory", byCategory, "suggestions", suggestions);
    }

    private List<String> heuristicSuggestions(BigDecimal totalBudget,
                                              List<BigDecimal> dailyBudgets,
                                              Map<Integer, BigDecimal> spentPerDay,
                                              Map<String, BigDecimal> byCategory) {
        List<String> tips = new ArrayList<>();
        if (spentPerDay != null && !spentPerDay.isEmpty() && dailyBudgets != null && !dailyBudgets.isEmpty()) {
            for (int i = 0; i < Math.min(spentPerDay.size(), dailyBudgets.size()); i++) {
                BigDecimal s = spentPerDay.get(i);
                BigDecimal b = dailyBudgets.get(i);
                if (s != null && b != null && s.compareTo(b) > 0) {
                    tips.add("第" + (i + 1) + "天消费" + s + "元，超过预算" + b + "元，建议减少购物或餐饮支出，利用公共交通等举措。");
                }
            }
        }
        if (byCategory != null && !byCategory.isEmpty()) {
            String topCat = byCategory.entrySet().stream().max(java.util.Map.Entry.comparingByValue()).map(java.util.Map.Entry::getKey).orElse(null);
            if (topCat != null) tips.add("主要消费集中在：" + topCat + "，建议通过优惠、替代方案或提前预订降低开支。");
        }
        if (totalBudget != null && spentPerDay != null && !spentPerDay.isEmpty()) {
            BigDecimal totalSpent = BigDecimal.ZERO; for (BigDecimal v : spentPerDay.values()) totalSpent = totalSpent.add(v);
            BigDecimal diff = totalBudget.subtract(totalSpent);
            if (diff.compareTo(BigDecimal.ZERO) < 0) tips.add("当前已超出总预算" + diff.abs() + "元，建议收紧后续每日预算并优先保障核心项目。");
            else tips.add("距离总预算仍有剩余" + diff + "元，可适度提升体验，如增加特色餐厅或活动但注意节制。");
        }
        if (tips.isEmpty()) tips.add("预算整体良好，保持当前节奏并注意必要记录即可。");
        return tips;
    }

    public Map<String, Object> summaryForItinerary(String itineraryId) {
        ItineraryEntity it = itineraryMapper.selectById(itineraryId);
        if (it == null) return Map.of("error", "itinerary_not_found");
        LambdaQueryWrapper<ItineraryDayEntity> dqw = new LambdaQueryWrapper<>();
        dqw.eq(ItineraryDayEntity::getItineraryId, itineraryId).orderByAsc(ItineraryDayEntity::getDayIndex);
        List<ItineraryDayEntity> days = dayMapper.selectList(dqw);
        List<Map<String, Object>> daySummaries = new ArrayList<>();
        LocalDate start = it.getStartDate();
        for (ItineraryDayEntity d : days) {
            LocalDate date = (start != null && d.getDayIndex() != null) ? start.plusDays(d.getDayIndex() - 1) : null;
            BigDecimal spent = BigDecimal.ZERO;
            if (date != null) {
                LambdaQueryWrapper<BudgetRecord> rw = new LambdaQueryWrapper<>();
                rw.eq(BudgetRecord::getItineraryId, itineraryId).eq(BudgetRecord::getDate, date);
                List<BudgetRecord> recs = mapper.selectList(rw);
                spent = recs.stream().map(BudgetRecord::getAmount)
                        .filter(Objects::nonNull)
                        .filter(a -> a.compareTo(BigDecimal.ZERO) < 0)
                        .map(BigDecimal::abs)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            BigDecimal dailyBudget = Optional.ofNullable(d.getDailyBudget()).orElse(BigDecimal.ZERO);
            BigDecimal remaining = dailyBudget.subtract(spent);
            daySummaries.add(
                    Map.of(
                            "dayIndex", d.getDayIndex(),
                            "date", date != null ? date.toString() : null,
                            "dailyBudget", dailyBudget,
                            "spent", spent,
                            "remaining", remaining
                    )
            );
        }
        BigDecimal totalBudget = Optional.ofNullable(it.getBudget()).orElse(BigDecimal.ZERO);
        BigDecimal totalSpent = daySummaries.stream()
                .map(m -> (BigDecimal) m.get("spent"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRemaining = totalBudget.subtract(totalSpent);
        return Map.of(
                "totalBudget", totalBudget,
                "totalSpent", totalSpent,
                "totalRemaining", totalRemaining,
                "days", daySummaries
        );
    }

    public Map<String, Object> reallocateDailyBudget(String itineraryId, BigDecimal newTotal, String mode) {
        ItineraryEntity it = itineraryMapper.selectById(itineraryId);
        if (it == null) return Map.of("error", "itinerary_not_found");
        LambdaQueryWrapper<ItineraryDayEntity> dqw = new LambdaQueryWrapper<>();
        dqw.eq(ItineraryDayEntity::getItineraryId, itineraryId).orderByAsc(ItineraryDayEntity::getDayIndex);
        List<ItineraryDayEntity> days = dayMapper.selectList(dqw);
        if (days.isEmpty()) return Map.of("error", "no_days");

        BigDecimal totalBudget = (newTotal != null && newTotal.compareTo(BigDecimal.ZERO) > 0)
                ? newTotal
                : Optional.ofNullable(it.getBudget()).orElse(BigDecimal.ZERO);
        int n = days.size();

        // 计算各天已消费（仅统计负数记录的绝对值）
        LocalDate start = it.getStartDate();
        List<BigDecimal> spent = new ArrayList<>();
        for (ItineraryDayEntity d : days) {
            LocalDate date = (start != null && d.getDayIndex() != null) ? start.plusDays(d.getDayIndex() - 1) : null;
            BigDecimal s = BigDecimal.ZERO;
            if (date != null) {
                LambdaQueryWrapper<BudgetRecord> rw = new LambdaQueryWrapper<>();
                rw.eq(BudgetRecord::getItineraryId, itineraryId).eq(BudgetRecord::getDate, date);
                List<BudgetRecord> recs = mapper.selectList(rw);
                s = recs.stream().map(BudgetRecord::getAmount)
                        .filter(Objects::nonNull)
                        .filter(a -> a.compareTo(BigDecimal.ZERO) < 0)
                        .map(BigDecimal::abs)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            spent.add(s);
        }

        List<BigDecimal> allocations = new ArrayList<>();
        if ("equal".equalsIgnoreCase(mode)) {
            BigDecimal each = n == 0 ? BigDecimal.ZERO : totalBudget.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
            for (int i = 0; i < n; i++) allocations.add(each);
        } else {
            BigDecimal sumSpent = spent.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sumSpent.compareTo(BigDecimal.ZERO) > 0) {
                for (BigDecimal s : spent) {
                    BigDecimal v = totalBudget.multiply(s).divide(sumSpent, 2, RoundingMode.HALF_UP);
                    allocations.add(v);
                }
            } else {
                BigDecimal each = n == 0 ? BigDecimal.ZERO : totalBudget.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
                for (int i = 0; i < n; i++) allocations.add(each);
            }
        }

        // 更新每日预算
        for (int i = 0; i < n; i++) {
            ItineraryDayEntity d = days.get(i);
            d.setDailyBudget(allocations.get(i));
            dayMapper.updateById(d);
        }
        // 同步总预算（如提供 newTotal）
        if (newTotal != null && newTotal.compareTo(BigDecimal.ZERO) > 0) {
            it.setBudget(newTotal);
            itineraryMapper.updateById(it);
        }
        return summaryForItinerary(itineraryId);
    }

    // 新增：智能重分配（调用大语言模型，根据每天消费情况分配每天预算）
    public Map<String, Object> aiReallocateDailyBudget(String itineraryId) {
        ItineraryEntity it = itineraryMapper.selectById(itineraryId);
        if (it == null) return Map.of("error", "itinerary_not_found");
        LambdaQueryWrapper<ItineraryDayEntity> dqw = new LambdaQueryWrapper<>();
        dqw.eq(ItineraryDayEntity::getItineraryId, itineraryId).orderByAsc(ItineraryDayEntity::getDayIndex);
        List<ItineraryDayEntity> days = dayMapper.selectList(dqw);
        if (days.isEmpty()) return Map.of("error", "no_days");
        BigDecimal totalBudget = Optional.ofNullable(it.getBudget()).orElse(BigDecimal.ZERO);
        int n = days.size();

        // 收集每天的消费（仅统计负数记录的绝对值）
        LocalDate start = it.getStartDate();
        List<BigDecimal> spent = new ArrayList<>();
        for (ItineraryDayEntity d : days) {
            LocalDate date = (start != null && d.getDayIndex() != null) ? start.plusDays(d.getDayIndex() - 1) : null;
            BigDecimal s = BigDecimal.ZERO;
            if (date != null) {
                LambdaQueryWrapper<BudgetRecord> rw = new LambdaQueryWrapper<>();
                rw.eq(BudgetRecord::getItineraryId, itineraryId).eq(BudgetRecord::getDate, date);
                List<BudgetRecord> recs = mapper.selectList(rw);
                s = recs.stream().map(BudgetRecord::getAmount)
                        .filter(Objects::nonNull)
                        .filter(a -> a.compareTo(BigDecimal.ZERO) < 0)
                        .map(BigDecimal::abs)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            spent.add(s);
        }

        log.info("Budget.AIReallocate[call]: itineraryId={} totalBudget={} dayCount={} spentPerDay={}", itineraryId, totalBudget, n, spent);
        // 调用 LLM 进行智能分配
        List<BigDecimal> allocations = llmService.allocateDailyBudget(totalBudget, spent, n);
        if (allocations == null || allocations.size() != n) {
            log.warn("Budget.AIReallocate[fallback-used]: itineraryId={} reason={}", itineraryId, allocations == null ? "LLM returned null" : ("invalid size:" + allocations.size()));
            // 兜底：若 LLM 失败，按已消费比例分配；若无消费，平均分配
            BigDecimal sumSpent = spent.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            allocations = new ArrayList<>();
            if (sumSpent.compareTo(BigDecimal.ZERO) > 0) {
                for (BigDecimal s : spent) {
                    BigDecimal v = totalBudget.multiply(s).divide(sumSpent, 2, RoundingMode.HALF_UP);
                    allocations.add(v);
                }
            } else {
                BigDecimal each = n == 0 ? BigDecimal.ZERO : totalBudget.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
                for (int i = 0; i < n; i++) allocations.add(each);
            }
        } else {
            log.info("Budget.AIReallocate[success]: itineraryId={} budgets={}", itineraryId, allocations);
        }

        // 更新每日预算
        for (int i = 0; i < n; i++) {
            ItineraryDayEntity d = days.get(i);
            d.setDailyBudget(allocations.get(i));
            dayMapper.updateById(d);
        }
        log.info("Budget.AIReallocate[applied]: itineraryId={} dayCount={} budgets={}", itineraryId, n, allocations);
        return summaryForItinerary(itineraryId);
    }

    // 扩展：智能重分配，接受前端完整数据（records/totalBudget/dayBudgets）
    public Map<String, Object> aiReallocateDailyBudgetWithData(String itineraryId, BigDecimal totalBudgetParam,
                                                               List<Map<String, Object>> recordsParam,
                                                               List<BigDecimal> dayBudgetsParam) {
        ItineraryEntity it = itineraryMapper.selectById(itineraryId);
        if (it == null) return Map.of("error", "itinerary_not_found");
        LambdaQueryWrapper<ItineraryDayEntity> dqw = new LambdaQueryWrapper<>();
        dqw.eq(ItineraryDayEntity::getItineraryId, itineraryId).orderByAsc(ItineraryDayEntity::getDayIndex);
        List<ItineraryDayEntity> days = dayMapper.selectList(dqw);
        if (days.isEmpty()) return Map.of("error", "no_days");
        BigDecimal totalBudget = (totalBudgetParam != null && totalBudgetParam.compareTo(BigDecimal.ZERO) > 0)
                ? totalBudgetParam
                : Optional.ofNullable(it.getBudget()).orElse(BigDecimal.ZERO);
        int n = days.size();
        LocalDate start = it.getStartDate();

        // 计算每天消费（优先使用前端 records，仅统计负数记录的绝对值）
        List<BigDecimal> spent = new ArrayList<>(Collections.nCopies(n, BigDecimal.ZERO));
        if (recordsParam != null) {
            for (Map<String, Object> r : recordsParam) {
                Object amtObj = r.get("amount");
                Object dateObj = r.get("date");
                BigDecimal amt = null;
                if (amtObj instanceof Number) amt = new BigDecimal(((Number) amtObj).toString());
                else if (amtObj instanceof String && !((String) amtObj).isBlank()) {
                    try { amt = new BigDecimal((String) amtObj); } catch (Exception ignored) {}
                }
                LocalDate dt = null;
                if (dateObj instanceof String && !((String) dateObj).isBlank()) {
                    try { dt = LocalDate.parse((String) dateObj); } catch (Exception ignored) {}
                }
                if (amt == null || dt == null) continue;
                if (amt.compareTo(BigDecimal.ZERO) < 0 && start != null) {
                    long diff = dt.toEpochDay() - start.toEpochDay();
                    int idx = (int) diff + 1;
                    if (idx >= 1 && idx <= n) {
                        BigDecimal current = spent.get(idx - 1);
                        spent.set(idx - 1, current.add(amt.abs()));
                    }
                }
            }
        } else {
            // 回退：从数据库聚合
            for (ItineraryDayEntity d : days) {
                LocalDate dt = (start != null && d.getDayIndex() != null) ? start.plusDays(d.getDayIndex() - 1) : null;
                BigDecimal s = BigDecimal.ZERO;
                if (dt != null) {
                    LambdaQueryWrapper<BudgetRecord> rw = new LambdaQueryWrapper<>();
                    rw.eq(BudgetRecord::getItineraryId, itineraryId).eq(BudgetRecord::getDate, dt);
                    List<BudgetRecord> recs = mapper.selectList(rw);
                    s = recs.stream().map(BudgetRecord::getAmount)
                            .filter(Objects::nonNull)
                            .filter(a -> a.compareTo(BigDecimal.ZERO) < 0)
                            .map(BigDecimal::abs)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                }
                spent.set(d.getDayIndex() - 1, s);
            }
        }

        // 调用 LLM 进行智能分配
        List<BigDecimal> allocations = llmService.allocateDailyBudget(totalBudget, spent, n);
        if (allocations == null || allocations.size() != n) {
            BigDecimal sumSpent = spent.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            allocations = new ArrayList<>();
            if (sumSpent.compareTo(BigDecimal.ZERO) > 0) {
                for (BigDecimal s : spent) {
                    BigDecimal v = totalBudget.multiply(s).divide(sumSpent, 2, RoundingMode.HALF_UP);
                    allocations.add(v);
                }
            } else {
                BigDecimal each = n == 0 ? BigDecimal.ZERO : totalBudget.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
                for (int i = 0; i < n; i++) allocations.add(each);
            }
        }

        // 更新每日预算
        for (int i = 0; i < n; i++) {
            ItineraryDayEntity d = days.get(i);
            d.setDailyBudget(allocations.get(i));
            dayMapper.updateById(d);
        }
        return summaryForItinerary(itineraryId);
    }

    // 按天：设置某一天的预算为指定值
    public Map<String, Object> updateDayBudget(String itineraryId, Integer dayIndex, java.time.LocalDate date, java.math.BigDecimal newBudget) {
        ItineraryEntity it = itineraryMapper.selectById(itineraryId);
        if (it == null) return Map.of("error", "itinerary_not_found");
        if (newBudget == null) return Map.of("error", "newBudget_required");
        if (newBudget.compareTo(java.math.BigDecimal.ZERO) < 0) newBudget = java.math.BigDecimal.ZERO;

        // 通过 dayIndex 或 date 定位当天
        ItineraryDayEntity target = null;
        if (dayIndex != null && dayIndex > 0) {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ItineraryDayEntity> qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            qw.eq(ItineraryDayEntity::getItineraryId, itineraryId).eq(ItineraryDayEntity::getDayIndex, dayIndex);
            target = dayMapper.selectOne(qw);
        } else if (date != null) {
            java.time.LocalDate start = it.getStartDate();
            if (start != null) {
                long diff = date.toEpochDay() - start.toEpochDay();
                int idx = (int) diff + 1;
                com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ItineraryDayEntity> qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
                qw.eq(ItineraryDayEntity::getItineraryId, itineraryId).eq(ItineraryDayEntity::getDayIndex, idx);
                target = dayMapper.selectOne(qw);
            }
        }
        if (target == null) return Map.of("error", "day_not_found");
        target.setDailyBudget(newBudget);
        dayMapper.updateById(target);
        return summaryForItinerary(itineraryId);
    }

    // 按天：在当前预算基础上增减（delta 可为正/负）
    public Map<String, Object> adjustDayBudget(String itineraryId, Integer dayIndex, java.time.LocalDate date, java.math.BigDecimal delta) {
        ItineraryEntity it = itineraryMapper.selectById(itineraryId);
        if (it == null) return Map.of("error", "itinerary_not_found");
        if (delta == null) return Map.of("error", "delta_required");

        ItineraryDayEntity target = null;
        if (dayIndex != null && dayIndex > 0) {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ItineraryDayEntity> qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            qw.eq(ItineraryDayEntity::getItineraryId, itineraryId).eq(ItineraryDayEntity::getDayIndex, dayIndex);
            target = dayMapper.selectOne(qw);
        } else if (date != null) {
            java.time.LocalDate start = it.getStartDate();
            if (start != null) {
                long diff = date.toEpochDay() - start.toEpochDay();
                int idx = (int) diff + 1;
                com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ItineraryDayEntity> qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
                qw.eq(ItineraryDayEntity::getItineraryId, itineraryId).eq(ItineraryDayEntity::getDayIndex, idx);
                target = dayMapper.selectOne(qw);
            }
        }
        if (target == null) return Map.of("error", "day_not_found");
        java.math.BigDecimal base = java.util.Optional.ofNullable(target.getDailyBudget()).orElse(java.math.BigDecimal.ZERO);
        java.math.BigDecimal after = base.add(delta).setScale(2, java.math.RoundingMode.HALF_UP);
        if (after.compareTo(java.math.BigDecimal.ZERO) < 0) after = java.math.BigDecimal.ZERO;
        target.setDailyBudget(after);
        dayMapper.updateById(target);
        return summaryForItinerary(itineraryId);
    }

    // 按天：重置为“平均/按比例”分配中的该天份额
    public Map<String, Object> resetDayBudget(String itineraryId, Integer dayIndex, java.time.LocalDate date, String mode) {
        ItineraryEntity it = itineraryMapper.selectById(itineraryId);
        if (it == null) return Map.of("error", "itinerary_not_found");
        String m = (mode == null || mode.isBlank()) ? "equal" : mode.toLowerCase();
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ItineraryDayEntity> dqw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        dqw.eq(ItineraryDayEntity::getItineraryId, itineraryId).orderByAsc(ItineraryDayEntity::getDayIndex);
        java.util.List<ItineraryDayEntity> days = dayMapper.selectList(dqw);
        if (days.isEmpty()) return Map.of("error", "no_days");
        int n = days.size();
        java.math.BigDecimal totalBudget = java.util.Optional.ofNullable(it.getBudget()).orElse(java.math.BigDecimal.ZERO);

        // 计算各天已消费
        java.time.LocalDate start = it.getStartDate();
        java.util.List<java.math.BigDecimal> spent = new java.util.ArrayList<>();
        for (ItineraryDayEntity d : days) {
            java.time.LocalDate dt = (start != null && d.getDayIndex() != null) ? start.plusDays(d.getDayIndex() - 1) : null;
            java.math.BigDecimal s = java.math.BigDecimal.ZERO;
            if (dt != null) {
                com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BudgetRecord> rw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
                rw.eq(BudgetRecord::getItineraryId, itineraryId).eq(BudgetRecord::getDate, dt);
                java.util.List<BudgetRecord> recs = mapper.selectList(rw);
                s = recs.stream().map(BudgetRecord::getAmount).filter(java.util.Objects::nonNull)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            }
            spent.add(s);
        }

        java.util.List<java.math.BigDecimal> allocations = new java.util.ArrayList<>();
        if ("equal".equalsIgnoreCase(m)) {
            java.math.BigDecimal each = n == 0 ? java.math.BigDecimal.ZERO : totalBudget.divide(java.math.BigDecimal.valueOf(n), 2, java.math.RoundingMode.HALF_UP);
            for (int i = 0; i < n; i++) allocations.add(each);
        } else {
            java.math.BigDecimal sumSpent = spent.stream().reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            if (sumSpent.compareTo(java.math.BigDecimal.ZERO) > 0) {
                for (java.math.BigDecimal s : spent) {
                    java.math.BigDecimal v = totalBudget.multiply(s).divide(sumSpent, 2, java.math.RoundingMode.HALF_UP);
                    allocations.add(v);
                }
            } else {
                java.math.BigDecimal each = n == 0 ? java.math.BigDecimal.ZERO : totalBudget.divide(java.math.BigDecimal.valueOf(n), 2, java.math.RoundingMode.HALF_UP);
                for (int i = 0; i < n; i++) allocations.add(each);
            }
        }

        // 找到目标天并重置其预算为对应份额
        ItineraryDayEntity target = null;
        if (dayIndex != null && dayIndex > 0) {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ItineraryDayEntity> qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            qw.eq(ItineraryDayEntity::getItineraryId, itineraryId).eq(ItineraryDayEntity::getDayIndex, dayIndex);
            target = dayMapper.selectOne(qw);
        } else if (date != null && start != null) {
            long diff = date.toEpochDay() - start.toEpochDay();
            int idx = (int) diff + 1;
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ItineraryDayEntity> qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            qw.eq(ItineraryDayEntity::getItineraryId, itineraryId).eq(ItineraryDayEntity::getDayIndex, idx);
            target = dayMapper.selectOne(qw);
        }
        if (target == null) return Map.of("error", "day_not_found");
        Integer idx = target.getDayIndex();
        if (idx == null || idx < 1 || idx > allocations.size()) return Map.of("error", "day_index_invalid");
        target.setDailyBudget(allocations.get(idx - 1));
        dayMapper.updateById(target);
        return summaryForItinerary(itineraryId);
    }
}