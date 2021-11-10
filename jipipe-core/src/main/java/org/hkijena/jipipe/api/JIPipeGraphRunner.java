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

package org.hkijena.jipipe.api;

import com.google.common.collect.BiMap;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.grouping.GraphWrapperAlgorithm;
import org.hkijena.jipipe.api.grouping.NodeGroup;
import org.hkijena.jipipe.api.looping.LoopGroup;
import org.hkijena.jipipe.api.looping.LoopStartNode;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Executes an {@link JIPipeGraph}.
 * This does not do any data storage or caching.
 * Use this class for nested algorithm graph runs (like {@link org.hkijena.jipipe.api.grouping.GraphWrapperAlgorithm})
 * Use {@link JIPipeRun} for full project runs.
 */
public class JIPipeGraphRunner implements JIPipeRunnable {

    private final JIPipeGraph algorithmGraph;
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
    private Set<JIPipeGraphNode> algorithmsWithExternalInput = new HashSet<>();
    private Set<JIPipeGraphNode> persistentDataNodes = new HashSet<>();
    private JIPipeFixedThreadPool threadPool;

    /**
     * Creates a new instance
     *
     * @param algorithmGraph the algorithm graph to run
     */
    public JIPipeGraphRunner(JIPipeGraph algorithmGraph) {
        this.algorithmGraph = algorithmGraph;
    }

