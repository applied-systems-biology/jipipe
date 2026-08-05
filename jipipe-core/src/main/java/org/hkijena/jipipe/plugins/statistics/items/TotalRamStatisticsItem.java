package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;
import org.hkijena.jipipe.utils.HardwareDetector;

public class TotalRamStatisticsItem implements JIPipeStatisticsItem {
    @Override
    public String getId() { return "total-ram-mb"; }
    @Override
    public String getName() { return "Total RAM (MB)"; }
    @Override
    public String getDescription() { return "Total system RAM in megabytes"; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.Installation; }
    @Override
    public JsonNode serialize() {
        long ramMb = HardwareDetector.detect().getSystemRamTotalMB();
        return IntNode.valueOf((int) ramMb);
    }
    @Override
    public void deserialize(JsonNode node) { }
    @Override
    public void reset() { }
    @Override
    public boolean isTimeTracked() { return true; }

    @Override
    public String getIcon32() { return "actions/memory.png"; }
    @Override
    public org.hkijena.jipipe.desktop.commons.components.cards.JIPipeDesktopCardVariant getCardVariant() {
        return org.hkijena.jipipe.desktop.commons.components.cards.JIPipeDesktopCardVariant.Info;
    }
}
