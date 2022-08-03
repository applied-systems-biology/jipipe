package org.hkijena.jipipe.extensions.omnipose;

import org.hkijena.jipipe.api.JIPipeDocumentationDescription;

@JIPipeDocumentationDescription(description = "See https://github.com/kevinjohncutler/omnipose for a description of all models")
public enum OmniposeModel {
    BactPhaseCp("bact_phase_cp"),
    BactFluorCp("bact_fluor_cp"),
    PlantCp("plant_cp"),
    WormCp("worm_cp"),
    Cyto2Omni("cyto2_omni"),
    BactPhaseOmni("bact_phase_omni"),
    BactFluorOmni("bact_fluor_omni"),
    PlantOmni("plant_omni"),
    WormOmni("worm_omni"),
    WormBactOmni("worm_bact_omni"),
    WormHighResOmni("worm_high_res_omni"),
    Custom("");

    private final String id;

    OmniposeModel(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
