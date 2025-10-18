package com.puxinheihei.backend.model;

import java.util.List;
import java.math.BigDecimal;

public class Itinerary {
    private String destination;
    private String startDate;
    private Integer days;
    private BigDecimal budget;
    private List<ItineraryDay> schedule;

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }
    public List<ItineraryDay> getSchedule() { return schedule; }
    public void setSchedule(List<ItineraryDay> schedule) { this.schedule = schedule; }
}