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

package org.hkijena.jipipe.extensions.omero.datatypes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.nio.file.Path;

/**
 * Data that stores a reference to an OMERO project
 */
@JIPipeDocumentation(name = "OMERO Project", description = "An OMERO project ID")
public class OMEROProjectReferenceData implements JIPipeData {
    private final long projectId;

    public OMEROProjectReferenceData(long projectId) {
        this.projectId = projectId;
    }

    public long getProjectId() {
        return projectId;
    }

    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName) {

    }

    @Override
    public JIPipeData duplicate() {
        return new OMEROProjectReferenceData(projectId);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench) {

    }

    @Override
    public String toString() {
        return "OMERO Project ID=" + projectId;
    }
}
