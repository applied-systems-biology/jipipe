package org.hkijena.jipipe.extensions.cellpose;

public enum CellposePretrainedModel {
    Cytoplasm("cyto"),
    Nucleus("nuclei"),
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
