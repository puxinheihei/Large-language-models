package com.puxinheihei.entity;

import lombok.Data;
import javafx.scene.paint.Color;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
public class WatermarkConfig {
    // 文本水印配置
    private String text;
    private String fontFamily;
    private double fontSize;

    @JsonIgnore  // 忽略JavaFX Color的序列化
    private Color textColor;

    // 添加用于JSON序列化的颜色字段
    private String textColorHex;

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
        this.textColorHex = "#000000";
        this.textOpacity = 0.7;
        this.position = Position.BOTTOM_RIGHT;
    }

    // Color和Hex颜色转换方法
    public void setTextColor(Color color) {
        this.textColor = color;
        if (color != null) {
            this.textColorHex = String.format("#%02X%02X%02X",
                    (int) (color.getRed() * 255),
                    (int) (color.getGreen() * 255),
                    (int) (color.getBlue() * 255));
        }
    }

    public Color getTextColor() {
        if (textColorHex != null && !textColorHex.isEmpty()) {
            return Color.web(textColorHex);
        }
        return textColor;
    }
}