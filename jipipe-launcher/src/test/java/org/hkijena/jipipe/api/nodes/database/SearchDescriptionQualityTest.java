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

package org.hkijena.jipipe.api.nodes.database;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The description quality gate of the search finetuning (#1404): every
 * in-scope registered node's description must contain its name's content
 * words, judged with the SAME tokenizer and containment rule the enhanced
 * search uses ({@link NodeSearchText#contentWords(String)} and
 * {@link NodeSearchText#coveredBy(String, Set)} over
 * {@code info.getDescription().toPlainText()}), so the gate and the search
 * agree by construction.
 *
 * <p>
 * Scope (user decision A, 2026-11-24): the auto-import families
 * {@code ij2:op:} (ImageJ2 ops; their third-party metadata carries no
 * authored description) and {@code clij:plugin:} (CLIJ kernels; their text
 * comes from CLIJ's own documentation) are OUT of the gate by structural
 * rule - their descriptions are not JIPipe-authored text. This is a scope
 * rule, not an exemption list; every other registered node is in scope and
 * there are no exemptions.
 *
 * <p>
 * With {@code -Djipipe.desc-sweep.dump=<path>} the test writes the failing
 * worklist as JSON (the input of the description sweep, see
 * {@code utils/search-usage/build-description-sweep-worklist.py}) before
 * asserting - the {@code NodeRegistryDumpToolTest} pattern.
 */
class SearchDescriptionQualityTest {

    /**
     * The node id prefixes whose descriptions are third-party metadata, not
     * JIPipe-authored text (user decision A, 2026-11-24; see the class
     * javadoc). Everything else is in scope.
     */
    private static final List<String> AUTO_IMPORT_ID_PREFIXES =
            List.of("ij2:op:", "clij:plugin:");

    private static final String DUMP_PROPERTY = "jipipe.desc-sweep.dump";

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
     * The gate's scope rule: false only for the auto-import families.
     */
    static boolean isInSweepScope(String nodeId) {
        if (nodeId == null) {
            return false;
        }
        for (String prefix : AUTO_IMPORT_ID_PREFIXES) {
            if (nodeId.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The missing name content words of one node (empty when the gate
     * passes for it).
     */
    static List<String> missingWords(JIPipeNodeInfo info) {
        HTMLText description = info.getDescription();
        String plain = description == null ? "" : description.toPlainText();
        Set<String> tokens = NodeSearchText.wordSet(plain);
        List<String> missing = new ArrayList<>();
        for (String word : NodeSearchText.contentWords(info.getName())) {
            if (!NodeSearchText.coveredBy(word, tokens)) {
                missing.add(word);
            }
        }
        return missing;
    }

    @Test
    void scopeFilterOnlySkipsTheAutoImportFamilies() {
        // The scope rule must be precise: JIPipe-authored ids that CONTAIN a
        // colon (jipipe:comment, ...) stay in scope.
        assertFalse(isInSweepScope("ij2:op:stats.sum"));
        assertFalse(isInSweepScope("clij:plugin:net.haesleinhuepf.clij2.plugins.GetMaximumOfAllPixels"));
        assertTrue(isInSweepScope("jipipe:comment"));
        assertTrue(isInSweepScope("jipipe:compartment-output"));
        assertTrue(isInSweepScope("ij1-threshold-auto2d"));
        assertTrue(isInSweepScope("annotation-table-from-file"));
    }

    @Test
    void everyInScopeNodeDescriptionContainsItsNameWords() {
        Map<String, JIPipeNodeInfo> nodeInfos = JIPipe.getNodes().getRegisteredNodeInfos();
        Map<String, List<String>> failures = new TreeMap<>();
        for (Map.Entry<String, JIPipeNodeInfo> entry : nodeInfos.entrySet()) {
            String nodeId = entry.getKey();
            if (!isInSweepScope(nodeId)) {
                continue;
            }
            JIPipeNodeInfo info = entry.getValue();
            List<String> missing = missingWords(info);
            if (!missing.isEmpty()) {
                failures.put(nodeId, missing);
            }
        }

        String dumpPath = System.getProperty(DUMP_PROPERTY);
        if (dumpPath != null && !dumpPath.isEmpty()) {
            writeDump(dumpPath, nodeInfos, failures);
            System.out.println("SearchDescriptionQualityTest: dumped " + failures.size()
                    + " failing descriptions to " + dumpPath);
        }

        if (!failures.isEmpty()) {
            StringBuilder report = new StringBuilder();
            report.append(failures.size()).append(" in-scope node descriptions do not contain their name words:\n");
            for (Map.Entry<String, List<String>> failure : failures.entrySet()) {
                report.append("  ").append(failure.getKey()).append(" (")
                        .append(nodeInfos.get(failure.getKey()).getName()).append("): missing ")
                        .append(failure.getValue()).append('\n');
            }
            report.append("Fix the descriptions (see the description sweep plan); do not weaken this gate.");
            assertTrue(failures.isEmpty(), report.toString());
        }
    }

    private static void writeDump(String dumpPath, Map<String, JIPipeNodeInfo> nodeInfos,
                                  Map<String, List<String>> failures) {
        ObjectNode document = JsonUtils.getObjectMapper().createObjectNode();
        document.put("generated", java.time.LocalDate.now().toString());
        ArrayNode rows = document.putArray("failing");
        for (Map.Entry<String, List<String>> failure : failures.entrySet()) {
            JIPipeNodeInfo info = nodeInfos.get(failure.getKey());
            HTMLText description = info.getDescription();
            ObjectNode row = rows.addObject();
            row.put("nodeId", failure.getKey());
            row.put("name", info.getName());
            ArrayNode missing = row.putArray("missing");
            for (String word : failure.getValue()) {
                missing.add(word);
            }
            row.put("description", description == null ? "" : description.toPlainText());
        }
        try {
            Path path = Paths.get(dumpPath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.write(path, Collections.singletonList(JsonUtils.toJsonString(document)),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write the description sweep dump to " + dumpPath, e);
        }
    }
}
