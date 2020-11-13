/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.testbench;

import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeRun;
import org.hkijena.jipipe.api.JIPipeRunSettings;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeRunnableInfo;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Allows to test one algorithm with multiple parameters
 */
public class JIPipeTestBench implements JIPipeRunnable, JIPipeValidatable {
    private JIPipeRunnableInfo info = new JIPipeRunnableInfo();
    private JIPipeProject project;
    private JIPipeGraphNode projectAlgorithm;
    private JIPipeTestBenchSettings settings;

    private JIPipeRun testBenchRun;
    private JIPipeGraphNode benchedAlgorithm;
    private volatile JIPipeTestbenchSnapshot initialBackup;

    private List<JIPipeTestbenchSnapshot> backupList = new ArrayList<>();

    /**
     * @param project          The project
     * @param projectAlgorithm The tested algorithm
     * @param settings         The settings
     */
    public JIPipeTestBench(JIPipeProject project, JIPipeGraphNode projectAlgorithm, JIPipeTestBenchSettings settings) {
        this.project = project;
        this.projectAlgorithm = projectAlgorithm;
        this.settings = settings;

        initialize();
    }

    private void initialize() {
        JIPipeRunSettings configuration = new JIPipeRunSettings();
        configuration.setOutputPath(settings.getOutputPath().resolve("initial"));
        configuration.setLoadFromCache(settings.isLoadFromCache());
        configuration.setStoreToCache(settings.isStoreToCache());
        configuration.setNumThreads(settings.getNumThreads());
        configuration.setSaveOutputs(settings.isSaveOutputs());

        testBenchRun = new JIPipeRun(project, configuration);
        testBenchRun.setInfo(info);
        benchedAlgorithm = testBenchRun.getGraph().getNodes().get(projectAlgorithm.getIdInGraph());
        ((JIPipeAlgorithm) benchedAlgorithm).setEnabled(true);

        // Disable all algorithms that are not dependencies of the benched algorithm
        List<JIPipeGraphNode> predecessorAlgorithms = testBenchRun.getGraph()
                .getPredecessorAlgorithms(benchedAlgorithm, testBenchRun.getGraph().traverseAlgorithms());
        if (!settings.isExcludeSelected())
            predecessorAlgorithms.add(benchedAlgorithm);
        for (JIPipeGraphNode node : testBenchRun.getGraph().getNodes().values()) {
            if (!predecessorAlgorithms.contains(node)) {
                if (node instanceof JIPipeAlgorithm) {
                    ((JIPipeAlgorithm) node).setEnabled(false);
                }
            }
        }

    }

    @Override
    public void run() {
        // Remove the benched algorithm from cache. This is a workaround.
        if (settings.isLoadFromCache()) {
            getProject().getCache().clear(projectAlgorithm);
        }

        // Run the internal graph runner
        testBenchRun.run();

        // Create initial backup
        if (initialBackup == null) {
            initialBackup = new JIPipeTestbenchSnapshot(this);
            backupList.add(initialBackup);
        }

        // Clear all data
        for (JIPipeGraphNode node : testBenchRun.getGraph().getNodes().values()) {
            for (JIPipeDataSlot inputSlot : node.getInputSlots()) {
                inputSlot.clearData(false);
            }
            for (JIPipeDataSlot outputSlot : node.getOutputSlots()) {
                outputSlot.clearData(false);
            }
        }

    }

    public JIPipeProject getProject() {
        return project;
    }


    public JIPipeGraphNode getProjectAlgorithm() {
        return projectAlgorithm;
    }

    public JIPipeTestBenchSettings getSettings() {
        return settings;
    }

    public JIPipeRun getTestBenchRun() {
        return testBenchRun;
    }

    public JIPipeGraphNode getBenchedAlgorithm() {
        return benchedAlgorithm;
    }

    public List<JIPipeTestbenchSnapshot> getBackupList() {
        return backupList;
    }

    /**
     * Creates a backup
     */
    public void createBackup() {
        backupList.add(new JIPipeTestbenchSnapshot(this));
    }

    /**
     * @return the latest backup
     */
    public JIPipeTestbenchSnapshot getLatestBackup() {
        return backupList.get(backupList.size() - 1);
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        benchedAlgorithm.reportValidity(report);
    }

    /**
     * Creates a new test
     */
    public void newTest() {
        JIPipeValidityReport report = new JIPipeValidityReport();
        reportValidity(report);
        if (!report.isValid())
            throw new RuntimeException("Quick run is not valid!");

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
    public JIPipeTestbenchSnapshot getInitialBackup() {
        return initialBackup;
    }

    @Override
    public JIPipeRunnableInfo getInfo() {
        return info;
    }

    public void setInfo(JIPipeRunnableInfo info) {
        this.info = info;
    }
}
