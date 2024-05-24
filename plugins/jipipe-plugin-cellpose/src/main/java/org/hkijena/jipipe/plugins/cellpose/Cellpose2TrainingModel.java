/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.cellpose;

import org.hkijena.jipipe.api.AddJIPipeDocumentationDescription;

@AddJIPipeDocumentationDescription(description = "See https://cellpose.readthedocs.io/en/latest/models.html for a description of all models")
public enum Cellpose2TrainingModel {
    Cytoplasm("cyto"),
    Cytoplasm2("cyto2"),
    Nucleus("nuclei"),
    TissueNet("tissuenet"),
    TissueNet1("TN1"),
    TissueNet2("TN2"),
    TissueNet3("TN3"),
    LiveCell("livecell"),
    LiveCell1("LC1"),
    LiveCell2("LC2"),
    LiveCell3("LC3"),
    LiveCell4("LC4"),
    General("general"),
    CP("CP"),
    CPx("CPx"),
    Custom(""),
    None("");

    private final String id;

    Cellpose2TrainingModel(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
