package com.puxinheihei.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WatermarkTemplate {
    private String name;
    private WatermarkConfig config;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}