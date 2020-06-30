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

package org.hkijena.acaq5.api;

import org.hkijena.acaq5.api.algorithm.ACAQGraph;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Executes an {@link ACAQGraph}.
 * This does not do any data storage or caching.
 * Use this class for nested algorithm graph runs (like {@link org.hkijena.acaq5.api.grouping.GraphWrapperAlgorithm})
 * Use {@link ACAQRun} for full project runs.
 */
public class ACAQGraphRunner implements ACAQRunnable {

    private final ACAQGraph algorithmGraph;
    private Set<ACAQGraphNode> algorithmsWithExternalInput = new HashSet<>();

    /**
     * Creates a new instance
     *
     * @param algorithmGraph the algorithm graph to run
     */
    public ACAQGraphRunner(ACAQGraph algorithmGraph) {
        this.algorithmGraph = algorithmGraph;
    }

    @Override
    public void run(Consumer<ACAQRunnerStatus> onProgress, Supplier<Boolean> isCancelled) {
        Set<ACAQGraphNode> unExecutableAlgorithms = algorithmGraph.getDeactivatedAlgorithms(algorithmsWithExternalInput);
        Set<ACAQGraphNode> executedAlgorithms = new HashSet<>();
        List<ACAQDataSlot> traversedSlots = algorithmGraph.traverseSlots();

        for (int index = 0; index < traversedSlots.size(); ++index) {
            if (isCancelled.get())
                throw new UserFriendlyRuntimeException("Execution was cancelled",
                        "You cancelled the execution of the algorithm pipeline.",
                        "Pipeline run", "You clicked 'Cancel'.",
                        "Do not click 'Cancel' if you do not want to cancel the execution.");
            ACAQDataSlot slot = traversedSlots.get(index);
            logStatus(onProgress, new ACAQRunnerStatus(index, algorithmGraph.getSlotCount(), slot.getNameWithAlgorithmName()));

            // If an algorithm cannot be executed, skip it automatically
            if (unExecutableAlgorithms.contains(slot.getAlgorithm()))
                continue;

            // Let algorithms provide sub-progress
            String statusMessage = "Algorithm: " + slot.getAlgorithm().getName();
            int traversingIndex = index;
            Consumer<ACAQRunnerSubStatus> algorithmProgress = s -> logStatus(onProgress, new ACAQRunnerStatus(traversingIndex, traversedSlots.size(),
                    statusMessage + " | " + s));

            if (slot.isInput()) {
                if (!algorithmsWithExternalInput.contains(slot.getAlgorithm())) {
                    // Copy data from source
                    ACAQDataSlot sourceSlot = algorithmGraph.getSourceSlot(slot);
                    slot.copyFrom(sourceSlot);
                }
            } else if (slot.isOutput()) {
                // Ensure the algorithm has run
                if (!executedAlgorithms.contains(slot.getAlgorithm())) {
                    onProgress.accept(new ACAQRunnerStatus(index, traversedSlots.size(), statusMessage));

                    try {
                        slot.getAlgorithm().run(new ACAQRunnerSubStatus(), algorithmProgress, isCancelled);
                    } catch (Exception e) {
                        throw new UserFriendlyRuntimeException("Algorithm " + slot.getAlgorithm() + " raised an exception!",
                                e,
                                "An error occurred during processing",
                                "On running the algorithm '" + slot.getAlgorithm().getName() + "', within graph '" + algorithmGraph + "'",
                                "Please refer to the other error messages.",
                                "Please follow the instructions for the other error messages.");
                    }

                    executedAlgorithms.add(slot.getAlgorithm());
                }
            }
        }
    }

    private void logStatus(Consumer<ACAQRunnerStatus> onProgress, ACAQRunnerStatus status) {
        onProgress.accept(status);
    }

    public ACAQGraph getAlgorithmGraph() {
        return algorithmGraph;
    }

    /**
     * @return Algorithms that have an external input and therefore are not invalid and are runnable
     */
    public Set<ACAQGraphNode> getAlgorithmsWithExternalInput() {
        return algorithmsWithExternalInput;
    }

    public void setAlgorithmsWithExternalInput(Set<ACAQGraphNode> algorithmsWithExternalInput) {
        this.algorithmsWithExternalInput = algorithmsWithExternalInput;
    }
}
