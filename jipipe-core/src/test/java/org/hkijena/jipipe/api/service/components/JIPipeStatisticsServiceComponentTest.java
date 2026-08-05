package org.hkijena.jipipe.api.service.components;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsRegistry;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JIPipeStatisticsServiceComponentTest {

    @TempDir
    Path tempDir;

    private JIPipeStatisticsItem createCounterItem(String id) {
        return new JIPipeStatisticsItem() {
            int value = 0;
            @Override public String getId() { return id; }
            @Override public String getName() { return id; }
            @Override public String getDescription() { return id; }
            @Override public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.RoughProjects; }
            @Override public JsonNode serialize() { return JsonNodeFactory.instance.numberNode(value); }
            @Override public void deserialize(JsonNode node) { if (node != null && !node.isNull()) value = node.asInt(); }
            @Override public void reset() { value = 0; }
        };
    }

    @Test
    void machineIdGeneratedOnFirstLoad() throws Exception {
        Path statsFile = tempDir.resolve("statistics.json");
        ObjectNode root = JIPipeStatisticsServiceComponent.loadOrCreate(statsFile, new JIPipeStatisticsRegistry());
        String machineId = root.get("machineId").asText();
        assertNotNull(machineId);
        assertFalse(machineId.isEmpty());
    }

    @Test
    void machineIdPersistedAcrossLoads() throws Exception {
        Path statsFile = tempDir.resolve("statistics.json");
        JIPipeStatisticsRegistry registry1 = new JIPipeStatisticsRegistry();
        ObjectNode root1 = JIPipeStatisticsServiceComponent.loadOrCreate(statsFile, registry1);
        String machineId1 = root1.get("machineId").asText();

        JIPipeStatisticsRegistry registry2 = new JIPipeStatisticsRegistry();
        ObjectNode root2 = JIPipeStatisticsServiceComponent.loadOrCreate(statsFile, registry2);
        String machineId2 = root2.get("machineId").asText();

        assertEquals(machineId1, machineId2);
    }

    @Test
    void itemsDeserializedOnLoad() throws Exception {
        Path statsFile = tempDir.resolve("statistics.json");
        JIPipeStatisticsItem counter = createCounterItem("counter");

        // First save with value 42
        JIPipeStatisticsRegistry registry1 = new JIPipeStatisticsRegistry();
        registry1.registerItem(counter);
        ObjectNode root1 = JIPipeStatisticsServiceComponent.loadOrCreate(statsFile, registry1);
        ObjectNode itemsNode = root1.putObject("items");
        itemsNode.put("counter", 42);
        JIPipeStatisticsServiceComponent.save(statsFile, root1);
        counter.deserialize(itemsNode.get("counter"));
        assertEquals(42, counter.serialize().asInt());

        // Second load should restore value
        JIPipeStatisticsRegistry registry2 = new JIPipeStatisticsRegistry();
        JIPipeStatisticsItem counter2 = createCounterItem("counter");
        registry2.registerItem(counter2);
        ObjectNode root2 = JIPipeStatisticsServiceComponent.loadOrCreate(statsFile, registry2);
        JsonNode items2 = root2.get("items");
        assertNotNull(items2);
        counter2.deserialize(items2.get("counter"));
        assertEquals(42, counter2.serialize().asInt());
    }
}
