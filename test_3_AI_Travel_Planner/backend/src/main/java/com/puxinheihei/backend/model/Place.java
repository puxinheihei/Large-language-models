package com.puxinheihei.backend.model;

public class Place {
    private String name;
    private String type; // 景点, 餐厅, 酒店, 交通
    private String address;
    private Double lat;
    private Double lng;
    private String notes;

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