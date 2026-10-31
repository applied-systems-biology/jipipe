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

package org.hkijena.jipipe.api.project.jipx;

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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * JIPX round-trip fixpoint harness (phase 1 done-gate, #753).
 * <p>
 * This test is the <b>verification instrument</b> for the JIPX container
 * format: it round-trips a corpus of real, user-produced <code>.jip</code>
 * project files through BOTH the legacy cycle and the JIPX cycle and captures
 * a deterministic verification report. The legacy cycle is the inheritance of
 * the phase 0 gate ({@code CorpusRoundTripFixpointTest} - the verified
 * baseline, kept byte-identical); the JIPX cycle is the subject under test:
 * <ol>
 * <li>Copy the corpus file into a fresh, index-named per-project working
 * directory as <code>project.jip</code> (the corpus itself is never touched).</li>
 * <li><b>Legacy cycle</b>: <code>M0 = loadProject(copy)</code> → save
 * <code>S1 = workdir/s1.jip</code> (dispatches to the legacy writer) →
 * <code>M1 = loadProject(S1)</code> → save <code>S2</code> from M1 →
 * <code>legacyFixpoint = canonical(S1 tree) == canonical(S2 tree)</code>
 * (the phase 0 fixpoint property).</li>
 * <li><b>JIPX cycle</b> (from M1 - the legacy-fixpoint model): write
 * <code>workdir/project.jipx</code> via
 * {@link JIPipeXProjectWriter#writeWithViolations(JIPipeProject, Path, JIPipeProgressInfo, List)}
 * (collecting the compliance violations) → <code>M2 = loadProject(project.jipx)</code>
 * (the read dispatch routes ZIP → JIPX reader - the dispatch seam itself is
 * proven here; the reader is never instantiated directly) →
 * <code>jipxModelEquivalent = canonical(monolithTree(M1)) ==
 * canonical(monolithTree(M2))</code>, where {@code monolithTree(X)} is the
 * full monolith serialization of the model:
 * <code>readValue(writeValueAsString(X))</code>. The full monolith
 * serialization of both models is compared - the JIPX load re-runs the same
 * regeneration machinery (e.g. external environments), and the canonical
 * comparison erases the known array-order nondeterminism of the current
 * save path (the phase 0 ruling, duplicated here by design).</li>
 * <li><b>Crate check</b>: open the <code>.jipx</code> with
 * {@link java.util.zip.ZipFile}; the <code>ro-crate-metadata.json</code>
 * entry exists, parses as JSON, and its root <code>./</code> entity carries
 * a <code>mainEntity</code> (an RO-Crate id reference -
 * <code>{"@id": "project/graph.json"}</code>) and a <code>license</code>
 * reference; the <code>jipx.json</code> entry exists and parses.
 * Boolean only - the crate content is deliberately NOT compared byte-wise:
 * <code>datePublished</code> is non-deterministic by design.</li>
 * </ol>
 * <p>
 * <b>Scope: jipipe-launcher.</b> The corpus references node ids from every
 * plugin - like the phase 0 gate, this harness must live in the launcher, the
 * only module with all plugin modules on its classpath.
 * <p>
 * <b>Gated:</b> the test is skipped unless the system property
 * <code>jipipe.jipx.roundtrip.corpus</code> points at an existing directory,
 * so CI stays green (on this machine: <code>/data/JIPipe</code>, 367
 * <code>.jip</code> files recursive). Optional properties:
 * <ul>
 * <li><code>jipipe.jipx.roundtrip.report</code> - report output path
 * (default: <code>&lt;corpus&gt;/.roundtrip/jipx.json</code>)</li>
 * <li><code>jipipe.jipx.roundtrip.limit</code> - process only the first N
 * files (smoke runs); a present-but-malformed value fails loudly rather
 * than silently falling back (the phase 0 pattern)</li>
 * </ul>
 * <p>
 * <b>Per-project failure policy:</b> every project runs in its own
 * try/catch and a failure CONTINUEs the run. The outcome is mapped from an
 * explicit stage variable updated before each step (the phase 0 pattern) -
 * no single file may abort the corpus run:
 * <code>OK</code> (legacy fixpoint + JIPX model equivalence + crate checks
 * all recorded) | <code>PARSE_FAILED</code> (copy or JSON parse of the
 * corpus file) | <code>LOAD_FAILED</code> (legacy cycle load) |
 * <code>SAVE_FAILED</code> (legacy cycle save) | <code>JIPX_SAVE_FAILED</code> |
 * <code>JIPX_LOAD_FAILED</code>. For a legacy-cycle failure the JIPX steps are
 * skipped and the JIPX fields are recorded as false/0.
 * <p>
 * <b>Never writes into the corpus</b> except the sanctioned report location:
 * a top-level <code>*.jip</code> count snapshot before/after the run is
 * asserted unchanged (the phase 0 pollution guard).
 * <p>
 * <b>Report schema</b> (deterministic - no timestamps, timings, or absolute
 * temporary paths):
 * <pre>
 * {
 *   "corpus": "&lt;dir&gt;",
 *   "totalFiles": 367,
 *   "outcomes": {"OK":0,"PARSE_FAILED":0,"LOAD_FAILED":0,"SAVE_FAILED":0,"JIPX_SAVE_FAILED":0,"JIPX_LOAD_FAILED":0},
 *   "entries": [
 *     {"file":"&lt;abs path&gt;","outcome":"OK",
 *      "legacyFixpoint":true,"jipxModelEquivalent":true,"jipxCrateValid":true,
 *      "loadedNodesM1":0,"jipxViolations":0,"error":null}
 *   ]
 * }
 * </pre>
 * <code>loadedNodesM1</code> (the M1 graph traversal size) is the
 * non-vacuity guard of the JIPX comparison; <code>jipxViolations</code> is
 * the compliance-violation count of the JIPX save (expected 0 for
 * well-formed saves); <code>error</code> is a one-line exception summary
 * (diagnostic only - known to flap for JDK NPE helpful-message forms; never
 * part of any comparison).
 */
class JipxRoundTripFixpointTest {

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
     * report and notification inbox, through the format dispatcher - the
     * same entry point every load site in the code base uses (legacy JSON
     * reader for plain files, JIPX reader for ZIP content).
     */
    private static JIPipeProject load(Path path, UnspecifiedValidationReportContext context) throws IOException {
        return JIPipeProject.loadProject(path, context,
                new JIPipeValidationReport(), new JIPipeNotificationInbox(), JIPipeProgressInfo.SILENT);
    }

    /**
     * The full monolith serialization of a project as a JSON tree -
     * {@code readValue(writeValueAsString(project))}. Used to compare the
     * legacy-fixpoint model (M1) with the model loaded back from the JIPX
     * container (M2): both models are serialized the same way, so the
     * comparison isolates the JIPX write→read round trip itself.
     */
    private static JsonNode monolithTree(JIPipeProject project) throws IOException {
        String json = JsonUtils.getObjectMapper().writeValueAsString(project);
        return JsonUtils.getObjectMapper().readValue(json, JsonNode.class);
    }

    /**
     * The JIPX crate validity check (boolean only - see the class javadoc:
     * the crate content is deliberately NOT compared byte-wise because
     * {@code datePublished} is non-deterministic by design):
     * <ul>
     * <li><code>ro-crate-metadata.json</code> exists in the container and
     * parses as JSON;</li>
     * <li>its root <code>./</code> entity has a <code>mainEntity</code>
     * property (== <code>project/graph.json</code>, the manifest's main
     * graph part) and a <code>license</code> property;</li>
     * <li><code>jipx.json</code> exists and parses as JSON.</li>
     * </ul>
     */
    private static boolean crateValid(Path jipxPath) throws IOException {
        try (ZipFile zip = new ZipFile(jipxPath.toFile())) {
            // The manifest.
            ZipEntry manifestEntry = zip.getEntry(JIPipeXManifest.MANIFEST_FILE_NAME);
            if (manifestEntry == null) {
                return false;
            }
            try (InputStream is = zip.getInputStream(manifestEntry)) {
                JsonUtils.getObjectMapper().readTree(is);
            } catch (Exception e) {
                return false;
            }
            // The crate metadata.
            ZipEntry crateEntry = zip.getEntry(JIPipeXProjectWriter.CRATE_METADATA_FILE_NAME);
            if (crateEntry == null) {
                return false;
            }
            JsonNode crate;
            try (InputStream is = zip.getInputStream(crateEntry)) {
                crate = JsonUtils.getObjectMapper().readTree(is);
            } catch (Exception e) {
                return false;
            }
            JsonNode rootEntity = null;
            JsonNode graph = crate.path("@graph");
            if (graph.isArray()) {
                for (JsonNode entity : graph) {
                    if (entity.isObject() && "./".equals(entity.path("@id").asText())) {
                        rootEntity = entity;
                        break;
                    }
                }
            }
            // mainEntity / license are written by the crate as RO-Crate id
            // references ({ "@id": "..." }), so resolve the @id sub-field.
            if (rootEntity == null
                    || !JIPipeXManifest.MAIN_GRAPH.equals(resolveId(rootEntity.path("mainEntity")))
                    || resolveId(rootEntity.path("license")) == null) {
                return false;
            }
            return true;
        }
    }

    /**
     * Resolves an RO-Crate id reference to its id string: a plain string is
     * returned as-is; an object with an <code>@id</code> field returns that
     * field; anything else (missing, other types) returns null.
     */
    private static String resolveId(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isObject()) {
            JsonNode id = node.get("@id");
            return id != null && id.isTextual() ? id.asText() : null;
        }
        return null;
    }

    /**
     * Canonical form of a JSON tree for order-insensitive comparison
     * (DUPLICATED from the phase 0 gate {@code CorpusRoundTripFixpointTest}
     * by design - that file is the verified baseline and keeps zero diff;
     * two test files sharing the canonical helper is acceptable for tests):
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

    /**
     * One-line exception summary (the phase 0 pattern):
     * <code>Class: message</code>, whitespace-collapsed, 500-char cap.
     * Diagnostic only - known to flap for JDK NPE helpful-message forms;
     * never part of any comparison.
     */
    private static String errorLine(Throwable e) {
        String message = e.getMessage() == null ? e.getClass().getName() : e.getMessage();
        String oneLine = e.getClass().getName() + ": " + message.replaceAll("\\s+", " ").trim();
        return oneLine.length() > 500 ? oneLine.substring(0, 500) : oneLine;
    }

    @Test
    void jipxRoundTripFixpoint(@TempDir Path tmp) throws IOException {
        String corpusProp = System.getProperty("jipipe.jipx.roundtrip.corpus");
        assumeTrue(corpusProp != null && !corpusProp.isBlank(),
                "No -Djipipe.jipx.roundtrip.corpus=<dir> given; skipping JIPX corpus round-trip harness");
        Path corpus = Path.of(corpusProp).toAbsolutePath().normalize();
        assertTrue(Files.isDirectory(corpus), "Corpus directory does not exist: " + corpus);

        Path reportPath = Path.of(System.getProperty("jipipe.jipx.roundtrip.report",
                corpus.resolve(".roundtrip").resolve("jipx.json").toString())).toAbsolutePath().normalize();
        // -1 (unbounded) only when the property is ABSENT; a present-but-malformed
        // value must fail loudly rather than silently fall back.
        int limit = -1;
        String limitProp = System.getProperty("jipipe.jipx.roundtrip.limit");
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
        outcomes.put("JIPX_SAVE_FAILED", 0);
        outcomes.put("JIPX_LOAD_FAILED", 0);
        List<ObjectNode> entries = new ArrayList<>();

        UnspecifiedValidationReportContext context = new UnspecifiedValidationReportContext();

        for (int i = 0; i < inputs.size(); i++) {
            Path input = inputs.get(i);
            ObjectNode entry = JsonUtils.getObjectMapper().createObjectNode();
            entry.put("file", input.toString());
            String outcome;
            String error = null;
            boolean legacyFixpoint = false;
            boolean jipxModelEquivalent = false;
            boolean jipxCrateValid = false;
            int loadedNodesM1 = 0;
            int jipxViolations = 0;
            JsonNode s1Tree = null;
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

                // ---- Legacy cycle (the phase 0 inheritance) ----
                stage = "LOAD1";
                JIPipeProject m0 = load(copy, context);

                stage = "SAVE1";
                Path s1Path = workDir.resolve("s1.jip");
                m0.saveProject(s1Path, false);
                s1Tree = JsonUtils.getObjectMapper().readTree(s1Path.toFile());

                // M1 - the legacy-fixpoint model; also the JIPX cycle's source.
                stage = "LOAD2";
                JIPipeProject m1 = load(s1Path, context);
                loadedNodesM1 = m1.getGraph().traverse().size();

                stage = "SAVE2";
                Path s2Path = workDir.resolve("s2.jip");
                m1.saveProject(s2Path, false);
                JsonNode s2Tree = JsonUtils.getObjectMapper().readTree(s2Path.toFile());

                // Canonical comparison (see class javadoc / phase 0 ruling):
                // array ordering is non-semantic in the current save path,
                // so order is erased.
                legacyFixpoint = canonical(s1Tree).equals(canonical(s2Tree));

                // ---- JIPX cycle (from M1) ----
                stage = "JIPX_SAVE";
                Path jipxPath = workDir.resolve("project.jipx");
                List<String> violations = new ArrayList<>();
                new JIPipeXProjectWriter().writeWithViolations(m1, jipxPath, JIPipeProgressInfo.SILENT, violations);
                jipxViolations = violations.size();

                // The read dispatch routes ZIP → JIPX reader - never
                // instantiate the reader directly; the dispatch seam is part
                // of what this harness proves.
                stage = "JIPX_LOAD";
                JIPipeProject m2 = load(jipxPath, context);

                jipxModelEquivalent = canonical(monolithTree(m1)).equals(canonical(monolithTree(m2)));
                jipxCrateValid = crateValid(jipxPath);

                entry.put("legacyFixpoint", legacyFixpoint);
                entry.put("jipxModelEquivalent", jipxModelEquivalent);
                entry.put("jipxCrateValid", jipxCrateValid);
                entry.put("loadedNodesM1", loadedNodesM1);
                entry.put("jipxViolations", jipxViolations);
                entry.putNull("error");
                outcome = "OK";
            } catch (Exception e) {
                error = errorLine(e);
                // Map the failing stage to an outcome (a legacy-cycle failure
                // skips the JIPX steps; the JIPX fields keep their false/0
                // defaults):
                //   PARSE_FAILED     - the corpus file could not be copied (COPY);
                //   LOAD_FAILED      - legacy cycle loadProject threw (LOAD1/LOAD2);
                //   SAVE_FAILED      - legacy cycle saveProject threw (SAVE1/SAVE2);
                //   JIPX_SAVE_FAILED - writeWithViolations threw (JIPX_SAVE);
                //   JIPX_LOAD_FAILED - the JIPX container could not be loaded
                //                      (JIPX_LOAD, incl. the crate check that
                //                      runs inside this stage).
                switch (stage) {
                    case "COPY":
                        outcome = "PARSE_FAILED";
                        break;
                    case "LOAD1":
                    case "LOAD2":
                        outcome = "LOAD_FAILED";
                        break;
                    case "SAVE1":
                    case "SAVE2":
                        outcome = "SAVE_FAILED";
                        break;
                    case "JIPX_SAVE":
                        outcome = "JIPX_SAVE_FAILED";
                        break;
                    default:
                        outcome = "JIPX_LOAD_FAILED";
                        break;
                }
                entry.put("legacyFixpoint", legacyFixpoint);
                entry.put("jipxModelEquivalent", jipxModelEquivalent);
                entry.put("jipxCrateValid", jipxCrateValid);
                entry.put("loadedNodesM1", loadedNodesM1);
                entry.put("jipxViolations", jipxViolations);
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

        // The done-gate assertion: for every project the legacy cycle loaded
        // (outcome OK), the JIPX round trip must have been model-equivalent,
        // crate-valid, and violation-free. A JIPX regression must turn the
        // gated run RED, not merely write an ugly report.
        // Scanned: entries with outcome "OK" only - non-OK entries carry
        // false/0 by construction (their JIPX steps were skipped) and are
        // triaged through their outcome, not this assertion.
        // Checked: jipxModelEquivalent (must be true), jipxCrateValid (must
        // be true), jipxViolations (must be 0).
        // Deliberately NOT checked: legacyFixpoint (74 corpus files are
        // legitimately false - pre-existing .jip save churn, baselined in
        // Phase 0) and the error diagnostic (known JDK NPE flap).
        // Safety (no added flakiness): the three JIPX booleans are
        // deterministic given a successfully loaded model - the known
        // environmental flakiness (IOInterface registry warmup) affects the
        // LOAD outcome classification, not the equivalence of loaded models.
        List<String> jipxGateFailures = new ArrayList<>();
        for (ObjectNode entry : entries) {
            if (!"OK".equals(entry.path("outcome").asText())) {
                continue;
            }
            String file = entry.path("file").asText();
            if (!entry.path("jipxModelEquivalent").asBoolean(false)) {
                jipxGateFailures.add(file + " - jipxModelEquivalent=false");
            }
            if (!entry.path("jipxCrateValid").asBoolean(false)) {
                jipxGateFailures.add(file + " - jipxCrateValid=false");
            }
            if (entry.path("jipxViolations").asInt(0) != 0) {
                jipxGateFailures.add(file + " - jipxViolations=" + entry.path("jipxViolations").asInt());
            }
        }
        assertTrue(jipxGateFailures.isEmpty(),
                "JIPX round-trip gate FAILED for " + jipxGateFailures.size() + " entr"
                        + (jipxGateFailures.size() == 1 ? "y" : "ies") + ":\n  "
                        + String.join("\n  ", jipxGateFailures));

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
