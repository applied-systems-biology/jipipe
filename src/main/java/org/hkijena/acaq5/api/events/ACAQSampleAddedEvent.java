package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.ACAQProjectSample;

/**
 * Triggered when a sample is added to an {@link org.hkijena.acaq5.api.ACAQProject}
 */
public class ACAQSampleAddedEvent {
    private ACAQProjectSample sample;

    public ACAQSampleAddedEvent(ACAQProjectSample sample) {
        this.sample = sample;
    }

    public ACAQProjectSample getSample() {
        return sample;
    }
}
