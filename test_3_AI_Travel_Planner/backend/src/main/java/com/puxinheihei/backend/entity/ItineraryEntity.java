package com.puxinheihei.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

@TableName("itineraries")
public class ItineraryEntity {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String userId;
    private String destination;
    private java.time.LocalDate startDate; // yyyy-MM-dd
    private Integer days;
    private java.math.BigDecimal budget;
    private Integer peopleCount;
    private String preferences; // 逗号分隔
    private String summary; // 可选摘要
    private java.time.LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public java.time.LocalDate getStartDate() { return startDate; }
    public void setStartDate(java.time.LocalDate startDate) { this.startDate = startDate; }
    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
    public java.math.BigDecimal getBudget() { return budget; }
    public void setBudget(java.math.BigDecimal budget) { this.budget = budget; }
    public Integer getPeopleCount() { return peopleCount; }
    public void setPeopleCount(Integer peopleCount) { this.peopleCount = peopleCount; }
    public String getPreferences() { return preferences; }
    public void setPreferences(String preferences) { this.preferences = preferences; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
}