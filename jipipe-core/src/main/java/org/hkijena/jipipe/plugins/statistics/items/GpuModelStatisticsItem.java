package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;
import org.hkijena.jipipe.utils.HardwareDetector;

public class GpuModelStatisticsItem implements JIPipeStatisticsItem {
    @Override
    public String getId() { return "gpu-model"; }
    @Override
    public String getName() { return "GPU model"; }
    @Override
    public String getDescription() { return "The GPU model name"; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.Everything; }
    @Override
    public JsonNode serialize() {
        return TextNode.valueOf(HardwareDetector.detect().getGpuType());
    }
    @Override
    public void deserialize(JsonNode node) { }
    @Override
    public void reset() { }
}
