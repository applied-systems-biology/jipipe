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

package org.hkijena.jipipe.ui.quickrun;

import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Allows to test one algorithm with multiple parameters
 */
public class QuickRun implements JIPipeRunnable, JIPipeValidatable {
    private final JIPipeProject project;
    private final JIPipeGraphNode targetNode;
    private final QuickRunSettings settings;
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
    private JIPipeProjectRun run;
    private JIPipeGraphNode targetNodeCopy;

    /**
     * @param project    The project
     * @param targetNode The tested algorithm
     * @param settings   The settings
     */
    public QuickRun(JIPipeProject project, JIPipeGraphNode targetNode, QuickRunSettings settings) {
        this.project = project;
        this.targetNode = targetNode;
        this.settings = settings;

        initialize();
    }

    private void initialize() {
        JIPipeRunSettings configuration = new JIPipeRunSettings();
        configuration.setOutputPath(settings.getOutputPath());
        configuration.setLoadFromCache(settings.isLoadFromCache());
        configuration.setStoreToCache(settings.isStoreToCache());
        configuration.setNumThreads(settings.getNumThreads());
        configuration.setSaveToDisk(settings.isSaveToDisk());

        // This setting is needed to prevent cascading intelligent deactivation of nodes
        // due to cache optimization down to the target.
        // The test bench will handle this!
        configuration.setIgnoreDeactivatedInputs(true);

        run = new JIPipeProjectRun(project, configuration);
        run.setProgressInfo(progressInfo);
        targetNodeCopy = run.getGraph().getEquivalentAlgorithm(targetNode);
        ((JIPipeAlgorithm) targetNodeCopy).setEnabled(true);

        // Disable storing intermediate results
        if (!settings.isStoreIntermediateResults()) {
            HashSet<JIPipeGraphNode> disabled = new HashSet<>(run.getGraph().getGraphNodes());
            disabled.remove(targetNodeCopy);
            if(!settings.isStoreIntermediateResults() && settings.isExcludeSelected()) {
                for (JIPipeDataSlot inputSlot : targetNodeCopy.getInputSlots()) {
                    for (JIPipeDataSlot sourceSlot : getRun().getGraph().getSourceSlots(inputSlot)) {
                        JIPipeGraphNode node = sourceSlot.getNode();
                        disabled.remove(node);
                    }
                }
            }
            configuration.setDisableSaveToDiskNodes(disabled);
            configuration.setDisableStoreToCacheNodes(disabled);
        }
    }

    private Set<JIPipeGraphNode> findPredecessorsWithoutCache() {
        JIPipeProjectCacheQuery query = new JIPipeProjectCacheQuery(project);
        Set<JIPipeGraphNode> predecessors = new HashSet<>();
        Set<JIPipeGraphNode> handledNodes = new HashSet<>();
        Stack<JIPipeGraphNode> stack = new Stack<>();
        stack.push(targetNodeCopy);
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        while (!stack.isEmpty()) {
            JIPipeGraphNode node = stack.pop();
            for (JIPipeDataSlot inputSlot : node.getInputSlots()) {
                for (JIPipeDataSlot sourceSlot : run.getGraph().getSourceSlots(inputSlot)) {
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
        predecessors.remove(targetNodeCopy);
        return predecessors;
    }

    @Override
    public void run() {

        // Disable all algorithms that are not dependencies of the benched algorithm
        Set<JIPipeGraphNode> predecessorAlgorithms = findPredecessorsWithoutCache();
        if (!settings.isExcludeSelected())
            predecessorAlgorithms.add(targetNodeCopy);
        for (JIPipeGraphNode node : run.getGraph().getGraphNodes()) {
            if (!predecessorAlgorithms.contains(node)) {
                if (node instanceof JIPipeAlgorithm) {
                    ((JIPipeAlgorithm) node).setEnabled(false);
                }
            }
        }
        if(settings.isExcludeSelected() && !settings.isStoreIntermediateResults()) {
            for (JIPipeDataSlot inputSlot : targetNodeCopy.getInputSlots()) {
                for (JIPipeDataSlot sourceSlot : getRun().getGraph().getSourceSlots(inputSlot)) {
                    JIPipeGraphNode node = sourceSlot.getNode();
                    if(node instanceof JIPipeAlgorithm) {
                        ((JIPipeAlgorithm) node).setEnabled(true);
                    }
                }
            }
        }

        // Remove the benched algorithm from cache. This is a workaround.
        if (settings.isLoadFromCache()) {
            getProject().getCache().clear(targetNode);
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

        // Remove outdated cache if needed
        if (GeneralDataSettings.getInstance().isAutoRemoveOutdatedCachedData()) {
            project.getCache().autoClean(true, true, getProgressInfo().resolveAndLog("Remove outdated cache"));
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
    public QuickRunSettings getSettings() {
        return settings;
    }

    /**
     * The run that runs the pipeline
     *
     * @return the pipeline run
     */
    public JIPipeProjectRun getRun() {
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
    public void reportValidity(JIPipeIssueReport report) {
        targetNodeCopy.reportValidity(report);
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
