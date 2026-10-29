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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.service.components.JIPipeNodesServiceComponent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Full-registry taxonomy validation gate - the branch gate of the node
 * taxonomy redesign (phase 1b, task 22).
 * <p>
 * <b>Scope: the entire node registry.</b> This test lives in
 * <b>jipipe-launcher</b>, the only module that aggregates all
 * {@code jipipe-plugin-*} modules plus {@code jipipe-core} on its classpath,
 * so after {@code JIPipe.ensureInstance()} every node of every plugin is
 * registered. jipipe-core cannot host this gate: plugins depend on core, not
 * vice versa, so core's own classpath cannot see the plugin nodes.
 * <p>
 * <b>Production parity.</b> The test runs the 5-argument
 * {@link TaxonomyValidationReport#run} overload - the exact entry point
 * that {@link JIPipeNodesServiceComponent#postprocess} invokes as the final
 * pass of every JIPipe initialization - over the live registries: the
 * curated function tree, the framework set, the family steps (via the
 * {@link FamilyAssembler}), and the node → function/framework membership of
 * the whole registry.
 * <p>
 * <b>Assertions.</b>
 * <ol>
 * <li>{@code report.ok()} - no validation errors (broken function tree,
 * dangling framework/function references, family steps referencing node ids
 * that no longer resolve). NOTE: {@code ok()} ignores unassigned nodes by
 * design (they are the expected state during conversion), so it is not the
 * conversion-complete check itself.</li>
 * <li><b>The branch gate:</b> {@code report.unassignedNodeIds()} is empty -
 * every registered node has a non-{@code null} primary taxonomy function.
 * This is the proof that the ~945-annotated + dynamic-node conversion
 * (tasks 19.1–20) is complete across the entire product.</li>
 * </ol>
 * <p>
 * This gate is intentionally ungated (no system-property switch): once the
 * conversion is complete it must stay green for every change to the node
 * registry - a node registered without a taxonomy function vanishes from
 * the function-based browser and fails this test.
 */
class TaxonomyFullValidationTest {

    @BeforeAll
    static void init() {
        // The ImageJLegacyPatcherLauncherSessionListener registered in this
        // module preinits the ImageJ1 legacy patcher before any test class
        // loads, so ensureInstance() here is safe. Idempotent: reuses the
        // JIPipe singleton if another test class in the same JVM already
        // initialized it.
        JIPipe.ensureInstance();
    }

    @Test
    void everyNodeInTheRegistryHasATaxonomyFunction() {
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        Map<String, JIPipeNodeInfo> nodeInfos = nodes.getRegisteredNodeInfos();
        System.out.println("TaxonomyFullValidationTest: " + nodeInfos.size()
                + " registered nodes in the registry (full gate)");

        TaxonomyValidationReport report = TaxonomyValidationReport.run(
                nodes.getFunctions(),
                nodes.getFrameworks(),
                nodeInfos,
                nodes.getFamilyAssembler(),
                nodeInfos.keySet());

        // (1) Production-parity validity: function tree + framework set +
        // family steps + node -> function/framework membership. NOTE: ok()
        // ignores unassigned nodes by design, so (2) is the
        // conversion-complete check.
        assertTrue(report.ok(), "Taxonomy validation errors:\n" + String.join("\n", report.errors()));

        // (2) THE BRANCH GATE: every registered node has a non-null primary
        // taxonomy function - the 19.1-20 conversion is complete.
        List<String> unassigned = new ArrayList<>(report.unassignedNodeIds());
        Collections.sort(unassigned);
        assertTrue(unassigned.isEmpty(),
                "Taxonomy conversion is INCOMPLETE: " + unassigned.size()
                        + " registered node(s) have no primary taxonomy function:\n  "
                        + String.join("\n  ", unassigned));
    }
}
