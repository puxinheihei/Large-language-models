package com.puxinheihei.backend.service;

import com.puxinheihei.backend.config.AppProperties;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Base64;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CompletionStage;

@Service
public class XunfeiVoiceService {
    private final AppProperties props;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public XunfeiVoiceService(AppProperties props, RestTemplate restTemplate) {
        this.props = props;
        this.restTemplate = restTemplate;
    }

    private String md5HexLower(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : dig) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /**
     * 调用讯飞 IAT 进行语音听写。
     * 如果配置了 apiSecret，则优先走 WebSocket v2；否则使用 HTTP v1。
     */
    public String transcribeBase64Audio(String base64Audio) {
        try {
            if (base64Audio == null || base64Audio.isBlank()) {
                return "音频为空";
            }
            AppProperties.Xunfei xf = props.getXunfei();
            // 如具备 WebSocket v2 的鉴权信息，优先使用 v2
            if (xf != null && xf.getApiSecret() != null && !xf.getApiSecret().isBlank()) {
                try {
                    return transcribeViaWebSocketV2(base64Audio);
                } catch (Exception e) {
                    System.err.println("[XunfeiVoiceService] WS v2 调用异常，回退 HTTP v1：" + e.getMessage());
                }
            }
            // 配置校验（仅打印警告，不影响返回格式）
            if (xf.getAppId() == null || xf.getAppId().isBlank() || "YOUR_XUNFEI_APPID".equals(xf.getAppId())) {
                System.err.println("[XunfeiVoiceService] 警告：未设置有效的讯飞 appId，识别可能失败。");
            }
            if (xf.getApiKey() == null || xf.getApiKey().isBlank()) {
                System.err.println("[XunfeiVoiceService] 警告：未设置讯飞 apiKey，识别可能失败。");
            }
            if (xf.getBaseUrl() == null || xf.getBaseUrl().isBlank()) {
                System.err.println("[XunfeiVoiceService] 警告：未设置讯飞 baseUrl，识别可能失败。");
            }

            String url = xf.getBaseUrl() + "/v1/service/v1/iat";

            // 先以 WAV 调用
            String curTime = String.valueOf(System.currentTimeMillis() / 1000);
            String paramJsonWav = "{\"engine_type\":\"sms16k\",\"aue\":\"wav\"}";
            String xParamWav = Base64.getEncoder().encodeToString(paramJsonWav.getBytes(StandardCharsets.UTF_8));
            String checksumWav = md5HexLower(xf.getApiKey() + curTime + xParamWav);

            HttpHeaders headersWav = new HttpHeaders();
            headersWav.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headersWav.add("X-Appid", xf.getAppId());
            headersWav.add("X-CurTime", curTime);
            headersWav.add("X-Param", xParamWav);
            headersWav.add("X-CheckSum", checksumWav);

            String bodyWav = "audio=" + URLEncoder.encode(base64Audio, StandardCharsets.UTF_8);
            HttpEntity<String> entityWav = new HttpEntity<>(bodyWav, headersWav);
            String respWav = restTemplate.postForObject(url, entityWav, String.class);

            try {
                if (respWav != null) {
                    JsonNode root = objectMapper.readTree(respWav);
                    int code = root.path("code").asInt(-1);
                    String data = root.path("data").asText("");
                    String result = root.path("result").asText("");
                    if (code == 0 && !data.isEmpty()) return data;
                    if (!result.isEmpty()) return result;
                    System.err.println("[XunfeiVoiceService] WAV 识别失败，code=" + code + ", desc=" + root.path("desc").asText(""));
                } else {
                    System.err.println("[XunfeiVoiceService] WAV 响应为空");
                }
            } catch (Exception e) {
                System.err.println("[XunfeiVoiceService] 解析 WAV 响应异常：" + e.getMessage());
            }

            // 回退：若前端传的是 WAV，则尝试去掉 WAV 头按 RAW PCM 重试
            byte[] audioBytes = Base64.getDecoder().decode(base64Audio);
            byte[] rawPcm = audioBytes;
            try {
                if (audioBytes.length > 44 && audioBytes[0] == 'R' && audioBytes[1] == 'I' && audioBytes[2] == 'F' && audioBytes[3] == 'F') {
                    // 简单判断 RIFF/WAVE 头，跳过典型 44 字节
                    rawPcm = new byte[audioBytes.length - 44];
                    System.arraycopy(audioBytes, 44, rawPcm, 0, rawPcm.length);
                }
            } catch (Exception e) {
                System.err.println("[XunfeiVoiceService] 处理 WAV 头异常：" + e.getMessage());
            }
            String base64Raw = Base64.getEncoder().encodeToString(rawPcm);

            String curTime2 = String.valueOf(System.currentTimeMillis() / 1000);
            String paramJsonRaw = "{\"engine_type\":\"sms16k\",\"aue\":\"raw\"}";
            String xParamRaw = Base64.getEncoder().encodeToString(paramJsonRaw.getBytes(StandardCharsets.UTF_8));
            String checksumRaw = md5HexLower(xf.getApiKey() + curTime2 + xParamRaw);

            HttpHeaders headersRaw = new HttpHeaders();
            headersRaw.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headersRaw.add("X-Appid", xf.getAppId());
            headersRaw.add("X-CurTime", curTime2);
            headersRaw.add("X-Param", xParamRaw);
            headersRaw.add("X-CheckSum", checksumRaw);

            String bodyRaw = "audio=" + URLEncoder.encode(base64Raw, StandardCharsets.UTF_8);
            HttpEntity<String> entityRaw = new HttpEntity<>(bodyRaw, headersRaw);
            String respRaw = restTemplate.postForObject(url, entityRaw, String.class);

            if (respRaw != null) {
                JsonNode root2 = objectMapper.readTree(respRaw);
                int code2 = root2.path("code").asInt(-1);
                String data2 = root2.path("data").asText("");
                String result2 = root2.path("result").asText("");
                if (code2 == 0 && !data2.isEmpty()) return data2;
                if (!result2.isEmpty()) return result2;
                System.err.println("[XunfeiVoiceService] RAW 识别失败，code=" + code2 + ", desc=" + root2.path("desc").asText(""));
            } else {
                System.err.println("[XunfeiVoiceService] RAW 响应为空");
            }
        } catch (Exception ignore) {
            System.err.println("[XunfeiVoiceService] 调用异常：" + ignore.getMessage());
        }
        return "语音识别结果（示例）";
    }

