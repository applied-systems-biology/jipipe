package org.hkijena.acaq5;

public abstract class ACAQData {
    private String name;
    private String description;

    public ACAQData(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
