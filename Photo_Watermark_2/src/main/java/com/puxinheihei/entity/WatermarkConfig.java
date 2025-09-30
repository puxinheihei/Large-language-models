package com.puxinheihei.entity;

import lombok.Data;
import javafx.scene.paint.Color;

@Data
public class WatermarkConfig {
    // 文本水印配置
    private String text;
    private String fontFamily;
    private double fontSize;
    private Color textColor;
    private double textOpacity;

    // 位置配置
    private Position position;
    private double customX;
    private double customY;

    public enum Position {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT,
        MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT,
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT,
        CUSTOM
    }

    // 构造函数，设置默认值
    public WatermarkConfig() {
        this.text = "Watermark";
        this.fontFamily = "Microsoft YaHei";
        this.fontSize = 48;
        this.textColor = Color.BLACK;
        this.textOpacity = 0.7;
        this.position = Position.BOTTOM_RIGHT;
    }
}