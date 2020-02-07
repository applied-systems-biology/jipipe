package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.ACAQProjectSample;

/**
 * Triggered when a sample is removed from an {@link org.hkijena.acaq5.api.ACAQProject}
 */
public class ACAQSampleRemovedEvent {
    private ACAQProjectSample sample;

    public ACAQSampleRemovedEvent(ACAQProjectSample sample) {
        this.sample = sample;
    }

    public ACAQProjectSample getSample() {
        return sample;
    }
}
