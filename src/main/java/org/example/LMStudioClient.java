package org.example;

// AI-assisted: LM Studio integration
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Talks to LM Studio's OpenAI-compatible endpoint: POST {baseUrl}/v1/chat/completions.
 */
public class LMStudioClient {
    public static final String DEFAULT_URL = "http://localhost:1234";
    public static final String DEFAULT_MODEL = "local-model";

    private final String baseUrl;
    private final String model;
    private final HttpClient http;

    public LMStudioClient() {
        this(DEFAULT_URL, DEFAULT_MODEL);
    }

    public LMStudioClient(String baseUrl, String model) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model = model;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    public String getBaseUrl() { return baseUrl; }
    public String getModel() { return model; }

    public LMResponse send(List<Message> messages) throws LMException {
        String body = buildBody(messages);
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
        } catch (IllegalArgumentException e) {
            throw new LMException("Bad server URL: " + baseUrl, e);
        }

        long start = System.nanoTime();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LMException("Request was interrupted", e);
        } catch (Exception e) {
            throw new LMException("Cannot reach LM Studio at " + baseUrl
                    + " (is the local server running?): " + e.getClass().getSimpleName(), e);
        }
        double seconds = (System.nanoTime() - start) / 1_000_000_000.0;

        if (response.statusCode() != 200) {
            throw new LMException("LM Studio answered HTTP " + response.statusCode() + ": " + shorten(response.body()));
        }
        return parse(response.body(), seconds);
    }

    private String buildBody(List<Message> messages) {
        StringBuilder json = new StringBuilder();
        json.append("{\"model\":").append(Json.quote(model)).append(",\"stream\":false,\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);
            if (i > 0) json.append(',');
            json.append("{\"role\":").append(Json.quote(m.getRole()))
                .append(",\"content\":").append(Json.quote(m.getContent())).append('}');
        }
        return json.append("]}").toString();
    }

    @SuppressWarnings("unchecked")
    private LMResponse parse(String body, double seconds) throws LMException {
        try {
            Map<String, Object> root = (Map<String, Object>) Json.parse(body);
            List<Object> choices = (List<Object>) root.get("choices");
            Map<String, Object> first = (Map<String, Object>) choices.get(0);
            Map<String, Object> message = (Map<String, Object>) first.get("message");
            Object content = message.get("content");
            if (!(content instanceof String)) throw new LMException("The answer has no text content");

            int promptTokens = 0;
            int completionTokens = 0;
            if (root.get("usage") instanceof Map<?, ?> usage) {
                if (usage.get("prompt_tokens") instanceof Double d) promptTokens = d.intValue();
                if (usage.get("completion_tokens") instanceof Double d) completionTokens = d.intValue();
            }
            return new LMResponse((String) content, promptTokens, completionTokens, seconds);
        } catch (LMException e) {
            throw e;
        } catch (RuntimeException e) { // bad JSON, missing fields, wrong types
            throw new LMException("The answer cannot be read: " + shorten(body), e);
        }
    }

    private String shorten(String s) {
        if (s == null) return "(empty)";
        s = s.replaceAll("\\s+", " ").trim();
        return s.length() <= 120 ? s : s.substring(0, 120) + "...";
    }
}
