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
}