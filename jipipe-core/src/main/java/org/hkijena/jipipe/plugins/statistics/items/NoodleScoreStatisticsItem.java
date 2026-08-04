package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui.JIPipeDesktopGraphEdgeUI;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItemCategory;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;
import org.hkijena.jipipe.utils.json.JsonUtils;

public class NoodleScoreStatisticsItem implements JIPipeStatisticsItem {
    private double min = Double.MAX_VALUE;
    private double avg = 0;
    private double max = 0;

    @Override
    public String getId() { return "noodle-score"; }
    @Override
    public String getName() { return "Noodle score"; }
    @Override
    public String getDescription() { return "Edge path lengths (min/avg/max) across all opened graph editors"; }
    @Override
    public JIPipeStatisticsItemCategory getCategory() { return JIPipeStatisticsItemCategory.Fun; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.Everything; }

    @Override
    public JsonNode serialize() {
        double currentMin = Double.MAX_VALUE;
        double currentMax = 0;
        double sum = 0;
        int count = 0;

        for (JIPipeDesktopProjectWindow window : JIPipeDesktopProjectWindow.getOpenWindows()) {
            JIPipeDesktopProjectWorkbench workbench = window.getProjectWorkbench();
            if (workbench != null) {
                for (JIPipeDesktopTabPane.DocumentTab tab : workbench.getDocumentTabPane().getTabs()) {
                    if (tab.getContent() instanceof JIPipeDesktopGraphEditorUI editorUI) {
                        JIPipeDesktopGraphCanvasUI canvasUI = editorUI.getCanvasUI();
                        for (JIPipeDesktopGraphEdgeUI edgeUI : canvasUI.getEdgeUIs().values()) {
                            int dist = edgeUI.getUIManhattanDistance();
                            if (dist > 0) {
                                currentMin = Math.min(currentMin, dist);
                                currentMax = Math.max(currentMax, dist);
                                sum += dist;
                                count++;
                            }
                        }
                    }
                }
            }
        }

        if (count > 0) {
            min = Math.min(min, currentMin);
            max = Math.max(max, currentMax);
            avg = sum / count;
        }

        ObjectMapper mapper = JsonUtils.getObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("min", min == Double.MAX_VALUE ? 0 : min);
        node.put("avg", avg);
        node.put("max", max);
        return node;
    }

    @Override
    public void deserialize(JsonNode node) {
        if (node != null && node.isObject()) {
            min = node.has("min") ? node.get("min").asDouble() : Double.MAX_VALUE;
            avg = node.has("avg") ? node.get("avg").asDouble() : 0;
            max = node.has("max") ? node.get("max").asDouble() : 0;
        }
    }

    @Override
    public void reset() { min = Double.MAX_VALUE; avg = 0; max = 0; }
}
