package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.service.components.JIPipeStatisticsServiceComponent;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.commons.events.WindowOpenedEvent;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;

import javax.swing.*;

public class LargestProjectNodesStatisticsItem implements JIPipeStatisticsItem {
    private int maxNodes = 0;
    private JIPipeStatisticsServiceComponent service;

    @Override
    public String getId() { return "largest-project-nodes"; }
    @Override
    public String getName() { return "Largest project (nodes)"; }
    @Override
    public String getDescription() { return "Maximum number of nodes in any opened project"; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.Everything; }
    @Override
    public JsonNode serialize() { return IntNode.valueOf(maxNodes); }
    @Override
    public void deserialize(JsonNode node) { if (node != null && !node.isNull()) maxNodes = node.asInt(); }
    @Override
    public void reset() { maxNodes = 0; }
    @Override
    public boolean isTimeTracked() { return true; }

    @Override
    public void initialize(JIPipeStatisticsServiceComponent service) {
        this.service = service;
        JIPipeDesktopProjectWindow.WINDOW_OPENED_EVENT_EMITTER.subscribe(this::onWindowOpened);
        for (JIPipeDesktopProjectWindow window : JIPipeDesktopProjectWindow.getOpenWindows()) {
            updateMax(window);
        }
    }

    private void onWindowOpened(WindowOpenedEvent event) {
        if (event.getWindow() instanceof JIPipeDesktopProjectWindow window) {
            SwingUtilities.invokeLater(() -> updateMax(window));
        }
    }

    private void updateMax(JIPipeDesktopProjectWindow window) {
        JIPipeProject project = window.getProject();
        if (project != null) {
            int count = project.getGraph().getGraphNodes().size();
            if (count > maxNodes) {
                maxNodes = count;
                if (service != null) service.saveLater();
            }
        }
    }
}
