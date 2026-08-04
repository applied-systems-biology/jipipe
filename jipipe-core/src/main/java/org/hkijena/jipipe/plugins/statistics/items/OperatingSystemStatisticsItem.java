package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItemCategory;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;

public class OperatingSystemStatisticsItem implements JIPipeStatisticsItem {
    @Override
    public String getId() { return "operating-system"; }
    @Override
    public String getName() { return "Operating system"; }
    @Override
    public String getDescription() { return "The operating system name and version"; }
    @Override
    public JIPipeStatisticsItemCategory getCategory() { return JIPipeStatisticsItemCategory.Machine; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.Installation; }
    @Override
    public JsonNode serialize() {
        String os = System.getProperty("os.name") + " " + System.getProperty("os.version");
        return TextNode.valueOf(os);
    }
    @Override
    public void deserialize(JsonNode node) { }
    @Override
    public void reset() { }
}
