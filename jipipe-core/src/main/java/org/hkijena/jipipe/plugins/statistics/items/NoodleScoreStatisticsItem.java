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
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;
import org.hkijena.jipipe.utils.json.JsonUtils;

public class NoodleScoreStatisticsItem implements JIPipeStatisticsItem {

    @Override
    public String getId() { return "noodle-score"; }
    @Override
    public String getName() { return "Noodle score"; }
    @Override
    public String getDescription() { return "Edge path lengths (min/avg/max) across all opened graph editors"; }
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

        ObjectMapper mapper = JsonUtils.getObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        if (count > 0) {
            node.put("min", currentMin == Double.MAX_VALUE ? 0 : currentMin);
            node.put("avg", sum / count);
            node.put("max", currentMax);
        } else {
            node.put("min", 0);
            node.put("avg", 0);
            node.put("max", 0);
        }
        return node;
    }

    @Override
    public void deserialize(JsonNode node) {
    }

    @Override
    public void reset() {
    }
    @Override
    public boolean isTimeTracked() { return true; }

    @Override
    public String getIcon32() { return "actions/bezier-curve.png"; }
    @Override
    public org.hkijena.jipipe.desktop.commons.components.cards.JIPipeDesktopCardVariant getCardVariant() {
        return org.hkijena.jipipe.desktop.commons.components.cards.JIPipeDesktopCardVariant.Default;
    }
    @Override
    public int getDefaultColumnSpan() { return 6; }
}
