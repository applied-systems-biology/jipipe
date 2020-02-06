package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.ACAQAlgorithm;

/**
 * Triggered when an algorithm's slots change
 */
public class AlgorithmSlotsChangedEvent {
    private ACAQAlgorithm algorithm;

    public AlgorithmSlotsChangedEvent(ACAQAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public ACAQAlgorithm getAlgorithm() {
        return algorithm;
    }
}
