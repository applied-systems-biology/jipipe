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

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

        List<JIPipeGraphNode> preprocessorNodes = new ArrayList<>();
        List<JIPipeGraphNode> postprocessorNodes = new ArrayList<>();
        for (JIPipeGraphNode node : algorithmGraph.getNodes().values()) {
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
                JIPipeProgressInfo subProgress = progressInfo.resolve("Algorithm: " + node.getName());
                node.run(subProgress);
                executedAlgorithms.add(node);
            }
        }

        progressInfo.setMaxProgress(traversedSlots.size());
        for (int index = 0; index < traversedSlots.size(); ++index) {
            if (progressInfo.isCancelled().get())
                throw new UserFriendlyRuntimeException("Execution was cancelled",
                        "You cancelled the execution of the algorithm pipeline.",
                        "Pipeline run", "You clicked 'Cancel'.",
                        "Do not click 'Cancel' if you do not want to cancel the execution.");
            JIPipeDataSlot slot = traversedSlots.get(index);
            progressInfo.setProgress(index, traversedSlots.size());
            JIPipeProgressInfo subProgress = progressInfo.resolveAndLog("Algorithm: " + slot.getNode().getName());

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
                }

                // Ensure the algorithm has run
                runNode(executedAlgorithms, node, subProgress);
            }
        }

        // There might be some algorithms missing (ones that do not have an output)
        // Will also run any postprocessor
        List<JIPipeGraphNode> additionalAlgorithms = new ArrayList<>();
        for (JIPipeGraphNode node : algorithmGraph.getNodes().values()) {
            if (progressInfo.isCancelled().get())
                break;
            if (!executedAlgorithms.contains(node) && !unExecutableAlgorithms.contains(node)) {
                additionalAlgorithms.add(node);
            }
        }
        progressInfo.setMaxProgress(progressInfo.getProgress() + additionalAlgorithms.size());
        for (int index = 0; index < additionalAlgorithms.size(); index++) {
            if (progressInfo.isCancelled().get())
                break;
            JIPipeGraphNode node = additionalAlgorithms.get(index);
            int absoluteIndex = index + preprocessorNodes.size() + traversedSlots.size() - 1;
            progressInfo.setProgress(absoluteIndex);
            JIPipeProgressInfo subProgress = progressInfo.resolve("Algorithm: " + node.getName());
            runNode(executedAlgorithms, node, subProgress);
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
}
