package com.puxinheihei.backend.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.List;

public class PlanRequest {
    @NotBlank
    private String destination;
    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}")
    private String startDate; // yyyy-MM-dd
    @NotNull
    private Integer days;
    @NotNull
    private java.math.BigDecimal budget;
    @NotNull
    private Integer peopleCount;
    private java.util.List<String> preferences; // e.g., 美食, 动漫, 亲子
    private String notes;
    // 新增：语音文本（当使用语音输入时，其他字段可为空，由AI解析）
    private String voiceText;

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
    public java.math.BigDecimal getBudget() { return budget; }
    public void setBudget(java.math.BigDecimal budget) { this.budget = budget; }
    public Integer getPeopleCount() { return peopleCount; }
    public void setPeopleCount(Integer peopleCount) { this.peopleCount = peopleCount; }
    public java.util.List<String> getPreferences() { return preferences; }
    public void setPreferences(java.util.List<String> preferences) { this.preferences = preferences; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getVoiceText() { return voiceText; }
    public void setVoiceText(String voiceText) { this.voiceText = voiceText; }
}