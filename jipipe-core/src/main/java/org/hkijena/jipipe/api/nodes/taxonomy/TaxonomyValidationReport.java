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

import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The result of a full taxonomy validation pass: the combined errors of the
 * three registries and the node membership, plus the node ids that are not
 * yet converted to the taxonomy (their primary function is
 * {@code null}).
 * <p>
 * The two kinds of findings have deliberately different consequences
 * (Review Focus #6, the "vanishes from browser" detector):
 * <ul>
 * <li>{@link #errors() errors} are hard problems — a cyclic or dangling
 * function tree, a broken framework set, a dangling family step, or a node
 * pointing at an unregistered function/framework. A broken-but-partly-
 * converted taxonomy must never ship a wrong browser, so these fail
 * initialization.</li>
 * <li>{@link #unassignedNodeIds() unassigned node ids} are the expected
 * state during phase 1a, when no node has been converted yet: they are
 * reported (and logged) but must NOT fail initialization, so that the
 * per-node conversion can proceed incrementally.</li>
 * </ul>
 * Hence {@link #ok()} is {@code true} as long as {@link #errors()} is
 * empty, regardless of {@link #unassignedNodeIds()}.
 * <p>
 * The report is immutable and constructed only through the static
 * {@link #run(FunctionRegistry, FrameworkRegistry, Map, Map)} method,
 * which is pure (no JIPipe service access) and therefore unit-testable
 * with synthetic registries. In production it is invoked exactly once,
 * by the node service component's final initialization pass.
 */
public final class TaxonomyValidationReport {

    /**
     * The immutable result: no errors and no unassigned nodes.
     */
    private static final TaxonomyValidationReport NO_FINDINGS =
        new TaxonomyValidationReport(List.of(), List.of());

    private final List<String> errors;
    private final List<String> unassignedNodeIds;

    private TaxonomyValidationReport(List<String> errors, List<String> unassignedNodeIds) {
        this.errors = errors;
        this.unassignedNodeIds = unassignedNodeIds;
    }

    /**
     * @return {@code true} if the taxonomy is valid, i.e.
     * {@link #errors()} is empty. Unassigned (not yet converted) nodes
     * do not affect this: they are the expected state during the
     * conversion, not a validation failure.
     */
    public boolean ok() {
        return errors.isEmpty();
    }

    /**
     * @return the validation errors (function tree, framework set,
     * family steps, and node→function/framework membership); empty if
     * the taxonomy is valid. The list is unmodifiable.
     */
    public List<String> errors() {
        return errors;
    }

    /**
     * @return the ids of nodes whose primary function is
     * {@code null}, i.e. nodes that are not yet converted to the
     * taxonomy and would vanish from the function-based browser. The
     * list is unmodifiable; empty if every node is converted.
     */
    public List<String> unassignedNodeIds() {
        return unassignedNodeIds;
    }

    /**
     * Runs a full taxonomy validation pass over the given registries and
     * node infos:
     * <ul>
     * <li>{@link FunctionRegistry#validate()} — the function tree,</li>
     * <li>{@link FrameworkRegistry#validate()} — the framework set,</li>
     * <li>membership of every node with a non-{@code null} primary
     * function: its primary function must be present in the function
     * registry, and its framework (unless it is the
     * {@link NoFramework} sentinel) must be present in the framework
     * registry;</li>
     * <li>every node with a {@code null} primary function is collected
     * in {@link #unassignedNodeIds()} instead.</li>
     * </ul>
     * <p>
     * This method is static and pure: it performs no JIPipe service
     * access, so it can be unit-tested with synthetic registries.
     *
     * @param functions     the function registry (the curated function
     *                      tree)
     * @param frameworks    the framework registry
     * @param families      the known families, id → family; reserved for
     *                      the family-step validation that the
     *                      {@code FamilyAssembler} performs against the
     *                      node set (the assembler owns its steps)
     * @param nodeInfos     nodeId → node info, the current node registry
     * @return the validation report
     */
    public static TaxonomyValidationReport run(FunctionRegistry functions,
                                               FrameworkRegistry frameworks,
                                               Map<String, JIPipeNodeTaxonomyFamily> families,
                                               Map<String, JIPipeNodeInfo> nodeInfos) {
        Set<String> errors = new LinkedHashSet<>();
        errors.addAll(functions.validate());
        errors.addAll(frameworks.validate());

        List<String> unassignedNodeIds = new ArrayList<>();
        for (Map.Entry<String, JIPipeNodeInfo> entry : nodeInfos.entrySet()) {
            JIPipeNodeInfo info = entry.getValue();
            JIPipeNodeTaxonomyFunction primary = info.getPrimaryFunction();
            if (primary == null) {
                // Unconverted node: expected during the conversion,
                // reported separately — not an error.
                unassignedNodeIds.add(info.getId());
                continue;
            }
            if (functions.byId(primary.getId()) == null) {
                errors.add("Node '" + info.getId() + "' points at taxonomy function '"
                    + primary.getId() + "' that is not registered in the function registry");
            }
            JIPipeNodeTaxonomyFramework framework = info.getFramework();
            if (framework != null && !(framework instanceof NoFramework)
                && frameworks.byId(framework.getId()) == null) {
                errors.add("Node '" + info.getId() + "' points at taxonomy framework '"
                    + framework.getId() + "' that is not registered in the framework registry");
            }
        }

        if (errors.isEmpty() && unassignedNodeIds.isEmpty()) {
            return NO_FINDINGS;
        }
        return new TaxonomyValidationReport(
            Collections.unmodifiableList(new ArrayList<>(errors)),
            Collections.unmodifiableList(unassignedNodeIds));
    }
}
