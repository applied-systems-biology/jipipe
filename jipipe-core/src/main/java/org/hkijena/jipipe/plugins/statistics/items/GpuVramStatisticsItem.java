package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItemCategory;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;
import org.hkijena.jipipe.utils.HardwareDetector;

public class GpuVramStatisticsItem implements JIPipeStatisticsItem {
    @Override
    public String getId() { return "gpu-vram-total-mb"; }
    @Override
    public String getName() { return "GPU VRAM total (MB)"; }
    @Override
    public String getDescription() { return "Total GPU VRAM in megabytes"; }
    @Override
    public JIPipeStatisticsItemCategory getCategory() { return JIPipeStatisticsItemCategory.Machine; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.Everything; }
    @Override
    public JsonNode serialize() {
        return IntNode.valueOf((int) HardwareDetector.detect().getGpuVramTotalMB());
    }
    @Override
    public void deserialize(JsonNode node) { }
    @Override
    public void reset() { }
}
