package com.puxinheihei.controller;

import com.puxinheihei.entity.ImageFile;
import com.puxinheihei.entity.WatermarkConfig;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.ResourceBundle;

@Slf4j
public class WatermarkController implements Initializable {

    @FXML private TextField watermarkText;
    @FXML private ComboBox<String> fontFamilyComboBox;
    @FXML private Spinner<Integer> fontSizeSpinner;
    @FXML private ColorPicker textColorPicker;
    @FXML private Slider textOpacitySlider;
    @FXML private Label textOpacityLabel;

    // 位置控件
    @FXML private TextField positionXField;
    @FXML private TextField positionYField;

    private MainController mainController;
    private WatermarkConfig watermarkConfig;
    private Stage stage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            setupFontFamilies();
            setupSpinners();
            setupSliders();
            setupEventHandlers();

        } catch (Exception e) {
            log.error("初始化水印控制器失败", e);
        }
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setWatermarkConfig(WatermarkConfig config) {
        this.watermarkConfig = config;
        loadConfigToUI();
    }

    public WatermarkConfig getWatermarkConfig() {
        return watermarkConfig;
    }

    private void setupFontFamilies() {
        // 添加支持中文的字体
        fontFamilyComboBox.getItems().addAll(
                "Microsoft YaHei",  // 微软雅黑 - 支持中文
                "SimSun",           // 宋体 - 支持中文
                "Arial",
                "Times New Roman",
                "Courier New",
                "Verdana"
        );
        fontFamilyComboBox.setValue("Microsoft YaHei"); // 默认使用微软雅黑
    }

    private void setupSpinners() {
        // 字体大小微调器
        SpinnerValueFactory.IntegerSpinnerValueFactory fontSizeFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 2000, 48);
        fontSizeSpinner.setValueFactory(fontSizeFactory);
    }

    private void setupSliders() {
        // 文本透明度滑块
        textOpacitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            textOpacityLabel.setText(String.format("%d%%", (int)(newVal.doubleValue() * 100)));
        });
    }

    private void setupEventHandlers() {
        // 设置舞台引用
        watermarkText.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                stage = (Stage) newScene.getWindow();
            }
        });
    }

    private void loadConfigToUI() {
        if (watermarkConfig == null) return;

        try {
            // 文本设置
            watermarkText.setText(watermarkConfig.getText());
            if (watermarkConfig.getFontFamily() != null) {
                // 验证字体是否存在
                boolean fontExists = fontFamilyComboBox.getItems().contains(watermarkConfig.getFontFamily());
                if (fontExists) {
                    fontFamilyComboBox.setValue(watermarkConfig.getFontFamily());
                } else {
                    fontFamilyComboBox.setValue("Microsoft YaHei");
                    System.out.println("字体 '" + watermarkConfig.getFontFamily() + "' 不存在，使用微软雅黑");
                }
            }
            if (watermarkConfig.getFontSize() > 0) {
                fontSizeSpinner.getValueFactory().setValue((int) watermarkConfig.getFontSize());
            }
            if (watermarkConfig.getTextColor() != null) {
                textColorPicker.setValue(watermarkConfig.getTextColor());
            }
            if (watermarkConfig.getTextOpacity() > 0) {
                textOpacitySlider.setValue(watermarkConfig.getTextOpacity());
            }

            // 位置设置
            if (watermarkConfig.getCustomX() > 0) {
                positionXField.setText(String.valueOf((int) watermarkConfig.getCustomX()));
            }
            if (watermarkConfig.getCustomY() > 0) {
                positionYField.setText(String.valueOf((int) watermarkConfig.getCustomY()));
            }

        } catch (Exception e) {
            log.error("加载配置到UI失败", e);
        }
    }

    private void saveUIToConfig() {
        if (watermarkConfig == null) {
            watermarkConfig = new WatermarkConfig();
        }

        try {
            // 文本配置
            watermarkConfig.setText(watermarkText.getText());
            watermarkConfig.setFontFamily(fontFamilyComboBox.getValue());

            // 修复字体大小保存问题
            try {
                Integer fontSizeValue = fontSizeSpinner.getValue();
                if (fontSizeValue != null) {
                    watermarkConfig.setFontSize(fontSizeValue);
                    System.out.println("保存字体大小: " + fontSizeValue);
                }
            } catch (Exception e) {
                System.out.println("字体大小保存异常，使用默认值");
                watermarkConfig.setFontSize(48);
            }

            watermarkConfig.setTextColor(textColorPicker.getValue());
            watermarkConfig.setTextOpacity(textOpacitySlider.getValue());

            // 位置配置
            if (watermarkConfig.getPosition() == null) {
                watermarkConfig.setPosition(WatermarkConfig.Position.BOTTOM_RIGHT);
            }

            // 自定义位置
            try {
                if (!positionXField.getText().isEmpty()) {
                    watermarkConfig.setCustomX(Double.parseDouble(positionXField.getText()));
                }
                if (!positionYField.getText().isEmpty()) {
                    watermarkConfig.setCustomY(Double.parseDouble(positionYField.getText()));
                }
            } catch (NumberFormatException e) {
                log.warn("位置坐标格式错误");
            }

            System.out.println("配置已保存 - 字体大小: " + watermarkConfig.getFontSize() + ", 位置: " + watermarkConfig.getPosition());

        } catch (Exception e) {
            log.error("保存UI到配置失败", e);
        }
    }

    // 位置按钮处理
    @FXML private void handlePositionTopLeft() {
        setPresetPosition(WatermarkConfig.Position.TOP_LEFT);
        positionXField.setText("");
        positionYField.setText("");
    }
    @FXML private void handlePositionTopCenter() {
        setPresetPosition(WatermarkConfig.Position.TOP_CENTER);
        positionXField.setText("");
        positionYField.setText("");
    }
    @FXML private void handlePositionTopRight() {
        setPresetPosition(WatermarkConfig.Position.TOP_RIGHT);
        positionXField.setText("");
        positionYField.setText("");
    }
    @FXML private void handlePositionMiddleLeft() {
        setPresetPosition(WatermarkConfig.Position.MIDDLE_LEFT);
        positionXField.setText("");
        positionYField.setText("");
    }
    @FXML private void handlePositionMiddleCenter() {
        setPresetPosition(WatermarkConfig.Position.MIDDLE_CENTER);
        positionXField.setText("");
        positionYField.setText("");
    }
    @FXML private void handlePositionMiddleRight() {
        setPresetPosition(WatermarkConfig.Position.MIDDLE_RIGHT);
        positionXField.setText("");
        positionYField.setText("");
    }
    @FXML private void handlePositionBottomLeft() {
        setPresetPosition(WatermarkConfig.Position.BOTTOM_LEFT);
        positionXField.setText("");
        positionYField.setText("");
    }
    @FXML private void handlePositionBottomCenter() {
        setPresetPosition(WatermarkConfig.Position.BOTTOM_CENTER);
        positionXField.setText("");
        positionYField.setText("");
    }
    @FXML private void handlePositionBottomRight() {
        setPresetPosition(WatermarkConfig.Position.BOTTOM_RIGHT);
        positionXField.setText("");
        positionYField.setText("");
    }

    private void setPresetPosition(WatermarkConfig.Position position) {
        if (watermarkConfig != null) {
            watermarkConfig.setPosition(position);
            System.out.println("位置设置为: " + position);
        }
    }

    @FXML
    private void handleResetPosition() {
        positionXField.setText("");
        positionYField.setText("");
        if (watermarkConfig != null) {
            watermarkConfig.setPosition(WatermarkConfig.Position.BOTTOM_RIGHT);
        }
    }

    @FXML
    private void handleApply() {
        saveUIToConfig();
        if (mainController != null) {
            // 修复变量名冲突 - 使用不同的变量名
            ImageFile currentSelectedImage = mainController.getSelectedImage();
            if (currentSelectedImage != null) {
                // 将配置保存到当前选中的图片
                currentSelectedImage.setWatermarkConfig(watermarkConfig);
            }
            // 更新主控制器的配置
            mainController.updateWatermarkConfig(watermarkConfig);

            // 手动触发水印应用
            mainController.handleApplyWatermark();
        }
        closeDialog();
    }

    @FXML
    private void handleCancel() {
        watermarkConfig = null; // 表示取消操作
        closeDialog();
    }

    private void closeDialog() {
        if (stage != null) {
            stage.close();
        }
    }

    /**
     * 更新位置字段显示
     */
    public void updatePositionFields(double x, double y) {
        if (positionXField != null && positionYField != null) {
            positionXField.setText(String.valueOf((int) x));
            positionYField.setText(String.valueOf((int) y));
        }
    }
}