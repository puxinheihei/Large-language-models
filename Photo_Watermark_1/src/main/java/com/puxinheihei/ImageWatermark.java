package com.puxinheihei;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import java.util.Scanner;

public class ImageWatermark {

    // 定义位置常量
    private static final String TOP_LEFT = "top-left";
    private static final String TOP_RIGHT = "top-right";
    private static final String BOTTOM_LEFT = "bottom-left";
    private static final String BOTTOM_RIGHT = "bottom-right";
    private static final String CENTER = "center";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // 获取用户输入的图片目录路径
        System.out.print("请输入图片目录路径: ");
        String directoryPath = scanner.nextLine().trim();

        // 移除路径中的引号（如果有）
        if (directoryPath.startsWith("\"") && directoryPath.endsWith("\"")) {
            directoryPath = directoryPath.substring(1, directoryPath.length() - 1);
        }

        // 检查目录是否存在
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            System.out.println("目录不存在或不是有效目录!");
            System.out.println("您输入的路径: " + directoryPath);
            System.out.println("绝对路径: " + directory.getAbsolutePath());
            return;
        }

        // 显示目录中的文件列表
        System.out.println("找到目录: " + directory.getAbsolutePath());
        File[] files = directory.listFiles();
        if (files != null && files.length > 0) {
            System.out.println("目录中的文件:");
            for (File file : files) {
                if (isImageFile(file)) {
                    System.out.println("  - " + file.getName() + " (图片文件)");
                } else {
                    System.out.println("  - " + file.getName());
                }
            }
        } else {
            System.out.println("目录为空!");
            return;
        }

        // 获取字体大小
        System.out.print("请输入字体大小 (默认300): ");
        String fontSizeInput = scanner.nextLine();
        int fontSize = fontSizeInput.isEmpty() ? 300 : Integer.parseInt(fontSizeInput);

        // 获取字体颜色
        System.out.print("请输入字体颜色 (支持: black, white, red, blue, green 或 RGB值如255,0,0): ");
        String colorInput = scanner.nextLine();
        Color color = parseColor(colorInput);

        // 获取水印位置
        System.out.print("请输入水印位置 (top-left, top-right, bottom-left, bottom-right, center): ");
        String position = scanner.nextLine();

        // 处理目录中的所有图片文件
        processImagesInDirectory(directory, fontSize, color, position);

        System.out.println("处理完成!");
        scanner.close();
    }

    /**
     * 解析颜色输入
     */
    private static Color parseColor(String colorInput) {
        if (colorInput == null || colorInput.isEmpty()) {
            return Color.WHITE; // 默认白色
        }

        switch (colorInput.toLowerCase()) {
            case "black": return Color.BLACK;
            case "white": return Color.WHITE;
            case "red": return Color.RED;
            case "blue": return Color.BLUE;
            case "green": return Color.GREEN;
            default:
                // 尝试解析RGB值
                if (colorInput.contains(",")) {
                    String[] rgb = colorInput.split(",");
                    if (rgb.length == 3) {
                        try {
                            int r = Integer.parseInt(rgb[0].trim());
                            int g = Integer.parseInt(rgb[1].trim());
                            int b = Integer.parseInt(rgb[2].trim());
                            return new Color(r, g, b);
                        } catch (NumberFormatException e) {
                            System.out.println("颜色格式错误，使用默认白色");
                            return Color.WHITE;
                        }
                    }
                }
                return Color.WHITE;
        }
    }

    /**
     * 处理目录中的所有图片
     */
    private static void processImagesInDirectory(File directory, int fontSize, Color color, String position) {
        // 创建水印输出目录
        File outputDir = new File(directory.getAbsolutePath() + "_watermark");
        if (!outputDir.exists()) {
            if (outputDir.mkdir()) {
                System.out.println("创建输出目录: " + outputDir.getAbsolutePath());
            } else {
                System.out.println("无法创建输出目录!");
                return;
            }
        }

        // 获取目录中的所有文件
        File[] files = directory.listFiles();
        if (files == null) {
            System.out.println("目录为空或无法访问");
            return;
        }

        int processedCount = 0;
        for (File file : files) {
            if (isImageFile(file)) {
                try {
                    // 获取EXIF拍摄时间
                    String dateTime = getExifDateTime(file);

                    // 如果EXIF中没有日期信息，使用文件最后修改时间
                    if (dateTime == null) {
                        dateTime = new SimpleDateFormat("yyyy:MM:dd").format(new Date(file.lastModified()));
                        System.out.println("文件 " + file.getName() + " 无EXIF信息，使用文件修改时间");
                    } else {
                        System.out.println("文件 " + file.getName() + " 从EXIF获取时间: " + dateTime);
                    }

                    // 添加水印并保存
                    addWatermarkToImage(file, outputDir, dateTime, fontSize, color, position);
                    processedCount++;

                } catch (Exception e) {
                    System.out.println("处理文件 " + file.getName() + " 时出错: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        System.out.println("共处理了 " + processedCount + " 个图片文件");
    }

    /**
     * 检查文件是否为图片
     */
    private static boolean isImageFile(File file) {
        if (file.isDirectory()) {
            return false;
        }
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                name.endsWith(".png") || name.endsWith(".bmp") ||
                name.endsWith(".gif") || name.endsWith(".tiff");
    }

    /**
     * 从EXIF信息中获取拍摄时间
     */
    private static String getExifDateTime(File imageFile) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
            ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

            if (directory != null) {
                Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (date != null) {
                    return new SimpleDateFormat("yyyy:MM:dd").format(date);
                }

                // 如果原始日期时间不存在，尝试其他日期标签
                date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME);
                if (date != null) {
                    return new SimpleDateFormat("yyyy:MM:dd").format(date);
                }
            }
        } catch (ImageProcessingException | IOException e) {
            System.out.println("无法读取 " + imageFile.getName() + " 的EXIF信息: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("处理 " + imageFile.getName() + " 时发生未知错误: " + e.getMessage());
        }

        return null;
    }

    /**
     * 添加水印到图片
     */
    private static void addWatermarkToImage(File imageFile, File outputDir, String watermarkText,
                                            int fontSize, Color color, String position) {
        try {
            // 读取原始图片
            BufferedImage originalImage = ImageIO.read(imageFile);
            if (originalImage == null) {
                System.out.println("无法读取图片: " + imageFile.getName());
                return;
            }

            // 创建可编辑的图片副本
            BufferedImage watermarkedImage = new BufferedImage(
                    originalImage.getWidth(),
                    originalImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );

            Graphics2D g2d = watermarkedImage.createGraphics();
            g2d.drawImage(originalImage, 0, 0, null);

            // 设置水印字体和颜色
            Font font = new Font("Arial", Font.BOLD, fontSize);
            g2d.setFont(font);
            g2d.setColor(color);

            // 设置抗锯齿
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 计算水印位置
            Point watermarkPosition = calculateWatermarkPosition(g2d, watermarkText,
                    originalImage.getWidth(),
                    originalImage.getHeight(),
                    position);

            // 添加水印
            g2d.drawString(watermarkText, watermarkPosition.x, watermarkPosition.y);
            g2d.dispose();

            // 保存水印图片
            String outputFileName = getOutputFileName(imageFile.getName());
            File outputFile = new File(outputDir, outputFileName);
            ImageIO.write(watermarkedImage, getImageFormat(imageFile.getName()), outputFile);

            System.out.println("已处理: " + imageFile.getName() + " -> " + outputFileName);

        } catch (IOException e) {
            System.out.println("处理图片 " + imageFile.getName() + " 时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 计算水印位置
     */
    private static Point calculateWatermarkPosition(Graphics2D g2d, String text,
                                                    int imageWidth, int imageHeight,
                                                    String position) {
        FontMetrics metrics = g2d.getFontMetrics();
        int textWidth = metrics.stringWidth(text);
        int textHeight = metrics.getHeight();

        int x, y;

        switch (position.toLowerCase()) {
            case TOP_LEFT:
                x = 20;
                y = textHeight + 10;
                break;
            case TOP_RIGHT:
                x = imageWidth - textWidth - 20;
                y = textHeight + 10;
                break;
            case BOTTOM_LEFT:
                x = 20;
                y = imageHeight - 20;
                break;
            case BOTTOM_RIGHT:
                x = imageWidth - textWidth - 20;
                y = imageHeight - 20;
                break;
            case CENTER:
                x = (imageWidth - textWidth) / 2;
                y = (imageHeight - textHeight) / 2 + textHeight;
                break;
            default:
                x = 20;
                y = imageHeight - 20;
        }

        return new Point(x, y);
    }

    /**
     * 生成输出文件名
     */
    private static String getOutputFileName(String originalName) {
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0) {
            return originalName.substring(0, dotIndex) + "_watermarked" + originalName.substring(dotIndex);
        }
        return originalName + "_watermarked";
    }

    /**
     * 获取图片格式
     */
    private static String getImageFormat(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        if ("jpg".equals(extension) || "jpeg".equals(extension)) {
            return "JPEG";
        } else if ("png".equals(extension)) {
            return "PNG";
        } else if ("bmp".equals(extension)) {
            return "BMP";
        } else if ("gif".equals(extension)) {
            return "GIF";
        } else if ("tiff".equals(extension)) {
            return "TIFF";
        }
        return "JPEG"; // 默认
    }
}