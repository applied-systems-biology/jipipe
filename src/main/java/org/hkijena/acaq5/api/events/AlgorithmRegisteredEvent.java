package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;

/**
 * Triggered when an algorithm is registered
 */
public class AlgorithmRegisteredEvent {
    private ACAQAlgorithmDeclaration algorithmDeclaration;

    /**
     * @param algorithmDeclaration the algorithm type
     */
    public AlgorithmRegisteredEvent(ACAQAlgorithmDeclaration algorithmDeclaration) {
        this.algorithmDeclaration = algorithmDeclaration;
    }

    public ACAQAlgorithmDeclaration getAlgorithmDeclaration() {
        return algorithmDeclaration;
    }
}
