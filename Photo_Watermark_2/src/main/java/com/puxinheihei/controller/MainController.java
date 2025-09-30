package com.puxinheihei.controller;

import com.puxinheihei.entity.ImageFile;
import com.puxinheihei.entity.WatermarkConfig;
import com.puxinheihei.service.ImageService;
import com.puxinheihei.service.WatermarkService;
import com.puxinheihei.service.TemplateService;
import com.puxinheihei.util.ConfigManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Slf4j
public class MainController implements Initializable {

    // 添加这个字段
    private WatermarkController watermarkController;

    @FXML private BorderPane previewPane;
    @FXML private Label previewPlaceholder;
    @FXML private VBox imageListContainer;
    @FXML private Label imageCountLabel;
    @FXML private Label statusLabel;
    @FXML private ComboBox<String> outputFormatComboBox;
    @FXML private TextField prefixField;
    @FXML private TextField suffixField;

    private ImageService imageService;
    private WatermarkService watermarkService;
    private TemplateService templateService;

    private WatermarkConfig currentConfig;
    private ImageFile selectedImage;
    private ImageView previewImageView;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            // 初始化服务
            imageService = new ImageService();
            watermarkService = new WatermarkService();
            templateService = new TemplateService();

            // 暂时注释配置加载
            // currentConfig = ConfigManager.loadLastConfig();
            currentConfig = new WatermarkConfig(); // 使用默认配置

            // 初始化预览区域
            previewImageView = new ImageView();
            previewImageView.setPreserveRatio(true);
            previewImageView.setSmooth(true);
            previewImageView.setCache(true);

            // 初始化输出格式
            outputFormatComboBox.getItems().addAll("JPEG", "PNG");
            outputFormatComboBox.setValue("JPEG");

            // 初始化前缀后缀
            prefixField.setText("wm_");
            suffixField.setText("_watermarked");

            // 初始化预览区域大小监听
            initializePreviewResizeListener();

