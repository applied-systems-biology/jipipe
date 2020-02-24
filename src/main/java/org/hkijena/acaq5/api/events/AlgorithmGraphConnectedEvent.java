package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQDataSlot;

public class AlgorithmGraphConnectedEvent {
    private ACAQAlgorithmGraph graph;
    private ACAQDataSlot<?> source;
    private ACAQDataSlot<?> target;

    public AlgorithmGraphConnectedEvent(ACAQAlgorithmGraph graph, ACAQDataSlot<?> source, ACAQDataSlot<?> target) {
        this.graph = graph;
        this.source = source;
        this.target = target;
    }

    public ACAQAlgorithmGraph getGraph() {
        return graph;
    }

    public ACAQDataSlot<?> getSource() {
        return source;
    }

    public ACAQDataSlot<?> getTarget() {
        return target;
    }
}
