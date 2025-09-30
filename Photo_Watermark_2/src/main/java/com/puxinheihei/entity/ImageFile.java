package com.puxinheihei.entity;

import lombok.Data;
import javafx.scene.image.Image;

@Data
public class ImageFile {
    private String fileName;
    private String filePath;
    private Image thumbnail;
    private long fileSize;
    private String format;

    // 为每个图片添加独立的水印配置
    private WatermarkConfig watermarkConfig;

    // 获取水印配置，如果为空则创建默认配置
    public WatermarkConfig getWatermarkConfig() {
        if (watermarkConfig == null) {
            watermarkConfig = new WatermarkConfig();
        }
        return watermarkConfig;
    }

    public void setWatermarkConfig(WatermarkConfig watermarkConfig) {
        this.watermarkConfig = watermarkConfig;
    }
}