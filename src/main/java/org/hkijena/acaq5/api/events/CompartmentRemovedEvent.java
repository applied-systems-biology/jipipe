package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;

/**
 * Triggered when a sample is removed from an {@link org.hkijena.acaq5.api.ACAQProject}
 */
public class CompartmentRemovedEvent {
    private ACAQProjectCompartment compartment;

    /**
     * @param compartment the compartment
     */
    public CompartmentRemovedEvent(ACAQProjectCompartment compartment) {
        this.compartment = compartment;
    }

    public ACAQProjectCompartment getCompartment() {
        return compartment;
    }
}
