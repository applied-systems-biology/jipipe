package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.hkijena.jipipe.api.service.components.JIPipeStatisticsServiceComponent;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItemCategory;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;

public class MachineIdStatisticsItem implements JIPipeStatisticsItem {
    private JIPipeStatisticsServiceComponent service;

    @Override
    public String getId() { return "machine-id"; }
    @Override
    public String getName() { return "Machine ID"; }
    @Override
    public String getDescription() { return "A unique identifier for this machine"; }
    @Override
    public JIPipeStatisticsItemCategory getCategory() { return JIPipeStatisticsItemCategory.Machine; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.Installation; }
    @Override
    public JsonNode serialize() {
        String id = service != null ? service.getMachineId() : "";
        return TextNode.valueOf(id != null ? id : "");
    }
    @Override
    public void deserialize(JsonNode node) { }
    @Override
    public void reset() { }
    @Override
    public void initialize(JIPipeStatisticsServiceComponent service) {
        this.service = service;
    }
}
