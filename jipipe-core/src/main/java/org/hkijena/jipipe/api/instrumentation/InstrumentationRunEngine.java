package org.hkijena.jipipe.api.instrumentation;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.grouping.JIPipeGraphWrapperAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeGraphRun;
import org.hkijena.jipipe.api.run.JIPipeGraphRunConfiguration;
import org.hkijena.jipipe.api.runtimepartitioning.JIPipeRuntimePartition;
import org.hkijena.jipipe.plugins.settings.application.JIPipeGeneralDataApplicationSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeRuntimeApplicationSettings;

import java.util.*;

/**
 * Core run engine that extracts the pipeline execution logic from
 * {@link org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRun}.
 * This class is headless-compatible (no Swing dependencies).
 */
public class InstrumentationRunEngine {

    private final JIPipeProject project;
    private final JIPipeProgressInfo progressInfo;

    public InstrumentationRunEngine(JIPipeProject project, JIPipeProgressInfo progressInfo) {
        this.project = project;
        this.progressInfo = progressInfo != null ? progressInfo : new JIPipeProgressInfo();
    }

    /**
     * Runs the pipeline up to one or more target nodes with the given mode.
     */
    public void runNodes(List<JIPipeGraphNode> targetNodes, RunMode mode, InstrumentationJob job) {
        boolean storeToCache = mode != RunMode.PIPELINE_DISCARD;
        boolean storeToDisk = mode == RunMode.PIPELINE_FILESYSTEM;
        boolean storeIntermediate = mode == RunMode.CACHE_INTERMEDIATE;
        boolean excludeSelected = mode == RunMode.UPDATE_PREDECESSOR_CACHE;

        JIPipeGraphRunConfiguration config = new JIPipeGraphRunConfiguration();
        config.setOutputPath(project.newTemporaryDirectory());
        config.setLoadFromCache(true);
        config.setStoreToCache(storeToCache);
        config.setNumThreads(JIPipeRuntimeApplicationSettings.getInstance().getDefaultQuickRunThreads());
        config.setStoreToDisk(storeToDisk);
        config.setSilent(true);
        config.setIgnoreDeactivatedInputs(true);

        JIPipeGraphRun run = new JIPipeGraphRun(project, config);
        run.setProgressInfo(progressInfo);

        List<JIPipeGraphNode> targetNodeCopies = new ArrayList<>();
        for (JIPipeGraphNode targetNode : targetNodes) {
            JIPipeGraphNode equivalentNode = run.getGraph().getEquivalentNode(targetNode);
            targetNodeCopies.add(equivalentNode);
            if (equivalentNode instanceof JIPipeAlgorithm) {
                ((JIPipeAlgorithm) equivalentNode).setEnabled(true);
            }
        }

        if (!storeIntermediate) {
            HashSet<UUID> disabled = new HashSet<>(run.getGraph().getGraphNodeUUIDs());
            for (JIPipeGraphNode targetNodeCopy : targetNodeCopies) {
                disabled.remove(targetNodeCopy.getUUIDInParentGraph());
            }
            if (excludeSelected) {
                for (JIPipeGraphNode targetNodeCopy : targetNodeCopies) {
                    for (JIPipeDataSlot inputSlot : targetNodeCopy.getInputSlots()) {
                        for (JIPipeDataSlot sourceSlot : run.getGraph().getInputIncomingSourceSlots(inputSlot)) {
                            JIPipeGraphNode node = sourceSlot.getNode();
                            disabled.remove(node.getUUIDInParentGraph());
                        }
                    }
                }
            }
            config.setDisableStoreToDiskNodes(disabled);
            config.setDisableStoreToCacheNodes(disabled);
        }

        // Find predecessors without cache
        Set<JIPipeGraphNode> predecessorAlgorithms = findPredecessorsWithoutCache(run, targetNodeCopies);
        if (!excludeSelected) {
            predecessorAlgorithms.addAll(targetNodeCopies);
        }
        for (JIPipeGraphNode node : run.getGraph().getGraphNodes()) {
            if (!predecessorAlgorithms.contains(node)) {
                if (node instanceof JIPipeAlgorithm) {
                    ((JIPipeAlgorithm) node).setSkipped(true);
                }
            }
        }
        if (excludeSelected && !storeIntermediate) {
            for (JIPipeGraphNode targetNodeCopy : targetNodeCopies) {
                for (JIPipeDataSlot inputSlot : targetNodeCopy.getInputSlots()) {
                    for (JIPipeDataSlot sourceSlot : run.getGraph().getInputIncomingSourceSlots(inputSlot)) {
                        JIPipeGraphNode node = sourceSlot.getNode();
                        if (node instanceof JIPipeAlgorithm) {
                            ((JIPipeAlgorithm) node).setSkipped(false);
                        }
                    }
                }
            }
        }

        // Remove target from cache
        if (config.isLoadFromCache()) {
            for (JIPipeGraphNode targetNode : targetNodes) {
                project.getCache().softClear(targetNode.getUUIDInParentGraph(), progressInfo);
            }
        }

        // Remove outdated cache
        if (JIPipeGeneralDataApplicationSettings.getInstance().isAutoRemoveOutdatedCachedData()) {
            project.getCache().clearOutdated(progressInfo.resolveAndLog("Remove outdated cache"));
        }

        job.setStatus(InstrumentationJobStatus.RUNNING);
        run.run();

        // Clear data
        for (JIPipeGraphNode node : run.getGraph().getGraphNodes()) {
            for (JIPipeDataSlot inputSlot : node.getInputSlots()) {
                inputSlot.clearData(false, progressInfo);
            }
            for (JIPipeDataSlot outputSlot : node.getOutputSlots()) {
                outputSlot.clearData(false, progressInfo);
            }
        }
    }

