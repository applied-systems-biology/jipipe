package org.hkijena.acaq5.api;

public class ACAQProjectSample {
    private ACAQProject project;

    public ACAQProjectSample(ACAQProject project) {
        this.project = project;
    }

    public ACAQProject getProject() {
        return project;
    }

    public String getName() {
        return project.getSamples().inverse().get(this);
    }
}
