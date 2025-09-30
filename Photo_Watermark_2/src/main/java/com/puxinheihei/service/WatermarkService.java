package com.puxinheihei.service;

import com.puxinheihei.entity.ImageFile;
import com.puxinheihei.entity.WatermarkConfig;
import com.puxinheihei.util.FileUtils;
import com.puxinheihei.util.ImageUtils;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

@Slf4j
public class WatermarkService {

    /**
     * 应用水印到预览图
     */
    public Image applyWatermarkToPreview(ImageFile imageFile, WatermarkConfig config) {
        try {
            if (imageFile == null || config == null) {
                log.warn("图片文件或水印配置为空");
                return null;
            }

            // 加载原图
            File file = new File(imageFile.getFilePath());
            BufferedImage originalImage = ImageUtils.loadImage(file);
            if (originalImage == null) {
                log.error("无法加载图片: {}", imageFile.getFilePath());
                return null;
            }

            // 应用水印
            BufferedImage watermarkedImage = applyWatermark(originalImage, config);
            if (watermarkedImage == null) {
                log.error("应用水印失败");
                return null;
            }

            // 转换为JavaFX Image
            return SwingFXUtils.toFXImage(watermarkedImage, null);

        } catch (Exception e) {
            log.error("应用水印到预览图失败: {}", imageFile.getFileName(), e);
            return null;
        }
    }

    /**
     * 批量导出带水印的图片
     */
    public int exportImages(List<ImageFile> imageFiles, File outputDir,
                            WatermarkConfig config, String outputFormat,
                            String prefix, String suffix) {
        if (imageFiles == null || imageFiles.isEmpty()) {
            log.warn("没有图片需要导出");
            return 0;
        }

        if (!outputDir.exists() && !outputDir.mkdirs()) {
            log.error("无法创建输出目录: {}", outputDir.getAbsolutePath());
            return 0;
        }

        int successCount = 0;
        int totalCount = imageFiles.size();

        log.info("开始导出 {} 张图片到: {}", totalCount, outputDir.getAbsolutePath());

        for (int i = 0; i < imageFiles.size(); i++) {
            ImageFile imageFile = imageFiles.get(i);

            try {
                boolean success = exportSingleImage(imageFile, outputDir, config, outputFormat, prefix, suffix);
                if (success) {
                    successCount++;
                }

                // 进度日志
                if ((i + 1) % 10 == 0 || (i + 1) == totalCount) {
                    log.info("导出进度: {}/{}", i + 1, totalCount);
                }

            } catch (Exception e) {
                log.error("导出图片失败: {}", imageFile.getFileName(), e);
            }
        }

        log.info("导出完成: 成功 {}/{}", successCount, totalCount);
        return successCount;
    }

    /**
     * 导出单张带水印的图片
     */
    public boolean exportSingleImage(ImageFile imageFile, File outputDir,
                                     WatermarkConfig config, String outputFormat,
                                     String prefix, String suffix) {
        try {
            // 加载原图
            File inputFile = new File(imageFile.getFilePath());
            BufferedImage originalImage = ImageUtils.loadImage(inputFile);
            if (originalImage == null) {
                log.error("无法加载图片: {}", imageFile.getFilePath());
                return false;
            }

            // 应用水印
            BufferedImage watermarkedImage = applyWatermark(originalImage, config);
            if (watermarkedImage == null) {
                log.error("应用水印失败: {}", imageFile.getFileName());
                return false;
            }

            // 生成输出文件名
            String outputFileName = generateOutputFileName(imageFile.getFileName(), prefix, suffix, outputFormat);
            File outputFile = new File(outputDir, outputFileName);

            // 保存图片
            boolean saveSuccess = ImageUtils.saveImage(watermarkedImage, outputFile, outputFormat);
            if (saveSuccess) {
                log.debug("成功导出图片: {}", outputFile.getName());
            } else {
                log.error("保存图片失败: {}", outputFile.getAbsolutePath());
            }

            return saveSuccess;

        } catch (Exception e) {
            log.error("导出单张图片失败: {}", imageFile.getFileName(), e);
            return false;
        }
    }

    /**
     * 应用水印到BufferedImage
     */
    private BufferedImage applyWatermark(BufferedImage originalImage, WatermarkConfig config) {
        try {
            return applyTextWatermark(originalImage, config);
        } catch (Exception e) {
            log.error("应用水印失败", e);
            return null;
        }
    }

