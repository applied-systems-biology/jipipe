package org.hkijena.acaq5.api;

public class ACAQRun {
    private ACAQProject project;
    ACAQAlgorithmGraph algorithmGraph;

    public ACAQRun(ACAQProject project) {
        this.project = project;
        initializeAlgorithmGraph();
    }

    private void initializeAlgorithmGraph() {
        algorithmGraph = new ACAQAlgorithmGraph();

    }


    public ACAQProject getProject() {
        return project;
    }
}
