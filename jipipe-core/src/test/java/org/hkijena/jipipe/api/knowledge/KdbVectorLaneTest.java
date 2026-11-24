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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the vector lane wiring (design spec 5.5, lane 2): a deterministic
 * stub embedder stands in for the JEMB model, so the fusion weight, the merge,
 * and the lane attribution are pinned without a running embedding model
 * (ruling 2026-11-20: the vector lane is not part of the golden test).
 * <p>
 * The stub maps a tiny synonym lexicon into a 4 dimensional space: "cat" and
 * "feline" share a direction, as do "dog"/"canine", "orbit"/"space", and
 * "reef"/"coral". The lexicon is deliberately disjoint from the tokenizer
 * surface of the query: the corpus fixture pairs a "feline" entry with a
 * "cat" query, so the vector hit has no lexical overlap and can only be
 * reached through the vector lane. The lexicon is also disjoint from every
 * other token the rendered embedding text can carry (the section wording, the
 * entry keys of neighbors in the next steps lines, kind and domain ids): the
 * lane embeds the rendered text since the renderer exists (design spec 5.6),
 * and a lexicon token hiding in a neighbor key would silently turn an
 * unrelated entry into a vector hit.
 */
class KdbVectorLaneTest {

    private static final int STUB_DIMENSION = 4;

