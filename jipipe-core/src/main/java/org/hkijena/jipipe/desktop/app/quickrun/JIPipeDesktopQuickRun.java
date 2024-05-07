/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.app.quickrun;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.grouping.JIPipeGraphWrapperAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeGraphRun;
import org.hkijena.jipipe.api.run.JIPipeGraphRunConfiguration;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.runtimepartitioning.JIPipeRuntimePartition;
import org.hkijena.jipipe.api.validation.JIPipeValidatable;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.plugins.settings.JIPipeGeneralDataApplicationSettings;

import java.util.*;

/**
 * Allows to test one algorithm with multiple parameters
 */
public class JIPipeDesktopQuickRun implements JIPipeRunnable, JIPipeValidatable {
    private final JIPipeProject project;
    private final JIPipeGraphNode targetNode;
    private final JIPipeDesktopQuickRunSettings settings;
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
    private JIPipeGraphRun run;
    private JIPipeGraphNode targetNodeCopy;

    /**
     * @param project    The project
     * @param targetNode The tested algorithm
     * @param settings   The settings
     */
    public JIPipeDesktopQuickRun(JIPipeProject project, JIPipeGraphNode targetNode, JIPipeDesktopQuickRunSettings settings) {
        this.project = project;
        this.targetNode = targetNode;
        this.settings = settings;

        initialize();
    }

    private void initialize() {
        JIPipeGraphRunConfiguration configuration = new JIPipeGraphRunConfiguration();
        configuration.setOutputPath(settings.getOutputPath());
        configuration.setLoadFromCache(settings.isLoadFromCache());
        configuration.setStoreToCache(settings.isStoreToCache());
        configuration.setNumThreads(settings.getNumThreads());
        configuration.setStoreToDisk(settings.isSaveToDisk());
        configuration.setSilent(settings.isSilent());

        // This setting is needed to prevent cascading intelligent deactivation of nodes
        // due to cache optimization down to the target.
        // The test bench will handle this!
        configuration.setIgnoreDeactivatedInputs(true);

        run = new JIPipeGraphRun(project, configuration);
        run.setProgressInfo(progressInfo);
        targetNodeCopy = run.getGraph().getEquivalentNode(targetNode);
        ((JIPipeAlgorithm) targetNodeCopy).setEnabled(true);

        // Disable storing intermediate results
        if (!settings.isStoreIntermediateResults()) {
            HashSet<UUID> disabled = new HashSet<>(run.getGraph().getGraphNodeUUIDs());
            disabled.remove(targetNodeCopy.getUUIDInParentGraph());
            if (!settings.isStoreIntermediateResults() && settings.isExcludeSelected()) {
                for (JIPipeDataSlot inputSlot : targetNodeCopy.getInputSlots()) {
                    for (JIPipeDataSlot sourceSlot : getRun().getGraph().getInputIncomingSourceSlots(inputSlot)) {
                        JIPipeGraphNode node = sourceSlot.getNode();
                        disabled.remove(node.getUUIDInParentGraph());
                    }
                }
            }
            configuration.setDisableStoreToDiskNodes(disabled);
            configuration.setDisableStoreToCacheNodes(disabled);
        }
    }

