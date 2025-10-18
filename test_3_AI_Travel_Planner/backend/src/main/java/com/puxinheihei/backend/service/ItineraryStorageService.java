package com.puxinheihei.backend.service;

import com.puxinheihei.backend.entity.ItineraryEntity;
import com.puxinheihei.backend.entity.ItineraryDayEntity;
import com.puxinheihei.backend.entity.ItineraryPlaceEntity;
import com.puxinheihei.backend.mapper.ItineraryMapper;
import com.puxinheihei.backend.mapper.ItineraryDayMapper;
import com.puxinheihei.backend.mapper.ItineraryPlaceMapper;
import com.puxinheihei.backend.model.Itinerary;
import com.puxinheihei.backend.model.ItineraryDay;
import com.puxinheihei.backend.model.Place;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ItineraryStorageService {
    private final ItineraryMapper itineraryMapper;
    private final ItineraryDayMapper dayMapper;
    private final ItineraryPlaceMapper placeMapper;
    private final GaodeWebApiService gaodeService;

    public ItineraryStorageService(ItineraryMapper itineraryMapper, ItineraryDayMapper dayMapper, ItineraryPlaceMapper placeMapper, GaodeWebApiService gaodeService) {
        this.itineraryMapper = itineraryMapper;
        this.dayMapper = dayMapper;
        this.placeMapper = placeMapper;
        this.gaodeService = gaodeService;
    }

    public String save(String userId, Itinerary itinerary) {
        String itineraryId = UUID.randomUUID().toString();
        ItineraryEntity e = new ItineraryEntity();
        e.setId(itineraryId);
        e.setUserId(userId);
        e.setDestination(itinerary.getDestination());
        e.setStartDate(java.time.LocalDate.parse(itinerary.getStartDate()));
        e.setDays(itinerary.getDays());
        e.setBudget(itinerary.getBudget() != null ? itinerary.getBudget() : java.math.BigDecimal.ZERO);
        e.setPeopleCount(0);
        e.setPreferences("");
        e.setSummary("AI自动生成行程");
        e.setCreatedAt(java.time.LocalDateTime.now());
        itineraryMapper.insert(e);

        if (itinerary.getSchedule() != null) {
            for (ItineraryDay d : itinerary.getSchedule()) {
                String dayId = UUID.randomUUID().toString();
                ItineraryDayEntity de = new ItineraryDayEntity();
                de.setId(dayId);
                de.setItineraryId(itineraryId);
                de.setDayIndex(d.getDayIndex());
                de.setSummary(d.getSummary());
                de.setDailyBudget(d.getDailyBudget());
                dayMapper.insert(de);

                if (d.getPlaces() != null) {
                    for (Place p : d.getPlaces()) {
                        ItineraryPlaceEntity pe = new ItineraryPlaceEntity();
                        pe.setId(UUID.randomUUID().toString());
                        pe.setDayId(dayId);
                        pe.setName(p.getName());
                        pe.setType(p.getType());
                        pe.setAddress(p.getAddress());
                        pe.setLat(p.getLat());
                        pe.setLng(p.getLng());
                        pe.setNotes(p.getNotes());
                        placeMapper.insert(pe);
                    }
                }
            }
        }
        return itineraryId;
    }

    public List<ItineraryEntity> listByUser(String userId) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ItineraryEntity> qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        qw.eq(ItineraryEntity::getUserId, userId).orderByDesc(ItineraryEntity::getCreatedAt);
        return itineraryMapper.selectList(qw);
    }

    public Map<String, Object> get(String itineraryId) {
        ItineraryEntity e = itineraryMapper.selectById(itineraryId);
        if (e == null) return Map.of();
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ItineraryDayEntity> dqw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        dqw.eq(ItineraryDayEntity::getItineraryId, itineraryId).orderByAsc(ItineraryDayEntity::getDayIndex);
        List<ItineraryDayEntity> days = dayMapper.selectList(dqw);

        Map<String, List<ItineraryPlaceEntity>> placesByDay = new HashMap<>();
        for (ItineraryDayEntity d : days) {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ItineraryPlaceEntity> pqw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            pqw.eq(ItineraryPlaceEntity::getDayId, d.getId());
            placesByDay.put(d.getId(), placeMapper.selectList(pqw));
        }

        // 新增：根据 address/name 补全缺失坐标
        enrichPlacesLatLng(e.getDestination(), placesByDay);

        List<Map<String, Object>> schedule = days.stream().map(d -> {
            List<Map<String, Object>> places = placesByDay.getOrDefault(d.getId(), List.of()).stream().map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", p.getName());
                m.put("type", p.getType());
                m.put("address", p.getAddress());
                m.put("lat", p.getLat());
                m.put("lng", p.getLng());
                m.put("notes", p.getNotes());
                return m;
            }).collect(Collectors.toList());
            Map<String, Object> dm = new LinkedHashMap<>();
            dm.put("dayIndex", d.getDayIndex());
            dm.put("summary", d.getSummary());
            dm.put("dailyBudget", d.getDailyBudget());
            dm.put("places", places);
            return dm;
        }).collect(Collectors.toList());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", e.getId());
        out.put("userId", e.getUserId());
        out.put("destination", e.getDestination());
        out.put("startDate", e.getStartDate() != null ? e.getStartDate().toString() : null);
        out.put("days", e.getDays());
        out.put("budget", e.getBudget());
        out.put("summary", e.getSummary());
        out.put("schedule", schedule);
        return out;
    }

    private void enrichPlacesLatLng(String destination, Map<String, List<ItineraryPlaceEntity>> placesByDay) {
        if (placesByDay == null || placesByDay.isEmpty()) return;
        for (List<ItineraryPlaceEntity> list : placesByDay.values()) {
            for (ItineraryPlaceEntity p : list) {
                if (p.getLat() == null || p.getLng() == null) {
                    String query = (p.getAddress() != null && !p.getAddress().isBlank()) ? p.getAddress() : p.getName();
                    double[] loc = gaodeService.geocodeAddress(query, destination);
                    if (loc == null) {
                        loc = gaodeService.topLocationFromPlaceText(query, destination);
                    }
                    if (loc != null) {
                        p.setLng(loc[0]);
                        p.setLat(loc[1]);
                        placeMapper.updateById(p);
                    }
                }
            }
        }
    }

    public boolean delete(String itineraryId) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ItineraryDayEntity> dqw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        dqw.eq(ItineraryDayEntity::getItineraryId, itineraryId);
        List<ItineraryDayEntity> days = dayMapper.selectList(dqw);
        for (ItineraryDayEntity d : days) {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ItineraryPlaceEntity> pqw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            pqw.eq(ItineraryPlaceEntity::getDayId, d.getId());
            placeMapper.delete(pqw);
        }
        dayMapper.delete(dqw);
        return itineraryMapper.deleteById(itineraryId) > 0;
    }
}