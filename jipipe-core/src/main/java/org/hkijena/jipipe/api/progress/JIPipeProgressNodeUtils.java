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

package org.hkijena.jipipe.api.progress;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

/**
 * Read-side helpers over a {@link JIPipeProgressLog}'s node index, shared by batch-event consumers
 * (queue UI, instrumentation) that display the <b>live node state</b> of a run instead of the root
 * handle's counters (which stay at their initial values).
 */
public final class JIPipeProgressNodeUtils {

    private JIPipeProgressNodeUtils() {
    }

    /**
     * Joins the labels of all ancestors of the given node up to the root with {@code |}.
     * Empty or blank labels (a detached progress node) contribute no segment; the root's label is
     * not part of the chain.
     *
     * @param store  the progress store
     * @param nodeId the node id
     * @return the label chain (empty for root or unknown nodes)
     */
    public static String labelChain(JIPipeProgressLog store, int nodeId) {
        Deque<String> labels = new ArrayDeque<>();
        JIPipeProgressNode node = store.getNode(nodeId);
        while (node != null && node.getId() != store.getRootNodeId()) {
            if (node.getLabel() != null && !node.getLabel().isBlank()) {
                labels.addFirst(node.getLabel());
            }
            int parentId = node.getParentId();
            if (parentId <= 0 || parentId == store.getRootNodeId()) {
                break;
            }
            node = store.getNode(parentId);
        }
        return String.join(" | ", labels);
    }

    /**
     * Returns the id of the most recently active node among the given changed node ids (the node
     * whose last attributed record has the highest line number). Consumers track this node to
     * display the run's latest progress state.
     *
     * @param store           the progress store
     * @param changedNodeIds the node ids changed within a batch
     * @return the latest node id, or {@code 0} if none of the ids is known to the store
     */
    public static int latestNodeId(JIPipeProgressLog store, Set<Integer> changedNodeIds) {
        int bestId = 0;
        int bestLine = -1;
        for (Integer changedId : changedNodeIds) {
            if (changedId == null) {
                continue;
            }
            JIPipeProgressNode node = store.getNode(changedId);
            if (node != null && node.getLastLine() > bestLine) {
                bestLine = node.getLastLine();
                bestId = node.getId();
            }
        }
        return bestId;
    }

    /**
     * Returns the progress text of a node ({@code v/mx}), or an empty string for indeterminate
     * progress ({@code max <= 0}, including the unset initial state).
     *
     * @param node the node
     * @return the progress text, or ""
     */
    public static String progressText(JIPipeProgressNode node) {
        if (node == null || node.getMaxProgress() <= 0) {
            return "";
        }
        return node.getProgress() + "/" + node.getMaxProgress();
    }
}
