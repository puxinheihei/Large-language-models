package com.puxinheihei.util;

import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class FileUtils {

    private static final List<String> SUPPORTED_IMAGE_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "bmp", "tiff", "tif"
    );

    public static boolean isImageFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }

        String fileName = file.getName().toLowerCase();
        String extension = getFileExtension(fileName);

        return SUPPORTED_IMAGE_EXTENSIONS.contains(extension);
    }

    public static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

    public static List<File> getImageFilesFromFolder(File folder) {
        try {
            return Files.walk(Paths.get(folder.toURI()))
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(FileUtils::isImageFile)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error reading folder: {}", folder.getAbsolutePath(), e);
            return List.of();
        }
    }

    public static String generateOutputFileName(String originalName, String prefix, String suffix, String outputFormat) {
        String nameWithoutExt = originalName.substring(0, originalName.lastIndexOf('.'));
        return prefix + nameWithoutExt + suffix + "." + outputFormat.toLowerCase();
    }

    public static boolean isValidOutputFolder(File outputFolder, List<File> inputFiles) {
        if (outputFolder == null || !outputFolder.exists() || !outputFolder.isDirectory()) {
            return false;
        }

        // 检查输出文件夹是否与任何输入文件所在文件夹相同
        for (File inputFile : inputFiles) {
            if (inputFile.getParentFile().equals(outputFolder)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 将JavaFX Color转换为AWT Color
     */
    private static java.awt.Color convertFxColorToAwt(javafx.scene.paint.Color fxColor) {
        if (fxColor == null) {
            return java.awt.Color.BLACK; // 默认黑色
        }
        return new java.awt.Color(
                (float) fxColor.getRed(),
                (float) fxColor.getGreen(),
                (float) fxColor.getBlue(),
                (float) fxColor.getOpacity()
        );
    }
}