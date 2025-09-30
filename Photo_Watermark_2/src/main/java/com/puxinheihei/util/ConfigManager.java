package com.puxinheihei.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.puxinheihei.entity.WatermarkConfig;
import com.puxinheihei.entity.WatermarkTemplate;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
public class ConfigManager {

    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.photo_watermark_2";
    private static final String TEMPLATES_FILE = CONFIG_DIR + "/templates.json";
    private static final String LAST_CONFIG_FILE = CONFIG_DIR + "/last_config.json";

    private static final ObjectMapper objectMapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 注册Java 8时间模块
        mapper.registerModule(new JavaTimeModule());
        // 配置Jackson忽略未知属性
        mapper.findAndRegisterModules();
        return mapper;
    }

    static {
        // 确保配置目录存在
        try {
            Files.createDirectories(Paths.get(CONFIG_DIR));
        } catch (Exception e) {
            log.error("Error creating config directory", e);
        }
    }

    public static void saveTemplate(WatermarkTemplate template) {
        try {
            List<WatermarkTemplate> templates = loadAllTemplates();

            // 移除同名的旧模板
            templates.removeIf(t -> t.getName().equals(template.getName()));

            // 添加新模板
            template.setUpdateTime(new Date().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
            templates.add(template);

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(TEMPLATES_FILE), templates);
        } catch (Exception e) {
            log.error("Error saving template", e);
        }
    }

    public static List<WatermarkTemplate> loadAllTemplates() {
        try {
            File templatesFile = new File(TEMPLATES_FILE);
            if (templatesFile.exists()) {
                WatermarkTemplate[] templates = objectMapper.readValue(templatesFile, WatermarkTemplate[].class);
                return new ArrayList<>(Arrays.asList(templates));
            }
        } catch (Exception e) {
            log.error("Error loading templates", e);
        }
        return new ArrayList<>();
    }

    public static boolean deleteTemplate(String templateName) {
        try {
            List<WatermarkTemplate> templates = loadAllTemplates();
            boolean removed = templates.removeIf(t -> t.getName().equals(templateName));

            if (removed) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(TEMPLATES_FILE), templates);
                return true;
            }
        } catch (Exception e) {
            log.error("Error deleting template", e);
        }
        return false;
    }

    public static void saveLastConfig(WatermarkConfig config) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(LAST_CONFIG_FILE), config);
        } catch (Exception e) {
            log.error("Error saving last config", e);
        }
    }

    public static WatermarkConfig loadLastConfig() {
        try {
            File configFile = new File(LAST_CONFIG_FILE);
            if (configFile.exists()) {
                return objectMapper.readValue(configFile, WatermarkConfig.class);
            }
        } catch (Exception e) {
            log.error("Error loading last config", e);
        }
        return createDefaultConfig();
    }

    private static WatermarkConfig createDefaultConfig() {
        WatermarkConfig config = new WatermarkConfig();
        return config;
    }
}