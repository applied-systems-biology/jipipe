package org.hkijena.jipipe.extensions.omnipose;

import org.hkijena.jipipe.api.AddJIPipeDocumentationDescription;

@AddJIPipeDocumentationDescription(description = "See https://github.com/kevinjohncutler/omnipose for a description of all models")
public enum OmniposePretrainedModel {
    BactOmni("bact_omni"),
    Bact("bact"),
    Cyto2Omni("cyto2_omni"),
    Cytoplasm("cyto"),
    Cytoplasm2("cyto2"),
    Nucleus("nuclei"),
    Custom(""),
    None("");

    private final String id;

    OmniposePretrainedModel(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
