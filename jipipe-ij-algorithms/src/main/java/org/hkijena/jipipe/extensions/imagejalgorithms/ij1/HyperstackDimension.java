package org.hkijena.jipipe.extensions.imagejalgorithms.ij1;

public enum HyperstackDimension {
    Depth("Depth (Z)"),
    Channel("Channel (C)"),
    Frame("Frame (T)");

    private final String label;

    HyperstackDimension(String label) {

        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
