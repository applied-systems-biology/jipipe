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

    @Test
    void itemsHaveValidIconAndVariant() {
        JIPipeStatisticsRegistry registry = new JIPipeStatisticsRegistry();
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.MachineIdStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.JIPipeVersionStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.OperatingSystemStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.AccelerationStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.GpuModelStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.GpuVramStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.TotalRamStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.RecentProjectsCountStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.WorkflowRunsStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.RoCratesCreatedStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.LargestProjectNodesStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.LargestProjectCompartmentsStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.PopularNodesStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.NodeMoveDistanceStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.LongestNodeWidthStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.NoodleScoreStatisticsItem());

        for (JIPipeStatisticsItem item : registry.getItems()) {
            assertNotNull(item.getIcon32(), "Icon must not be null for " + item.getId());
            assertFalse(item.getIcon32().isEmpty(), "Icon must not be empty for " + item.getId());
            assertNotNull(item.getCardVariant(), "Variant must not be null for " + item.getId());
            assertTrue(item.getDefaultColumnSpan() > 0 && item.getDefaultColumnSpan() <= 12,
                    "Span must be 1-12 for " + item.getId());
        }
    }
}
