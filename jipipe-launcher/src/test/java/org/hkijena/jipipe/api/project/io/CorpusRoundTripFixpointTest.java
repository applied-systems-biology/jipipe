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

package org.hkijena.jipipe.api.project.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Corpus round-trip fixpoint harness for the JIPX project-format migration
 * (phase 0, #753).
 * <p>
 * This test is the <b>verification instrument</b> for the .jip IO refactor: it
 * round-trips a corpus of real, user-produced <code>.jip</code> project files
 * through the <i>current, unrefactored</i>
 * {@link JIPipeProject#loadProject(Path, org.hkijena.jipipe.api.validation.JIPipeValidationReportContext, JIPipeValidationReport, JIPipeNotificationInbox, JIPipeProgressInfo)}
 * and {@link JIPipeProject#saveProject(Path, boolean)} and captures a
 * baseline behavior report. In phase 0 task 6 it is re-run, unchanged, after
 * the refactor, and the two reports must be <b>exactly equal</b> - the report
 * is therefore deliberately free of timestamps, timings, and absolute
 * temporary paths (the "corpus" field echoes the passed property value).
 * <p>
 * <b>Scope: jipipe-launcher.</b> The corpus references node ids from every
 * plugin (ij-algorithms, trackmate, tables, ...). Like
 * {@code TaxonomyFullValidationTest}, this test must live in the launcher -
 * the only module with all plugin modules on its classpath. On jipipe-core's
 * classpath {@code JIPipeGraph.nodesFromJson} would silently skip every node
 * of an unknown plugin and the baseline would be vacuous.
 * <p>
 * <b>Gated:</b> the test is skipped unless the system property
 * <code>jipipe.jip.roundtrip.corpus</code> points at an existing directory, so
 * CI stays green. Optional properties:
 * <ul>
 * <li><code>jipipe.jip.roundtrip.report</code> - report output path
 * (default: <code>&lt;corpus&gt;/.roundtrip/report.json</code>)</li>
 * <li><code>jipipe.jip.roundtrip.limit</code> - process only the first N
 * files (smoke runs)</li>
 * </ul>
 * <p>
 * <b>Never writes into the corpus</b> except the sanctioned report location.
 * Each project is copied into a fresh working directory and all loading and
 * saving happens on the copies. The test itself only <i>fails</i> on corpus
 * pollution (a change of the top-level <code>*.jip</code> count of the corpus
 * root) or a harness bug - per-project parse/load/save failures are expected
 * (the corpus contains historical files) and are captured as outcomes.
 * <p>
 * <b>Report schema</b> (a single JSON document):
 * <pre>
 * {
 *   "corpus": "&lt;corpus dir&gt;",
 *   "totalFiles": 367,
 *   "outcomes": {"OK": 0, "PARSE_FAILED": 0, "LOAD_FAILED": 0, "SAVE_FAILED": 0},
 *   "entries": [
 *     {"file": "&lt;absolute corpus path&gt;", "outcome": "OK",
 *      "s1vs2SemanticallyEqual": true,
 *      "pvs1TopLevelChangedKeys": ["jipipe:project-jipipe-version"],
 *      "loadedNodesS1": 0, "loadedNodesS2": 0,
 *      "error": null}
 *   ]
 * }
 * </pre>
 * Per project: the file is loaded, saved to S1, reloaded, and saved to S2.
 * <code>s1vs2SemanticallyEqual</code> is the <b>canonical</b> JSON equality
 * of S1 and S2 - the fixpoint property: saving a freshly loaded project must
 * be idempotent. <code>pvs1TopLevelChangedKeys</code> is the sorted list of
 * top-level keys that were added, removed, or changed between the original
 * file P and S1 (diagnostic only - expected for version upgrades and
 * regenerated caches, and never a failure).
 * <p>
 * <b>Canonical comparison (design decision, fix round 1).</b> Both the
 * fixpoint check and the P→S1 key diff compare a <i>canonical form</i> of the
 * JSON, not the raw trees: objects are re-emitted with fields sorted by key,
 * arrays are canonicalized element-wise and then sorted by their canonical
 * serialized string (i.e. array equality is multiset equality, not
 * sequence equality). This is required because the current, unrefactored
 * save path writes the {@code dependencies} and {@code compartments} arrays in
 * <b>non-deterministic order</b>: {@code JIPipeProject.getSimplifiedMinimalDependencies()}
 * calls {@code JIPipeDependency.simplifyAndMinimize(...)}, which builds a
 * fresh {@code HashSet} of freshly copied {@code JIPipeMutableDependency}
 * instances on every save, and that type has no value-based
 * {@code equals}/{@code hashCode} - so iteration order depends on
 * {@code identityHashCode} and differs between saves and between JVMs. Raw
 * Jackson equality (key-order-insensitive for objects, but array-order
 * sensitive) therefore flips on an ordering coin flip and the report would
 * not be reproducible; array ordering is provably non-semantic in the
 * current code, so erasing it does not hide any real value change (the
 * deterministic content churn - e.g. {@code null} vs {@code "null"} - is
 * still detected, identically, in both runs).
 */
class CorpusRoundTripFixpointTest {

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
     * Loads a project from the given path with an empty, throwaway validation
     * report and notification inbox.
     */
    private static JIPipeProject load(Path path, UnspecifiedValidationReportContext context) throws IOException {
        return JIPipeProject.loadProject(path, context,
                new JIPipeValidationReport(), new JIPipeNotificationInbox(), JIPipeProgressInfo.STDOUT);
    }

    /**
     * Collects the top-level keys that are added, removed, or changed between
     * the two trees (diagnostic only, never fails the run). Values are
     * compared in their canonical form, so array-order-only differences do
     * not count as changes.
     */
    private static List<String> topLevelChangedKeys(JsonNode a, JsonNode b) {
        TreeSet<String> changed = new TreeSet<>();
        if (a == null || b == null || !a.isObject() || !b.isObject()) {
            return new ArrayList<>(changed);
        }
        Map<String, JsonNode> fieldsA = fields(a);
        Map<String, JsonNode> fieldsB = fields(b);
        TreeSet<String> unionKeys = new TreeSet<>(fieldsA.keySet());
        unionKeys.addAll(fieldsB.keySet());
        for (String key : unionKeys) {
            JsonNode va = fieldsA.get(key);
            JsonNode vb = fieldsB.get(key);
            if (va == null || vb == null || !canonical(va).equals(canonical(vb))) {
                changed.add(key);
            }
        }
        return new ArrayList<>(changed);
    }

    /**
     * Canonical form of a JSON tree for order-insensitive comparison:
     * objects are re-emitted with fields sorted by key; arrays have each
     * element canonicalized and are then sorted by the element's canonical
     * string form (multiset equality, order erased); scalars are unchanged.
     */
    private static JsonNode canonical(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            ObjectNode out = JsonUtils.getObjectMapper().createObjectNode();
            for (Map.Entry<String, JsonNode> field : new TreeMap<>(fields(node)).entrySet()) {
                out.set(field.getKey(), canonical(field.getValue()));
            }
            return out;
        }
        if (node.isArray()) {
            ArrayNode out = JsonUtils.getObjectMapper().createArrayNode();
            List<JsonNode> items = new ArrayList<>();
            for (JsonNode element : node) {
                items.add(canonical(element));
            }
            items.sort(Comparator.comparing(JsonNode::toString));
            for (JsonNode element : items) {
                out.add(element);
            }
            return out;
        }
        return node;
    }

    private static Map<String, JsonNode> fields(JsonNode object) {
        Map<String, JsonNode> fields = new LinkedHashMap<>();
        object.fields().forEachRemaining(e -> fields.put(e.getKey(), e.getValue()));
        return fields;
    }

    private static long countTopLevelJipFiles(Path corpus) throws IOException {
        try (Stream<Path> stream = Files.list(corpus)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> endsWithJip(p.getFileName().toString()))
                    .count();
        }
    }

    private static boolean endsWithJip(String fileName) {
        return fileName.length() >= 4 && fileName.regionMatches(true, fileName.length() - 4, ".jip", 0, 4);
    }

    private static String errorLine(Throwable e) {
        String message = e.getMessage() == null ? e.getClass().getName() : e.getMessage();
        String oneLine = e.getClass().getName() + ": " + message.replaceAll("\\s+", " ").trim();
        return oneLine.length() > 500 ? oneLine.substring(0, 500) : oneLine;
    }

    @Test
    void corpusRoundTripFixpoint(@TempDir Path tmp) throws IOException {
        String corpusProp = System.getProperty("jipipe.jip.roundtrip.corpus");
        assumeTrue(corpusProp != null && !corpusProp.isBlank(),
                "No -Djipipe.jip.roundtrip.corpus=<dir> given; skipping corpus round-trip fixpoint harness");
        Path corpus = Path.of(corpusProp).toAbsolutePath().normalize();
        assertTrue(Files.isDirectory(corpus), "Corpus directory does not exist: " + corpus);

        Path reportPath = Path.of(System.getProperty("jipipe.jip.roundtrip.report",
                corpus.resolve(".roundtrip").resolve("report.json").toString())).toAbsolutePath().normalize();
        // -1 (unbounded) only when the property is ABSENT; a present-but-malformed
        // value must fail loudly rather than silently fall back.
        int limit = -1;
        String limitProp = System.getProperty("jipipe.jip.roundtrip.limit");
        if (limitProp != null && !limitProp.isBlank()) {
            limit = Integer.parseInt(limitProp.trim());
        }

        // Discover inputs: all *.jip files (case-insensitive) below the
        // corpus root, sorted for deterministic ordering.
        List<Path> inputs;
        try (Stream<Path> stream = Files.walk(corpus)) {
            inputs = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> endsWithJip(p.getFileName().toString()))
                    .sorted(Comparator.comparing(Path::toString))
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        if (limit > 0) {
            inputs = new ArrayList<>(inputs.subList(0, Math.min(limit, inputs.size())));
        }

        // Snapshot the top-level *.jip count of the corpus root: the harness
        // must never create or delete corpus files (the only sanctioned write
        // target is the report, which lives in a .roundtrip/ subdirectory and
        // is not a top-level *.jip).
        long topLevelCountBefore = countTopLevelJipFiles(corpus);

        ObjectNode report = JsonUtils.getObjectMapper().createObjectNode();
        report.put("corpus", corpusProp);
        ObjectNode outcomes = report.putObject("outcomes");
        outcomes.put("OK", 0);
        outcomes.put("PARSE_FAILED", 0);
        outcomes.put("LOAD_FAILED", 0);
        outcomes.put("SAVE_FAILED", 0);
        List<ObjectNode> entries = new ArrayList<>();

        UnspecifiedValidationReportContext context = new UnspecifiedValidationReportContext();

        for (int i = 0; i < inputs.size(); i++) {
            Path input = inputs.get(i);
            ObjectNode entry = JsonUtils.getObjectMapper().createObjectNode();
            entry.put("file", input.toString());
            String outcome;
            String error = null;
            JsonNode pTree = null;
            JsonNode s1Tree = null;
            int loadedNodesS1 = 0;
            int loadedNodesS2 = 0;
            // The stage about to be executed, updated immediately before each
            // step so the catch block can map a failure to the correct
            // outcome without relying on which locals happened to be assigned.
            String stage = "COPY";

            try {
                // Never touch the corpus file: all work happens on a copy in a
                // fresh, index-named per-project working directory (the index
                // is unique within the run, so no two projects can collide).
                Path workDir = tmp.resolve("work-" + i);
                Files.createDirectories(workDir);
                Path copy = workDir.resolve("project.jip");
                Files.copy(input, copy);

                // P → parse the original (for the diagnostic P→S1 diff) and
                // load it (cycle 1).
                stage = "LOAD1";
                pTree = JsonUtils.getObjectMapper().readTree(input.toFile());
                JIPipeProject p1 = load(copy, context);
                loadedNodesS1 = p1.getGraph().traverse().size();

                stage = "SAVE1";
                Path s1Path = workDir.resolve("s1.jip");
                p1.saveProject(s1Path, false);
                s1Tree = JsonUtils.getObjectMapper().readTree(s1Path.toFile());

                // Cycle 2: reload S1, save S2 (fixpoint check S1 vs. S2).
                stage = "LOAD2";
                JIPipeProject p2 = load(s1Path, context);
                loadedNodesS2 = p2.getGraph().traverse().size();

                stage = "SAVE2";
                Path s2Path = workDir.resolve("s2.jip");
                p2.saveProject(s2Path, false);
                JsonNode s2Tree = JsonUtils.getObjectMapper().readTree(s2Path.toFile());

                // Canonical comparison (see class javadoc): array ordering is
                // non-semantic in the current save path, so order is erased.
                entry.put("s1vs2SemanticallyEqual", canonical(s1Tree).equals(canonical(s2Tree)));
                ArrayNode changedKeys = entry.putArray("pvs1TopLevelChangedKeys");
                for (String key : topLevelChangedKeys(pTree, s1Tree)) {
                    changedKeys.add(key);
                }
                entry.put("loadedNodesS1", loadedNodesS1);
                entry.put("loadedNodesS2", loadedNodesS2);
                entry.putNull("error");
                outcome = "OK";
            } catch (Exception e) {
                error = errorLine(e);
                // Map the failing stage to an outcome:
                //   PARSE_FAILED - the corpus file could not be read/copied
                //                  (COPY) or parsed as JSON (start of LOAD1,
                //                  before the project is loaded);
                //   LOAD_FAILED  - loadProject/fromJson threw (LOAD1 or LOAD2);
                //   SAVE_FAILED  - saveProject threw (SAVE1 or SAVE2).
                if (stage.equals("COPY") || (stage.equals("LOAD1") && pTree == null)) {
                    outcome = "PARSE_FAILED";
                } else if (stage.equals("LOAD1") || stage.equals("LOAD2")) {
                    outcome = "LOAD_FAILED";
                } else {
                    outcome = "SAVE_FAILED";
                }
                entry.put("s1vs2SemanticallyEqual", false);
                entry.putArray("pvs1TopLevelChangedKeys");
                entry.put("loadedNodesS1", loadedNodesS1);
                entry.put("loadedNodesS2", loadedNodesS2);
                entry.put("error", error);
            }

            entry.put("outcome", outcome);
            outcomes.put(outcome, outcomes.get(outcome).asInt() + 1);
            entries.add(entry);
            System.out.println("[jipx-roundtrip] " + outcome + " " + input + (error == null ? "" : " - " + error));
        }

        // The harness must not have polluted the corpus.
        long topLevelCountAfter = countTopLevelJipFiles(corpus);
        assertEquals(topLevelCountBefore, topLevelCountAfter,
                "Corpus pollution: top-level *.jip count of " + corpus + " changed from "
                        + topLevelCountBefore + " to " + topLevelCountAfter);

        report.put("totalFiles", entries.size());
        ArrayNode entriesNode = report.putArray("entries");
        for (ObjectNode entry : entries) {
            entriesNode.add(entry);
        }

        // Write the report (the only write into the corpus area).
        Files.createDirectories(reportPath.getParent());
        JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(reportPath.toFile(), report);
        System.out.println("[jipx-roundtrip] Report written to " + reportPath
                + " (" + entries.size() + " files, outcomes: " + outcomes + ")");
    }
}
