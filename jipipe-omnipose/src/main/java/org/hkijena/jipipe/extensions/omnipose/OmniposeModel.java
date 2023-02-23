package org.hkijena.jipipe.extensions.omnipose;

import org.hkijena.jipipe.api.JIPipeDocumentationDescription;

@JIPipeDocumentationDescription(description = "See https://github.com/kevinjohncutler/omnipose for a description of all models")
public enum OmniposeModel {
    BactOmni("bact_omni"),
    Bact("bact"),
    Cyto2Omni("cyto2_omni"),
    Cytoplasm("cyto"),
    Cytoplasm2("cyto2"),
    Nucleus("nuclei"),
    Custom("");

    private final String id;

    OmniposeModel(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
