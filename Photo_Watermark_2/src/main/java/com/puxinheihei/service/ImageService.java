package com.puxinheihei.service;

import com.puxinheihei.entity.ImageFile;
import com.puxinheihei.entity.WatermarkConfig;
import com.puxinheihei.util.FileUtils;
import com.puxinheihei.util.ImageUtils;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class ImageService {

    private final List<ImageFile> imageFiles;
    private static final int THUMBNAIL_WIDTH = 120;
    private static final int THUMBNAIL_HEIGHT = 80;

    public ImageService() {
        this.imageFiles = new CopyOnWriteArrayList<>();
    }

    /**
     * 添加单个图片文件
     */
    public boolean addImageFile(File file) {
        try {
            if (!FileUtils.isImageFile(file)) {
                log.warn("不支持的文件格式: {}", file.getName());
                return false;
            }

            // 检查是否已存在
            if (imageFiles.stream().anyMatch(img -> img.getFilePath().equals(file.getAbsolutePath()))) {
                log.info("图片已存在: {}", file.getName());
                return false;
            }

            // 使用JavaFX直接加载图片创建缩略图
            Image thumbnail = new Image("file:" + file.getAbsolutePath(), THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, true, true, true);
            if (thumbnail.isError()) {
                log.error("无法加载图片文件: {}", file.getAbsolutePath());
                return false;
            }

            // 创建ImageFile对象
            ImageFile imageFile = new ImageFile();
            imageFile.setFileName(file.getName());
            imageFile.setFilePath(file.getAbsolutePath());
            imageFile.setThumbnail(thumbnail);
            imageFile.setFileSize(file.length());
            imageFile.setFormat(FileUtils.getFileExtension(file.getName()));

            // 为每个图片初始化默认水印配置
            imageFile.setWatermarkConfig(new WatermarkConfig());

            imageFiles.add(imageFile);
            log.info("成功添加图片: {}", file.getName());
            return true;

        } catch (Exception e) {
            log.error("添加图片文件失败: {}", file.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * 添加多个图片文件
     */
    public int addImageFiles(List<File> files) {
        int successCount = 0;
        for (File file : files) {
            if (addImageFile(file)) {
                successCount++;
            }
        }
        log.info("成功添加 {}/{} 张图片", successCount, files.size());
        return successCount;
    }

    /**
     * 从文件夹扫描图片文件
     */
    public List<File> scanImageFilesFromFolder(File folder) {
        try {
            if (!folder.exists() || !folder.isDirectory()) {
                log.warn("文件夹不存在或不是目录: {}", folder.getAbsolutePath());
                return new ArrayList<>();
            }

            List<File> imageFiles = FileUtils.getImageFilesFromFolder(folder);
            log.info("从文件夹扫描到 {} 张图片: {}", imageFiles.size(), folder.getAbsolutePath());
            return imageFiles;

        } catch (Exception e) {
            log.error("扫描文件夹失败: {}", folder.getAbsolutePath(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取所有图片文件
     */
    public List<ImageFile> getImageFiles() {
        return new ArrayList<>(imageFiles);
    }

    /**
     * 根据文件路径获取图片文件
     */
    public ImageFile getImageFileByPath(String filePath) {
        return imageFiles.stream()
                .filter(img -> img.getFilePath().equals(filePath))
                .findFirst()
                .orElse(null);
    }

    /**
     * 移除图片文件
     */
    public boolean removeImageFile(ImageFile imageFile) {
        boolean removed = imageFiles.remove(imageFile);
        if (removed) {
            log.info("移除图片: {}", imageFile.getFileName());
        }
        return removed;
    }

    /**
     * 清空所有图片文件
     */
    public void clearImageFiles() {
        int count = imageFiles.size();
        imageFiles.clear();
        log.info("清空所有图片，共 {} 张", count);
    }

    /**
     * 获取图片数量
     */
    public int getImageCount() {
        return imageFiles.size();
    }

    /**
     * 验证输出文件夹是否有效
     */
    public boolean isValidOutputFolder(File outputFolder) {
        if (outputFolder == null || !outputFolder.exists() || !outputFolder.isDirectory()) {
            return false;
        }

        // 检查输出文件夹是否与任何输入文件所在文件夹相同
        for (ImageFile imageFile : imageFiles) {
            File inputFolder = new File(imageFile.getFilePath()).getParentFile();
            if (inputFolder.equals(outputFolder)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 加载完整尺寸的图片
     */
    public Image loadFullSizeImage(ImageFile imageFile) {
        try {
            File file = new File(imageFile.getFilePath());
            return new Image("file:" + file.getAbsolutePath());
        } catch (Exception e) {
            log.error("加载完整尺寸图片失败: {}", imageFile.getFilePath(), e);
            return null;
        }
    }

    /**
     * 检查图片文件是否存在
     */
    public boolean validateImageFiles() {
        List<ImageFile> invalidFiles = new ArrayList<>();

        for (ImageFile imageFile : imageFiles) {
            File file = new File(imageFile.getFilePath());
            if (!file.exists()) {
                invalidFiles.add(imageFile);
            }
        }

        if (!invalidFiles.isEmpty()) {
            imageFiles.removeAll(invalidFiles);
            log.warn("移除 {} 个无效的图片文件", invalidFiles.size());
            return false;
        }

        return true;
    }
}