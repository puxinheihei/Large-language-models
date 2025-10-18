package com.puxinheihei.backend.service;

import com.puxinheihei.backend.config.AppProperties;
import com.puxinheihei.backend.model.Itinerary;
import com.puxinheihei.backend.model.ItineraryDay;
import com.puxinheihei.backend.model.Place;
import com.puxinheihei.backend.model.PlanRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class BailianLLMService {
    private static final Logger log = LoggerFactory.getLogger(BailianLLMService.class);
    private final AppProperties props;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BailianLLMService(AppProperties props, RestTemplate restTemplate) {
        this.props = props;
        this.restTemplate = restTemplate;
    }

    /**
     * 调用阿里云百炼文生文接口生成行程，并解析为 Itinerary。
     * 当调用失败时，回退到本地示例生成逻辑。
     */
    public Itinerary generateItinerary(PlanRequest req) {
        try {
            String url = props.getBailian().getBaseUrl() + "/services/aigc/text-generation/generation";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(props.getBailian().getApiKey());

            boolean hasVoice = req.getVoiceText() != null && !req.getVoiceText().isBlank();
            log.info("AI.GenerateItinerary[request]: bailian model={} voiceText={} dest={} startDate={} days={} budget={}",
                    props.getBailian().getModel(), hasVoice, req.getDestination(), req.getStartDate(), req.getDays(), req.getBudget());

            Map<String, Object> systemMsg;
            Map<String, Object> userMsg;
            if (hasVoice) {
                // 语音文本：AI需自动解析关键信息并生成详细、按小时安排的行程
                systemMsg = Map.of(
                        "role", "system",
                        "content", "你是旅行规划助手。仅输出合法 JSON，不要附加解释或代码块。请基于用户自然语言描述自动解析目的地、开始日期、天数、总预算、人数、偏好等，并生成包含 destination,startDate,days,budget,peopleCount,preferences,schedule 的 JSON。schedule 为数组，每天必须包含：dayIndex,summary,dailyBudget,places。places 为按时间排序的数组，元素包含 name,type,address,notes，并在 notes 内给出精确到小时的时间段，如 \"09:00-10:30 早餐\"。每天至少包含3个景点（type=\"景点\"），1处住宿（type=\"住宿\"），2个餐厅（type=\"餐厅\"），并包含交通（type=\"交通\"，描述从上一地点到下一地点的方式、时长与费用估计）。如果无法提供坐标 lat,lng 可省略，但尽量给出地址。不要返回除 JSON 外的任何内容。"
                );
                userMsg = Map.of(
                        "role", "user",
                        "content", req.getVoiceText()
                );
            } else {
                // 结构化请求：按给定参数生成详细、按小时安排的行程
                systemMsg = Map.of(
                        "role", "system",
                        "content", "你是旅行规划助手。仅输出合法 JSON，不要附加解释或代码块。JSON 字段包含 destination,startDate,days,budget,peopleCount,preferences,schedule[dayIndex,summary,dailyBudget,places{name,type,address,lat,lng,notes}]。请为每天提供按小时的安排（在 notes 中标注如 \"08:00-09:00\"）。每天至少3个景点、1处住宿、2个餐厅，并包含交通（type=\"交通\"）。确保可直接解析。"
                );
                String userContent = String.format(
                        "目的地：%s；出发日期：%s；天数：%d；总预算：%s；人数：%d；偏好：%s。请生成详细行程 JSON，并为每一天给出 dailyBudget 金额。",
                        req.getDestination(), req.getStartDate(), req.getDays(), req.getBudget(), req.getPeopleCount(), req.getPreferences()
                );
                userMsg = Map.of(
                        "role", "user",
                        "content", userContent
                );
            }

            Map<String, Object> input = Map.of("messages", List.of(systemMsg, userMsg));
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("result_format", "message"); // 优先返回 message 结构
            Map<String, Object> body = new HashMap<>();
            body.put("model", props.getBailian().getModel());
            body.put("input", input);
            body.put("parameters", parameters);

            String payload = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(payload, headers);
            log.info("AI.GenerateItinerary[call]: POST {}", url);
            String resp = restTemplate.postForObject(url, entity, String.class);

            if (resp != null) {
                JsonNode root = objectMapper.readTree(resp);
                String content = null;
                JsonNode choices = root.path("output").path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    content = choices.get(0).path("message").path("content").asText(null);
                }
                if (content == null || content.isEmpty()) {
                    content = root.path("output").path("text").asText("");
                }
                if (content != null && !content.isEmpty()) {
                    Itinerary it = parseItineraryJson(content, req);
                    int schedSize = (it.getSchedule() == null) ? 0 : it.getSchedule().size();
                    log.info("AI.GenerateItinerary[success]: bailian dest={} days={} budget={} scheduleSize={}", it.getDestination(), it.getDays(), it.getBudget(), schedSize);
                    return it;
                } else {
                    log.warn("AI.GenerateItinerary[empty-content]: bailian returned no content, fallback");
                }
            } else {
                log.warn("AI.GenerateItinerary[null-response]: bailian returned null, fallback");
            }
        } catch (Exception e) {
            log.warn("AI.GenerateItinerary[exception]: bailian error={}, fallback", e.toString());
        }
        Itinerary fb = fallbackItinerary(req);
        int schedSize = (fb.getSchedule() == null) ? 0 : fb.getSchedule().size();
        log.warn("AI.GenerateItinerary[fallback-used]: dest={} days={} budget={} scheduleSize={}", fb.getDestination(), fb.getDays(), fb.getBudget(), schedSize);
        return fb;
    }

    private Itinerary parseItineraryJson(String content, PlanRequest req) {
        try {
            String cleaned = content.replace("```json", "").replace("```", "").trim();
            JsonNode maybeRoot = objectMapper.readTree(cleaned);
            JsonNode root = maybeRoot.has("itinerary") ? maybeRoot.path("itinerary") : maybeRoot;

            Itinerary itinerary = new Itinerary();
            itinerary.setDestination(root.path("destination").asText(req.getDestination()));
            itinerary.setStartDate(root.path("startDate").asText(req.getStartDate()));
            Integer fallbackDays = req.getDays();
            int days = root.path("days").isInt() ? root.path("days").asInt() : (fallbackDays != null ? fallbackDays : 3);
            itinerary.setDays(days);
            if (root.has("budget") && root.path("budget").isNumber()) {
                itinerary.setBudget(new BigDecimal(root.path("budget").asText()));
            } else if (req.getBudget() != null) {
                itinerary.setBudget(req.getBudget());
            }

            List<ItineraryDay> schedule = new ArrayList<>();
            JsonNode sched = root.path("schedule");
            if (sched.isArray()) {
                for (JsonNode d : sched) {
                    ItineraryDay day = new ItineraryDay();
                    int idx = d.path("dayIndex").isInt() ? d.path("dayIndex").asInt() : (schedule.size() + 1);
                    day.setDayIndex(idx);
                    day.setSummary(d.path("summary").asText(""));
                    if (d.has("dailyBudget") && d.path("dailyBudget").isNumber()) {
                        day.setDailyBudget(new BigDecimal(d.path("dailyBudget").asText()));
                    }

                    List<Place> places = new ArrayList<>();
                    JsonNode placesNode = d.path("places");
                    if (placesNode.isArray()) {
                        for (JsonNode p : placesNode) {
                            Place place = new Place();
                            place.setName(p.path("name").asText(""));
                            place.setType(p.path("type").asText(""));
                            place.setAddress(p.path("address").asText(""));
                            if (p.has("lat") && p.path("lat").isNumber()) place.setLat(p.path("lat").asDouble());
                            if (p.has("lng") && p.path("lng").isNumber()) place.setLng(p.path("lng").asDouble());
                            place.setNotes(p.path("notes").asText(""));
                            places.add(place);
                        }
                    }
                    day.setPlaces(places);
                    schedule.add(day);
                }
            }
            // 如果LLM未给出每日预算但存在总预算，则进行均匀分配
            if (schedule.size() > 0 && itinerary.getBudget() != null && schedule.stream().noneMatch(d -> d.getDailyBudget() != null)) {
                BigDecimal daily = itinerary.getBudget().divide(BigDecimal.valueOf(schedule.size()), 2, RoundingMode.HALF_UP);
                for (ItineraryDay d : schedule) d.setDailyBudget(daily);
            }
            itinerary.setSchedule(schedule);
            return itinerary;
        } catch (Exception e) {
            return fallbackItinerary(req);
        }
    }

    private Itinerary fallbackItinerary(PlanRequest req) {
        Itinerary itinerary = new Itinerary();
        itinerary.setDestination(req.getDestination());
        itinerary.setStartDate(req.getStartDate());
        int safeDays = (req.getDays() != null && req.getDays() > 0) ? req.getDays() : 3;
        itinerary.setDays(safeDays);
        if (req.getBudget() != null) itinerary.setBudget(req.getBudget());
        List<ItineraryDay> days = new ArrayList<>();
        for (int i = 1; i <= safeDays; i++) {
            ItineraryDay day = new ItineraryDay();
            day.setDayIndex(i);
            day.setSummary("自动生成的行程概要（示例）");
            List<Place> places = new ArrayList<>();
            Place p1 = new Place();
            p1.setName("示例景点" + i);
            p1.setType("景点");
            p1.setAddress("目的地城市内");
            places.add(p1);
            Place r1 = new Place();
            r1.setName("示例餐厅A" + i);
            r1.setType("餐厅");
            r1.setAddress("市中心");
            places.add(r1);
            Place r2 = new Place();
            r2.setName("示例餐厅B" + i);
            r2.setType("餐厅");
            r2.setAddress("市中心");
            places.add(r2);
            Place hotel = new Place();
            hotel.setName("示例酒店" + i);
            hotel.setType("住宿");
            hotel.setAddress("主干道附近");
            places.add(hotel);
            day.setPlaces(places);
            days.add(day);
        }
        // 均匀分配每日预算
        if (req.getBudget() != null && safeDays > 0) {
            BigDecimal daily = req.getBudget().divide(BigDecimal.valueOf(safeDays), 2, RoundingMode.HALF_UP);
            for (ItineraryDay d : days) d.setDailyBudget(daily);
        }
        itinerary.setSchedule(days);
        return itinerary;
    }

    // 新增：智能日预算分配（仅输出 JSON {"budgets":[...]}）
    public List<BigDecimal> allocateDailyBudget(BigDecimal totalBudget, List<BigDecimal> spentPerDay, int dayCount) {
        try {
            String url = props.getBailian().getBaseUrl() + "/services/aigc/text-generation/generation";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(props.getBailian().getApiKey());

            Map<String, Object> systemMsg = Map.of(
                    "role", "system",
                    "content", "你是预算分配助手。仅输出合法 JSON，不要附加任何解释或代码块。输出格式：{\"budgets\":[b1,b2,...]}，长度为 dayCount，保留两位小数，总和≈totalBudget（允许0.01误差）。依据每天已消费金额进行合理分配。"
            );
            Map<String, Object> userMsg = Map.of(
                    "role", "user",
                    "content", String.format("totalBudget=%s; dayCount=%d; spentPerDay=%s", totalBudget, dayCount, spentPerDay)
            );
            Map<String, Object> input = Map.of("messages", List.of(systemMsg, userMsg));
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("result_format", "message");
            Map<String, Object> body = new HashMap<>();
            body.put("model", props.getBailian().getModel());
            body.put("input", input);
            body.put("parameters", parameters);

            String payload = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(payload, headers);
            log.info("AI.BudgetAllocate[call]: POST {}", url);
            String resp = restTemplate.postForObject(url, entity, String.class);
            if (resp != null) {
                JsonNode root = objectMapper.readTree(resp);
                String content = null;
                JsonNode choices = root.path("output").path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    content = choices.get(0).path("message").path("content").asText(null);
                }
                if (content == null || content.isEmpty()) {
                    content = root.path("output").path("text").asText("");
                }
                if (content != null && !content.isEmpty()) {
                    JsonNode budgetsNode = objectMapper.readTree(content).path("budgets");
                    if (budgetsNode.isArray()) {
                        List<BigDecimal> budgets = new ArrayList<>();
                        for (JsonNode b : budgetsNode) budgets.add(new BigDecimal(b.asText("0")));
                        log.info("AI.BudgetAllocate[success]: bailian dayCount={} totalBudget={} budgets={}", dayCount, totalBudget, budgets);
                        return budgets;
                    } else {
                        log.warn("AI.BudgetAllocate[bad-content]: no budgets array, fallback");
                    }
                } else {
                    log.warn("AI.BudgetAllocate[empty-content]: bailian returned no content, fallback");
                }
            } else {
                log.warn("AI.BudgetAllocate[null-response]: bailian returned null, fallback");
            }
        } catch (Exception e) {
            log.warn("AI.BudgetAllocate[exception]: bailian error={}, fallback", e.toString());
        }
        return null; // 交由调用方执行回退分配
    }

    public java.util.List<String> analyzeBudget(java.math.BigDecimal totalBudget,
                                                java.util.List<java.math.BigDecimal> dailyBudgets,
                                                java.util.List<java.math.BigDecimal> spentPerDay,
                                                java.util.Map<String, java.math.BigDecimal> categoryTotals) {
        try {
            String url = props.getBailian().getBaseUrl() + "/services/aigc/text-generation/generation";
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.setBearerAuth(props.getBailian().getApiKey());
    
            java.util.Map<String, Object> systemMsg = java.util.Map.of(
                    "role", "system",
                    "content", "你是旅行预算分析助手。仅输出合法 JSON，不要附加解释或代码块。输出格式：{\"suggestions\":[...]}。依据每天预算与消费、各类别支出，给出3-6条可执行的中文建议，指出超支天数与主要超支类别，并给出调整建议。"
            );
            String userContent = String.format(
                    "totalBudget=%s; dailyBudgets=%s; spentPerDay=%s; categoryTotals=%s",
                    totalBudget, dailyBudgets, spentPerDay, categoryTotals
            );
            java.util.Map<String, Object> userMsg = java.util.Map.of("role", "user", "content", userContent);
            java.util.Map<String, Object> input = java.util.Map.of("messages", java.util.List.of(systemMsg, userMsg));
            java.util.Map<String, Object> parameters = new java.util.HashMap<>();
            parameters.put("result_format", "message");
            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("model", props.getBailian().getModel());
            body.put("input", input);
            body.put("parameters", parameters);
    
            String payload = objectMapper.writeValueAsString(body);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(payload, headers);
            log.info("AI.BudgetAnalyze[call]: POST {}", url);
            String resp = restTemplate.postForObject(url, entity, String.class);
            if (resp != null) {
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(resp);
                String content = null;
                com.fasterxml.jackson.databind.JsonNode choices = root.path("output").path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    content = choices.get(0).path("message").path("content").asText(null);
                }
                if (content == null || content.isEmpty()) {
                    content = root.path("output").path("text").asText("");
                }
                if (content != null && !content.isEmpty()) {
                    com.fasterxml.jackson.databind.JsonNode suggestions = objectMapper.readTree(content).path("suggestions");
                    java.util.List<String> list = new java.util.ArrayList<>();
                    if (suggestions.isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode s : suggestions) list.add(s.asText());
                        log.info("AI.BudgetAnalyze[success]: bailian suggestionsCount={}", list.size());
                        return list;
                    } else {
                        log.warn("AI.BudgetAnalyze[bad-content]: no suggestions array");
                    }
                } else {
                    log.warn("AI.BudgetAnalyze[empty-content]: bailian returned no content");
                }
            } else {
                log.warn("AI.BudgetAnalyze[null-response]: bailian returned null");
            }
        } catch (Exception e) {
            log.warn("AI.BudgetAnalyze[exception]: bailian error={}", e.toString());
        }
        return java.util.List.of();
    }
}