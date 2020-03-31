package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;

/**
 * Event is triggered when algorithm graph is changed
 */
public class AlgorithmGraphChangedEvent {
    private ACAQAlgorithmGraph algorithmGraph;

    /**
     * @param algorithmGraph the graph
     */
    public AlgorithmGraphChangedEvent(ACAQAlgorithmGraph algorithmGraph) {
        this.algorithmGraph = algorithmGraph;
    }

    public ACAQAlgorithmGraph getAlgorithmGraph() {
        return algorithmGraph;
    }
}
