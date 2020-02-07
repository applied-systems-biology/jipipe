package org.hkijena.acaq5.api;

public class ACAQRun {
    private ACAQProject project;
    public ACAQRun(ACAQProject project) {
        this.project = project;
    }

    public ACAQProject getProject() {
        return project;
    }
}
