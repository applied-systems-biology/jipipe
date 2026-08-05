package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;

public class LongestNodeWidthStatisticsItem implements JIPipeStatisticsItem {
    private static int maxWidthSeen = 0;
    private int maxWidth = 0;

    @Override
    public String getId() { return "longest-node-width"; }
    @Override
    public String getName() { return "Longest node width"; }
    @Override
    public String getDescription() { return "The widest node UI seen (pixels)"; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.Everything; }

    @Override
    public JsonNode serialize() {
        if (maxWidthSeen > maxWidth) maxWidth = maxWidthSeen;
        return IntNode.valueOf(maxWidth);
    }

    @Override
    public void deserialize(JsonNode node) {
        if (node != null && !node.isNull()) maxWidth = node.asInt();
    }

    @Override
    public void reset() { maxWidth = 0; maxWidthSeen = 0; }
    @Override
    public boolean isTimeTracked() { return true; }

    public static void reportWidth(int width) {
        maxWidthSeen = Math.max(maxWidthSeen, width);
    }

    @Override
    public String getIcon32() { return "actions/resizecol.png"; }
}
