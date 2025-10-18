package com.puxinheihei.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

@TableName("itinerary_places")
public class ItineraryPlaceEntity {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String dayId;
    private String name;
    private String type;
    private String address;
    private Double lat;
    private Double lng;
    private String notes;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDayId() { return dayId; }
    public void setDayId(String dayId) { this.dayId = dayId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}