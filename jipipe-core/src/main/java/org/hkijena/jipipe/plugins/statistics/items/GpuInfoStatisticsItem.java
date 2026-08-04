package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItemCategory;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

public class GpuInfoStatisticsItem implements JIPipeStatisticsItem {
    @Override
    public String getId() { return "gpu-info"; }
    @Override
    public String getName() { return "GPU information"; }
    @Override
    public String getDescription() { return "Available graphics devices"; }
    @Override
    public JIPipeStatisticsItemCategory getCategory() { return JIPipeStatisticsItemCategory.Machine; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.Everything; }
    @Override
    public JsonNode serialize() {
        GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < devices.length; i++) {
            if (i > 0) sb.append("; ");
            sb.append(devices[i].getIDstring());
        }
        return TextNode.valueOf(sb.toString());
    }
    @Override
    public void deserialize(JsonNode node) { }
    @Override
    public void reset() { }
}
