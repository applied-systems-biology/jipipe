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

package org.hkijena.jipipe.api.nodes.taxonomy;

import java.util.List;

/**
 * An immutable description of one node's role inside a workflow family:
 * which node it is ({@link #nodeId()}), what it does in the workflow
 * ({@link #label()}, {@link #purpose()}) and which steps must run before
 * it ({@link #afterIds()}, in annotation order).
 * <p>
 * A node's step id is {@code familyId + "#" + nodeId} (see
 * {@link #getStepId()}); the same node can be a member of several
 * families, each with its own step id.
 */
public final class JIPipeNodeTaxonomyFamilyStep {

    private final String familyId;
    private final String label;
    private final String nodeId;
    private final String purpose;
    private final List<String> afterIds;

    /**
     * @param familyId the id of the family this step belongs to
     * @param label    the display label of the step in the workflow
     * @param nodeId   the node id of the node this step refers to
     * @param purpose  a short description of why the step is part of the workflow;
     *                 may be empty
     * @param afterIds the step ids of the predecessor steps, in annotation
     *                 order; empty for a start step
     */
    public JIPipeNodeTaxonomyFamilyStep(String familyId, String label, String nodeId,
                                        String purpose, List<String> afterIds) {
        this.familyId = familyId;
        this.label = label;
        this.nodeId = nodeId;
        this.purpose = purpose;
        this.afterIds = List.copyOf(afterIds);
    }

    /**
     * @return the id of the family this step belongs to
     */
    public String familyId() {
        return familyId;
    }

    /**
     * @return the display label of the step in the workflow
     */
    public String label() {
        return label;
    }

    /**
     * @return the node id of the node this step refers to
     */
    public String nodeId() {
        return nodeId;
    }

    /**
     * @return a short description of why the step is part of the workflow;
     * may be empty
     */
    public String purpose() {
        return purpose;
    }

    /**
     * @return the step ids of the predecessor steps, in annotation order;
     * empty for a start step
     */
    public List<String> afterIds() {
        return afterIds;
    }

    /**
     * The unique id of this step within its family.
     *
     * @return {@code familyId + "#" + nodeId}
     */
    public String getStepId() {
        return familyId + "#" + nodeId;
    }
}
