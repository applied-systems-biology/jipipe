package org.hkijena.jipipe.plugins.statistics;

import com.fasterxml.jackson.databind.node.IntNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JIPipeStatisticsRegistryTest {

    private JIPipeStatisticsItem createItem(String id, StatisticsPrivacyLevel level, JIPipeStatisticsItemCategory category) {
        return new JIPipeStatisticsItem() {
            @Override
            public String getId() { return id; }
            @Override
            public String getName() { return id; }
            @Override
            public String getDescription() { return id; }
            @Override
            public JIPipeStatisticsItemCategory getCategory() { return category; }
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
        JIPipeStatisticsItem item = createItem("test-item", StatisticsPrivacyLevel.Installation, JIPipeStatisticsItemCategory.Machine);
        registry.registerItem(item);
        assertSame(item, registry.getItem("test-item"));
    }

    @Test
    void getItemsForLevel_filtersByRequiredLevel() {
        JIPipeStatisticsRegistry registry = new JIPipeStatisticsRegistry();
        registry.registerItem(createItem("a", StatisticsPrivacyLevel.Installation, JIPipeStatisticsItemCategory.Machine));
        registry.registerItem(createItem("b", StatisticsPrivacyLevel.Everything, JIPipeStatisticsItemCategory.Fun));
        registry.registerItem(createItem("c", StatisticsPrivacyLevel.RoughProjects, JIPipeStatisticsItemCategory.Usage));

        List<JIPipeStatisticsItem> result = registry.getItemsForLevel(StatisticsPrivacyLevel.RoughProjects);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(i -> i.getId().equals("a")));
        assertTrue(result.stream().anyMatch(i -> i.getId().equals("c")));
        assertFalse(result.stream().anyMatch(i -> i.getId().equals("b")));
    }

    @Test
    void getItemsByCategory_groupsCorrectly() {
        JIPipeStatisticsRegistry registry = new JIPipeStatisticsRegistry();
        registry.registerItem(createItem("a", StatisticsPrivacyLevel.Installation, JIPipeStatisticsItemCategory.Machine));
        registry.registerItem(createItem("b", StatisticsPrivacyLevel.Everything, JIPipeStatisticsItemCategory.Fun));
        registry.registerItem(createItem("c", StatisticsPrivacyLevel.Installation, JIPipeStatisticsItemCategory.Machine));

        Map<JIPipeStatisticsItemCategory, List<JIPipeStatisticsItem>> grouped = registry.getItemsByCategory();
        assertEquals(2, grouped.get(JIPipeStatisticsItemCategory.Machine).size());
        assertEquals(1, grouped.get(JIPipeStatisticsItemCategory.Fun).size());
    }

    @Test
    void registerItem_duplicateId_throws() {
        JIPipeStatisticsRegistry registry = new JIPipeStatisticsRegistry();
        registry.registerItem(createItem("dup", StatisticsPrivacyLevel.Installation, JIPipeStatisticsItemCategory.Machine));
        assertThrows(IllegalArgumentException.class, () ->
                registry.registerItem(createItem("dup", StatisticsPrivacyLevel.Installation, JIPipeStatisticsItemCategory.Machine)));
    }
}
