package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;

public class NodeMoveDistanceStatisticsItem implements JIPipeStatisticsItem {
    private static double accumulatedDistance = 0;
    private double distance = 0;

    @Override
    public String getId() { return "node-move-distance"; }
    @Override
    public String getName() { return "Node move distance"; }
    @Override
    public String getDescription() { return "Total distance nodes have been moved by the user (pixels)"; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.Everything; }

    @Override
    public JsonNode serialize() {
        if (accumulatedDistance > 0) {
            distance += accumulatedDistance;
            accumulatedDistance = 0;
        }
        return DoubleNode.valueOf(distance);
    }

    @Override
    public void deserialize(JsonNode node) {
        if (node != null && !node.isNull()) distance = node.asDouble();
    }

    @Override
    public void reset() { distance = 0; accumulatedDistance = 0; }

    public static void addDistance(double delta, Runnable onSave) {
        accumulatedDistance += delta;
        if (onSave != null) onSave.run();
    }
}
