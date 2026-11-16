package org.hkijena.jipipe.plugins.statistics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.utils.VersionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsReporterTest {

    private JIPipeStatisticsItem createItem(String id, StatisticsPrivacyLevel level, int value) {
        return new JIPipeStatisticsItem() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public String getName() {
                return id;
            }

            @Override
            public String getDescription() {
                return id;
            }

            @Override
            public StatisticsPrivacyLevel getRequiredPrivacyLevel() {
                return level;
            }

            @Override
            public JsonNode serialize() {
                return IntNode.valueOf(value);
            }

            @Override
            public void deserialize(JsonNode node) {
            }

            @Override
            public void reset() {
            }
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

    @AfterEach
    void clearDisableProperty() {
        System.clearProperty(StatisticsReporter.DISABLED_PROPERTY);
    }

    @Test
    void sendingDisabled_whenSystemPropertyTrue() {
        assertTrue(StatisticsReporter.isSendingDisabled("true", null));
        assertTrue(StatisticsReporter.isSendingDisabled("TRUE", null));
    }

    @Test
    void sendingEnabled_whenSystemPropertyAbsentOrNotTrue() {
        assertFalse(StatisticsReporter.isSendingDisabled(null, null));
        assertFalse(StatisticsReporter.isSendingDisabled("false", null));
        assertFalse(StatisticsReporter.isSendingDisabled("0", null));
    }

    @Test
    void sendingDisabled_whenEnvironmentVariablePresent() {
        assertTrue(StatisticsReporter.isSendingDisabled(null, "1"));
        assertTrue(StatisticsReporter.isSendingDisabled("false", "0"));
    }

    @Test
    void sendNow_disabledBySystemProperty_reportsFalseWithoutSending() {
        System.setProperty(StatisticsReporter.DISABLED_PROPERTY, "true");
        AtomicReference<Boolean> received = new AtomicReference<>();
        StatisticsReporter.sendNow(received::set);
        assertEquals(Boolean.FALSE, received.get());
    }
}
