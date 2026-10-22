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
 * ({@link #label()}, {@link #purpose()}) and which node classes must run
 * before it ({@link #afterClasses()}, in annotation order).
 * <p>
 * A node's step id is {@code familyId + "#" + nodeId} (see
 * {@link #getStepId()}); the same node can be a member of several
 * families, each with its own step id.
 * <p>
 * The {@code after} entry holds node <em>classes</em>, not node ids: the
 * {@link FamilyAssembler} resolves them against its current class→node-id
 * mapping <em>lazily, at query time</em> ({@link #afterIds(String, FamilyAssembler)}
 * — see below). Registration order therefore does not matter: a step whose
 * predecessor registers (or maps) later still resolves correctly. A class
 * that maps to several node ids (e.g. a shared factory class behind many
 * programmatically generated node entries) contributes one predecessor per
 * node id.
 */
public final class JIPipeNodeTaxonomyFamilyStep {

    private final String familyId;
    private final String label;
    private final String nodeId;
    private final String purpose;
    private final List<Class<?>> afterClasses;

    /**
     * @param familyId     the id of the family this step belongs to
     * @param label        the display label of the step in the workflow
     * @param nodeId       the node id of the node this step refers to
     * @param purpose      a short description of why the step is part of the
     *                     workflow; may be empty
     * @param afterClasses the classes of the predecessor steps, in annotation
     *                     order; empty for a start step
     */
    public JIPipeNodeTaxonomyFamilyStep(String familyId, String label, String nodeId,
                                        String purpose, List<Class<?>> afterClasses) {
        this.familyId = familyId;
        this.label = label;
        this.nodeId = nodeId;
        this.purpose = purpose;
        this.afterClasses = List.copyOf(afterClasses);
    }

    /**
     * @return the classes of the predecessor steps, in annotation order;
     * empty for a start step
     */
    public List<Class<?>> afterClasses() {
        return afterClasses;
    }

    /**
     * The step ids of this step's predecessors, resolved through the given
     * assembler's <em>current</em> class→node-id mapping (deferred
     * resolution — see the class Javadoc). A class the mapping does not
     * know falls back to its simple name; a class mapped to several node
     * ids yields one step id per node id, in mapping order. Duplicates are
     * removed, first occurrence wins.
     *
     * @param assembler the assembler whose mapping resolves the classes
     * @return the predecessor step ids, in annotation order
     */
    public List<String> afterIds(FamilyAssembler assembler) {
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        for (Class<?> afterClass : afterClasses) {
            for (String predecessorNodeId : assembler.resolveNodeIds(afterClass)) {
                seen.add(FamilyAssembler.stepId(familyId, predecessorNodeId));
            }
        }
        return List.copyOf(seen);
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
     * The unique id of this step within its family.
     *
     * @return {@code familyId + "#" + nodeId}
     */
    public String getStepId() {
        return familyId + "#" + nodeId;
    }
}
