package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.ACAQProjectCompartment;

/**
 * Triggered when a sample is added to an {@link org.hkijena.acaq5.api.ACAQProject}
 */
public class CompartmentAddedEvent {
    private ACAQProjectCompartment sample;

    public CompartmentAddedEvent(ACAQProjectCompartment sample) {
        this.sample = sample;
    }

    public ACAQProjectCompartment getSample() {
        return sample;
    }
}
