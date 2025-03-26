package org.hkijena.jipipe.plugins.imagejdatatypes.util;

public enum InvalidRoiOutlineBehavior {
    Error("Throw error"),
    Skip("Skip"),
    KeepOriginal("Keep original");

    private final String label;

    InvalidRoiOutlineBehavior(String label) {
        this.label = label;
    }


    @Override
    public String toString() {
        return label;
    }
}