    private Set<JIPipeGraphNode> findPredecessorsWithoutCache() {
        Set<JIPipeGraphNode> predecessors = new HashSet<>();
        Set<JIPipeGraphNode> handledNodes = new HashSet<>();
        Stack<JIPipeGraphNode> stack = new Stack<>();
        stack.push(targetNodeCopy);
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        while (!stack.isEmpty()) {
            JIPipeGraphNode node = stack.pop();
            for (JIPipeDataSlot inputSlot : node.getInputSlots()) {
                for (JIPipeDataSlot sourceSlot : run.getGraph().getInputIncomingSourceSlots(inputSlot)) {
                    JIPipeGraphNode predecessorNode = sourceSlot.getNode();
                    if (handledNodes.contains(predecessorNode)) {
                        continue;
                    }
                    handledNodes.add(predecessorNode);
                    if (!predecessorNode.getInfo().isRunnable()) {
                        continue;
                    }

                    // Check if we are in an iterating partition --> cache not available
                    if (predecessorNode instanceof JIPipeAlgorithm) {
                        JIPipeRuntimePartition runtimePartition = run.getRuntimePartition(((JIPipeAlgorithm) predecessorNode).getRuntimePartition());
                        if (runtimePartition.getIterationMode() != JIPipeGraphWrapperAlgorithm.IterationMode.PassThrough) {
                            if (!settings.isStoreToCache() || !runtimePartition.isForcePassThroughLoopIterationInCaching()) {
                                // predecessor is in a loop condition --> don't access cache
                                predecessors.add(predecessorNode);
                                stack.push(predecessorNode);
                                continue;
                            }
                        }
                    }

                    // Check the cache
                    JIPipeGraphNode projectPredecessorNode = project.getGraph().getEquivalentNode(predecessorNode);
                    Map<String, JIPipeDataTable> slotMap = project.getCache().query(projectPredecessorNode, projectPredecessorNode.getUUIDInParentGraph(), progressInfo);

                    if (slotMap.isEmpty()) {
                        // The cache is empty -> This is now a predecessor and must be executed.
                        // Continue to search for its predecessors
                        predecessors.add(predecessorNode);
                        stack.push(predecessorNode);
                    } else {
                        // If the cache is not empty, end searching this branch (we are satisfied)
                        // We will copy over the values
                        for (Map.Entry<String, JIPipeDataTable> cacheEntry : slotMap.entrySet()) {
                            JIPipeDataSlot outputSlot = predecessorNode.getOutputSlot(cacheEntry.getKey());
                            outputSlot.addDataFromTable(cacheEntry.getValue(), progressInfo);
                        }
                    }
                }
            }
        }
        // To be sure
        predecessors.remove(targetNodeCopy);
        return predecessors;
    }

    @Override
    public void run() {

        // Remove outdated cache if needed
        if (JIPipeGeneralDataApplicationSettings.getInstance().isAutoRemoveOutdatedCachedData()) {
            project.getCache().clearOutdated(getProgressInfo().resolveAndLog("Remove outdated cache"));
        }

        // Disable all algorithms that are not dependencies of the target algorithm
        Set<JIPipeGraphNode> predecessorAlgorithms = findPredecessorsWithoutCache();
        if (!settings.isExcludeSelected())
            predecessorAlgorithms.add(targetNodeCopy);
        for (JIPipeGraphNode node : run.getGraph().getGraphNodes()) {
            if (!predecessorAlgorithms.contains(node)) {
                if (node instanceof JIPipeAlgorithm) {
                    ((JIPipeAlgorithm) node).setSkipped(true);
                }
            }
        }
        if (settings.isExcludeSelected() && !settings.isStoreIntermediateResults()) {
            for (JIPipeDataSlot inputSlot : targetNodeCopy.getInputSlots()) {
                for (JIPipeDataSlot sourceSlot : getRun().getGraph().getInputIncomingSourceSlots(inputSlot)) {
                    JIPipeGraphNode node = sourceSlot.getNode();
                    if (node instanceof JIPipeAlgorithm) {
                        ((JIPipeAlgorithm) node).setSkipped(false);
                    }
                }
            }
        }

        // Remove the benched algorithm from cache. This is a workaround.
        if (settings.isLoadFromCache()) {
            getProject().getCache().softClear(targetNode.getUUIDInParentGraph(), progressInfo);
        }

        // Run the internal graph runner
        run.run();

        // Clear all data
        for (JIPipeGraphNode node : run.getGraph().getGraphNodes()) {
            for (JIPipeDataSlot inputSlot : node.getInputSlots()) {
                inputSlot.clearData();
            }
            for (JIPipeDataSlot outputSlot : node.getOutputSlots()) {
                outputSlot.clearData();
            }
        }
    }

    /**
     * The project where the run was created
     *
     * @return the project
     */
    public JIPipeProject getProject() {
        return project;
    }

    /**
     * The node targeted to be executed
     * In project graph
     *
     * @return the target node
     */
    public JIPipeGraphNode getTargetNode() {
        return targetNode;
    }

    /**
     * Settings for this quick run
     *
     * @return the settings
     */
    public JIPipeDesktopQuickRunSettings getSettings() {
        return settings;
    }

    /**
     * The run that runs the pipeline
     *
     * @return the pipeline run
     */
    public JIPipeGraphRun getRun() {
        return run;
    }

    /**
     * Copy of the targeted node (in the run's graph)
     *
     * @return copy of the targeted node
     */
    public JIPipeGraphNode getTargetNodeCopy() {
        return targetNodeCopy;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        targetNodeCopy.reportValidity(reportContext, report);
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
        return "Run";
    }
}
