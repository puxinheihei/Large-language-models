package com.puxinheihei.service;

import com.puxinheihei.entity.WatermarkConfig;
import com.puxinheihei.entity.WatermarkTemplate;
import com.puxinheihei.util.ConfigManager;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
public class TemplateService {

    /**
     * 保存水印模板
     */
    public boolean saveTemplate(String name, WatermarkConfig config) {
        try {
            if (name == null || name.trim().isEmpty()) {
                log.error("模板名称为空");
                return false;
            }

            if (config == null) {
                log.error("水印配置为空");
                return false;
            }

            // 创建模板对象
            WatermarkTemplate template = new WatermarkTemplate();
            template.setName(name.trim());

            // 深拷贝配置对象，避免引用问题
            WatermarkConfig configCopy = deepCopyConfig(config);
            template.setConfig(configCopy);

            LocalDateTime now = LocalDateTime.now();
            template.setCreateTime(now);
            template.setUpdateTime(now);

            // 保存到配置管理器
            ConfigManager.saveTemplate(template);
            log.info("成功保存模板: {}", name);
            return true;

        } catch (Exception e) {
            log.error("保存模板失败: {}", name, e);
            return false;
        }
    }

    /**
     * 获取所有模板
     */
    public List<WatermarkTemplate> getAllTemplates() {
        try {
            List<WatermarkTemplate> templates = ConfigManager.loadAllTemplates();
            log.debug("加载 {} 个模板", templates.size());
            return templates;
        } catch (Exception e) {
            log.error("获取模板列表失败", e);
            return List.of();
        }
    }

    /**
     * 根据名称获取模板
     */
    public WatermarkTemplate getTemplateByName(String name) {
        try {
            return getAllTemplates().stream()
                    .filter(template -> template.getName().equals(name))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.error("根据名称获取模板失败: {}", name, e);
            return null;
        }
    }

    /**
     * 删除模板
     */
    public boolean deleteTemplate(String name) {
        try {
            boolean success = ConfigManager.deleteTemplate(name);
            if (success) {
                log.info("成功删除模板: {}", name);
            } else {
                log.warn("删除模板失败，模板可能不存在: {}", name);
            }
            return success;
        } catch (Exception e) {
            log.error("删除模板失败: {}", name, e);
            return false;
        }
    }

    /**
     * 更新模板
     */
    public boolean updateTemplate(String name, WatermarkConfig newConfig) {
        try {
            // 先删除旧模板，再保存新模板
            deleteTemplate(name);
            return saveTemplate(name, newConfig);
        } catch (Exception e) {
            log.error("更新模板失败: {}", name, e);
            return false;
        }
    }

    /**
     * 检查模板是否存在
     */
    public boolean templateExists(String name) {
        return getTemplateByName(name) != null;
    }

    /**
     * 深拷贝水印配置
     */
    private WatermarkConfig deepCopyConfig(WatermarkConfig original) {
        try {
            WatermarkConfig copy = new WatermarkConfig();

            // 复制文本水印配置
            if (original.getText() != null) {
                copy.setText(new String(original.getText()));
            }
            copy.setFontFamily(original.getFontFamily());
            copy.setFontSize(original.getFontSize());
            if (original.getTextColor() != null) {
                // 复制JavaFX Color
                javafx.scene.paint.Color originalColor = original.getTextColor();
                copy.setTextColor(javafx.scene.paint.Color.color(
                        originalColor.getRed(),
                        originalColor.getGreen(),
                        originalColor.getBlue(),
                        originalColor.getOpacity()
                ));
            }
            copy.setTextOpacity(original.getTextOpacity());

            // 复制位置配置
            copy.setPosition(original.getPosition());
            copy.setCustomX(original.getCustomX());
            copy.setCustomY(original.getCustomY());

            return copy;

        } catch (Exception e) {
            log.error("深拷贝水印配置失败", e);
            // 如果深拷贝失败，返回原始对象（虽然不理想，但比空指针好）
            return original;
        }
    }

    /**
     * 验证模板名称是否有效
     */
    public boolean isValidTemplateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        // 检查是否包含非法字符
        String trimmedName = name.trim();
        return !trimmedName.contains("/") &&
                !trimmedName.contains("\\") &&
                !trimmedName.contains(":") &&
                !trimmedName.contains("*") &&
                !trimmedName.contains("?") &&
                !trimmedName.contains("\"") &&
                !trimmedName.contains("<") &&
                !trimmedName.contains(">") &&
                !trimmedName.contains("|");
    }

    /**
     * 获取默认模板
     */
    public WatermarkTemplate getDefaultTemplate() {
        WatermarkConfig defaultConfig = new WatermarkConfig();

        WatermarkTemplate template = new WatermarkTemplate();
        template.setName("默认模板");
        template.setConfig(defaultConfig);
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());

        return template;
    }
}