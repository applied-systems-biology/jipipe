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

package org.hkijena.pipelinej.api.history;

import org.hkijena.pipelinej.api.ACAQProject;
import org.hkijena.pipelinej.api.compartments.algorithms.ACAQProjectCompartment;

public class AddCompartmentGraphHistorySnapshot implements ACAQAlgorithmGraphHistorySnapshot {

    private final ACAQProject project;
    private final String compartmentName;
    private ACAQProjectCompartment compartmentInstance;

    public AddCompartmentGraphHistorySnapshot(ACAQProject project, String compartmentName) {
        this.project = project;
        this.compartmentName = compartmentName;
    }

    @Override
    public String getName() {
        return "Add compartment '" + compartmentName + "'";
    }

    @Override
    public void undo() {
        project.removeCompartment(compartmentInstance);
    }

    @Override
    public void redo() {
        compartmentInstance = project.addCompartment(compartmentName);
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