            updateStatus("就绪");
            updateImageCount();

        } catch (Exception e) {
            log.error("初始化主控制器失败", e);
            showError("初始化失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleImportImages() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择图片文件");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("图片文件", "*.jpg", "*.jpeg", "*.png", "*.bmp", "*.tiff", "*.tif"),
                    new FileChooser.ExtensionFilter("所有文件", "*.*")
            );

            List<File> files = fileChooser.showOpenMultipleDialog(getStage());
            if (files != null && !files.isEmpty()) {
                imageService.addImageFiles(files);
                refreshImageList();
                updateStatus("成功导入 " + files.size() + " 张图片");
            }
        } catch (Exception e) {
            log.error("导入图片失败", e);
            showError("导入图片失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleImportFolder() {
        try {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("选择图片文件夹");

            File folder = directoryChooser.showDialog(getStage());
            if (folder != null && folder.isDirectory()) {
                List<File> imageFiles = imageService.scanImageFilesFromFolder(folder);
                if (!imageFiles.isEmpty()) {
                    imageService.addImageFiles(imageFiles);
                    refreshImageList();
                    updateStatus("成功从文件夹导入 " + imageFiles.size() + " 张图片");
                } else {
                    showWarning("所选文件夹中没有找到支持的图片文件");
                }
            }
        } catch (Exception e) {
            log.error("导入文件夹失败", e);
            showError("导入文件夹失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleExportImages() {
        if (imageService.getImageFiles().isEmpty()) {
            showWarning("请先导入图片");
            return;
        }

        try {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("选择输出文件夹");

            File outputDir = directoryChooser.showDialog(getStage());
            if (outputDir != null) {
                if (!imageService.isValidOutputFolder(outputDir)) {
                    showError("不能将图片导出到原文件夹，请选择其他文件夹");
                    return;
                }

                String outputFormat = outputFormatComboBox.getValue();
                String prefix = prefixField.getText();
                String suffix = suffixField.getText();

                int successCount = watermarkService.exportImages(
                        imageService.getImageFiles(),
                        outputDir,
                        currentConfig,
                        outputFormat,
                        prefix,
                        suffix
                );

                updateStatus("成功导出 " + successCount + " 张图片到 " + outputDir.getAbsolutePath());
                showInfo("导出完成", "成功导出 " + successCount + " 张图片");
            }
        } catch (Exception e) {
            log.error("导出图片失败", e);
            showError("导出图片失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleExportAll() {
        handleExportImages();
    }

    @FXML
    private void handleApplyWatermark() {
        if (selectedImage == null) {
            showWarning("请先选择一张图片");
            return;
        }

        try {
            // 应用水印到预览图
            Image watermarkedImage = watermarkService.applyWatermarkToPreview(selectedImage, currentConfig);
            if (watermarkedImage != null) {
                previewImageView.setImage(watermarkedImage);
                previewPane.setCenter(previewImageView);
                adjustPreviewSize();
                updateStatus("水印应用成功");
            }
        } catch (Exception e) {
            log.error("应用水印失败", e);
            showError("应用水印失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleTextWatermark() {
        openWatermarkSettingsDialog();
    }

    @FXML
    private void handleSaveTemplate() {
        try {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("保存模板");
            dialog.setHeaderText("请输入模板名称");
            dialog.setContentText("名称:");

            String templateName = dialog.showAndWait().orElse(null);
            if (templateName != null && !templateName.trim().isEmpty()) {
                templateService.saveTemplate(templateName.trim(), currentConfig);
                updateStatus("模板 '" + templateName + "' 保存成功");
            }
        } catch (Exception e) {
            log.error("保存模板失败", e);
            showError("保存模板失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleLoadTemplate() {
        openTemplateManagementDialog(true);
    }

    @FXML
    private void handleManageTemplates() {
        openTemplateManagementDialog(false);
    }

    @FXML
    private void handleClearList() {
        imageService.clearImageFiles();
        refreshImageList();
        clearPreview();
        updateStatus("已清空图片列表");
    }

    @FXML
    private void handleExit() {
        // 保存当前配置
        ConfigManager.saveLastConfig(currentConfig);
        getStage().close();
    }

    private void openWatermarkSettingsDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/watermark.fxml"));
            Parent root = loader.load();

            WatermarkController controller = loader.getController();
            controller.setMainController(this);
            controller.setWatermarkConfig(currentConfig);

            // 保存引用
            this.watermarkController = controller;

            Stage stage = new Stage();
            stage.setTitle("水印设置");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(getStage());
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            // 更新配置
            WatermarkConfig newConfig = controller.getWatermarkConfig();
            if (newConfig != null) {
                currentConfig = newConfig;
                ConfigManager.saveLastConfig(currentConfig);
                updateStatus("水印设置已更新");

                // 重新应用水印到预览
                if (selectedImage != null) {
                    handleApplyWatermark();
                }
            }

        } catch (Exception e) {
            log.error("打开水印设置对话框失败", e);
            showError("打开水印设置失败: " + e.getMessage());
        }
    }

    private void openTemplateManagementDialog(boolean forLoading) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/template.fxml"));
            Parent root = loader.load();

            TemplateController controller = loader.getController();
            controller.setMainController(this);
            controller.setForLoading(forLoading);

            Stage stage = new Stage();
            stage.setTitle("模板管理");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(getStage());
            stage.setScene(new Scene(root, 600, 400));
            stage.showAndWait();

        } catch (Exception e) {
            log.error("打开模板管理对话框失败", e);
            showError("打开模板管理失败: " + e.getMessage());
        }
    }

    private void refreshImageList() {
        imageListContainer.getChildren().clear();

        for (ImageFile imageFile : imageService.getImageFiles()) {
            VBox imageItem = createImageListItem(imageFile);
            imageListContainer.getChildren().add(imageItem);
        }

        updateImageCount();
    }

    private VBox createImageListItem(ImageFile imageFile) {
        VBox item = new VBox(5);
        item.getStyleClass().add("image-item");
        item.setPrefWidth(280);

        // 缩略图
        ImageView thumbnailView = new ImageView(imageFile.getThumbnail());
        thumbnailView.setFitWidth(120);
        thumbnailView.setFitHeight(80);
        thumbnailView.setPreserveRatio(true);

        // 文件名
        Label fileNameLabel = new Label(imageFile.getFileName());
        fileNameLabel.setStyle("-fx-font-weight: bold;");

        // 文件信息
        Label fileInfoLabel = new Label(
                String.format("%.1f KB", imageFile.getFileSize() / 1024.0)
        );
        fileInfoLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 10px;");

        item.getChildren().addAll(thumbnailView, fileNameLabel, fileInfoLabel);

        // 点击事件
        item.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                selectImageItem(item, imageFile);
            }
        });

        return item;
    }

    private void selectImageItem(VBox item, ImageFile imageFile) {
        // 清除之前的选择
        for (var child : imageListContainer.getChildren()) {
            child.getStyleClass().remove("selected");
        }

        // 设置当前选择
        item.getStyleClass().add("selected");
        selectedImage = imageFile;

        // 显示预览
        showImagePreview(imageFile);
        updateStatus("已选择: " + imageFile.getFileName());

        // 强制调整一次大小
        Platform.runLater(() -> {
            adjustPreviewSize();
        });
    }

    private void showImagePreview(ImageFile imageFile) {
        try {
            // 先清除之前的图片
            previewImageView.setImage(null);

            // 直接加载图片
            String imagePath = "file:" + imageFile.getFilePath();
            System.out.println("加载图片路径: " + imagePath);

            Image image = new Image(imagePath);

            if (image.isError()) {
                Throwable exception = image.getException();
                if (exception != null) {
                    log.error("图片加载错误: {}", exception.getMessage());
                    showError("图片加载错误: " + exception.getMessage());
                } else {
                    log.error("未知图片加载错误");
                    showError("无法加载图片");
                }
                previewPane.setCenter(previewPlaceholder);
                return;
            }

            // 设置图片到ImageView
            previewImageView.setImage(image);
            previewPane.setCenter(previewImageView);

            // 添加鼠标拖拽事件
            setupDragHandling();

            // 立即调整预览图大小
            adjustPreviewSize();

            updateStatus("预览加载完成: " + imageFile.getFileName());

        } catch (Exception e) {
            log.error("加载预览图失败: {}", imageFile.getFilePath(), e);
            showError("加载预览图失败: " + imageFile.getFileName());
            previewPane.setCenter(previewPlaceholder);
        }
    }

    /**
     * 设置拖拽处理
     */
    private void setupDragHandling() {
        previewImageView.setOnMousePressed(event -> {
            if (event.isPrimaryButtonDown()) {
                // 开始拖拽
                double mouseX = event.getX();
                double mouseY = event.getY();

                // 计算相对于原图的位置
                double scaleX = previewImageView.getImage().getWidth() / previewImageView.getFitWidth();
                double scaleY = previewImageView.getImage().getHeight() / previewImageView.getFitHeight();

                int imageX = (int) (mouseX * scaleX);
                int imageY = (int) (mouseY * scaleY);

                // 设置自定义位置
                currentConfig.setPosition(WatermarkConfig.Position.CUSTOM);
                currentConfig.setCustomX(imageX);
                currentConfig.setCustomY(imageY);

                // 更新位置显示
                Platform.runLater(() -> {
                    if (watermarkController != null) {
                        watermarkController.updatePositionFields(imageX, imageY);
                    }
                });

                System.out.println("设置水印位置: " + imageX + ", " + imageY);

                // 立即应用水印
                handleApplyWatermark();
            }
        });
    }

    /**
     * 调整预览图大小以适应预览区域
     */
    private void adjustPreviewSize() {
        if (previewImageView.getImage() == null) return;

        Image image = previewImageView.getImage();
        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();

        // 获取预览区域的实际大小，确保不会出现负值
        double paneWidth = Math.max(100, previewPane.getWidth() - 40); // 最小宽度100
        double paneHeight = Math.max(100, previewPane.getHeight() - 40); // 最小高度100

        System.out.println("图片尺寸: " + imageWidth + "x" + imageHeight);
        System.out.println("预览区域: " + paneWidth + "x" + paneHeight);

        // 如果图片尺寸为0，使用默认大小
        if (imageWidth <= 0 || imageHeight <= 0) {
            previewImageView.setFitWidth(400);
            previewImageView.setFitHeight(300);
            return;
        }

        // 计算缩放比例，确保比例为正数
        double scaleX = paneWidth / imageWidth;
        double scaleY = paneHeight / imageHeight;
        double scale = Math.min(scaleX, scaleY);

        // 确保缩放比例为正数且合理
        scale = Math.max(0.1, Math.min(scale, 2.0)); // 限制在0.1到2.0之间

        System.out.println("缩放比例: " + scale);

        // 设置缩放后的尺寸
        double fitWidth = imageWidth * scale;
        double fitHeight = imageHeight * scale;

        previewImageView.setFitWidth(fitWidth);
        previewImageView.setFitHeight(fitHeight);

        System.out.println("设置预览尺寸: " + fitWidth + "x" + fitHeight);
    }

    /**
     * 当预览区域大小改变时调整图片大小
     */
    public void initializePreviewResizeListener() {
        // 只监听预览区域大小变化，移除场景监听
        previewPane.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            Platform.runLater(() -> adjustPreviewSize());
        });

        previewPane.heightProperty().addListener((obs, oldHeight, newHeight) -> {
            Platform.runLater(() -> adjustPreviewSize());
        });
    }

    private void clearPreview() {
        previewImageView.setImage(null);
        previewPane.setCenter(previewPlaceholder);
        selectedImage = null;
    }

    private void updateImageCount() {
        int count = imageService.getImageFiles().size();
        imageCountLabel.setText("共 " + count + " 张图片");
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
        log.info("状态更新: {}", message);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("警告");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Stage getStage() {
        return (Stage) previewPane.getScene().getWindow();
    }

    // 供其他控制器调用的方法
    public void updateWatermarkConfig(WatermarkConfig config) {
        this.currentConfig = config;
        ConfigManager.saveLastConfig(config);

        if (selectedImage != null) {
            handleApplyWatermark();
        }
    }

    public WatermarkConfig getCurrentConfig() {
        return currentConfig;
    }

    public List<ImageFile> getImageFiles() {
        return imageService.getImageFiles();
    }
}