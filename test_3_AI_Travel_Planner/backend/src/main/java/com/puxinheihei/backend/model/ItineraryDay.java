package com.puxinheihei.backend.model;

import java.util.List;
import java.math.BigDecimal;

public class ItineraryDay {
    private Integer dayIndex;
    private String summary;
    private List<Place> places;
    private BigDecimal dailyBudget;

    public Integer getDayIndex() { return dayIndex; }
    public void setDayIndex(Integer dayIndex) { this.dayIndex = dayIndex; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public List<Place> getPlaces() { return places; }
    public void setPlaces(List<Place> places) { this.places = places; }
    public BigDecimal getDailyBudget() { return dailyBudget; }
    public void setDailyBudget(BigDecimal dailyBudget) { this.dailyBudget = dailyBudget; }
}