package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItemCategory;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;

public class TotalRamStatisticsItem implements JIPipeStatisticsItem {
    @Override
    public String getId() { return "total-ram-mb"; }
    @Override
    public String getName() { return "Total RAM (MB)"; }
    @Override
    public String getDescription() { return "Total system RAM in megabytes"; }
    @Override
    public JIPipeStatisticsItemCategory getCategory() { return JIPipeStatisticsItemCategory.Machine; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.Installation; }
    @Override
    public JsonNode serialize() {
        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long ramMb = Math.max(maxMemory, totalMemory) / (1024 * 1024);
        return IntNode.valueOf((int) ramMb);
    }
    @Override
    public void deserialize(JsonNode node) { }
    @Override
    public void reset() { }
}
