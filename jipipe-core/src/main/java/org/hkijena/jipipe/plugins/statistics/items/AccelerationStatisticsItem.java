package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.hkijena.jipipe.api.system.SystemResources;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;
import org.hkijena.jipipe.utils.HardwareDetector;

public class AccelerationStatisticsItem implements JIPipeStatisticsItem {
    @Override
    public String getId() { return "acceleration"; }
    @Override
    public String getName() { return "Acceleration"; }
    @Override
    public String getDescription() { return "Hardware acceleration mode and CUDA version"; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.Installation; }
    @Override
    public JsonNode serialize() {
        SystemResources resources = HardwareDetector.detect();
        String mode = resources.getAccelerationMode();
        int cudaVersion = resources.getCudaMaxVersion();
        if ("CUDA".equals(mode) && cudaVersion > 0) {
            String versionStr = (cudaVersion / 10) + "." + (cudaVersion % 10);
            return TextNode.valueOf(mode + " " + versionStr);
        }
        return TextNode.valueOf(mode);
    }
    @Override
    public void deserialize(JsonNode node) { }
    @Override
    public void reset() { }
}
