package org.hkijena.jipipe.extensions.cellpose;

public enum CellPoseModel {
    Cytoplasm("cyto"),
    Nucleus("nuclei");

    private final String id;

    CellPoseModel(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
