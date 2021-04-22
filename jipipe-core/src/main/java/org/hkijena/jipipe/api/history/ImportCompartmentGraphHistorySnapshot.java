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

package org.hkijena.jipipe.api.history;

import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;

import java.util.UUID;

public class ImportCompartmentGraphHistorySnapshot implements JIPipeAlgorithmGraphHistorySnapshot {

    private final JIPipeProject project;
    private JIPipeProjectCompartment compartmentInstance;
    private UUID uuid;

    public ImportCompartmentGraphHistorySnapshot(JIPipeProject project, JIPipeProjectCompartment compartmentInstance) {
        this.project = project;
        this.compartmentInstance = compartmentInstance;
        this.uuid = compartmentInstance.getProjectCompartmentUUID();
    }

    @Override
    public String getName() {
        return "Import compartment";
    }

    @Override
    public void undo() {
        project.removeCompartment(compartmentInstance);
    }

    @Override
    public void redo() {
        compartmentInstance = project.addCompartment(compartmentInstance, uuid);
    }

    public JIPipeProjectCompartment getCompartmentInstance() {
        return compartmentInstance;
    }

    public void setCompartmentInstance(JIPipeProjectCompartment compartmentInstance) {
        this.compartmentInstance = compartmentInstance;
    }

    public JIPipeProject getProject() {
        return project;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
}