    /**
     * 应用文本水印
     */
    private BufferedImage applyTextWatermark(BufferedImage originalImage, WatermarkConfig config) {
        try {
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

            // 创建字体 - 支持中文
            Font font;
            try {
                // 尝试创建指定字体
                font = new Font(config.getFontFamily(), Font.PLAIN, (int) config.getFontSize());

                // 简化字体检测：直接检查是否能显示文本
                if (font.canDisplayUpTo(config.getText()) != -1) {
                    System.out.println("字体 '" + config.getFontFamily() + "' 可能不支持文本，使用微软雅黑");
                    font = new Font("Microsoft YaHei", Font.PLAIN, (int) config.getFontSize());
                }
            } catch (Exception e) {
                System.out.println("字体创建失败，使用默认字体: " + e.getMessage());
                font = new Font("Microsoft YaHei", Font.PLAIN, (int) config.getFontSize());
            }

            // 将JavaFX Color转换为AWT Color
            java.awt.Color awtColor = new java.awt.Color(
                    (float) config.getTextColor().getRed(),
                    (float) config.getTextColor().getGreen(),
                    (float) config.getTextColor().getBlue(),
                    (float) config.getTextColor().getOpacity()
            );

            // 设置水印样式
            g2d.setFont(font);
            g2d.setColor(awtColor);

            // 设置透明度
            AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) config.getTextOpacity());
            g2d.setComposite(alphaComposite);

            // 计算位置 - 获取文本尺寸
            FontMetrics metrics = g2d.getFontMetrics(font);
            int textWidth = metrics.stringWidth(config.getText());
            int textHeight = metrics.getHeight();

            int[] position = calculateTextWatermarkPosition(originalImage, config, textWidth, textHeight);
            int x = position[0];
            int y = position[1];

            System.out.println("绘制文本: '" + config.getText() + "'");
            System.out.println("字体: " + font.getFontName() + ", 大小: " + font.getSize());
            System.out.println("文本尺寸: " + textWidth + "x" + textHeight);
            System.out.println("绘制位置: (" + x + ", " + y + ")");

            // 绘制文本水印
            g2d.drawString(config.getText(), x, y);

            g2d.dispose();

            System.out.println("文本水印绘制完成");
            return watermarkedImage;

        } catch (Exception e) {
            log.error("应用文本水印失败", e);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 计算文本水印位置
     */
    private int[] calculateTextWatermarkPosition(BufferedImage originalImage, WatermarkConfig config, int textWidth, int textHeight) {
        int imageWidth = originalImage.getWidth();
        int imageHeight = originalImage.getHeight();

        System.out.println("图片尺寸: " + imageWidth + "x" + imageHeight);
        System.out.println("配置位置: " + config.getPosition());

        // 如果有自定义位置，使用自定义位置
        if (config.getPosition() == WatermarkConfig.Position.CUSTOM) {
            int x = (int) Math.max(10, Math.min(config.getCustomX(), imageWidth - textWidth));
            int y = (int) Math.max(textHeight, Math.min(config.getCustomY(), imageHeight - 10));
            System.out.println("自定义位置: " + x + ", " + y);
            return new int[]{x, y};
        }

        // 计算预设位置
        int[] position = calculatePresetPosition(imageWidth, imageHeight, config.getPosition(), textWidth, textHeight);
        System.out.println("预设位置: " + position[0] + ", " + position[1]);
        return position;
    }

    /**
     * 计算预设位置
     */
    private int[] calculatePresetPosition(int imageWidth, int imageHeight,
                                          WatermarkConfig.Position position,
                                          int watermarkWidth, int watermarkHeight) {
        int x, y;
        int margin = 50;

        switch (position) {
            case TOP_LEFT:
                x = margin;
                y = margin + watermarkHeight;
                break;
            case TOP_CENTER:
                x = (imageWidth - watermarkWidth) / 2;
                y = margin + watermarkHeight;
                break;
            case TOP_RIGHT:
                x = imageWidth - watermarkWidth - margin;
                y = margin + watermarkHeight;
                break;
            case MIDDLE_LEFT:
                x = margin;
                y = (imageHeight - watermarkHeight) / 2 + watermarkHeight;
                break;
            case MIDDLE_CENTER:
                x = (imageWidth - watermarkWidth) / 2;
                y = (imageHeight - watermarkHeight) / 2 + watermarkHeight;
                break;
            case MIDDLE_RIGHT:
                x = imageWidth - watermarkWidth - margin;
                y = (imageHeight - watermarkHeight) / 2 + watermarkHeight;
                break;
            case BOTTOM_LEFT:
                x = margin;
                y = imageHeight - margin;
                break;
            case BOTTOM_CENTER:
                x = (imageWidth - watermarkWidth) / 2;
                y = imageHeight - margin;
                break;
            case BOTTOM_RIGHT:
                x = imageWidth - watermarkWidth - margin;
                y = imageHeight - margin;
                break;
            default:
                x = imageWidth - watermarkWidth - margin;
                y = imageHeight - margin;
        }

        return new int[]{x, y};
    }

    /**
     * 生成输出文件名
     */
    private String generateOutputFileName(String originalName, String prefix, String suffix, String outputFormat) {
        String nameWithoutExt = originalName.substring(0, originalName.lastIndexOf('.'));
        return (prefix != null ? prefix : "") +
                nameWithoutExt +
                (suffix != null ? suffix : "") +
                "." + outputFormat.toLowerCase();
    }

    /**
     * 验证水印配置
     */
    public boolean validateWatermarkConfig(WatermarkConfig config) {
        if (config == null) {
            return false;
        }
        return config.getText() != null && !config.getText().trim().isEmpty();
    }
}