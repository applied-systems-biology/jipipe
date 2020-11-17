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
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

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
    private JIPipeProgressInfo info = new JIPipeProgressInfo();
    private Set<JIPipeGraphNode> algorithmsWithExternalInput = new HashSet<>();

    /**
     * Creates a new instance
     *
     * @param algorithmGraph the algorithm graph to run
     */
    public JIPipeGraphRunner(JIPipeGraph algorithmGraph) {
        this.algorithmGraph = algorithmGraph;
    }

    public void setInfo(JIPipeProgressInfo info) {
        this.info = info;
    }

    @Override
    public void run() {
        Set<JIPipeGraphNode> unExecutableAlgorithms = algorithmGraph.getDeactivatedAlgorithms(algorithmsWithExternalInput);
        Set<JIPipeGraphNode> executedAlgorithms = new HashSet<>();
        List<JIPipeDataSlot> traversedSlots = algorithmGraph.traverseSlots();
        info.setMaxProgress(traversedSlots.size());
        for (int index = 0; index < traversedSlots.size(); ++index) {
            if (info.isCancelled().get())
                throw new UserFriendlyRuntimeException("Execution was cancelled",
                        "You cancelled the execution of the algorithm pipeline.",
                        "Pipeline run", "You clicked 'Cancel'.",
                        "Do not click 'Cancel' if you do not want to cancel the execution.");
            JIPipeDataSlot slot = traversedSlots.get(index);
            info.setProgress(index, traversedSlots.size());
            JIPipeProgressInfo subInfo = info.resolveAndLog("Algorithm: " + slot.getNode().getName());

            // If an algorithm cannot be executed, skip it automatically
            if (unExecutableAlgorithms.contains(slot.getNode()))
                continue;

            if (slot.isInput()) {
                if (!algorithmsWithExternalInput.contains(slot.getNode())) {
                    // Copy data from source (merging rows)
                    Set<JIPipeDataSlot> sourceSlots = algorithmGraph.getSourceSlots(slot);
                    for (JIPipeDataSlot sourceSlot : sourceSlots) {
                        slot.addData(sourceSlot);
                    }
                }
            } else if (slot.isOutput()) {
                // Ensure the algorithm has run
                if (!executedAlgorithms.contains(slot.getNode())) {
                    try {
                        slot.getNode().run(subInfo);
                    } catch (Exception e) {
                        throw new UserFriendlyRuntimeException("Algorithm " + slot.getNode() + " raised an exception!",
                                e,
                                "An error occurred during processing",
                                "On running the algorithm '" + slot.getNode().getName() + "', within graph '" + algorithmGraph + "'",
                                "Please refer to the other error messages.",
                                "Please follow the instructions for the other error messages.");
                    }

                    executedAlgorithms.add(slot.getNode());
                }
            }
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
        return info;
    }

    @Override
    public String getTaskLabel() {
        return "Run graph";
    }
}
