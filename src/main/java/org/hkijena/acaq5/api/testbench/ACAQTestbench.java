package org.hkijena.acaq5.api.testbench;

import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ACAQTestbench implements ACAQRunnable {
    private ACAQProject project;
    private ACAQAlgorithmGraph graph;
    private ACAQAlgorithm targetAlgorithm;
    private Path workDirectory;

    private ACAQRun testbenchRun;
    private ACAQAlgorithm benchedAlgorithm;

    public ACAQTestbench(ACAQProject project, ACAQAlgorithmGraph graph, ACAQAlgorithm targetAlgorithm, Path workDirectory) {
        this.project = project;
        this.graph = graph;
        this.targetAlgorithm = targetAlgorithm;
        this.workDirectory = workDirectory;

        initialize();
    }

    private void initialize() {
        ACAQMutableRunConfiguration configuration = new ACAQMutableRunConfiguration();
        configuration.setFlushingEnabled(true);
        configuration.setFlushingKeepsDataEnabled(true);
        configuration.setOutputPath(workDirectory.resolve("initial"));
        configuration.setEndAlgorithmId(graph.getIdOf(targetAlgorithm));

        testbenchRun = new ACAQRun(project, configuration);
//        if(project.getAnalysis().getAlgorithmNodes().containsValue(targetAlgorithm))
//            benchedAlgorithm = testbenchRun.getGraph().getAlgorithmNodes().get()
    }

    @Override
    public void run(Consumer<ACAQRunnerStatus> onProgress, Supplier<Boolean> isCancelled) {

    }

    public ACAQProject getProject() {
        return project;
    }

    public ACAQAlgorithm getTargetAlgorithm() {
        return targetAlgorithm;
    }

    public Path getWorkDirectory() {
        return workDirectory;
    }

    public ACAQRun getTestbenchRun() {
        return testbenchRun;
    }

    public ACAQAlgorithm getBenchedAlgorithm() {
        return benchedAlgorithm;
    }
}
