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
import org.hkijena.jipipe.api.microservice.MicroserviceState;
import org.hkijena.jipipe.api.service.components.JIPipeAIServiceComponent;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Assumptions;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The merged-ranking evaluation harness of #1409: a <b>report-only</b>
 * measurement of the golden-set corpus through the live merged path
 * (classic scores fused with AI scores via
 * {@link JIPipeMergedNodeSearchRanking#fuse}, mirroring the merged branch of
 * {@link JIPipeNodeDatabase#query}) against the classic-only baseline. It is
 * deliberately NOT a hard CI gate (spec ruling: "Not a hard CI gate"): no
 * quality assertion pins any number, the report exists so a change of the
 * merged ranking is visible in per-tag hit rates.
 *
 * <p>
 * <b>What is measured.</b> For every case of {@code golden-set.json} (the
 * same 94-case corpus, loaded as a Jackson tree with the same pattern as
 * {@link NodeSearchGoldenSetTest}), the harness computes
 * <ul>
 * <li>(a) the classic-only ranking: {@code enhancedSearch.queryScored(...)}
 * and the rank of the best {@code expect} id; and</li>
 * <li>(b) the merged ranking: {@code fuse(classic, ai, Set.of())} where
 * {@code ai} comes from {@code aiSearch.queryScoredAsync(...)} awaited with
 * the AI search's inherited timeout
 * ({@link JIPipeAINodeDatabaseSearch#SEARCH_TIMEOUT_SECONDS} plus headroom
 * for the lazy first embedding of the global index); a null completion,
 * timeout, or failure counts as "no AI contribution" (the fusion's
 * degradation arm, identical to the production wait).</li>
 * </ul>
 * Ranks count every returned entry (nodes, examples, templates), exactly the
 * numbering the gate uses, and are 0-based ({@code -1} = no expect id in the
 * list). Result ids are the entries' origin node ids
 * ({@link JIPipeNodeDatabaseEntry#getOriginNodeId()}), the id space the
 * corpus is written in; entries without an origin id (templates, examples)
 * are skipped when matching expect ids (Task 4 review carry-over: they
 * cannot match corpus ids, the same id space the gate matches in), so the
 * report is directly comparable to the gate's corpus. The per-tag aggregates
 * count a case once per tag it carries; "top 5" is the fixed first-five
 * window ({@link #TOP_N}), a case's own {@code expectWithin} is reported per
 * case.
 *
 * <p>
 * <b>The assumption gate: actual model readiness, not configuration.</b> In
 * an isolated test JVM
 * {@link JIPipeMergedNodeSearchRanking#isAiContributionActive()} is TRUE (the
 * default artifact environment resolves as configured) while the embedding
 * model never becomes Ready, and every merged query would then consume the
 * full 300 s AI timeout. The harness therefore gates on the model's ACTUAL
 * state: {@link JIPipeAIServiceComponent#getEmbeddingModelStatus()}, the
 * {@link MicroserviceState} of the embedding model microservice (Ready only
 * once the model is actually serving). When not Ready, the harness attempts
 * a bounded JIT warm-up: it triggers the model service's own start path
 * (the same one {@link JIPipeAIServiceComponent#tryEmbed(String)} uses) on a
 * background thread and polls the state for at most
 * {@value #WARM_UP_TIMEOUT_MS} ms, stopping the service again when the bound
 * elapses while still Starting. If the model is still not Ready, the test is
 * skipped via {@link Assumptions#assumeTrue(boolean, java.util.function.Supplier)}
 * ("skipped: model unavailable"): silent in CI, visible in the run log. On a
 * machine with a working model the harness runs and reports.
 *
 * <p>
 * <b>Dump.</b> {@code -Djipipe.merged-harness.dump=<path>} writes the report
 * {@code {generated, modelId, embeddingModelState, aiContributionActive,
 * corpusCases, measuredCases, wallTimeMs, classicSearchMs, aiWaitSeconds,
 * [skipped], perTag: {tag: {cases, classicTop5, mergedTop5, classicTop5Pct,
 * mergedTop5Pct, classicInWindow, mergedInWindow}}, cases: [{id, tags,
 * expect, expectWithin, classicRank, mergedRank, classicInWindow,
 * mergedInWindow, classicCount, mergedCount}]}} as JSON BEFORE any assertion
 * (the gate's dump pattern); unset, no file is written. A compact per-tag
 * summary table is logged either way. The wall time of the warm-up and the
 * corpus run is logged (the report-only bounds: the warm-up bound and the
 * overall run bound below keep a broken model from hanging the run).
 *
 * <p>
 * <b>Assertions.</b> Structural sanity only: for every measured case whose
 * classic result list is non-empty, the fused list is non-empty. No quality
 * assertion exists by design.
 */
class NodeSearchMergedRankingHarnessTest {

    private static final String CORPUS_RESOURCE = "golden-set.json";

    private static final String DUMP_PROPERTY = "jipipe.merged-harness.dump";

    /**
     * The fixed first-result window the "top 5 hit rate" counts (the
     * corpus's default window; a case's own {@code expectWithin} is reported
     * per case, one case pins a wider window).
     */
    private static final int TOP_N = 5;

    /**
     * How long to wait for the asynchronously built database entries before
     * failing (same bound and pattern as {@link NodeSearchGoldenSetTest}).
     */
    private static final long DATABASE_AWAIT_TIMEOUT_MS = 120_000;

    /**
     * The bound of the JIT warm-up attempt (ms): how long the embedding
     * model start may take before the harness gives up and skips. In a test
     * JVM without a startable model the attempt fails within seconds; the
     * bound only caps a slow start.
     */
    private static final long WARM_UP_TIMEOUT_MS = 90_000;

    /**
     * The wait bound for one AI search future (seconds): the AI search's own
     * safety timeout (inherited unchanged, the value the production merged
     * branch waits with) plus headroom for the lazy first embedding of the
     * global index, which the first search run performs.
     */
    private static final long AI_WAIT_SECONDS = JIPipeAINodeDatabaseSearch.SEARCH_TIMEOUT_SECONDS + 60;

    /**
     * The overall wall-time bound of the corpus run (ms): a guard so a
     * pathologically slow embedding index cannot hang the harness; when the
     * bound elapses, the report covers the cases measured so far
     * ({@code measuredCases} vs {@code corpusCases} in the dump makes a
     * partial run visible).
     */
    private static final long TOTAL_RUN_BOUND_MS = 900_000;

    /**
     * The poll interval of the warm-up wait (ms).
     */
    private static final long WARM_UP_POLL_MS = 250;

    private static JIPipeNodeDatabase database;

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
     * Waits until the background rebuild has built the entries and drained
     * its queue (the shape {@link NodeSearchGoldenSetTest} uses).
     */
    private static void awaitDatabaseEntries(JIPipeNodeDatabase database) {
        long deadline = System.currentTimeMillis() + DATABASE_AWAIT_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (!database.getEntries().isEmpty() && database.getQueue().isEmpty()) {
                return;
            }
            sleepQuietly(100);
        }
        assertTrue(!database.getEntries().isEmpty() && database.getQueue().isEmpty(),
                "The node database did not finish building its entries within " + DATABASE_AWAIT_TIMEOUT_MS + " ms");
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * The corpus as a Jackson tree (no POJO: unknown fields such as
     * {@code evidence} are ignored by construction; the same loader pattern
     * as the golden gate).
     */
    private static JsonNode loadCorpus() {
        try (InputStream stream = NodeSearchMergedRankingHarnessTest.class.getResourceAsStream(CORPUS_RESOURCE)) {
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
            return TOP_N;
        }
        return value.asInt();
    }

    /**
     * The 0-based rank of the best (highest-ranked) expect id in the result
     * list, or -1 when no expect id is present. Entries without an origin
     * node id (templates, examples) are skipped: their ids are namespaced
     * and never equal a corpus id (the gate's matching semantics).
     */
    private static int bestRank(List<JIPipeNodeDatabaseEntry> results, Set<String> expect) {
        for (int i = 0; i < results.size(); i++) {
            String originNodeId = results.get(i).getOriginNodeId();
            if (originNodeId != null && expect.contains(originNodeId)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * The 0-based rank of the best (highest-ranked) expect id in the scored
     * result list, or -1 (see {@link #bestRank(List, Set)}).
     */
    private static int bestRankScored(List<ScoredNodeDatabaseEntry> results, Set<String> expect) {
        for (int i = 0; i < results.size(); i++) {
            String originNodeId = results.get(i).entry().getOriginNodeId();
            if (originNodeId != null && expect.contains(originNodeId)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * The embedding model microservice of the AI service (null-safe).
     */
    private static JIPipeAIServiceComponent.EmbeddingModelService embeddingModelService() {
        JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
        return aiService != null ? aiService.getEmbeddingModelService() : null;
    }

    private static String stateDetailOf(JIPipeAIServiceComponent.EmbeddingModelService service) {
        if (service == null) {
            return "";
        }
        String detail = service.getStateDetail();
        return detail != null ? detail : "";
    }

    /**
     * The bounded JIT warm-up: triggers the embedding model service's own
     * start path (the one {@link JIPipeAIServiceComponent#tryEmbed(String)}
     * uses before its first embed) on a background thread and waits up to
     * {@value #WARM_UP_TIMEOUT_MS} ms for the Ready state. A start attempt
     * that FAILED (the start threw: no resolvable artifact, a failed server
     * spawn) ends the wait at once; a start still Starting at the bound is
     * stopped again so no half-started model process is left behind.
     *
     * @return true if the model is Ready afterwards
     */
    private static boolean warmUpEmbeddingModel() {
        JIPipeAIServiceComponent.EmbeddingModelService service = embeddingModelService();
        if (service == null) {
            return false;
        }
        if (service.isReady()) {
            return true;
        }
        MicroserviceState stateBeforeAttempt = service.getState();
        Thread starter = new Thread(() -> {
            try {
                service.start();
            } catch (Exception e) {
                // start() records the failure on the service state; the
                // message is surfaced through the state detail below.
                System.out.println("NodeSearchMergedRankingHarnessTest: embedding model start attempt threw: " + e);
            }
        }, "merged-harness-embedding-warm-up");
        starter.setDaemon(true);
        starter.start();

        long deadline = System.currentTimeMillis() + WARM_UP_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (service.isReady()) {
                return true;
            }
            MicroserviceState state = service.getState();
            // Failed is only reachable after the start attempt entered
            // onStart() and threw (or the service failed before this run:
            // either way a retry within the bound cannot succeed).
            if (state == MicroserviceState.Failed && stateBeforeAttempt != MicroserviceState.Failed) {
                return false;
            }
            sleepQuietly(WARM_UP_POLL_MS);
        }
        System.out.println("NodeSearchMergedRankingHarnessTest: embedding model still " + service.getState()
                + " after the " + WARM_UP_TIMEOUT_MS + " ms warm-up bound (detail: " + stateDetailOf(service)
                + "), stopping it");
        try {
            service.stop();
        } catch (Exception e) {
            System.out.println("NodeSearchMergedRankingHarnessTest: embedding model stop failed: " + e);
        }
        return false;
    }

    @Test
    void mergedRankingHarness() {
        JsonNode corpus = loadCorpus();
        String dumpPath = System.getProperty(DUMP_PROPERTY);

        // ----- The readiness gate (see the class javadoc): actual model
        // state, with a bounded warm-up attempt; NOT isAiContributionActive()
        // (true in test JVMs while the model never becomes Ready). -----
        JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
        MicroserviceState stateBefore = embeddingModelService() != null ? embeddingModelService().getState() : null;
        long warmUpStart = System.currentTimeMillis();
        boolean ready = warmUpEmbeddingModel();
        long warmUpMs = System.currentTimeMillis() - warmUpStart;
        JIPipeAIServiceComponent.EmbeddingModelService modelService = embeddingModelService();
        MicroserviceState stateAfter = modelService != null ? modelService.getState() : MicroserviceState.Stopped;
        System.out.println("NodeSearchMergedRankingHarnessTest: embedding model before warm-up: " + stateBefore
                + ", after " + warmUpMs + " ms: " + stateAfter
                + (stateDetailOf(modelService).isEmpty() ? "" : " (detail: " + stateDetailOf(modelService) + ")"));

        if (!ready) {
            // A dump skeleton is written even on the skip path (when the
            // property is set), so a reader sees WHY the report is empty
            // instead of a stale or absent file.
            if (dumpPath != null && !dumpPath.isEmpty()) {
                Map<String, String> skipInfo = new LinkedHashMap<>();
                skipInfo.put("skipped", "model unavailable: embedding model state " + stateAfter
                        + " after a " + warmUpMs + " ms warm-up attempt"
                        + (stateDetailOf(modelService).isEmpty() ? "" : " (detail: " + stateDetailOf(modelService) + ")"));
                writeDump(dumpPath, skipInfo, JIPipeMergedNodeSearchRanking.isAiContributionActive(),
                        aiService != null ? aiService.getModelId() : null, stateAfter.name(),
                        new TreeMap<>(), List.of(), 0, 0, corpus.size());
            }
            Assumptions.assumeTrue(false, () -> "skipped: model unavailable (embedding model state "
                    + stateAfter + " after the " + WARM_UP_TIMEOUT_MS + " ms warm-up bound)");
        }

        // ----- Corpus run: classic-only vs merged for every case -----
        JIPipeEnhancedNodeDatabaseSearch classicSearch = new JIPipeEnhancedNodeDatabaseSearch(database);
        JIPipeAINodeDatabaseSearch aiSearch = database.getAiSearch();
        boolean aiContributionActive = JIPipeMergedNodeSearchRanking.isAiContributionActive();
        String modelId = aiService != null ? aiService.getModelId() : null;

        long runStart = System.currentTimeMillis();
        long classicMillis = 0;
        int aiPresent = 0;
        int aiMissing = 0;
        List<CaseResult> results = new ArrayList<>(corpus.size());

        for (JsonNode caseNode : corpus) {
            if (System.currentTimeMillis() - runStart > TOTAL_RUN_BOUND_MS) {
                System.out.println("NodeSearchMergedRankingHarnessTest: the overall run bound of " + TOTAL_RUN_BOUND_MS
                        + " ms elapsed, reporting the " + results.size() + " cases measured so far");
                break;
            }
            String id = caseId(caseNode);
            String query = caseNode.get("query").asText();
            List<String> tags = stringValues(caseNode, "tags");
            Set<String> expect = new HashSet<>(stringValues(caseNode, "expect"));
            List<String> expectOrder = stringValues(caseNode, "expect");
            int expectWithin = expectWithinOf(caseNode);

            long classicStart = System.nanoTime();
            List<ScoredNodeDatabaseEntry> classic =
                    classicSearch.queryScored(query, JIPipeNodeDatabasePipelineVisibility.Pipeline, true, true, Set.of());
            classicMillis += TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - classicStart);

            List<JIPipeNodeDatabaseEntry> merged;
            if (query.isBlank()) {
                // The merged branch returns the classic result for blank
                // queries (no relevance signal; the corpus pins non-blank
                // queries, the guard only mirrors the production branch).
                merged = classic.stream().map(ScoredNodeDatabaseEntry::entry).toList();
            } else {
                List<ScoredNodeDatabaseEntry> ai = awaitAiScored(aiSearch, query);
                if (ai == null || ai.isEmpty()) {
                    aiMissing++;
                } else {
                    aiPresent++;
                }
                merged = JIPipeMergedNodeSearchRanking.fuse(classic, ai, Set.of());
            }

            results.add(new CaseResult(id, tags, expectOrder, expectWithin,
                    bestRankScored(classic, expect),
                    bestRank(merged, expect),
                    classic.size(),
                    merged.size()));
        }
        long wallTimeMs = System.currentTimeMillis() - runStart;

        // ----- Aggregates: a case counts once per tag it carries -----
        Map<String, TagAggregate> perTag = new TreeMap<>();
        int classicInWindow = 0;
        int mergedInWindow = 0;
        for (CaseResult result : results) {
            if (result.classicInWindow()) {
                classicInWindow++;
            }
            if (result.mergedInWindow()) {
                mergedInWindow++;
            }
            for (String tag : result.tags()) {
                perTag.computeIfAbsent(tag, tagKey -> new TagAggregate()).add(result);
            }
        }

        // ----- The report BEFORE any assertion (the gate's dump pattern) -----
        if (dumpPath != null && !dumpPath.isEmpty()) {
            writeDump(dumpPath, null, aiContributionActive, modelId, stateAfter.name(),
                    perTag, results, wallTimeMs, classicMillis, corpus.size());
            System.out.println("NodeSearchMergedRankingHarnessTest: dumped the merged-ranking report to " + dumpPath);
        }

        // ----- Compact per-tag summary -----
        System.out.println("NodeSearchMergedRankingHarnessTest: model=" + modelId + " (state " + stateAfter
                + "), aiContributionActive=" + aiContributionActive + ", warm-up " + warmUpMs + " ms");
        System.out.printf("  measured %d of %d cases in %d ms (classic scoring %d ms);"
                        + " AI contribution present on %d cases, absent on %d%n",
                results.size(), corpus.size(), wallTimeMs, classicMillis, aiPresent, aiMissing);
        System.out.printf("  %-20s %6s %16s %16s%n", "tag", "cases",
                "classicTop" + TOP_N, "mergedTop" + TOP_N);
        for (Map.Entry<String, TagAggregate> entry : perTag.entrySet()) {
            TagAggregate aggregate = entry.getValue();
            System.out.printf("  %-20s %6d %9d (%3d%%) %9d (%3d%%)%n", entry.getKey(), aggregate.cases,
                    aggregate.classicTopN, aggregate.classicPercent(),
                    aggregate.mergedTopN, aggregate.mergedPercent());
        }
        System.out.printf("  %-20s %6d %9d (%3d%%) %9d (%3d%%)%n", "all", results.size(),
                classicInWindow, results.isEmpty() ? 0 : (100 * classicInWindow) / results.size(),
                mergedInWindow, results.isEmpty() ? 0 : (100 * mergedInWindow) / results.size());
        if (aiMissing > 0) {
            System.out.println("  note: " + aiMissing + " cases ran without an AI contribution"
                    + " (null completion, timeout, or cancelled run); their merged list equals the classic order.");
        }

        // ----- Structural sanity only (no quality assertions by design) -----
        StringBuilder emptyFusions = new StringBuilder();
        for (CaseResult result : results) {
            if (result.classicCount() > 0 && result.mergedCount() == 0) {
                emptyFusions.append("  case ").append(result.id()).append(": classic ")
                        .append(result.classicCount()).append(" results, fused 0\n");
            }
        }
        assertTrue(emptyFusions.length() == 0,
                "The fused list must not be empty when the classic list is not:\n" + emptyFusions);
    }

    /**
     * The AI contribution of one case, awaited with the AI search's inherited
     * timeout (plus headroom for the lazy first embedding). Null means "no AI
     * contribution" (the fusion's degradation arm): a null completion, a
     * timeout, or a failure - the same failure kinds the production merged
     * branch's wait maps to null.
     */
    private static List<ScoredNodeDatabaseEntry> awaitAiScored(JIPipeAINodeDatabaseSearch aiSearch, String query) {
        CompletableFuture<List<ScoredNodeDatabaseEntry>> future;
        try {
            future = aiSearch.queryScoredAsync(query, JIPipeNodeDatabasePipelineVisibility.Pipeline, true, true, Set.of());
        } catch (Exception e) {
            System.out.println("NodeSearchMergedRankingHarnessTest: AI search preparation failed for '" + query + "': " + e);
            return null;
        }
        if (future == null) {
            return null;
        }
        try {
            return future.get(AI_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            System.out.println("NodeSearchMergedRankingHarnessTest: AI search timed out after " + AI_WAIT_SECONDS
                    + " s for '" + query + "', the case runs with the classic order");
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            System.out.println("NodeSearchMergedRankingHarnessTest: AI search failed for '" + query + "': " + e);
            return null;
        }
    }

    /**
     * One measured case: the best expect rank under the classic and the
     * merged ranking (0-based, -1 = no expect id present), plus the result
     * list sizes.
     */
    private record CaseResult(String id,
                              List<String> tags,
                              List<String> expect,
                              int expectWithin,
                              int classicRank,
                              int mergedRank,
                              int classicCount,
                              int mergedCount) {

        boolean classicInWindow() {
            return classicRank >= 0 && classicRank < expectWithin;
        }

        boolean mergedInWindow() {
            return mergedRank >= 0 && mergedRank < expectWithin;
        }

        boolean classicTopN() {
            return classicRank >= 0 && classicRank < TOP_N;
        }

        boolean mergedTopN() {
            return mergedRank >= 0 && mergedRank < TOP_N;
        }
    }

    /**
     * Per-tag aggregate of the cases carrying the tag.
     */
    private static final class TagAggregate {
        private int cases;
        private int classicTopN;
        private int mergedTopN;
        private int classicWindow;
        private int mergedWindow;

        void add(CaseResult result) {
            cases++;
            if (result.classicTopN()) {
                classicTopN++;
            }
            if (result.mergedTopN()) {
                mergedTopN++;
            }
            if (result.classicInWindow()) {
                classicWindow++;
            }
            if (result.mergedInWindow()) {
                mergedWindow++;
            }
        }

        int classicPercent() {
            return cases == 0 ? 0 : (100 * classicTopN) / cases;
        }

        int mergedPercent() {
            return cases == 0 ? 0 : (100 * mergedTopN) / cases;
        }
    }

    /**
     * Writes the report JSON (the shape is documented in the class javadoc).
     */
    private static void writeDump(String dumpPath,
                                  Map<String, String> skipInfo,
                                  boolean aiContributionActive,
                                  String modelId,
                                  String embeddingModelState,
                                  Map<String, TagAggregate> perTag,
                                  List<CaseResult> results,
                                  long wallTimeMs,
                                  long classicMillis,
                                  int corpusSize) {
        ObjectNode document = JsonUtils.getObjectMapper().createObjectNode();
        document.put("generated", java.time.LocalDate.now().toString());
        document.put("modelId", modelId);
        document.put("embeddingModelState", embeddingModelState);
        document.put("aiContributionActive", aiContributionActive);
        document.put("corpusCases", corpusSize);
        document.put("measuredCases", results.size());
        document.put("wallTimeMs", wallTimeMs);
        document.put("classicSearchMs", classicMillis);
        document.put("aiWaitSeconds", AI_WAIT_SECONDS);
        if (skipInfo != null) {
            for (Map.Entry<String, String> skipped : skipInfo.entrySet()) {
                document.put(skipped.getKey(), skipped.getValue());
            }
        }
        ObjectNode tags = document.putObject("perTag");
        for (Map.Entry<String, TagAggregate> entry : perTag.entrySet()) {
            TagAggregate aggregate = entry.getValue();
            ObjectNode tagNode = tags.putObject(entry.getKey());
            tagNode.put("cases", aggregate.cases);
            tagNode.put("classicTop5", aggregate.classicTopN);
            tagNode.put("mergedTop5", aggregate.mergedTopN);
            tagNode.put("classicTop5Pct", aggregate.classicPercent());
            tagNode.put("mergedTop5Pct", aggregate.mergedPercent());
            tagNode.put("classicInWindow", aggregate.classicWindow);
            tagNode.put("mergedInWindow", aggregate.mergedWindow);
        }
        ArrayNode caseArray = document.putArray("cases");
        for (CaseResult result : results) {
            ObjectNode caseNode = caseArray.addObject();
            caseNode.put("id", result.id());
            ArrayNode tagArray = caseNode.putArray("tags");
            for (String tag : result.tags()) {
                tagArray.add(tag);
            }
            ArrayNode expectArray = caseNode.putArray("expect");
            for (String expectedId : result.expect()) {
                expectArray.add(expectedId);
            }
            caseNode.put("expectWithin", result.expectWithin());
            caseNode.put("classicRank", result.classicRank());
            caseNode.put("mergedRank", result.mergedRank());
            caseNode.put("classicInWindow", result.classicInWindow());
            caseNode.put("mergedInWindow", result.mergedInWindow());
            caseNode.put("classicCount", result.classicCount());
            caseNode.put("mergedCount", result.mergedCount());
        }
        try {
            Path path = Paths.get(dumpPath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.write(path, Collections.singletonList(JsonUtils.toJsonString(document)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write the merged-ranking report to " + dumpPath, e);
        }
    }
}