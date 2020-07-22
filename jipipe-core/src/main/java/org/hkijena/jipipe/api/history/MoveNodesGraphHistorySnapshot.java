/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.history;

import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.events.GraphChangedEvent;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class MoveNodesGraphHistorySnapshot implements JIPipeAlgorithmGraphHistorySnapshot {

    private final JIPipeGraph graph;
    private final Map<JIPipeGraphNode, Map<String, Map<String, Point>>> locations;
    private final String name;
    private Map<JIPipeGraphNode, Map<String, Map<String, Point>>> redoLocation;

    /**
     * Creates a snapshot of all node locations
     *
     * @param graph the graph
     * @param name  the name
     */
    public MoveNodesGraphHistorySnapshot(JIPipeGraph graph, String name) {
        this.graph = graph;
        this.name = name;
        this.locations = extractLocation();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void undo() {
        redoLocation = extractLocation();
        restoreLocations(locations);
    }

    @Override
    public void redo() {
        if (redoLocation != null) {
            restoreLocations(redoLocation);
        }
    }

    public Map<JIPipeGraphNode, Map<String, Map<String, Point>>> getLocations() {
        return locations;
    }

    public Map<JIPipeGraphNode, Map<String, Map<String, Point>>> getRedoLocation() {
        return redoLocation;
    }

    private void restoreLocations(Map<JIPipeGraphNode, Map<String, Map<String, Point>>> redoLocation) {
        for (Map.Entry<JIPipeGraphNode, Map<String, Map<String, Point>>> entry : redoLocation.entrySet()) {
            JIPipeGraphNode node = entry.getKey();
            for (Map.Entry<String, Map<String, Point>> compartmentEntry : entry.getValue().entrySet()) {
                for (Map.Entry<String, Point> modeEntry : compartmentEntry.getValue().entrySet()) {
                    node.setLocationWithin(compartmentEntry.getKey(), new Point(modeEntry.getValue().x, modeEntry.getValue().y), modeEntry.getKey());
                }
            }
        }
        graph.getEventBus().post(new GraphChangedEvent(graph));
    }

    private Map<JIPipeGraphNode, Map<String, Map<String, Point>>> extractLocation() {
        Map<JIPipeGraphNode, Map<String, Map<String, Point>>> result = new HashMap<>();
        for (JIPipeGraphNode node : graph.getNodes().values()) {
            Map<String, Map<String, Point>> locationMap = new HashMap<>();
            for (Map.Entry<String, Map<String, Point>> entry : node.getLocations().entrySet()) {
                Map<String, Point> modeMap = new HashMap<>();
                for (Map.Entry<String, Point> modeEntry : entry.getValue().entrySet()) {
                    modeMap.put(modeEntry.getKey(), new Point(modeEntry.getValue().x, modeEntry.getValue().y));
                }
                locationMap.put(entry.getKey(), modeMap);
            }
            result.put(node, locationMap);
        }
        return result;
    }
}
