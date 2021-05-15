package org.hkijena.jipipe.extensions.cellpose;

public enum CellPosePretrainedModel {
    Cytoplasm("cyto"),
    Nucleus("nuclei"),
    Custom(""),
    None("");

    private final String id;

    CellPosePretrainedModel(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
