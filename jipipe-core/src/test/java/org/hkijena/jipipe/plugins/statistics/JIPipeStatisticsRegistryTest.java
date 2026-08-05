package org.hkijena.jipipe.plugins.statistics;

import com.fasterxml.jackson.databind.node.IntNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JIPipeStatisticsRegistryTest {

    private JIPipeStatisticsItem createItem(String id, StatisticsPrivacyLevel level) {
        return new JIPipeStatisticsItem() {
            @Override
            public String getId() { return id; }
            @Override
            public String getName() { return id; }
            @Override
            public String getDescription() { return id; }
            @Override
            public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return level; }
            @Override
            public com.fasterxml.jackson.databind.JsonNode serialize() { return IntNode.valueOf(42); }
            @Override
            public void deserialize(com.fasterxml.jackson.databind.JsonNode node) {}
            @Override
            public void reset() {}
        };
    }

    @Test
    void registerAndRetrieveItem() {
        JIPipeStatisticsRegistry registry = new JIPipeStatisticsRegistry();
        JIPipeStatisticsItem item = createItem("test-item", StatisticsPrivacyLevel.Installation);
        registry.registerItem(item);
        assertSame(item, registry.getItem("test-item"));
    }

    @Test
    void getItemsForLevel_filtersByRequiredLevel() {
        JIPipeStatisticsRegistry registry = new JIPipeStatisticsRegistry();
        registry.registerItem(createItem("a", StatisticsPrivacyLevel.Installation));
        registry.registerItem(createItem("b", StatisticsPrivacyLevel.Everything));
        registry.registerItem(createItem("c", StatisticsPrivacyLevel.RoughProjects));

        List<JIPipeStatisticsItem> result = registry.getItemsForLevel(StatisticsPrivacyLevel.RoughProjects);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(i -> i.getId().equals("a")));
        assertTrue(result.stream().anyMatch(i -> i.getId().equals("c")));
        assertFalse(result.stream().anyMatch(i -> i.getId().equals("b")));
    }

    @Test
    void registerItem_duplicateId_throws() {
        JIPipeStatisticsRegistry registry = new JIPipeStatisticsRegistry();
        registry.registerItem(createItem("dup", StatisticsPrivacyLevel.Installation));
        assertThrows(IllegalArgumentException.class, () ->
                registry.registerItem(createItem("dup", StatisticsPrivacyLevel.Installation)));
    }
}