    private static Path fixture() {
        try {
            return Paths.get(KdbVectorLaneTest.class.getResource("/kdb-fixtures/lanes/vector").toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<KdbEntry> entries() {
        return new KdbEntryLoader().loadAll(fixture()).getEntries();
    }

    /**
     * The stub embedder: synonym groups map to unit axes, everything else
     * contributes nothing. A text without a lexicon token embeds to the zero
     * vector, which cosine similarity treats as no match (the vector
     * analogue of "no query token matches").
     */
    private static float[] stubEmbed(String text) {
        float[] vector = new float[STUB_DIMENSION];
        if (text == null) {
            return vector;
        }
        for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z]+")) {
            if (token.isEmpty()) {
                continue;
            }
            switch (token) {
                case "cat", "feline" -> vector[0] += 1.0f;
                case "dog", "canine" -> vector[1] += 1.0f;
                case "orbit", "space" -> vector[2] += 1.0f;
                case "reef", "coral" -> vector[3] += 1.0f;
            }
        }
        return vector;
    }

    private static List<String> keys(List<KnowledgeResult> results) {
        return results.stream().map(KnowledgeResult::entryKey).collect(Collectors.toList());
    }

    private static Map<String, KnowledgeResult> byKey(List<KnowledgeResult> results) {
        return results.stream().collect(Collectors.toMap(KnowledgeResult::entryKey, result -> result));
    }

    @Test
    void vectorLaneParticipatesInFusion() {
        List<KdbEntry> corpus = entries();
        KdbVectorLane lane = new KdbVectorLane(corpus, KdbVectorLaneTest::stubEmbed, null);
        // Lane level: "zebra cat" carries the feline direction. Only the two
        // feline entries point that way; the waypoint (pure feline) ranks
        // above the bridge (feline plus orbit). The zero vector entries of
        // the corpus are not vector hits at all.
        List<KdbVectorLane.ScoredEntry> laneHits = lane.search("zebra cat", 10);
        assertEquals(List.of("bioimage-analysis/patterns/feline-waypoint",
                "bioimage-analysis/patterns/bridge-crossing"),
                laneHits.stream().map(KdbVectorLane.ScoredEntry::entryKey).toList(),
                "Only the feline entries are vector hits, ordered by cosine similarity");
        assertEquals(1.0, laneHits.get(0).score(), 1e-9, "The waypoint points exactly at the query");
        assertEquals(1.0 / Math.sqrt(2.0), laneHits.get(1).score(), 1e-9,
                "The bridge mixes feline and orbit directions");

        // Fusion level: the lexical lane matches only the zebra crossing, the
        // relation lane reaches its ENTRY_REF neighbor, and the vector lane
        // reaches the two feline entries that no other lane can see.
        KnowledgeService service = new KnowledgeService(corpus, lane);
        List<KnowledgeResult> results = service.searchKnowledge("zebra cat", null, null, 10);
        assertEquals(List.of("bioimage-analysis/patterns/feline-waypoint",
                "bioimage-analysis/patterns/moor-plain",
                "bioimage-analysis/patterns/zebra-crossing",
                "bioimage-analysis/patterns/bridge-crossing",
                "bioimage-analysis/patterns/island-map"), keys(results),
                "All five entries surface, ordered by fused score then entry key");
        Map<String, KnowledgeResult> hits = byKey(results);
        assertEquals(Set.of(KnowledgeService.LANE_VECTOR), hits.get("bioimage-analysis/patterns/feline-waypoint").lanes(),
                "The waypoint is reachable only through the vector lane");
        assertEquals(Set.of(KnowledgeService.LANE_LEXICAL), hits.get("bioimage-analysis/patterns/zebra-crossing").lanes());
        assertEquals(Set.of(KnowledgeService.LANE_RELATION), hits.get("bioimage-analysis/patterns/moor-plain").lanes());
        assertEquals(Set.of(KnowledgeService.LANE_VECTOR), hits.get("bioimage-analysis/patterns/bridge-crossing").lanes());
        assertEquals(Set.of(KnowledgeService.LANE_RELATION), hits.get("bioimage-analysis/patterns/island-map").lanes(),
                "The island map is only a neighbor of the vector hit: the vector lane seeds the relation lane");
        // Reciprocal rank fusion, k = 60, 1-based ranks: the vector ranking
        // is [feline-waypoint, bridge-crossing], the lexical ranking is
        // [zebra-crossing], and the relation ranking is [moor-plain,
        // island-map] (the vector hit's neighbor enters at rank 2).
        assertEquals(1.0 / (KnowledgeService.RRF_K + 1),
                hits.get("bioimage-analysis/patterns/feline-waypoint").score(),
                "Vector rank 1 contributes 1/(k+1)");
        assertEquals(1.0 / (KnowledgeService.RRF_K + 2),
                hits.get("bioimage-analysis/patterns/bridge-crossing").score(),
                "Vector rank 2 contributes 1/(k+2)");
        assertEquals(1.0 / (KnowledgeService.RRF_K + 1),
                hits.get("bioimage-analysis/patterns/zebra-crossing").score(),
                "Lexical rank 1 contributes 1/(k+1)");
        assertEquals(1.0 / (KnowledgeService.RRF_K + 1),
                hits.get("bioimage-analysis/patterns/moor-plain").score(),
                "Relation rank 1 contributes 1/(k+1)");
        assertEquals(1.0 / (KnowledgeService.RRF_K + 2),
                hits.get("bioimage-analysis/patterns/island-map").score(),
                "Relation rank 2 contributes 1/(k+2)");
    }

    @Test
    void missingEmbedderDegradesGracefully() {
        List<KdbEntry> corpus = entries();
        Function<String, float[]> throwing = text -> {
            throw new IllegalStateException("no embedding model loaded");
        };
        KnowledgeService withLane = new KnowledgeService(corpus, new KdbVectorLane(corpus, throwing, null));
        KnowledgeService withoutLane = new KnowledgeService(corpus);
        List<KnowledgeResult> actual = assertDoesNotThrow(
                () -> withLane.searchKnowledge("zebra cat", null, null, 10),
                "A failing embedder must never fail the search");
        List<KnowledgeResult> expected = withoutLane.searchKnowledge("zebra cat", null, null, 10);
        assertEquals(expected, actual,
                "With a failing embedder the results are indistinguishable from a service without a vector lane");
        assertEquals(List.of("bioimage-analysis/patterns/moor-plain",
                "bioimage-analysis/patterns/zebra-crossing"), keys(actual),
                "Only the lexical and relation hits remain");
        assertFalse(actual.stream().anyMatch(result -> result.lanes().contains(KnowledgeService.LANE_VECTOR)),
                "The vector lane tag is absent");
    }

    @Test
    void cacheRoundTripAvoidsRecomputation(@TempDir Path cacheDir) {
        List<KdbEntry> corpus = entries();
        AtomicInteger calls = new AtomicInteger();
        Function<String, float[]> counting = text -> {
            calls.incrementAndGet();
            return stubEmbed(text);
        };
        KdbVectorLane cold = new KdbVectorLane(corpus, counting, cacheDir);
        List<KdbVectorLane.ScoredEntry> first = cold.search("zebra cat", 10);
        assertEquals(corpus.size() + 1, calls.get(),
                "The cold lane embeds every corpus entry once plus the query");
        List<KdbVectorLane.ScoredEntry> warm = cold.search("zebra cat", 10);
        assertEquals(corpus.size() + 2, calls.get(),
                "The warm lane serves the entry vectors from the cache: only the query is embedded again");
        assertEquals(first, warm, "Cached vectors produce identical rankings");
        KdbVectorLane fresh = new KdbVectorLane(corpus, counting, cacheDir);
        List<KdbVectorLane.ScoredEntry> persisted = fresh.search("zebra cat", 10);
        assertEquals(corpus.size() + 3, calls.get(),
                "A fresh lane reads the persisted cache: only the query embedding is computed");
        assertEquals(first, persisted, "The persisted cache produces identical rankings");
    }

    @Test
    void nullCacheDirectoryComputesEveryTime() {
        List<KdbEntry> corpus = entries();
        AtomicInteger calls = new AtomicInteger();
        Function<String, float[]> counting = text -> {
            calls.incrementAndGet();
            return stubEmbed(text);
        };
        KdbVectorLane lane = new KdbVectorLane(corpus, counting, null);
        lane.search("zebra cat", 10);
        assertEquals(corpus.size() + 1, calls.get());
        lane.search("zebra cat", 10);
        assertEquals(2 * (corpus.size() + 1), calls.get(),
                "Without a cache directory every search recomputes the entry vectors");
    }

    @Test
    void corruptCacheFilesFallBackToRecomputation(@TempDir Path cacheDir) {
        List<KdbEntry> corpus = entries();
        AtomicInteger calls = new AtomicInteger();
        Function<String, float[]> counting = text -> {
            calls.incrementAndGet();
            return stubEmbed(text);
        };
        KdbVectorLane cold = new KdbVectorLane(corpus, counting, cacheDir);
        List<KdbVectorLane.ScoredEntry> expected = cold.search("zebra cat", 10);
        int afterWarm = calls.get();
        try (Stream<Path> files = Files.list(cacheDir)) {
            for (Path file : files.toList()) {
                Files.write(file, new byte[]{1, 2, 3});
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        KdbVectorLane fresh = new KdbVectorLane(corpus, counting, cacheDir);
        List<KdbVectorLane.ScoredEntry> actual = assertDoesNotThrow(() -> fresh.search("zebra cat", 10),
                "A corrupt cache must never fail the search");
        assertEquals(afterWarm + corpus.size() + 1, calls.get(),
                "The corrupt entries are recomputed through the embedder");
        assertEquals(expected, actual, "The recomputed vectors produce identical rankings");
    }
}
