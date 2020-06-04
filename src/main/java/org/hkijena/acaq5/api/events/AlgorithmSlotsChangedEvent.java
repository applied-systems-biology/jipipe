package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;

/**
 * Triggered when an algorithm's slots change
 */
public class AlgorithmSlotsChangedEvent {
    private ACAQGraphNode algorithm;

    /**
     * @param algorithm the algorithm
     */
    public AlgorithmSlotsChangedEvent(ACAQGraphNode algorithm) {
        this.algorithm = algorithm;
    }

    public ACAQGraphNode getAlgorithm() {
        return algorithm;
    }
}
