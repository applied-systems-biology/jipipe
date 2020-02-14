package org.hkijena.acaq5.api;

import java.util.*;

public class ACAQRunSample {
    private ACAQRun run;
    private ACAQProjectSample projectSample;
    private Set<ACAQAlgorithm> algorithms;

    public ACAQRunSample(ACAQRun run, ACAQProjectSample projectSample, Set<ACAQAlgorithm> algorithms) {
        this.run = run;
        this.projectSample = projectSample;
        this.algorithms = algorithms;
    }

    public String getName() {
        return run.getSamples().inverse().get(this);
    }

    public Set<ACAQAlgorithm> getAlgorithms() {
        return Collections.unmodifiableSet(algorithms);
    }

    public ACAQProjectSample getProjectSample() {
        return projectSample;
    }

    public List<ACAQDataSlot<?>> getOutputData() {
        List<ACAQDataSlot<?>> result = new ArrayList<>();
        for(ACAQAlgorithm algorithm : algorithms) {
            result.addAll(algorithm.getOutputSlots());
        }
        result.sort(Comparator.comparing(o -> o.getClass().getCanonicalName()));
        return result;
    }
}
