package org.hkijena.jipipe.plugins.statistics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.service.components.JIPipeStatisticsServiceComponent;
import org.hkijena.jipipe.plugins.statistics.settings.JIPipeStatisticsApplicationSettings;
import org.hkijena.jipipe.utils.VersionUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class StatisticsReporter {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsReporter.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static ObjectNode buildPayload(JIPipeStatisticsRegistry registry,
                                          StatisticsPrivacyLevel level,
                                          String machineId) {
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        ObjectNode payload = mapper.createObjectNode();
        payload.put("machineId", machineId != null ? machineId : "");
        payload.put("jipipeVersion", VersionUtils.getJIPipeVersion());
        payload.put("timestamp", LocalDateTime.now().format(FORMATTER));
        payload.put("privacyLevel", level.name().toUpperCase());

        ObjectNode items = payload.putObject("items");
        if (level != StatisticsPrivacyLevel.None) {
            for (JIPipeStatisticsItem item : registry.getItemsForLevel(level)) {
                JsonNode itemData = item.serialize();
                if (itemData != null) {
                    items.set(item.getId(), itemData);
                }
            }
        }
        return payload;
    }

    public static void sendNow(Consumer<Boolean> callback) {
        JIPipeStatisticsApplicationSettings settings = JIPipeStatisticsApplicationSettings.getInstance();
        if (!settings.isEnabled() || settings.getPrivacyLevel() == StatisticsPrivacyLevel.None) {
            if (callback != null) callback.accept(false);
            return;
        }

        JIPipeStatisticsServiceComponent service = JIPipe.getInstance().getStatistics();
        ObjectNode payload = buildPayload(service.getRegistry(), settings.getPrivacyLevel(), service.getMachineId());
        String json = JsonUtils.toJsonString(payload);
        String url = settings.getServerUrl();

        Thread thread = new Thread(() -> {
            try (HttpClient client = HttpClient.newHttpClient()) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
                if (success) {
                    service.setLastSentTimestamp(LocalDateTime.now());
                }
                if (callback != null) callback.accept(success);
            } catch (Exception e) {
                logger.warn("Failed to send statistics to {}", url, e);
                if (callback != null) callback.accept(false);
            }
        }, "Statistics-Reporter");
        thread.setDaemon(true);
        thread.start();
    }
}
