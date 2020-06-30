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

package org.hkijena.acaq5.api.history;

import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;

public class DeleteCompartmentGraphHistorySnapshot implements ACAQAlgorithmGraphHistorySnapshot {

    private final ACAQProject project;
    private ACAQProjectCompartment compartmentInstance;

    public DeleteCompartmentGraphHistorySnapshot(ACAQProject project, ACAQProjectCompartment compartmentInstance) {
        this.project = project;
        this.compartmentInstance = compartmentInstance;
    }

    @Override
    public String getName() {
        return "Delete compartment '" + compartmentInstance.getName() + "'";
    }

    @Override
    public void redo() {
        project.removeCompartment(compartmentInstance);
    }

    @Override
    public void undo() {
        compartmentInstance = project.addCompartment(compartmentInstance);
    }

    public ACAQProjectCompartment getCompartmentInstance() {
        return compartmentInstance;
    }

    public void setCompartmentInstance(ACAQProjectCompartment compartmentInstance) {
        this.compartmentInstance = compartmentInstance;
    }

    public ACAQProject getProject() {
        return project;
    }
}