    @Override
    public void run() {
        Set<JIPipeGraphNode> unExecutableAlgorithms = algorithmGraph.getDeactivatedAlgorithms(algorithmsWithExternalInput);
        Set<JIPipeGraphNode> executedAlgorithms = new HashSet<>();
        List<JIPipeDataSlot> traversedSlots = algorithmGraph.traverseSlots();
        Set<JIPipeDataSlot> flushedSlots = new HashSet<>();

        List<JIPipeGraphNode> preprocessorNodes = new ArrayList<>();
        List<JIPipeGraphNode> postprocessorNodes = new ArrayList<>();
        for (JIPipeGraphNode node : algorithmGraph.getGraphNodes()) {
            if (!unExecutableAlgorithms.contains(node) && node.getInputSlots().isEmpty() &&
                    node instanceof JIPipeAlgorithm && ((JIPipeAlgorithm) node).isPreprocessor()) {
                preprocessorNodes.add(node);
            } else if (!unExecutableAlgorithms.contains(node) &&
                    node instanceof JIPipeAlgorithm && ((JIPipeAlgorithm) node).isPreprocessor()) {
                if (node.getOpenInputSlots().stream().allMatch(nd -> algorithmGraph.getTargetSlots(nd).isEmpty())) {
                    postprocessorNodes.add(node);
                }
            }
        }
        if (!preprocessorNodes.isEmpty()) {
            progressInfo.setProgress(0, preprocessorNodes.size() + traversedSlots.size());
            progressInfo.resolveAndLog("Preprocessing algorithms");
            for (int i = 0; i < preprocessorNodes.size(); i++) {
                JIPipeGraphNode node = preprocessorNodes.get(i);
                progressInfo.setProgress(i);
                JIPipeProgressInfo subProgress = progressInfo.resolve(node.getName());
                node.run(subProgress);
                executedAlgorithms.add(node);
            }
        }

        // Collect loop groups
        List<LoopGroup> loopGroups = algorithmGraph.extractLoopGroups(Collections.emptySet(), unExecutableAlgorithms);
        Map<JIPipeGraphNode, LoopGroup> nodeLoops = new HashMap<>();
        for (LoopGroup loopGroup : loopGroups) {
            for (JIPipeGraphNode node : loopGroup.getNodes()) {
                nodeLoops.put(node, loopGroup);
            }
        }
        Set<LoopGroup> executedLoops = new HashSet<>();

        progressInfo.setMaxProgress(traversedSlots.size());
        for (int currentIndex = 0; currentIndex < traversedSlots.size(); ++currentIndex) {
            if (progressInfo.isCancelled())
                throw new UserFriendlyRuntimeException("Execution was cancelled",
                        "You cancelled the execution of the algorithm pipeline.",
                        "Pipeline run", "You clicked 'Cancel'.",
                        "Do not click 'Cancel' if you do not want to cancel the execution.");
            JIPipeDataSlot slot = traversedSlots.get(currentIndex);
            progressInfo.setProgress(currentIndex, traversedSlots.size());
            JIPipeProgressInfo subProgress = progressInfo.resolveAndLog(slot.getNode().getName());

            // If an algorithm cannot be executed, skip it automatically
            if (unExecutableAlgorithms.contains(slot.getNode()))
                continue;

            if (slot.isInput()) {
                if (!algorithmsWithExternalInput.contains(slot.getNode())) {
                    // Copy data from source (merging rows)
                    Set<JIPipeDataSlot> sourceSlots = algorithmGraph.getSourceSlots(slot);
                    for (JIPipeDataSlot sourceSlot : sourceSlots) {
                        slot.addData(sourceSlot, subProgress);
                    }
                }
            } else if (slot.isOutput()) {

                JIPipeGraphNode node = slot.getNode();

                // Check if this is a postprocessor
                if (!executedAlgorithms.contains(node) && postprocessorNodes.contains(node)) {
                    subProgress.resolveAndLog("Node is postprocessor. Deferring the run.");
                    continue;
                }

                LoopGroup loop = nodeLoops.getOrDefault(node, null);
                if (loop == null) {
                    // Ensure the algorithm has run
                    runNode(executedAlgorithms, node, subProgress);
                    tryFlushSlot(slot, executedAlgorithms, traversedSlots, flushedSlots, currentIndex, progressInfo);
                } else {
                    // Encountered a loop
                    if (!executedLoops.contains(loop)) {
                        subProgress = progressInfo.resolveAndLog("Loop #" + (loopGroups.indexOf(loop) + 1));
                        JIPipeGraph loopGraph = algorithmGraph.extract(loop.getNodes(), true);
                        NodeGroup group = new NodeGroup(loopGraph, false, false, true);
                        BiMap<JIPipeDataSlot, JIPipeDataSlot> loopGraphSlotMap = group.autoCreateSlots();
                        group.setIterationMode(loop.getLoopStartNode().getIterationMode());
                        group.setThreadPool(threadPool);

                        // IMPORTANT! Otherwise the nested JIPipeGraphRunner will run into an infinite depth loop
                        ((LoopStartNode) loopGraph.getEquivalentAlgorithm(loop.getLoopStartNode()))
                                .setIterationMode(GraphWrapperAlgorithm.IterationMode.PassThrough);

                        // Pass input data from inputs of loop into equivalent input of group
                        for (JIPipeDataSlot inputSlot : loop.getLoopStartNode().getInputSlots()) {
                            JIPipeDataSlot groupInput = loopGraphSlotMap.get(loopGraph.getEquivalentSlot(inputSlot));
                            groupInput.addData(inputSlot, subProgress);
                        }

                        // Execute the loop
                        group.run(subProgress);

                        // Pass output data
                        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> entry : loopGraphSlotMap.entrySet()) {
                            // Info: We need the value; the key already has cleared data!
                            if (entry.getKey().isOutput()) {
                                JIPipeDataSlot originalSlot = algorithmGraph.getEquivalentSlot(entry.getKey());
                                JIPipeDataSlot sourceSlot = entry.getValue();
                                originalSlot.addData(sourceSlot, subProgress);
                            }
                        }

                        executedLoops.add(loop);
                    }

                    // IMPORTANT!
                    executedAlgorithms.add(slot.getNode());
                    tryFlushSlot(slot, executedAlgorithms, traversedSlots, flushedSlots, currentIndex, progressInfo);
                }
            }
        }

