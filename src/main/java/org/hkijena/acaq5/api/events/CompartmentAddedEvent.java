package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;

/**
 * Triggered when a sample is added to an {@link org.hkijena.acaq5.api.ACAQProject}
 */
public class CompartmentAddedEvent {
    private ACAQProjectCompartment compartment;

    /**
     * @param compartment the compartment
     */
    public CompartmentAddedEvent(ACAQProjectCompartment compartment) {
        this.compartment = compartment;
    }

    public ACAQProjectCompartment getCompartment() {
        return compartment;
    }
}
