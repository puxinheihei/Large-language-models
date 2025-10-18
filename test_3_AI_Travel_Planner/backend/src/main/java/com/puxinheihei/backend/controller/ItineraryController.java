package com.puxinheihei.backend.controller;

import com.puxinheihei.backend.model.Itinerary;
import com.puxinheihei.backend.model.PlanRequest;
import com.puxinheihei.backend.service.BailianLLMService;
import com.puxinheihei.backend.service.ItineraryStorageService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/itinerary")
public class ItineraryController {
    private final BailianLLMService llmService;
    private final ItineraryStorageService storageService;

    public ItineraryController(BailianLLMService llmService, ItineraryStorageService storageService) {
        this.llmService = llmService;
        this.storageService = storageService;
    }

    @PostMapping("/generate")
    public Itinerary generate(@RequestBody PlanRequest req) {
        Itinerary itinerary = llmService.generateItinerary(req);
        return itinerary;
    }

    @PostMapping("/save")
    public Map<String, String> save(@RequestBody Map<String, Object> body) {
        String userId = (String) body.get("userId");
        Map<String, Object> it = (Map<String, Object>) body.get("itinerary");
        if (userId == null || it == null) {
            return Map.of("status", "error", "message", "参数缺失");
        }
        Itinerary itinerary = new Itinerary();
        itinerary.setDestination((String) it.get("destination"));
        itinerary.setStartDate((String) it.get("startDate"));
        itinerary.setDays((Integer) it.get("days"));
        Object b = it.get("budget");
        if (b instanceof Number) itinerary.setBudget(new java.math.BigDecimal(((Number) b).toString()));
        List<Map<String, Object>> schedule = (List<Map<String, Object>>) it.get("schedule");
        List<com.puxinheihei.backend.model.ItineraryDay> days = new ArrayList<>();
        if (schedule != null) {
            for (Map<String, Object> d : schedule) {
                com.puxinheihei.backend.model.ItineraryDay day = new com.puxinheihei.backend.model.ItineraryDay();
                day.setDayIndex((Integer) d.get("dayIndex"));
                day.setSummary((String) d.get("summary"));
                Object db = d.get("dailyBudget");
                if (db instanceof Number) day.setDailyBudget(new java.math.BigDecimal(((Number) db).toString()));
                List<Map<String, Object>> places = (List<Map<String, Object>>) d.get("places");
                List<com.puxinheihei.backend.model.Place> placeList = new ArrayList<>();
                if (places != null) {
                    for (Map<String, Object> p : places) {
                        com.puxinheihei.backend.model.Place place = new com.puxinheihei.backend.model.Place();
                        place.setName((String) p.get("name"));
                        place.setType((String) p.get("type"));
                        place.setAddress((String) p.get("address"));
                        place.setLat(p.get("lat") instanceof Number ? ((Number) p.get("lat")).doubleValue() : null);
                        place.setLng(p.get("lng") instanceof Number ? ((Number) p.get("lng")).doubleValue() : null);
                        place.setNotes((String) p.get("notes"));
                        placeList.add(place);
                    }
                }
                day.setPlaces(placeList);
                days.add(day);
            }
        }
        itinerary.setSchedule(days);

        String id = storageService.save(userId, itinerary);
        return Map.of("status", "ok", "id", id);
    }

    @GetMapping("/list")
    public List<Map<String, Object>> list(@RequestParam String userId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (var e : storageService.listByUser(userId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId());
            m.put("destination", e.getDestination());
            m.put("startDate", e.getStartDate() != null ? e.getStartDate().toString() : null);
            m.put("days", e.getDays());
            m.put("summary", e.getSummary());
            out.add(m);
        }
        return out;
    }

    @GetMapping("/get")
    public Map<String, Object> get(@RequestParam String id) {
        return storageService.get(id);
    }

    @DeleteMapping("/delete")
    public Map<String, String> delete(@RequestParam String id) {
        boolean ok = storageService.delete(id);
        return Map.of("status", ok ? "ok" : "error");
    }
}