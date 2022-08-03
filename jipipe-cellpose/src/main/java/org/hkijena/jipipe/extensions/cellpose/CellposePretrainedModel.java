package org.hkijena.jipipe.extensions.cellpose;

import org.hkijena.jipipe.api.JIPipeDocumentationDescription;

@JIPipeDocumentationDescription(description = "See https://cellpose.readthedocs.io/en/latest/models.html for a description of all models")
public enum CellposePretrainedModel {
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
    Custom(""),
    None("");

    private final String id;

    CellposePretrainedModel(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
