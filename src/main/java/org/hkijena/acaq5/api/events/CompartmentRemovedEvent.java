package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.ACAQProjectCompartment;

/**
 * Triggered when a sample is removed from an {@link org.hkijena.acaq5.api.ACAQProject}
 */
public class CompartmentRemovedEvent {
    private ACAQProjectCompartment sample;

    public CompartmentRemovedEvent(ACAQProjectCompartment sample) {
        this.sample = sample;
    }

    public ACAQProjectCompartment getSample() {
        return sample;
    }
}
