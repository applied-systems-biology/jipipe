package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.service.components.JIPipeStatisticsServiceComponent;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.commons.events.WindowClosedEvent;
import org.hkijena.jipipe.desktop.commons.events.WindowOpenedEvent;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;

import javax.swing.*;
import java.util.HashSet;
import java.util.Set;

public class LargestProjectNodesStatisticsItem implements JIPipeStatisticsItem {
    private int maxNodes = 0;
    private final Set<JIPipeDesktopProjectWindow> attachedWindows = new HashSet<>();
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
        JIPipeDesktopProjectWindow.WINDOW_CLOSED_EVENT_EMITTER.subscribe(this::onWindowClosed);
        for (JIPipeDesktopProjectWindow window : JIPipeDesktopProjectWindow.getOpenWindows()) {
            attachToWindow(window);
        }
    }

    private void onWindowOpened(WindowOpenedEvent event) {
        if (event.getWindow() instanceof JIPipeDesktopProjectWindow window) {
            SwingUtilities.invokeLater(() -> attachToWindow(window));
        }
    }

    private void onWindowClosed(WindowClosedEvent event) {
        if (event.getWindow() instanceof JIPipeDesktopProjectWindow window) {
            attachedWindows.remove(window);
        }
    }

    private void attachToWindow(JIPipeDesktopProjectWindow window) {
        if (!attachedWindows.add(window)) {
            return;
        }
        JIPipeProject project = window.getProject();
        if (project != null) {
            JIPipeGraph graph = project.getGraph();
            graph.getNodeAddedEventEmitter().subscribe(this::onNodeAdded);
            updateMax(graph.getGraphNodes().size());
        }
    }

    private void onNodeAdded(JIPipeGraph.NodeAddedEvent event) {
        int count = event.getGraph().getGraphNodes().size();
        updateMax(count);
    }

    private void updateMax(int count) {
        if (count > maxNodes) {
            maxNodes = count;
            if (service != null) service.saveLater();
        }
    }

    @Override
    public String getIcon32() { return "actions/network-server-database.png"; }
    @Override
    public org.hkijena.jipipe.desktop.commons.components.cards.JIPipeDesktopCardVariant getCardVariant() {
        return org.hkijena.jipipe.desktop.commons.components.cards.JIPipeDesktopCardVariant.Primary;
    }
    @Override
    public int getDefaultColumnSpan() { return 6; }
}
