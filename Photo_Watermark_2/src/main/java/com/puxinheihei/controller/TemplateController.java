package com.puxinheihei.controller;

import com.puxinheihei.entity.WatermarkTemplate;
import com.puxinheihei.service.TemplateService;
import com.puxinheihei.util.ConfigManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.ResourceBundle;

@Slf4j
public class TemplateController implements Initializable {

    @FXML private TextField templateNameField;
    @FXML private TableView<WatermarkTemplate> templatesTableView;

    private MainController mainController;
    private TemplateService templateService;
    private boolean forLoading = false;
    private Stage stage;



    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            templateService = new TemplateService();
            setupTableView();
            refreshTemplates();

            // 修复stage引用问题
            templateNameField.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    stage = (Stage) newScene.getWindow();
                    // 如果forLoading已设置，现在可以安全设置标题
                    if (forLoading) {
                        stage.setTitle("加载模板");
                    }
                }
            });

        } catch (Exception e) {
            log.error("初始化模板控制器失败", e);
        }
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setForLoading(boolean forLoading) {
        this.forLoading = forLoading;
        // 只有当stage不为null时才设置标题
        if (stage != null && forLoading) {
            stage.setTitle("加载模板");
        }
    }

    private void setupTableView() {
        // 设置行选择监听
        templatesTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null && forLoading) {
                        // 如果是加载模式，选择后自动加载
                        handleLoadTemplate();
                    }
                }
        );
    }

    @FXML
    private void handleSaveTemplate() {
        try {
            String templateName = templateNameField.getText().trim();
            if (templateName.isEmpty()) {
                showError("请输入模板名称");
                return;
            }

            if (mainController == null || mainController.getCurrentConfig() == null) {
                showError("没有可保存的水印配置");
                return;
            }

            boolean success = templateService.saveTemplate(templateName, mainController.getCurrentConfig());
            if (success) {
                templateNameField.clear();
                refreshTemplates();
                showInfo("保存成功", "模板 '" + templateName + "' 保存成功");
            } else {
                showError("保存模板失败");
            }

        } catch (Exception e) {
            log.error("保存模板失败", e);
            showError("保存模板失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleLoadTemplate() {
        try {
            WatermarkTemplate selectedTemplate = templatesTableView.getSelectionModel().getSelectedItem();
            if (selectedTemplate == null) {
                showWarning("请先选择一个模板");
                return;
            }

            if (mainController != null) {
                mainController.updateWatermarkConfig(selectedTemplate.getConfig());
                showInfo("加载成功", "模板 '" + selectedTemplate.getName() + "' 加载成功");

                if (forLoading) {
                    // 如果是加载模式，加载后自动关闭
                    closeDialog();
                }
            }

        } catch (Exception e) {
            log.error("加载模板失败", e);
            showError("加载模板失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteTemplate() {
        try {
            WatermarkTemplate selectedTemplate = templatesTableView.getSelectionModel().getSelectedItem();
            if (selectedTemplate == null) {
                showWarning("请先选择一个模板");
                return;
            }

            // 确认删除
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("确认删除");
            confirmAlert.setHeaderText("删除模板");
            confirmAlert.setContentText("确定要删除模板 '" + selectedTemplate.getName() + "' 吗？");

            if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                boolean success = templateService.deleteTemplate(selectedTemplate.getName());
                if (success) {
                    refreshTemplates();
                    showInfo("删除成功", "模板 '" + selectedTemplate.getName() + "' 已删除");
                } else {
                    showError("删除模板失败");
                }
            }

        } catch (Exception e) {
            log.error("删除模板失败", e);
            showError("删除模板失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        closeDialog();
    }

    private void refreshTemplates() {
        try {
            templatesTableView.getItems().setAll(templateService.getAllTemplates());
        } catch (Exception e) {
            log.error("刷新模板列表失败", e);
            showError("刷新模板列表失败: " + e.getMessage());
        }
    }

    private void closeDialog() {
        if (stage != null) {
            stage.close();
        }
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
}