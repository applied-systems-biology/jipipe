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

import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Allows to test one algorithm with multiple parameters
 */
public class JIPipeTestBench implements JIPipeRunnable, JIPipeValidatable {
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
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
        if(settings.isSaveToDisk())
            configuration.setOutputPath(settings.getOutputPath().resolve("initial"));
        else
            configuration.setOutputPath(settings.getOutputPath());
        configuration.setLoadFromCache(settings.isLoadFromCache());
        configuration.setStoreToCache(settings.isStoreToCache());
        configuration.setNumThreads(settings.getNumThreads());
        configuration.setSaveToDisk(settings.isSaveToDisk());

        // This setting is needed to prevent cascading intelligent deactivation of nodes
        // due to cache optimization down to the target.
        // The test bench will handle this!
        configuration.setIgnoreDeactivatedInputs(true);

        testBenchRun = new JIPipeRun(project, configuration);
        testBenchRun.setProgressInfo(progressInfo);
        benchedAlgorithm = testBenchRun.getGraph().getEquivalentAlgorithm(projectAlgorithm);
        ((JIPipeAlgorithm) benchedAlgorithm).setEnabled(true);

        // Disable all algorithms that are not dependencies of the benched algorithm
//        List<JIPipeGraphNode> predecessorAlgorithms = testBenchRun.getGraph()
//                .getPredecessorAlgorithms(benchedAlgorithm, testBenchRun.getGraph().traverse());
        Set<JIPipeGraphNode> predecessorAlgorithms = findPredecessorsWithoutCache();
        if (!settings.isExcludeSelected())
            predecessorAlgorithms.add(benchedAlgorithm);
        for (JIPipeGraphNode node : testBenchRun.getGraph().getGraphNodes()) {
            if (!predecessorAlgorithms.contains(node)) {
                if (node instanceof JIPipeAlgorithm) {
                    ((JIPipeAlgorithm) node).setEnabled(false);
                }
            }
        }

        // Disable storing intermediate results
        if (!settings.isStoreIntermediateResults()) {
            HashSet<JIPipeGraphNode> disabled = new HashSet<>(testBenchRun.getGraph().getGraphNodes());
            disabled.remove(benchedAlgorithm);
            configuration.setDisableSaveToDiskNodes(disabled);
            configuration.setDisableStoreToCacheNodes(disabled);
        }
    }

    private Set<JIPipeGraphNode> findPredecessorsWithoutCache() {
        JIPipeProjectCacheQuery query = new JIPipeProjectCacheQuery(project);
        Set<JIPipeGraphNode> predecessors = new HashSet<>();
        Set<JIPipeGraphNode> handledNodes = new HashSet<>();
        Stack<JIPipeGraphNode> stack = new Stack<>();
        stack.push(benchedAlgorithm);
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        while (!stack.isEmpty()) {
            JIPipeGraphNode node = stack.pop();
            for (JIPipeDataSlot inputSlot : node.getInputSlots()) {
                for (JIPipeDataSlot sourceSlot : testBenchRun.getGraph().getSourceSlots(inputSlot)) {
                    JIPipeGraphNode predecessorNode = sourceSlot.getNode();
                    if (handledNodes.contains(predecessorNode))
                        continue;
                    handledNodes.add(predecessorNode);
                    if (!predecessorNode.getInfo().isRunnable())
                        continue;
                    JIPipeGraphNode projectPredecessorNode = project.getGraph().getEquivalentAlgorithm(predecessorNode);
                    Map<String, JIPipeDataSlot> cache = query.getCachedCache(projectPredecessorNode);

                    if (cache.isEmpty()) {
                        // The cache is empty -> This is now a predecessor and must be executed.
                        // Continue to search for its predecessors
                        predecessors.add(predecessorNode);
                        stack.push(predecessorNode);
                    } else {
                        // If the cache is not empty, end searching this branch (we are satisfied)
                        // We will copy over the values
                        for (Map.Entry<String, JIPipeDataSlot> cacheEntry : cache.entrySet()) {
                            JIPipeDataSlot outputSlot = predecessorNode.getOutputSlot(cacheEntry.getKey());
                            outputSlot.addData(cacheEntry.getValue(), progressInfo);
                        }
                    }
                }
            }
        }
        // To be sure
        predecessors.remove(benchedAlgorithm);
        return predecessors;
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
        for (JIPipeGraphNode node : testBenchRun.getGraph().getGraphNodes()) {
            for (JIPipeDataSlot inputSlot : node.getInputSlots()) {
                inputSlot.clearData();
            }
            for (JIPipeDataSlot outputSlot : node.getOutputSlots()) {
                outputSlot.clearData();
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
    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public void setProgressInfo(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    @Override
    public String getTaskLabel() {
        return "Quick run / Update cache";
    }
}
