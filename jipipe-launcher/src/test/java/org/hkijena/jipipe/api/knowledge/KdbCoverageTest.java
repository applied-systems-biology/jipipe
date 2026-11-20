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

package org.hkijena.jipipe.api.knowledge;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.FunctionRegistry;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * The corpus coverage report (design spec 5.8): for every taxonomy leaf
 * function of the live registry, whether any corpus entry references one of
 * the leaf's registered node ids. Per-leaf granularity keeps the report
 * usable; per-node would be noise. Coverage grows incrementally with the
 * corpus content, hence warn-not-fail: the report is printed to the build
 * output and this test only asserts that it is computable. An empty corpus
 * makes every leaf uncovered, which is the expected state until the corpus
 * batches land.
 */
class KdbCoverageTest {

    @BeforeAll
    static void init() {
        // The ImageJLegacyPatcherLauncherSessionListener registered in this
        // module preinits the ImageJ1 legacy patcher before any test class
        // loads, so ensureInstance() here is safe. Idempotent: reuses the
        // JIPipe singleton if another test class in the same JVM already
        // initialized it.
        JIPipe.ensureInstance();
    }

    /**
     * The packaged production corpus root, resolved from the classpath
     * (KdbProductionCorpus supports both the reactor classes directory and the
     * installed jipipe-core jar).
     */
    private static Path productionCorpusRoot() {
        return KdbProductionCorpus.root();
    }

    /**
     * The NODE_REF values of the whole corpus: every value of every slot
     * whose type is NODE_REF, across all loaded entries.
     */
    private static Set<String> referencedNodeIds(List<KdbEntry> entries) {
        Set<String> refs = new TreeSet<>();
        for (KdbEntry entry : entries) {
            for (KdbSlotDef slotDef : entry.getKind().getSlots()) {
                if (slotDef.type() != KdbValueType.NODE_REF) {
                    continue;
                }
                for (Object value : entry.getSlotValues().getOrDefault(slotDef.name(), List.of())) {
                    refs.add(String.valueOf(value));
                }
            }
        }
        return refs;
    }

    /**
     * Whether a registered node carries a taxonomy function. Functions are
     * value objects: match by stable id, never by object identity.
     */
    private static boolean carriesFunction(JIPipeNodeInfo info, String functionId) {
        JIPipeNodeTaxonomyFunction primary = info.getPrimaryFunction();
        if (primary != null && functionId.equals(primary.getId())) {
            return true;
        }
        for (JIPipeNodeTaxonomyFunction secondary : info.getSecondaryFunctions()) {
            if (functionId.equals(secondary.getId())) {
                return true;
            }
        }
        return false;
    }

    @Test
    void reportsUncoveredTaxonomyLeaves() {
        assertDoesNotThrow(() -> {
            KdbLoadResult corpus = new KdbEntryLoader().loadAll(productionCorpusRoot());
            Set<String> referencedNodeIds = referencedNodeIds(corpus.getEntries());

            FunctionRegistry functions = JIPipe.getNodes().getFunctions();
            Map<String, JIPipeNodeInfo> nodeInfos = JIPipe.getNodes().getRegisteredNodeInfos();

            // A leaf is a registered function with no registered children
            // (the function tree may nest deeper than two levels).
            List<JIPipeNodeTaxonomyFunction> leaves = new ArrayList<>();
            for (JIPipeNodeTaxonomyFunction function : functions.all()) {
                if (functions.children(function).isEmpty()) {
                    leaves.add(function);
                }
            }

            // A leaf is covered when at least one of its registered node ids
            // is referenced by a corpus NODE_REF. Presence, not counts:
            // plugins may add nodes and leaves.
            List<String> uncovered = new ArrayList<>();
            for (JIPipeNodeTaxonomyFunction leaf : leaves) {
                boolean covered = false;
                for (JIPipeNodeInfo info : nodeInfos.values()) {
                    if (referencedNodeIds.contains(info.getId()) && carriesFunction(info, leaf.getId())) {
                        covered = true;
                        break;
                    }
                }
                if (!covered) {
                    uncovered.add(leaf.getName() + " (" + leaf.getId() + ")");
                }
            }
            Collections.sort(uncovered);

            System.out.println("KdbCoverageTest: " + leaves.size() + " taxonomy leaves, "
                    + referencedNodeIds.size() + " distinct NODE_REF values in the corpus, "
                    + (leaves.size() - uncovered.size()) + " leaves covered, "
                    + uncovered.size() + " leaves uncovered (coverage is NOT a gate):");
            for (String leaf : uncovered) {
                System.out.println("  " + leaf);
            }
        });
    }
}
