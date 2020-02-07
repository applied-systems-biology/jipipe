package org.hkijena.acaq5.api;

public class ACAQProjectSample implements Comparable<ACAQProjectSample> {
    private ACAQProject project;
    private ACAQAlgorithmGraph preprocessingGraph;

    public ACAQProjectSample(ACAQProject project) {
        this.project = project;
        this.preprocessingGraph = new ACAQAlgorithmGraph();

        initializePreprocessingGraph();
    }

    private void initializePreprocessingGraph() {
        preprocessingGraph.insertNode(new ACAQPreprocessingOutput(getProject().getPreprocessingOutputConfiguration()));
    }

    public ACAQProject getProject() {
        return project;
    }

    public String getName() {
        return project.getSamples().inverse().get(this);
    }

    @Override
    public int compareTo(ACAQProjectSample o) {
        return getName().compareTo(o.getName());
    }

    public ACAQAlgorithmGraph getPreprocessingGraph() {
        return preprocessingGraph;
    }
}
