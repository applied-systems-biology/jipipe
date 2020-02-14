package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.ACAQProjectSample;


/**
 * Triggered when a sample in an {@link org.hkijena.acaq5.api.ACAQProject} is renamed
 */
public class ACAQSampleRenamedEvent {
    private ACAQProjectSample sample;

    public ACAQSampleRenamedEvent(ACAQProjectSample sample) {
        this.sample = sample;
    }

    public ACAQProjectSample getSample() {
        return sample;
    }
}
