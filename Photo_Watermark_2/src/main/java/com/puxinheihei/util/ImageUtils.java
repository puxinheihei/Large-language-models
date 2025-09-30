package com.puxinheihei.util;

import lombok.extern.slf4j.Slf4j;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Slf4j
public class ImageUtils {

    public static BufferedImage loadImage(File file) {
        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            log.error("Error loading image: {}", file.getAbsolutePath(), e);
            return null;
        }
    }

    public static boolean saveImage(BufferedImage image, File outputFile, String format) {
        try {
            String formatName = format.toUpperCase();
            if ("JPEG".equals(formatName)) {
                // 对于JPEG格式，确保图像没有透明通道
                BufferedImage rgbImage = new BufferedImage(
                        image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB
                );
                Graphics2D g = rgbImage.createGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, image.getWidth(), image.getHeight());
                g.drawImage(image, 0, 0, null);
                g.dispose();
                return ImageIO.write(rgbImage, "JPEG", outputFile);
            } else {
                return ImageIO.write(image, formatName, outputFile);
            }
        } catch (IOException e) {
            log.error("Error saving image: {}", outputFile.getAbsolutePath(), e);
            return false;
        }
    }

    public static BufferedImage createThumbnail(BufferedImage originalImage, int maxWidth, int maxHeight) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // 计算缩略图尺寸
        double scale = Math.min((double) maxWidth / originalWidth, (double) maxHeight / originalHeight);
        int thumbWidth = (int) (originalWidth * scale);
        int thumbHeight = (int) (originalHeight * scale);

        // 创建缩略图
        BufferedImage thumbnail = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = thumbnail.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, thumbWidth, thumbHeight, null);
        g2d.dispose();

        return thumbnail;
    }

    public static BufferedImage applyTextWatermark(BufferedImage originalImage, String text,
                                                   Font font, Color color, float opacity,
                                                   int x, int y) {
        // 创建带透明通道的图像
        BufferedImage watermarkedImage = new BufferedImage(
                originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2d = watermarkedImage.createGraphics();

        // 设置渲染质量
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 绘制原图
        g2d.drawImage(originalImage, 0, 0, null);

        // 设置水印样式
        g2d.setFont(font);
        g2d.setColor(color);

        // 设置透明度
        AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
        g2d.setComposite(alphaComposite);

        // 绘制文本水印
        g2d.drawString(text, x, y);

        g2d.dispose();

        return watermarkedImage;
    }

    public static BufferedImage applyImageWatermark(BufferedImage originalImage, BufferedImage watermarkImage,
                                                    float opacity, double scale, int x, int y) {
        BufferedImage watermarkedImage = new BufferedImage(
                originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2d = watermarkedImage.createGraphics();

        // 绘制原图
        g2d.drawImage(originalImage, 0, 0, null);

        // 计算水印尺寸
        int watermarkWidth = (int) (watermarkImage.getWidth() * scale);
        int watermarkHeight = (int) (watermarkImage.getHeight() * scale);

        // 设置透明度
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));

        // 绘制图片水印
        g2d.drawImage(watermarkImage, x, y, watermarkWidth, watermarkHeight, null);
        g2d.dispose();

        return watermarkedImage;
    }
}