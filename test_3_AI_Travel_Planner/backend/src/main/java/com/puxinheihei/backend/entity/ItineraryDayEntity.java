package com.puxinheihei.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

@TableName("itinerary_days")
public class ItineraryDayEntity {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String itineraryId;
    private Integer dayIndex;
    private String summary;
    private java.math.BigDecimal dailyBudget;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getItineraryId() { return itineraryId; }
    public void setItineraryId(String itineraryId) { this.itineraryId = itineraryId; }
    public Integer getDayIndex() { return dayIndex; }
    public void setDayIndex(Integer dayIndex) { this.dayIndex = dayIndex; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public java.math.BigDecimal getDailyBudget() { return dailyBudget; }
    public void setDailyBudget(java.math.BigDecimal dailyBudget) { this.dailyBudget = dailyBudget; }
}