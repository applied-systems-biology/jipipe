package org.hkijena.acaq5.api.testbench;

import org.hkijena.acaq5.api.*;
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
public class ACAQTestbench implements ACAQRunnable, ACAQValidatable {
    private ACAQProject project;
    private ACAQGraphNode targetAlgorithm;
    private Path workDirectory;

    private ACAQRun testbenchRun;
    private ACAQGraphNode benchedAlgorithm;
    private volatile ACAQTestbenchSnapshot initialBackup;

    private List<ACAQTestbenchSnapshot> backupList = new ArrayList<>();

    /**
     * @param project         The project
     * @param targetAlgorithm The tested algorithm
     * @param workDirectory   A temporary work directory
     */
    public ACAQTestbench(ACAQProject project, ACAQGraphNode targetAlgorithm, Path workDirectory) {
        this.project = project;
        this.targetAlgorithm = targetAlgorithm;
        this.workDirectory = workDirectory;

        initialize();
    }

    private void initialize() {
        ACAQMutableRunConfiguration configuration = new ACAQMutableRunConfiguration();
        configuration.setFlushingEnabled(true);
        configuration.setFlushingKeepsDataEnabled(true);
        configuration.setOutputPath(workDirectory.resolve("initial"));
        configuration.setEndAlgorithmId(targetAlgorithm.getIdInGraph());

        testbenchRun = new ACAQRun(project, configuration);
        benchedAlgorithm = testbenchRun.getGraph().getAlgorithmNodes().get(targetAlgorithm.getIdInGraph());
    }

    @Override
    public void run(Consumer<ACAQRunnerStatus> onProgress, Supplier<Boolean> isCancelled) {

        // Clear benched algorithm outputs
        for (ACAQDataSlot slot : benchedAlgorithm.getSlots().values()) {
            slot.clearData();
        }


        // Run the internal graph runner
        testbenchRun.run(onProgress, isCancelled);

        // Required after first run
        ((ACAQMutableRunConfiguration) testbenchRun.getConfiguration()).setOnlyRunningEndAlgorithm(true);

        // Create initial backup
        if (initialBackup == null) {
            initialBackup = new ACAQTestbenchSnapshot(this);
            backupList.add(initialBackup);
        }
    }

    public ACAQProject getProject() {
        return project;
    }

    /**
     * @return the algorithm that is targeted. This algorithm is part of the project.
     */
    public ACAQGraphNode getTargetAlgorithm() {
        return targetAlgorithm;
    }

    /**
     * @return the work directory
     */
    public Path getWorkDirectory() {
        return workDirectory;
    }

    /**
     * @return the run
     */
    public ACAQRun getTestbenchRun() {
        return testbenchRun;
    }

    /**
     * @return the target algorithm. This algorithm is a copy of the project algorithm.
     */
    public ACAQGraphNode getBenchedAlgorithm() {
        return benchedAlgorithm;
    }

    /**
     * @return all backups
     */
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
            throw new RuntimeException("Testbench is not valid!");

        Path outputBasePath = testbenchRun.getConfiguration().getOutputPath().getParent();
        Path outputPath;
        int index = 1;
        do {
            outputPath = outputBasePath.resolve("test-" + index);
            ++index;
        }
        while (Files.isDirectory(outputPath));
        ((ACAQMutableRunConfiguration) testbenchRun.getConfiguration()).setOutputPath(outputPath);
    }

    /**
     * @return the first backup. This is used as reference to get the intermediate data
     */
    public ACAQTestbenchSnapshot getInitialBackup() {
        return initialBackup;
    }
}
