package com.puxinheihei.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private final Gaode gaode = new Gaode();
    private final Bailian bailian = new Bailian();
    private final Xunfei xunfei = new Xunfei();
    private final Cors cors = new Cors();

    public Gaode getGaode() { return gaode; }
    public Bailian getBailian() { return bailian; }
    public Xunfei getXunfei() { return xunfei; }
    public Cors getCors() { return cors; }

    public static class Gaode {
        private String webApiKey;
        private String baseUrl;
        public String getWebApiKey() { return webApiKey; }
        public void setWebApiKey(String webApiKey) { this.webApiKey = webApiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }

    public static class Bailian {
        private String apiKey;
        private String baseUrl;
        private String model; // e.g. qwen-plus/qwen-max
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }

    public static class Xunfei {
        private String apiKey;
        private String baseUrl;
        private String appId;
        private String apiSecret; // NEW: for WebSocket v2 authentication
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }
        public String getApiSecret() { return apiSecret; }
        public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }
    }

    public static class Cors {
        private String allowedOrigin;
        public String getAllowedOrigin() { return allowedOrigin; }
        public void setAllowedOrigin(String allowedOrigin) { this.allowedOrigin = allowedOrigin; }
    }
}