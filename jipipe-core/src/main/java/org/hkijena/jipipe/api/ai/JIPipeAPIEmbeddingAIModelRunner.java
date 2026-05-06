package org.hkijena.jipipe.api.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class JIPipeAPIEmbeddingAIModelRunner implements JIPipeEmbeddingAIModelRunner {
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;

    private final String apiBase;
    private final String apiModel;
    private final String apiKey;
    private JIPipeAIModelRunnerStatus status = JIPipeAIModelRunnerStatus.Unloaded;
    private String lastError = null;
    private HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JIPipeAPIEmbeddingAIModelRunner(String apiBase, String apiModel, String apiKey) {
        this.apiBase = apiBase;
        this.apiModel = apiModel;
        this.apiKey = apiKey;
    }

    @Override
    public float[] embed(String text) {
        if (status != JIPipeAIModelRunnerStatus.Idle) {
            throw new RuntimeException("Model runner is not idle. Current status: " + status);
        }
        status = JIPipeAIModelRunnerStatus.Busy;
        try {
            String requestBody = objectMapper.writeValueAsString(new java.util.HashMap<>() {{
                put("model", apiModel);
                put("input", java.util.Collections.singletonList(text));
            }});

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(buildEmbeddingsUrl())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));

            if (apiKey != null && !apiKey.isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + apiKey);
            }

            HttpRequest request = requestBuilder.build();

            Exception lastException = null;
            for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
                try {
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        JsonNode root = objectMapper.readTree(response.body());
                        JsonNode embeddingNode = root.get("data").get(0).get("embedding");

                        float[] result = new float[embeddingNode.size()];
                        for (int i = 0; i < embeddingNode.size(); i++) {
                            result[i] = (float) embeddingNode.get(i).asDouble();
                        }

                        status = JIPipeAIModelRunnerStatus.Idle;
                        return result;
                    }

                    // Retry on 429 (rate limit) and 5xx (server error)
                    if ((response.statusCode() == 429 || response.statusCode() >= 500) && attempt < MAX_RETRIES) {
                        long backoff = INITIAL_BACKOFF_MS * (1L << attempt);
                        Thread.sleep(backoff);
                        continue;
                    }

                    throw new RuntimeException("API request failed with status " + response.statusCode() + ": " + response.body());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Embedding interrupted during retry", e);
                }
            }

            throw new RuntimeException("API request failed after " + MAX_RETRIES + " retries: " + (lastException != null ? lastException.getMessage() : "unknown error"));
        } catch (Exception e) {
            lastError = e.getMessage();
            status = JIPipeAIModelRunnerStatus.Failed;
            throw new RuntimeException("Embedding failed: " + e.getMessage(), e);
        }
    }

    @Override
    public JIPipeAIModelRunnerStatus getStatus() {
        return status;
    }

    @Override
    public String getLastError() {
        return lastError;
    }

    @Override
    public void start() {
        try {
            if (apiBase == null || apiBase.isBlank()) {
                throw new IllegalStateException("API base URL is not configured");
            }
            if (apiModel == null || apiModel.isBlank()) {
                throw new IllegalStateException("API model is not configured");
            }
            status = JIPipeAIModelRunnerStatus.Loading;
            httpClient = HttpClient.newHttpClient();
            status = JIPipeAIModelRunnerStatus.Idle;
        } catch (Exception e) {
            lastError = e.getMessage();
            status = JIPipeAIModelRunnerStatus.Failed;
        }
    }

    private URI buildEmbeddingsUrl() {
        String base = apiBase;
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/v1")) {
            return URI.create(base + "/embeddings");
        } else {
            return URI.create(base + "/v1/embeddings");
        }
    }

    @Override
    public void shutdown() {
        try {
            status = JIPipeAIModelRunnerStatus.Unloading;
            httpClient = null;
            status = JIPipeAIModelRunnerStatus.Unloaded;
        } catch (Exception e) {
            lastError = e.getMessage();
            status = JIPipeAIModelRunnerStatus.Failed;
        }
    }
}
