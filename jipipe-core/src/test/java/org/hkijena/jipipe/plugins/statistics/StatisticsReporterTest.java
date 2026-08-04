package org.hkijena.jipipe.plugins.statistics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.utils.VersionUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsReporterTest {

    private JIPipeStatisticsItem createItem(String id, StatisticsPrivacyLevel level, int value) {
        return new JIPipeStatisticsItem() {
            @Override public String getId() { return id; }
            @Override public String getName() { return id; }
            @Override public String getDescription() { return id; }
            @Override public JIPipeStatisticsItemCategory getCategory() { return JIPipeStatisticsItemCategory.Machine; }
            @Override public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return level; }
            @Override public JsonNode serialize() { return IntNode.valueOf(value); }
            @Override public void deserialize(JsonNode node) {}
            @Override public void reset() {}
        };
    }

    @Test
    void buildPayload_filtersByPrivacyLevel() {
        JIPipeStatisticsRegistry registry = new JIPipeStatisticsRegistry();
        registry.registerItem(createItem("a", StatisticsPrivacyLevel.Installation, 1));
        registry.registerItem(createItem("b", StatisticsPrivacyLevel.Everything, 2));

        ObjectNode payload = StatisticsReporter.buildPayload(
                registry, StatisticsPrivacyLevel.Installation, "test-machine-id");

        assertEquals("test-machine-id", payload.get("machineId").asText());
        assertEquals(VersionUtils.getJIPipeVersion(), payload.get("jipipeVersion").asText());
        assertNotNull(payload.get("timestamp"));
        assertEquals("INSTALLATION", payload.get("privacyLevel").asText());

        JsonNode items = payload.get("items");
        assertTrue(items.has("a"));
        assertFalse(items.has("b"));
    }

    @Test
    void buildPayload_noneLevel_includesNoItems() {
        JIPipeStatisticsRegistry registry = new JIPipeStatisticsRegistry();
        registry.registerItem(createItem("a", StatisticsPrivacyLevel.Installation, 1));

        ObjectNode payload = StatisticsReporter.buildPayload(
                registry, StatisticsPrivacyLevel.None, "test-machine-id");

        JsonNode items = payload.get("items");
        assertFalse(items.has("a"));
    }
}
