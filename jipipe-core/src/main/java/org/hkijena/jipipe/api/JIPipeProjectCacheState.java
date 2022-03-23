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

import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.utils.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Encapsulates a state
 */
public class JIPipeProjectCacheState implements Comparable<JIPipeProjectCacheState> {
    private final UUID nodeUUID;
    private final LocalDateTime generationTime;
    private final String stateId;
    private final Set<JIPipeProjectCacheState> predecessorStates;

    /**
     * Creates a new instance
     *
     * @param node              the node
     * @param predecessorStates the states of predecessor nodes
     * @param generationTime    the generation time. It is ignored during comparison.
     */
    public JIPipeProjectCacheState(JIPipeGraphNode node, Set<JIPipeProjectCacheState> predecessorStates, LocalDateTime generationTime) {
        this.nodeUUID = node.getUUIDInParentGraph();
        this.predecessorStates = predecessorStates;
        this.generationTime = generationTime;
        if (node instanceof JIPipeAlgorithm) {
            this.stateId = ((JIPipeAlgorithm) node).getStateId();
        } else {
            this.stateId = "";
        }
    }

    public LocalDateTime getGenerationTime() {
        return generationTime;
    }

    public String getStateId() {
        return stateId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(stateId, predecessorStates);
    }

    @Override
    public int compareTo(JIPipeProjectCacheState o) {
        return generationTime.compareTo(o.generationTime);
    }

    @Override
    public String toString() {
        return stateId;
    }

    /**
     * Formats the generation time
     *
     * @return formatted string
     */
    public String renderGenerationTime() {
        return StringUtils.formatDateTime(getGenerationTime());
    }

    public UUID getNodeUUID() {
        return nodeUUID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JIPipeProjectCacheState state = (JIPipeProjectCacheState) o;

        // Our state maps are generally mutable, so we need to recreate hashsets
        if (nodeUUID.equals(state.nodeUUID) && stateId.equals(state.stateId)) {
            // Check predecessor step
            return new HashSet<>(predecessorStates).equals(new HashSet<>(state.predecessorStates));
        }
        return false;
    }

    public Set<JIPipeProjectCacheState> getPredecessorStates() {
        return predecessorStates;
    }
}
