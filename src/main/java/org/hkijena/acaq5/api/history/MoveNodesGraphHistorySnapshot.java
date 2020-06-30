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

package org.hkijena.acaq5.api.history;

import org.hkijena.acaq5.api.algorithm.ACAQGraph;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

public class MoveNodesGraphHistorySnapshot implements ACAQAlgorithmGraphHistorySnapshot {

    private final ACAQGraph graph;
    private final Map<ACAQGraphNode, Map<String, Map<String, Point>>> locations;
    private Map<ACAQGraphNode, Map<String, Map<String, Point>>> redoLocation;
    private final String name;

    /**
     * Creates a snapshot of all node locations
     * @param graph the graph
     * @param name the name
     */
    public MoveNodesGraphHistorySnapshot(ACAQGraph graph, String name) {
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
        if(redoLocation != null) {
            restoreLocations(redoLocation);
        }
    }

    public Map<ACAQGraphNode, Map<String, Map<String, Point>>> getLocations() {
        return locations;
    }

    public Map<ACAQGraphNode, Map<String, Map<String, Point>>> getRedoLocation() {
        return redoLocation;
    }

    private void restoreLocations(Map<ACAQGraphNode, Map<String, Map<String, Point>>> redoLocation) {
        for (Map.Entry<ACAQGraphNode, Map<String, Map<String, Point>>> entry : redoLocation.entrySet()) {
            ACAQGraphNode node = entry.getKey();
            for (Map.Entry<String, Map<String, Point>> compartmentEntry : entry.getValue().entrySet()) {
                for (Map.Entry<String, Point> modeEntry : compartmentEntry.getValue().entrySet()) {
                    node.setLocationWithin(compartmentEntry.getKey(), new Point(modeEntry.getValue().x, modeEntry.getValue().y), modeEntry.getKey());
                }
            }
        }
        graph.getEventBus().post(new AlgorithmGraphChangedEvent(graph));
    }

    private Map<ACAQGraphNode, Map<String, Map<String, Point>>> extractLocation() {
        Map<ACAQGraphNode, Map<String, Map<String, Point>>> result = new HashMap<>();
        for (ACAQGraphNode node : graph.getAlgorithmNodes().values()) {
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
