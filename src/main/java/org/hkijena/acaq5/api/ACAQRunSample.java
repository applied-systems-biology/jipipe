package org.hkijena.acaq5.api;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.data.ACAQDataSlot;

import java.util.*;

/**
 * Sample within an {@link ACAQRun}
 */
public class ACAQRunSample implements Comparable<ACAQRunSample>{
    private ACAQRun run;
    private ACAQProjectCompartment projectSample;
    private Set<ACAQAlgorithm> algorithms;

    public ACAQRunSample(ACAQRun run, ACAQProjectCompartment projectSample, Set<ACAQAlgorithm> algorithms) {
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

    public ACAQProjectCompartment getProjectSample() {
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

    public ACAQRun getRun() {
        return run;
    }

    @Override
    public int compareTo(ACAQRunSample o) {
        return getName().compareTo(o.getName());
    }
}
