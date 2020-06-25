/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

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
