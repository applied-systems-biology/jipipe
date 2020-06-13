package org.hkijena.acaq5.api.testbench;

import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.data.ACAQDataSlot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Allows to test one algorithm with multiple parameters
 */
public class ACAQTestBench implements ACAQRunnable, ACAQValidatable {
    private ACAQProject project;
    private ACAQGraphNode projectAlgorithm;
    private ACAQTestBenchSettings settings;

    private ACAQRun testBenchRun;
    private ACAQGraphNode benchedAlgorithm;
    private volatile ACAQTestbenchSnapshot initialBackup;

    private List<ACAQTestbenchSnapshot> backupList = new ArrayList<>();

    /**
     * @param project          The project
     * @param projectAlgorithm The tested algorithm
     * @param settings         The settings
     */
    public ACAQTestBench(ACAQProject project, ACAQGraphNode projectAlgorithm, ACAQTestBenchSettings settings) {
        this.project = project;
        this.projectAlgorithm = projectAlgorithm;
        this.settings = settings;

        initialize();
    }

    private void initialize() {
        ACAQRunSettings configuration = new ACAQRunSettings();
        configuration.setOutputPath(settings.getOutputPath().resolve("initial"));
        configuration.setLoadFromCache(settings.isLoadFromCache());
        configuration.setStoreToCache(settings.isStoreToCache());

        testBenchRun = new ACAQRun(project, configuration);
        benchedAlgorithm = testBenchRun.getGraph().getAlgorithmNodes().get(projectAlgorithm.getIdInGraph());
        ((ACAQAlgorithm) benchedAlgorithm).setEnabled(true);

        // Disable all algorithms that are not dependencies of the benched algorithm
        List<ACAQGraphNode> predecessorAlgorithms = testBenchRun.getGraph()
                .getPredecessorAlgorithms(benchedAlgorithm, testBenchRun.getGraph().traverseAlgorithms());
        predecessorAlgorithms.add(benchedAlgorithm);
        for (ACAQGraphNode node : testBenchRun.getGraph().getAlgorithmNodes().values()) {
            if (!predecessorAlgorithms.contains(node)) {
                if (node instanceof ACAQAlgorithm) {
                    ((ACAQAlgorithm) node).setEnabled(false);
                }
            }
        }

    }

    @Override
    public void run(Consumer<ACAQRunnerStatus> onProgress, Supplier<Boolean> isCancelled) {
        // Run the internal graph runner
        testBenchRun.run(onProgress, isCancelled);

        // Create initial backup
        if (initialBackup == null) {
            initialBackup = new ACAQTestbenchSnapshot(this);
            backupList.add(initialBackup);
        }

        // Clear all data
        for (ACAQGraphNode node : testBenchRun.getGraph().getAlgorithmNodes().values()) {
            for (ACAQDataSlot inputSlot : node.getInputSlots()) {
                inputSlot.clearData();
            }
            for (ACAQDataSlot outputSlot : node.getOutputSlots()) {
                outputSlot.clearData();
            }
        }

    }

    public ACAQProject getProject() {
        return project;
    }


    public ACAQGraphNode getProjectAlgorithm() {
        return projectAlgorithm;
    }

    public ACAQTestBenchSettings getSettings() {
        return settings;
    }

    public ACAQRun getTestBenchRun() {
        return testBenchRun;
    }

    public ACAQGraphNode getBenchedAlgorithm() {
        return benchedAlgorithm;
    }

    public List<ACAQTestbenchSnapshot> getBackupList() {
        return backupList;
    }

    /**
     * Creates a backup
     */
    public void createBackup() {
        backupList.add(new ACAQTestbenchSnapshot(this));
    }

    /**
     * @return the latest backup
     */
    public ACAQTestbenchSnapshot getLatestBackup() {
        return backupList.get(backupList.size() - 1);
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        benchedAlgorithm.reportValidity(report);
    }

    /**
     * Creates a new test
     */
    public void newTest() {
        ACAQValidityReport report = new ACAQValidityReport();
        reportValidity(report);
        if (!report.isValid())
            throw new RuntimeException("Test bench is not valid!");

        Path outputBasePath = testBenchRun.getConfiguration().getOutputPath().getParent();
        Path outputPath;
        int index = 1;
        do {
            outputPath = outputBasePath.resolve("test-" + index);
            ++index;
        }
        while (Files.isDirectory(outputPath));
        testBenchRun.getConfiguration().setOutputPath(outputPath);
    }

    /**
     * @return the first backup. This is used as reference to get the intermediate data
     */
    public ACAQTestbenchSnapshot getInitialBackup() {
        return initialBackup;
    }
}
