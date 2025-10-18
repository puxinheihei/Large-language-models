package com.puxinheihei.backend.controller;

import com.puxinheihei.backend.model.BudgetRecord;
import com.puxinheihei.backend.service.BudgetService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/budget")
public class BudgetController {
    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping("/record")
    public Map<String, Object> addRecord(@RequestBody BudgetRecord record) {
        BudgetRecord saved = budgetService.addRecord(record);
        return Map.of("status", "ok", "record", saved);
    }

    @DeleteMapping("/record/delete")
    public Map<String, Object> deleteRecord(@RequestParam String id) {
        boolean ok = budgetService.deleteRecord(id);
        return ok ? Map.of("status", "ok") : Map.of("status", "error", "message", "delete_failed");
    }

    @GetMapping("/records")
    public Map<String, Object> listRecords(@RequestParam(required = false) String itineraryId) {
        List<BudgetRecord> list = (itineraryId == null || itineraryId.isBlank())
                ? budgetService.listAll()
                : budgetService.listByItinerary(itineraryId);
        return Map.of("records", list);
    }

    // 新增：预算摘要
    @GetMapping("/summary")
    public Map<String, Object> summary(@RequestParam String itineraryId) {
        return budgetService.summaryForItinerary(itineraryId);
    }

    // 新增：预算分析（总览或按行程）
    @GetMapping("/analyze")
    public Map<String, Object> analyze(@RequestParam(required = false) String itineraryId) {
        return (itineraryId == null || itineraryId.isBlank())
                ? budgetService.analyze()
                : budgetService.analyzeByItinerary(itineraryId);
    }

    // 新增：预算重分配（平均/AI）
    @PostMapping("/reallocate")
    public Map<String, Object> reallocate(@RequestBody Map<String, Object> body) {
        String itineraryId = (String) body.get("itineraryId");
        String mode = (String) body.getOrDefault("mode", "equal");
        BigDecimal newTotal = null;
        Object nt = body.get("newTotal");
        if (nt instanceof Number) newTotal = new BigDecimal(((Number) nt).toString());
        else if (nt instanceof String && !((String) nt).isBlank()) newTotal = new BigDecimal((String) nt);

        if ("ai".equalsIgnoreCase(mode)) {
            // 支持前端传全部数据
            BigDecimal totalBudget = null;
            Object tb = body.get("totalBudget");
            if (tb instanceof Number) totalBudget = new BigDecimal(((Number) tb).toString());
            else if (tb instanceof String && !((String) tb).isBlank()) totalBudget = new BigDecimal((String) tb);
            @SuppressWarnings("unchecked") List<Map<String, Object>> records = (List<Map<String, Object>>) body.get("records");
            @SuppressWarnings("unchecked") List<BigDecimal> dayBudgets = (List<BigDecimal>) body.get("dayBudgets");
            if (records != null || dayBudgets != null || totalBudget != null) {
                return budgetService.aiReallocateDailyBudgetWithData(itineraryId, totalBudget, records, dayBudgets);
            }
            return budgetService.aiReallocateDailyBudget(itineraryId);
        }
        return budgetService.reallocateDailyBudget(itineraryId, newTotal, mode);
    }

    // 新增：按天设置预算
    @PostMapping("/day/update")
    public Map<String, Object> updateDay(@RequestBody Map<String, Object> body) {
        String itineraryId = (String) body.get("itineraryId");
        Integer dayIndex = null;
        Object di = body.get("dayIndex");
        if (di instanceof Number) dayIndex = ((Number) di).intValue();
        else if (di instanceof String && !((String) di).isBlank()) dayIndex = Integer.valueOf((String) di);
        java.time.LocalDate date = null;
        Object ds = body.get("date");
        if (ds instanceof String && !((String) ds).isBlank()) date = java.time.LocalDate.parse((String) ds);
        java.math.BigDecimal newBudget = null;
        Object nb = body.get("newBudget");
        if (nb instanceof Number) newBudget = new java.math.BigDecimal(((Number) nb).toString());
        else if (nb instanceof String && !((String) nb).isBlank()) newBudget = new java.math.BigDecimal((String) nb);
        return budgetService.updateDayBudget(itineraryId, dayIndex, date, newBudget);
    }

    // 新增：按天增减预算（delta 可为正/负）
    @PostMapping("/day/adjust")
    public Map<String, Object> adjustDay(@RequestBody Map<String, Object> body) {
        String itineraryId = (String) body.get("itineraryId");
        Integer dayIndex = null;
        Object di = body.get("dayIndex");
        if (di instanceof Number) dayIndex = ((Number) di).intValue();
        else if (di instanceof String && !((String) di).isBlank()) dayIndex = Integer.valueOf((String) di);
        java.time.LocalDate date = null;
        Object ds = body.get("date");
        if (ds instanceof String && !((String) ds).isBlank()) date = java.time.LocalDate.parse((String) ds);
        java.math.BigDecimal delta = null;
        Object dl = body.get("delta");
        if (dl instanceof Number) delta = new java.math.BigDecimal(((Number) dl).toString());
        else if (dl instanceof String && !((String) dl).isBlank()) delta = new java.math.BigDecimal((String) dl);
        return budgetService.adjustDayBudget(itineraryId, dayIndex, date, delta);
    }

    // 新增：按天重置（equal / proportional）
    @PostMapping("/day/reset")
    public Map<String, Object> resetDay(@RequestBody Map<String, Object> body) {
        String itineraryId = (String) body.get("itineraryId");
        Integer dayIndex = null;
        Object di = body.get("dayIndex");
        if (di instanceof Number) dayIndex = ((Number) di).intValue();
        else if (di instanceof String && !((String) di).isBlank()) dayIndex = Integer.valueOf((String) di);
        java.time.LocalDate date = null;
        Object ds = body.get("date");
        if (ds instanceof String && !((String) ds).isBlank()) date = java.time.LocalDate.parse((String) ds);
        String mode = (String) body.getOrDefault("mode", "equal");
        return budgetService.resetDayBudget(itineraryId, dayIndex, date, mode);
    }
}