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

import javax.swing.*;
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

    /**
     * System property that disables all statistics sending. Intended for CI and
     * test environments, where every {@code JIPipe.ensureInstance()} would
     * otherwise report a fresh machine ID and create false installation surges
     * on the statistics server.
     */
    public static final String DISABLED_PROPERTY = "jipipe.statistics.disabled";

    /**
     * Environment variable that disables all statistics sending. Presence with
     * any value disables sending (same presence semantics as the
     * {@code JIPIPE_OVERRIDE_*} variables).
     */
    public static final String DISABLED_ENVIRONMENT_VARIABLE = "JIPIPE_DISABLE_STATISTICS";

    /**
     * @return true if statistics sending is disabled via {@link #DISABLED_PROPERTY}
     * or {@link #DISABLED_ENVIRONMENT_VARIABLE}
     */
    public static boolean isSendingDisabled() {
        return isSendingDisabled(System.getProperty(DISABLED_PROPERTY), System.getenv(DISABLED_ENVIRONMENT_VARIABLE));
    }

    static boolean isSendingDisabled(String systemPropertyValue, String environmentVariableValue) {
        if (systemPropertyValue != null && systemPropertyValue.equalsIgnoreCase("true")) {
            return true;
        }
        return environmentVariableValue != null;
    }

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
        if (isSendingDisabled()) {
            if (callback != null) callback.accept(false);
            return;
        }

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
                    SwingUtilities.invokeLater(() -> service.setLastSentTimestamp(LocalDateTime.now()));
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
