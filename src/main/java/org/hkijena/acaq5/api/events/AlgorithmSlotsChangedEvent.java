package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;

/**
 * Triggered when an algorithm's slots change
 */
public class AlgorithmSlotsChangedEvent {
    private ACAQAlgorithm algorithm;

    /**
     * @param algorithm the algorithm
     */
    public AlgorithmSlotsChangedEvent(ACAQAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public ACAQAlgorithm getAlgorithm() {
        return algorithm;
    }
}
