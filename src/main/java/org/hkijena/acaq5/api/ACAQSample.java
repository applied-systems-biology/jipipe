package org.hkijena.acaq5.api;

public class ACAQSample {
    private ACAQProject project;

    public ACAQSample(ACAQProject project) {
        this.project = project;
    }

    public ACAQProject getProject() {
        return project;
    }

}
