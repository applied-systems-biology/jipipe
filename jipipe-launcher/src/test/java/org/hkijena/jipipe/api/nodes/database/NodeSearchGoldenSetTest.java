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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The golden-set gate of the search finetuning (#1408): the 93-case corpus at
 * {@code golden-set.json} pins, per natural-language query, which registered
 * node ids the enhanced search must place within the first {@code expectWithin}
 * results (default 5), and optionally at rank 0 ({@code first}).
 *
 * <p>
 * Result ranks count every returned entry (node entries, node examples, and
 * node templates alike), exactly the numbering the corpus probes used when
 * the corpus was authored: a template at rank 1 makes a node at rank 2.
 * Result ids are the entries' <em>origin node ids</em>
 * ({@link JIPipeNodeDatabaseEntry#getOriginNodeId()}), which is the id space
 * the corpus is written in ({@code ij1-blur-gaussian2d},
 * {@code plot-table:plot-histogram}, ...). The entry ids
 * ({@link JIPipeNodeDatabaseEntry#getId()}) are namespaced
 * ({@code create-node-by-info:<node id>}, {@code node-template:<uuid>}) and
 * never equal a corpus id; entries that wrap no node type (templates,
 * compartments) keep their entry id in the dump so template walls stay
 * visible in the failure worklist.
 *
 * <p>
 * The corpus is parsed as a Jackson {@link JsonNode} tree, never a POJO: the
 * {@code evidence} field (and any future field) must not break
 * deserialization. Unknown ids in {@code expect} or {@code first} fail with
 * the case id named, validated across the whole corpus before any case runs.
 *
 * <p>
 * {@code corpusIntegrity()} validates the corpus itself: the pinned total of
 * 93 cases, unique case ids, non-empty {@code expect}, at least one tag per
 * case, {@code expectWithin >= 1}, and the per-tag case counts pinned when
 * the corpus was built (a case counts once per tag it carries; a case with
 * tags {@code [biologist-outcome, dimensionality]} increments both).
 * {@code goldenSet()} runs the cases against the live
 * registry search. With {@code -Djipipe.golden-set.dump=<path>} the failing
 * worklist ({@code {case, missing, actualTop10}} per failing case, where
 * {@code missing} holds the expect ids that fell outside the window plus a
 * {@code first: ...} entry when the rank-0 pin is violated) is written as
 * JSON before asserting - the {@code SearchDescriptionQualityTest} dump
 * pattern. The dump is the measured fix backlog for the scorer work.
 */
class NodeSearchGoldenSetTest {

    private static final String CORPUS_RESOURCE = "golden-set.json";

    private static final String DUMP_PROPERTY = "jipipe.golden-set.dump";

    /**
     * Default result window an expect id must land in.
     */
    private static final int DEFAULT_EXPECT_WITHIN = 5;

    /**
     * How long to wait for the asynchronously built database entries before
     * failing (the live registry is large; the rebuild runs on a background
     * {@link org.hkijena.jipipe.api.run.JIPipeRunnableWorker}).
     */
    private static final long DATABASE_AWAIT_TIMEOUT_MS = 120_000;

    /**
     * The per-tag case counts pinned with the corpus (2026-11-25). A case
     * counts once per tag it carries. These exact numbers are the contract:
     * changing them requires rebuilding the corpus, not editing this test.
     */
    private static final Map<String, Integer> PINNED_TAG_COUNTS = createPinnedTagCounts();

    private static JIPipeNodeDatabase database;

    private static Map<String, Integer> createPinnedTagCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("biologist-outcome", 21);
        counts.put("bioinfo-algorithm", 25);
        counts.put("imagej-vocab", 16);
        counts.put("typo", 12);
        counts.put("substring", 6);
        counts.put("ordering", 11);
        counts.put("dimensionality", 16);
        return Collections.unmodifiableMap(counts);
    }

    @BeforeAll
    static void init() {
        // The ImageJLegacyPatcherLauncherSessionListener registered in this
        // module preinits the ImageJ1 legacy patcher before any test class
        // loads, so ensureInstance() here is safe. Idempotent: reuses the
        // JIPipe singleton if another test class in the same JVM already
        // initialized it.
        JIPipe.ensureInstance();
        database = JIPipeNodeDatabase.getInstance();
        awaitDatabaseEntries(database);
    }

    /**
     * The database builds its entries on a background queue
     * ({@link JIPipeNodeDatabase#rebuildImmediately()}); wait until the
     * rebuild has run so the id validation and the queries see the full
     * registry. The builder sets the entries and then builds the index and
     * the relation index inside the same queued run, so the wait ends only
     * when the queue has drained.
     */
    private static void awaitDatabaseEntries(JIPipeNodeDatabase database) {
        long deadline = System.currentTimeMillis() + DATABASE_AWAIT_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (!database.getEntries().isEmpty() && database.getQueue().isEmpty()) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        assertTrue(!database.getEntries().isEmpty() && database.getQueue().isEmpty(),
                "The node database did not finish building its entries within " + DATABASE_AWAIT_TIMEOUT_MS + " ms"
                        + " (entries built: " + !database.getEntries().isEmpty()
                        + ", build queue drained: " + database.getQueue().isEmpty() + ")");
    }

    /**
     * The corpus as a Jackson tree (no POJO: unknown fields are ignored by
     * construction).
     */
    private static JsonNode loadCorpus() {
        try (InputStream stream = NodeSearchGoldenSetTest.class.getResourceAsStream(CORPUS_RESOURCE)) {
            if (stream == null) {
                throw new RuntimeException("Golden-set corpus resource not found on the classpath: " + CORPUS_RESOURCE);
            }
            return JsonUtils.getObjectMapper().readTree(stream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read the golden-set corpus resource " + CORPUS_RESOURCE, e);
        }
    }

    private static String caseId(JsonNode caseNode) {
        JsonNode id = caseNode.get("id");
        if (id == null || !id.isTextual() || id.asText().isEmpty()) {
            throw new RuntimeException("A golden-set case has no id: " + caseNode);
        }
        return id.asText();
    }

    private static List<String> stringValues(JsonNode node, String field) {
        JsonNode value = node.get(field);
        List<String> result = new ArrayList<>();
        if (value != null && value.isTextual() && !value.asText().isEmpty()) {
            result.add(value.asText());
        } else if (value != null && value.isArray()) {
            for (JsonNode element : value) {
                result.add(element.asText());
            }
        }
        return result;
    }

    private static int expectWithinOf(JsonNode caseNode) {
        JsonNode value = caseNode.get("expectWithin");
        if (value == null || value.isNull()) {
            return DEFAULT_EXPECT_WITHIN;
        }
        return value.asInt();
    }

    /**
     * The id of one result in the corpus's id space: the origin node id for
     * entries that wrap a node type, the entry id otherwise (see the class
     * javadoc).
     */
    private static String resultIdOf(JIPipeNodeDatabaseEntry entry) {
        String originNodeId = entry.getOriginNodeId();
        return originNodeId != null ? originNodeId : entry.getId();
    }

    /**
     * Corpus validation: the pinned total of 93 cases, unique case ids,
     * non-empty expect, at least one tag per case, expectWithin >= 1, and the
     * pinned per-tag counts. Validates the corpus, not the scorer.
     */
    @Test
    void corpusIntegrity() {
        JsonNode corpus = loadCorpus();
        assertTrue(corpus.isArray() && !corpus.isEmpty(), "The golden-set corpus must be a non-empty array");
        assertTrue(corpus.size() == 93,
                "The golden-set corpus pins 93 cases (the pinned per-tag counts sum over them), found " + corpus.size());

        Set<String> seenIds = new HashSet<>();
        Map<String, Integer> tagCounts = new TreeMap<>();
        StringBuilder problems = new StringBuilder();
        for (JsonNode caseNode : corpus) {
            String id = caseId(caseNode);
            if (!seenIds.add(id)) {
                problems.append("  duplicate case id: ").append(id).append('\n');
            }
            JsonNode query = caseNode.get("query");
            if (query == null || !query.isTextual() || query.asText().isEmpty()) {
                problems.append("  case ").append(id).append(": query must be a non-empty string\n");
            }
            List<String> tags = stringValues(caseNode, "tags");
            if (tags.isEmpty()) {
                problems.append("  case ").append(id).append(": at least one tag is required")
                        .append(" (a tag-less case evades the pinned per-tag counts)\n");
            }
            for (String tag : tags) {
                tagCounts.merge(tag, 1, Integer::sum);
            }
            List<String> expect = stringValues(caseNode, "expect");
            if (expect.isEmpty()) {
                problems.append("  case ").append(id).append(": expect must be a non-empty string or array\n");
            }
            int expectWithin = expectWithinOf(caseNode);
            if (expectWithin < 1) {
                problems.append("  case ").append(id).append(": expectWithin must be >= 1, is ").append(expectWithin).append('\n');
            }
        }
        for (Map.Entry<String, Integer> pinned : PINNED_TAG_COUNTS.entrySet()) {
            int actual = tagCounts.getOrDefault(pinned.getKey(), 0);
            if (actual != pinned.getValue()) {
                problems.append("  tag ").append(pinned.getKey()).append(": pinned count ")
                        .append(pinned.getValue()).append(", corpus has ").append(actual).append('\n');
            }
        }
        for (Map.Entry<String, Integer> actual : tagCounts.entrySet()) {
            if (!PINNED_TAG_COUNTS.containsKey(actual.getKey())) {
                problems.append("  unknown tag ").append(actual.getKey()).append(" (not part of the pinned set)\n");
            }
        }
        assertTrue(problems.length() == 0, "Golden-set corpus integrity problems:\n" + problems);
    }

    /**
     * The gate: every case's expect ids must land within the case's result
     * window, and first (when set) must be rank 0.
     */
    @Test
    void goldenSet() {
        JsonNode corpus = loadCorpus();
        JIPipeEnhancedNodeDatabaseSearch search = new JIPipeEnhancedNodeDatabaseSearch(database);

        // Validate the whole corpus up front: every referenced id must be one
        // the search can return; unknown ids fail with the case id named.
        Set<String> knownIds = new HashSet<>();
        for (JIPipeNodeDatabaseEntry entry : database.getEntries()) {
            String originNodeId = entry.getOriginNodeId();
            if (originNodeId != null) {
                knownIds.add(originNodeId);
            }
        }
        Map<String, Set<String>> unknownByCase = new TreeMap<>();
        for (JsonNode caseNode : corpus) {
            String id = caseId(caseNode);
            for (String referenced : stringValues(caseNode, "expect")) {
                if (!knownIds.contains(referenced)) {
                    unknownByCase.computeIfAbsent(id, k -> new TreeSet<>()).add(referenced);
                }
            }
            for (String referenced : stringValues(caseNode, "first")) {
                if (!knownIds.contains(referenced)) {
                    unknownByCase.computeIfAbsent(id, k -> new TreeSet<>()).add(referenced);
                }
            }
        }
        if (!unknownByCase.isEmpty()) {
            StringBuilder report = new StringBuilder();
            report.append(unknownByCase.size()).append(" golden-set cases reference unknown node ids:\n");
            for (Map.Entry<String, Set<String>> entry : unknownByCase.entrySet()) {
                report.append("  case ").append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
            }
            throw new RuntimeException(report.toString());
        }

        // Run every case, collect the failures.
        List<GoldenSetFailure> failures = new ArrayList<>();
        for (JsonNode caseNode : corpus) {
            String id = caseId(caseNode);
            JsonNode queryField = caseNode.get("query");
            if (queryField == null || !queryField.isTextual() || queryField.asText().isEmpty()) {
                // Fail with the case id named instead of a bare NPE on the
                // field read (corpusIntegrity() reports the same problem, but
                // the two tests run independently).
                throw new RuntimeException("Golden-set case " + id + " has no usable query field"
                        + " (query must be a non-empty string): " + caseNode);
            }
            String query = queryField.asText();
            List<String> expect = stringValues(caseNode, "expect");
            List<String> first = stringValues(caseNode, "first");
            int expectWithin = expectWithinOf(caseNode);

            List<JIPipeNodeDatabaseEntry> results =
                    search.query(query, JIPipeNodeDatabasePipelineVisibility.Pipeline, true, true, Set.of());
            List<String> resultIds = new ArrayList<>(results.size());
            for (JIPipeNodeDatabaseEntry result : results) {
                resultIds.add(resultIdOf(result));
            }
            List<String> window = resultIds.subList(0, Math.min(expectWithin, resultIds.size()));

            List<String> missing = new ArrayList<>();
            for (String expectedId : expect) {
                if (!window.contains(expectedId)) {
                    missing.add(expectedId);
                }
            }
            if (!first.isEmpty() && (resultIds.isEmpty() || !resultIds.get(0).equals(first.get(0)))) {
                missing.add("first: expected " + first.get(0) + " at rank 0, got "
                        + (resultIds.isEmpty() ? "<no results>" : resultIds.get(0)));
            }
            if (!missing.isEmpty()) {
                failures.add(new GoldenSetFailure(id, missing,
                        resultIds.subList(0, Math.min(10, resultIds.size()))));
            }
        }

        // Write the dump BEFORE asserting: the failing worklist is the
        // measured fix backlog for the scorer work.
        String dumpPath = System.getProperty(DUMP_PROPERTY);
        if (dumpPath != null && !dumpPath.isEmpty()) {
            writeDump(dumpPath, failures);
            System.out.println("NodeSearchGoldenSetTest: dumped " + failures.size()
                    + " failing cases to " + dumpPath);
        }

        if (!failures.isEmpty()) {
            StringBuilder report = new StringBuilder();
            report.append(failures.size()).append(" of ").append(corpus.size())
                    .append(" golden-set cases fail against the enhanced search:\n");
            for (GoldenSetFailure failure : failures) {
                report.append("  case ").append(failure.caseId()).append(": missing ")
                        .append(failure.missing()).append(", actualTop10=").append(failure.actualTop10()).append('\n');
            }
            report.append("Fix the scorer (see the golden-set lexicon plan); do not weaken this gate.");
            assertTrue(failures.isEmpty(), report.toString());
        }
    }

    private record GoldenSetFailure(String caseId, List<String> missing, List<String> actualTop10) {
    }

    private static void writeDump(String dumpPath, List<GoldenSetFailure> failures) {
        ObjectNode document = JsonUtils.getObjectMapper().createObjectNode();
        document.put("generated", java.time.LocalDate.now().toString());
        ArrayNode rows = document.putArray("failing");
        for (GoldenSetFailure failure : failures) {
            ObjectNode row = rows.addObject();
            row.put("case", failure.caseId());
            ArrayNode missing = row.putArray("missing");
            for (String id : failure.missing()) {
                missing.add(id);
            }
            ArrayNode actualTop10 = row.putArray("actualTop10");
            for (String id : failure.actualTop10()) {
                actualTop10.add(id);
            }
        }
        try {
            Path path = Paths.get(dumpPath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.write(path, Collections.singletonList(JsonUtils.toJsonString(document)),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write the golden-set dump to " + dumpPath, e);
        }
    }
}