    /**
     * Runs the entire pipeline with the given mode.
     */
    public void runPipeline(RunMode mode, InstrumentationJob job) {
        boolean storeToCache = mode != RunMode.PIPELINE_DISCARD;
        boolean storeToDisk = mode == RunMode.PIPELINE_FILESYSTEM;

        JIPipeGraphRunConfiguration config = new JIPipeGraphRunConfiguration();
        config.setOutputPath(project.newTemporaryDirectory());
        config.setLoadFromCache(true);
        config.setStoreToCache(storeToCache);
        config.setNumThreads(JIPipeRuntimeApplicationSettings.getInstance().getDefaultQuickRunThreads());
        config.setStoreToDisk(storeToDisk);
        config.setSilent(true);

        JIPipeGraphRun run = new JIPipeGraphRun(project, config);
        run.setProgressInfo(progressInfo);

        job.setStatus(InstrumentationJobStatus.RUNNING);
        run.run();
    }

    private Set<JIPipeGraphNode> findPredecessorsWithoutCache(JIPipeGraphRun run, List<JIPipeGraphNode> targetNodeCopies) {
        Set<JIPipeGraphNode> predecessors = new HashSet<>();
        Set<JIPipeGraphNode> handledNodes = new HashSet<>();
        Stack<JIPipeGraphNode> stack = new Stack<>();
        for (JIPipeGraphNode targetNodeCopy : targetNodeCopies) {
            stack.push(targetNodeCopy);
        }
        JIPipeProgressInfo pi = new JIPipeProgressInfo();
        while (!stack.isEmpty()) {
            JIPipeGraphNode node = stack.pop();
            for (JIPipeDataSlot inputSlot : node.getInputSlots()) {
                Set<JIPipeDataSlot> inputIncomingSourceSlots = run.getGraph().getInputIncomingSourceSlotsNoTunnel(inputSlot);
                for (JIPipeDataSlot sourceSlot : inputIncomingSourceSlots) {
                    JIPipeGraphNode predecessorNode = sourceSlot.getNode();
                    if (handledNodes.contains(predecessorNode)) {
                        continue;
                    }
                    handledNodes.add(predecessorNode);
                    if (!predecessorNode.getInfo().isRunnable()) {
                        continue;
                    }
                    if (predecessorNode instanceof JIPipeAlgorithm) {
                        JIPipeRuntimePartition runtimePartition = run.getRuntimePartition(((JIPipeAlgorithm) predecessorNode).getRuntimePartition());
                        if (runtimePartition.getIterationMode() != JIPipeGraphWrapperAlgorithm.IterationMode.PassThrough) {
                            predecessors.add(predecessorNode);
                            stack.push(predecessorNode);
                            continue;
                        }
                    }
                    JIPipeGraphNode projectPredecessorNode = project.getGraph().getEquivalentNode(predecessorNode);
                    Map<String, JIPipeDataTable> slotMap = project.getCache().query(projectPredecessorNode, projectPredecessorNode.getUUIDInParentGraph(), pi);
                    if (slotMap.isEmpty()) {
                        predecessors.add(predecessorNode);
                        stack.push(predecessorNode);
                    } else {
                        for (Map.Entry<String, JIPipeDataTable> cacheEntry : slotMap.entrySet()) {
                            JIPipeDataSlot outputSlot = predecessorNode.getOutputSlot(cacheEntry.getKey());
                            outputSlot.addDataFromTable(cacheEntry.getValue(), pi);
                        }
                    }
                }
            }
        }
        for (JIPipeGraphNode targetNodeCopy : targetNodeCopies) {
            predecessors.remove(targetNodeCopy);
        }
        return predecessors;
    }
}
