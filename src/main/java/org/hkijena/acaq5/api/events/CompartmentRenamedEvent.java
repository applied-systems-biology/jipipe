package org.hkijena.acaq5.api.events;


import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;

/**
 * Triggered when a sample in an {@link org.hkijena.acaq5.api.ACAQProject} is renamed
 */
public class CompartmentRenamedEvent {
    private ACAQProjectCompartment compartment;

    /**
     * @param compartment the compartment
     */
    public CompartmentRenamedEvent(ACAQProjectCompartment compartment) {
        this.compartment = compartment;
    }

    public ACAQProjectCompartment getCompartment() {
        return compartment;
    }
}