        // There might be some algorithms missing (ones that do not have an output)
        // Will also run any postprocessor
        List<JIPipeGraphNode> additionalAlgorithms = new ArrayList<>();
        for (JIPipeGraphNode node : algorithmGraph.getGraphNodes()) {
            if (progressInfo.isCancelled())
                break;
            if (!executedAlgorithms.contains(node) && !unExecutableAlgorithms.contains(node)) {
                additionalAlgorithms.add(node);
            }
        }
        progressInfo.setMaxProgress(progressInfo.getProgress() + additionalAlgorithms.size());
        for (int index = 0; index < additionalAlgorithms.size(); index++) {
            if (progressInfo.isCancelled())
                break;
            JIPipeGraphNode node = additionalAlgorithms.get(index);
            int absoluteIndex = index + preprocessorNodes.size() + traversedSlots.size() - 1;
            progressInfo.setProgress(absoluteIndex);
            JIPipeProgressInfo subProgress = progressInfo.resolve(node.getName());
            runNode(executedAlgorithms, node, subProgress);
        }
    }

    private void tryFlushSlot(JIPipeDataSlot slot, Set<JIPipeGraphNode> executedAlgorithms, List<JIPipeDataSlot> traversedSlots, Set<JIPipeDataSlot> flushedSlots, int currentIndex, JIPipeProgressInfo progressInfo) {
        if (!executedAlgorithms.contains(slot.getNode()))
            return;
        if (flushedSlots.contains(slot))
            return;
        if(persistentDataNodes.contains(slot.getNode()))
            return;
        boolean canFlush = true;
        for (int j = currentIndex + 1; j < traversedSlots.size(); ++j) {
            JIPipeDataSlot futureSlot = traversedSlots.get(j);
            boolean isDeactivated = (futureSlot.getNode() instanceof JIPipeAlgorithm) && (!((JIPipeAlgorithm) futureSlot.getNode()).isEnabled());
            if (!isDeactivated) {
                if(slot.isOutput() && futureSlot.isInput() && algorithmGraph.getSourceSlots(futureSlot).contains(slot)) {
                    canFlush = false;
                    break;
                }
                else if(slot.isInput() && futureSlot.isOutput() && algorithmGraph.getTargetSlots(futureSlot).contains(slot)) {
                    canFlush = false;
                    break;
                }
            }
        }
        if (canFlush) {
            progressInfo.log("Clearing slot " + slot.getDisplayName());
            slot.clearData();
        }
    }

    private void runNode(Set<JIPipeGraphNode> executedAlgorithms, JIPipeGraphNode node, JIPipeProgressInfo subProgress) {
        if (!executedAlgorithms.contains(node)) {
            try {
                node.run(subProgress);
            } catch (Exception e) {
                throw new UserFriendlyRuntimeException("Algorithm " + node + " raised an exception!",
                        e,
                        "An error occurred during processing",
                        "On running the algorithm '" + node.getName() + "', within graph '" + algorithmGraph + "'",
                        "Please refer to the other error messages.",
                        "Please follow the instructions for the other error messages.");
            }

            executedAlgorithms.add(node);
        }
    }

    public JIPipeGraph getAlgorithmGraph() {
        return algorithmGraph;
    }

    /**
     * @return Algorithms that have an external input and therefore are not invalid and are runnable
     */
    public Set<JIPipeGraphNode> getAlgorithmsWithExternalInput() {
        return algorithmsWithExternalInput;
    }

    public void setAlgorithmsWithExternalInput(Set<JIPipeGraphNode> algorithmsWithExternalInput) {
        this.algorithmsWithExternalInput = algorithmsWithExternalInput;
    }

    @Override
    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    public void setProgressInfo(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    @Override
    public String getTaskLabel() {
        return "Run graph";
    }

    public JIPipeFixedThreadPool getThreadPool() {
        return threadPool;
    }

    public void setThreadPool(JIPipeFixedThreadPool threadPool) {
        this.threadPool = threadPool;
    }

    public Set<JIPipeGraphNode> getPersistentDataNodes() {
        return persistentDataNodes;
    }

    public void setPersistentDataNodes(Set<JIPipeGraphNode> persistentDataNodes) {
        this.persistentDataNodes = persistentDataNodes;
    }
}