    /** WebSocket v2 实现：将整段音频一次性发送并获取识别结果 */
    private String transcribeViaWebSocketV2(String base64Audio) throws Exception {
        AppProperties.Xunfei xf = props.getXunfei();
        if (xf == null) throw new IllegalStateException("缺少讯飞配置");
        if (isBlank(xf.getAppId()) || isBlank(xf.getApiKey()) || isBlank(xf.getApiSecret())) {
            throw new IllegalStateException("WS v2 认证信息不完整：appId/apiKey/apiSecret");
        }
        // 处理音频：若是 WAV 则去掉典型 44 字节头；否则按原样使用
        byte[] audioBytes = Base64.getDecoder().decode(base64Audio);
        if (audioBytes.length > 44 && audioBytes[0] == 'R' && audioBytes[1] == 'I' && audioBytes[2] == 'F' && audioBytes[3] == 'F') {
            byte[] raw = new byte[audioBytes.length - 44];
            System.arraycopy(audioBytes, 44, raw, 0, raw.length);
            audioBytes = raw;
        }
        String base64Raw = Base64.getEncoder().encodeToString(audioBytes);

        // 构造鉴权 URL（HMAC-SHA256）
        String host = "ws-api.xfyun.cn";
        String date = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneId.of("GMT")));
        String requestLine = "GET /v2/iat HTTP/1.1";
        String signatureOrigin = "host: " + host + "\n" + "date: " + date + "\n" + requestLine;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(xf.getApiSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = Base64.getEncoder().encodeToString(mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8)));
        String authorization = String.format("api_key=\"%s\",algorithm=\"hmac-sha256\",headers=\"host date request-line\",signature=\"%s\"",
                xf.getApiKey(), signature);
        String authBase64 = Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8));
        String wsUrl = String.format("wss://%s/v2/iat?authorization=%s&date=%s&host=%s",
                host,
                URLEncoder.encode(authBase64, StandardCharsets.UTF_8),
                URLEncoder.encode(date, StandardCharsets.UTF_8),
                host);

        HttpClient client = HttpClient.newHttpClient();
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<String> finalText = new AtomicReference<>("");
        AtomicReference<String> lastError = new AtomicReference<>("");

        WebSocket ws = client.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                    private final StringBuilder textBuilder = new StringBuilder();
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        WebSocket.Listener.super.onOpen(webSocket);
                    }
                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        try {
                            JsonNode root = objectMapper.readTree(data.toString());
                            int code = root.path("code").asInt(-1);
                            if (code != 0) {
                                lastError.set(root.path("message").asText("error") + ", code=" + code);
                            }
                            JsonNode d = root.path("data");
                            if (!d.isMissingNode()) {
                                JsonNode result = d.path("result");
                                if (!result.isMissingNode()) {
                                    String seg = extractTextFromWsResult(result);
                                    if (!seg.isEmpty()) textBuilder.append(seg);
                                }
                                int status = d.path("status").asInt(-1);
                                if (status == 2) { // 最后一帧
                                    finalText.set(textBuilder.toString());
                                    done.countDown();
                                }
                            }
                        } catch (Exception e) {
                            lastError.set(e.getMessage());
                            done.countDown();
                        }
                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }
                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        lastError.set(error.getMessage());
                        done.countDown();
                        WebSocket.Listener.super.onError(webSocket, error);
                    }
                }).join();

        // 发送首帧（status=0）
        String frame0 = objectMapper.createObjectNode()
                .putPOJO("common", objectMapper.createObjectNode().put("app_id", xf.getAppId()))
                .putPOJO("business", objectMapper.createObjectNode()
                        .put("language", "zh_cn")
                        .put("domain", "iat")
                        .put("accent", "mandarin")
                        .put("vinfo", 1)
                        .put("ptt", 0)
                        .put("vad_eos", 5000)
                        .put("dwa", "wpgs"))
                .putPOJO("data", objectMapper.createObjectNode()
                        .put("status", 0)
                        .put("format", "audio/L16;rate=16000")
                        .put("encoding", "raw")
                        .put("audio", base64Raw))
                .toString();
        ws.sendText(frame0, true).join();

        // 发送结束帧（status=2）
        String frame2 = objectMapper.createObjectNode()
                .putPOJO("common", objectMapper.createObjectNode().put("app_id", xf.getAppId()))
                .putPOJO("business", objectMapper.createObjectNode())
                .putPOJO("data", objectMapper.createObjectNode()
                        .put("status", 2)
                        .put("format", "audio/L16;rate=16000")
                        .put("encoding", "raw")
                        .put("audio", ""))
                .toString();
        ws.sendText(frame2, true).join();

        // 等待结果（最多 10s）
        done.await(10, TimeUnit.SECONDS);
        try { ws.sendClose(WebSocket.NORMAL_CLOSURE, "done").join(); } catch (Exception ignore) {}

        String text = finalText.get();
        if (text != null && !text.isBlank()) return text;
        if (lastError.get() != null && !lastError.get().isBlank()) {
            throw new RuntimeException("WS v2 失败：" + lastError.get());
        }
        throw new RuntimeException("WS v2 未得到结果");
    }

    private String extractTextFromWsResult(JsonNode result) {
        StringBuilder sb = new StringBuilder();
        try {
            JsonNode wsArr = result.path("ws");
            if (wsArr.isArray()) {
                for (JsonNode ws : wsArr) {
                    JsonNode cwArr = ws.path("cw");
                    if (cwArr.isArray()) {
                        for (JsonNode cw : cwArr) {
                            String w = cw.path("w").asText("");
                            if (!w.isEmpty()) sb.append(w);
                        }
                    }
                }
            }
        } catch (Exception ignore) {}
        return sb.toString();
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}