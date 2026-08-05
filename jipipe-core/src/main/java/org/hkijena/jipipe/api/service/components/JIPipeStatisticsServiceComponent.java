package org.hkijena.jipipe.api.service.components;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsRegistry;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;
import org.hkijena.jipipe.plugins.statistics.StatisticsReporter;
import org.hkijena.jipipe.plugins.statistics.settings.JIPipeStatisticsApplicationSettings;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

import javax.swing.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class JIPipeStatisticsServiceComponent extends JIPipeServiceComponent {
    private static final Logger logger = LoggerFactory.getLogger(JIPipeStatisticsServiceComponent.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final JIPipeStatisticsRegistry registry = new JIPipeStatisticsRegistry();
    private ObjectNode statisticsData;
    private final Timer saveLaterTimer;
    private Timer reportingTimer;

    public JIPipeStatisticsServiceComponent(JIPipeService service) {
        super(service);
        this.saveLaterTimer = new Timer(250, e -> save());
        this.saveLaterTimer.setRepeats(false);
    }

    public JIPipeStatisticsRegistry getRegistry() {
        return registry;
    }

    public Path getStatisticsFile() {
        return JIPipe.getJIPipeUserDir(false).resolve("statistics.json");
    }

    public Path getLockFile() {
        return JIPipe.getJIPipeUserDir(false).resolve("statistics.lock");
    }

    public String getMachineId() {
        if (statisticsData != null && statisticsData.has("machineId")) {
            return statisticsData.get("machineId").asText();
        }
        return null;
    }

    public void rerollMachineId() {
        if (statisticsData != null) {
            statisticsData.put("machineId", UUID.randomUUID().toString());
            save();
        }
    }

    public LocalDateTime getFirstLaunchTimestamp() {
        if (statisticsData != null && statisticsData.has("firstLaunchTimestamp")) {
            return LocalDateTime.parse(statisticsData.get("firstLaunchTimestamp").asText(), FORMATTER);
        }
        return null;
    }

    public LocalDateTime getLastSentTimestamp() {
        if (statisticsData != null && statisticsData.has("lastSentTimestamp") && !statisticsData.get("lastSentTimestamp").isNull()) {
            return LocalDateTime.parse(statisticsData.get("lastSentTimestamp").asText(), FORMATTER);
        }
        return null;
    }

    public void setLastSentTimestamp(LocalDateTime timestamp) {
        if (statisticsData != null) {
            statisticsData.put("lastSentTimestamp", timestamp.format(FORMATTER));
            saveLater();
        }
    }

    public void saveLater() {
        saveLaterTimer.restart();
    }

    public void save() {
        Path file = getStatisticsFile();
        Path lockFile = getLockFile();
        try {
            PathUtils.ensureParentDirectoriesExist(file);
            if (!Files.exists(lockFile)) {
                Files.createFile(lockFile);
            }
            try (FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.WRITE);
                 FileLock lock = channel.tryLock()) {
                if (lock == null) {
                    logger.warn("Could not acquire statistics lock — save skipped, will retry");
                    saveLaterTimer.restart();
                    return;
                }
                ObjectMapper mapper = JsonUtils.getObjectMapper();
                ObjectNode currentOnDisk;
                if (Files.isRegularFile(file)) {
                    currentOnDisk = (ObjectNode) mapper.readTree(file.toFile());
                } else {
                    currentOnDisk = mapper.createObjectNode();
                }

                ObjectNode itemsOnDisk = currentOnDisk.has("items")
                        ? (ObjectNode) currentOnDisk.get("items")
                        : currentOnDisk.putObject("items");
                ObjectNode ourItems = statisticsData.has("items")
                        ? (ObjectNode) statisticsData.get("items")
                        : statisticsData.putObject("items");
                ourItems.fields().forEachRemaining(entry -> itemsOnDisk.set(entry.getKey(), entry.getValue()));
                currentOnDisk.set("items", itemsOnDisk);

                if (statisticsData.has("machineId")) {
                    currentOnDisk.put("machineId", statisticsData.get("machineId").asText());
                }
                if (statisticsData.has("lastSentTimestamp")) {
                    currentOnDisk.set("lastSentTimestamp", statisticsData.get("lastSentTimestamp"));
                }
                if (statisticsData.has("firstLaunchTimestamp")) {
                    currentOnDisk.put("firstLaunchTimestamp", statisticsData.get("firstLaunchTimestamp").asText());
                }
                if (statisticsData.has("history")) {
                    currentOnDisk.set("history", statisticsData.get("history"));
                }

                statisticsData = currentOnDisk;

                Path tmpFile = file.resolveSibling(file.getFileName() + ".tmp");
                mapper.writeValue(tmpFile.toFile(), currentOnDisk);
                Files.move(tmpFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            logger.error("Failed to save statistics to {}", file, e);
        }
    }

    public static void save(Path file, ObjectNode data) {
        try {
            PathUtils.ensureParentDirectoriesExist(file);
            Path tmpFile = file.resolveSibling(file.getFileName() + ".tmp");
            JsonUtils.getObjectMapper().writeValue(tmpFile.toFile(), data);
            Files.move(tmpFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            logger.error("Failed to save statistics to {}", file, e);
        }
    }

    public static ObjectNode loadOrCreate(Path file, JIPipeStatisticsRegistry registry) {
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        ObjectNode root;
        if (Files.isRegularFile(file)) {
            try {
                root = (ObjectNode) mapper.readTree(file.toFile());
            } catch (Exception e) {
                logger.error("Failed to load statistics from {}, creating new", file, e);
                root = mapper.createObjectNode();
            }
        } else {
            root = mapper.createObjectNode();
        }

        boolean createdMachineId = !root.has("machineId") || root.get("machineId").asText().isEmpty();
        boolean createdFirstLaunch = !root.has("firstLaunchTimestamp");
        boolean createdLastSent = !root.has("lastSentTimestamp");

        if (createdMachineId) {
            root.put("machineId", UUID.randomUUID().toString());
        }
        if (createdFirstLaunch) {
            root.put("firstLaunchTimestamp", LocalDateTime.now().format(FORMATTER));
        }
        if (createdLastSent) {
            root.putNull("lastSentTimestamp");
        }
        if (!root.has("items")) {
            root.putObject("items");
        }
        if (!root.has("history")) {
            root.putObject("history");
        }

        JsonNode itemsNode = root.get("items");
        for (var item : registry.getItems()) {
            JsonNode itemData = itemsNode.get(item.getId());
            if (itemData != null) {
                item.deserialize(itemData);
            }
        }

        if (createdMachineId || createdFirstLaunch || createdLastSent) {
            save(file, root);
        }

        return root;
    }

    @Override
    public void postprocess(JIPipeProgressInfo progressInfo) {
        statisticsData = loadOrCreate(getStatisticsFile(), registry);
        for (var item : registry.getItems()) {
            item.initialize(this);
        }

        // Start daily reporting timer (1-hour tick)
        reportingTimer = new Timer(60 * 60 * 1000, e -> checkAndSend());
        reportingTimer.setRepeats(true);
        reportingTimer.start();

        // Check on startup
        checkAndSend();
    }

    private void checkAndSend() {
        var settings = JIPipeStatisticsApplicationSettings.getInstance();
        if (!settings.isEnabled() || settings.getPrivacyLevel() == StatisticsPrivacyLevel.None) {
            return;
        }

        LocalDateTime lastSent = getLastSentTimestamp();
        if (lastSent == null || lastSent.plusHours(24).isBefore(LocalDateTime.now())) {
            StatisticsReporter.sendNow(success -> {
                if (success) {
                    logger.info("Statistics sent successfully");
                    SwingUtilities.invokeLater(this::sampleHistory);
                } else {
                    logger.info("Failed to send statistics (will retry later)");
                }
            });
        }
    }

    public ObjectNode getHistory() {
        if (statisticsData == null) {
            return null;
        }
        if (!statisticsData.has("history")) {
            statisticsData.putObject("history");
        }
        return (ObjectNode) statisticsData.get("history");
    }

    public void sampleHistory() {
        if (statisticsData == null) {
            return;
        }
        ObjectNode history = getHistory();
        String timestamp = LocalDateTime.now().format(FORMATTER);

        for (var item : registry.getItems()) {
            if (!item.isTimeTracked()) {
                continue;
            }
            JsonNode value = item.serialize();
            ArrayNode itemHistory;
            if (history.has(item.getId())) {
                itemHistory = (ArrayNode) history.get(item.getId());
            } else {
                itemHistory = history.putArray(item.getId());
            }
            ObjectNode dataPoint = itemHistory.addObject();
            dataPoint.put("timestamp", timestamp);
            dataPoint.set("value", value);

            while (itemHistory.size() > 90) {
                itemHistory.remove(0);
            }
        }

        statisticsData.set("history", history);
        saveLater();
    }

    public void sendNow(Consumer<Boolean> callback) {
        StatisticsReporter.sendNow(callback);
    }
}
