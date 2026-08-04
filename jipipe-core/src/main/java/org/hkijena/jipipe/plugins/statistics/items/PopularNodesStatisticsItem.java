package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.service.components.JIPipeStatisticsServiceComponent;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.commons.events.WindowOpenedEvent;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItemCategory;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class PopularNodesStatisticsItem implements JIPipeStatisticsItem {
    private final Map<String, Integer> nodeCounts = new HashMap<>();
    private JIPipeStatisticsServiceComponent service;

    @Override
    public String getId() { return "popular-nodes"; }
    @Override
    public String getName() { return "Popular nodes"; }
    @Override
    public String getDescription() { return "Top 5 most used node types"; }
    @Override
    public JIPipeStatisticsItemCategory getCategory() { return JIPipeStatisticsItemCategory.Fun; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.Everything; }

    @Override
    public JsonNode serialize() {
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        nodeCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> node.put(e.getKey(), e.getValue()));
        return node;
    }

    @Override
    public void deserialize(JsonNode node) {
        nodeCounts.clear();
        if (node != null && node.isObject()) {
            node.fields().forEachRemaining(e -> nodeCounts.put(e.getKey(), e.getValue().asInt()));
        }
    }

    @Override
    public void reset() { nodeCounts.clear(); }

    @Override
    public void initialize(JIPipeStatisticsServiceComponent service) {
        this.service = service;
        JIPipeDesktopProjectWindow.WINDOW_OPENED_EVENT_EMITTER.subscribe(this::onWindowOpened);
        for (JIPipeDesktopProjectWindow window : JIPipeDesktopProjectWindow.getOpenWindows()) {
            attachToWindow(window);
        }
    }

    private void onWindowOpened(WindowOpenedEvent event) {
        if (event.getWindow() instanceof JIPipeDesktopProjectWindow window) {
            SwingUtilities.invokeLater(() -> attachToWindow(window));
        }
    }

    private void attachToWindow(JIPipeDesktopProjectWindow window) {
        JIPipeProject project = window.getProject();
        if (project != null) {
            JIPipeGraph graph = project.getGraph();
            graph.getNodeAddedEventEmitter().subscribe(this::onNodeAdded);
            for (JIPipeGraphNode existingNode : graph.getGraphNodes()) {
                countNode(existingNode);
            }
        }
    }

    private void onNodeAdded(JIPipeGraph.NodeAddedEvent event) {
        countNode(event.getNode());
    }

    private void countNode(JIPipeGraphNode node) {
        String id = node.getInfo().getId();
        nodeCounts.merge(id, 1, Integer::sum);
        if (service != null) service.saveLater();
    }
}
