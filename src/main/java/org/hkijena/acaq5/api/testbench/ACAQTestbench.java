package org.hkijena.acaq5.api.testbench;

import org.hkijena.acaq5.api.ACAQMutableRunConfiguration;
import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.api.ACAQRunnable;
import org.hkijena.acaq5.api.ACAQRunnerStatus;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.data.ACAQDataSlot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ACAQTestbench implements ACAQRunnable, ACAQValidatable {
    private ACAQProject project;
    private ACAQAlgorithm targetAlgorithm;
    private Path workDirectory;

    private ACAQRun testbenchRun;
    private ACAQAlgorithm benchedAlgorithm;

    private List<ACAQAlgorithmBackup> backupList = new ArrayList<>();

    public ACAQTestbench(ACAQProject project, ACAQAlgorithm targetAlgorithm, Path workDirectory) {
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

    public List<ACAQAlgorithmBackup> getBackupList() {
        return backupList;
    }

    public void createBackup() {
        backupList.add(new ACAQAlgorithmBackup(benchedAlgorithm));
    }

    public ACAQAlgorithmBackup getLatestBackup() {
        return backupList.get(backupList.size() - 1);
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        benchedAlgorithm.reportValidity(report);
    }

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
}
