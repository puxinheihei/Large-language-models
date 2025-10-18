package com.puxinheihei.backend.controller;

import com.puxinheihei.backend.model.Itinerary;
import com.puxinheihei.backend.model.PlanRequest;
import com.puxinheihei.backend.service.BailianLLMService;
import com.puxinheihei.backend.service.GaodeWebApiService;
import com.puxinheihei.backend.service.XunfeiVoiceService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/planner")
public class PlannerController {
    private final BailianLLMService llmService;
    private final GaodeWebApiService gaodeService;
    private final XunfeiVoiceService voiceService;

    public PlannerController(BailianLLMService llmService, GaodeWebApiService gaodeService, XunfeiVoiceService voiceService) {
        this.llmService = llmService;
        this.gaodeService = gaodeService;
        this.voiceService = voiceService;
    }

    @PostMapping("/plan")
    public Itinerary plan(@Valid @RequestBody PlanRequest req) {
        return llmService.generateItinerary(req);
    }

    @GetMapping("/pois")
    public String searchPOIs(@RequestParam String keywords, @RequestParam(required = false) String city) {
        return gaodeService.searchPOIs(keywords, city);
    }

    @PostMapping(value = "/voice/transcribe", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> transcribe(@RequestBody Map<String, String> body) {
        String base64 = body.getOrDefault("audioBase64", "");
        String text = voiceService.transcribeBase64Audio(base64);
        return Map.of("text", text);
    }
}