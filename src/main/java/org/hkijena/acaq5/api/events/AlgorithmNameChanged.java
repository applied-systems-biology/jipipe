package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;

/**
 * Triggered when an algorithm changes its name
 */
public class AlgorithmNameChanged {
    private ACAQAlgorithm algorithm;

    public AlgorithmNameChanged(ACAQAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public ACAQAlgorithm getAlgorithm() {
        return algorithm;
    }
}
