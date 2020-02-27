package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.ACAQProjectCompartment;


/**
 * Triggered when a sample in an {@link org.hkijena.acaq5.api.ACAQProject} is renamed
 */
public class CompartmentRenamedEvent {
    private ACAQProjectCompartment sample;

    public CompartmentRenamedEvent(ACAQProjectCompartment sample) {
        this.sample = sample;
    }

    public ACAQProjectCompartment getSample() {
        return sample;
    }
}
