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

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link KnowledgeService} search skeleton: the lexical and
 * relation lanes fused with reciprocal rank fusion (k = 60), the version
 * drop against the pinned corpus version, and the exact-match filters
 * (design spec 5.5).
 */
class KnowledgeServiceTest {

    private static Path fixture(String name) {
        try {
            return Paths.get(KnowledgeServiceTest.class.getResource("/kdb-fixtures/lanes/" + name).toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static KnowledgeService service() {
        return new KnowledgeService(new KdbEntryLoader().loadAll(fixture("service")).getEntries());
    }

    @Test
    void versionDroppedEntriesLeaveSearchButStayResolvable() {
        // legacy-cellpose carries untilVersion 5.9.9, strictly below the
        // pinned corpus version 6.0.0: hard-dropped from search. current
        // carries untilVersion 6.0.0 itself: an untilVersion is inclusive,
        // so the entry stays searchable.
        KnowledgeService service = service();
        List<KnowledgeResult> results = service.searchKnowledge("cellpose", null, null, 10);
        assertEquals(List.of("bioimage-analysis/patterns/current-cellpose"),
                results.stream().map(KnowledgeResult::entryKey).toList(),
                "Only the entry that is valid at the corpus version survives");
        assertTrue(service.getKnowledgeEntryRaw("bioimage-analysis/patterns/legacy-cellpose").isPresent(),
                "A version-dropped entry stays resolvable by key for provenance");
    }

    @Test
    void knowledgeEntryPayloadCarriesTheStructuredEntryAndTheRenderedText() {
        KnowledgeService service = service();
        assertTrue(service.getKnowledgeEntry("bioimage-analysis/patterns/missing").isEmpty(),
                "An unknown key yields no payload");
        KnowledgeEntryPayload payload = service.getKnowledgeEntry("bioimage-analysis/patterns/current-cellpose")
                .orElseThrow();
        assertEquals("bioimage-analysis/patterns/current-cellpose", payload.entry().getEntryKey(),
                "The structured part is the corpus entry itself");
        assertTrue(payload.rendered().contains(payload.entry().getTitle()),
                "The rendered part is the agent facing text");
    }

    @Test
    void versionComparisonIsNumericNotLexicographic() {
        // untilVersion 10.0.0 is above the pinned 6.0.0 numerically. A string
        // comparison would read "10.0.0" < "6.0.0" and wrongly drop the
        // entry.
        List<KnowledgeResult> results = service().searchKnowledge("future", null, null, 10);
        assertEquals(List.of("bioimage-analysis/patterns/future-plan"),
                results.stream().map(KnowledgeResult::entryKey).toList(),
                "Versions must compare per numeric segment");
    }

    @Test
    void fusionAttributionMarksBothLanesOnRelationExpandedHits() {
        // hub and spoke both answer "calibration" lexically and are joined by
        // an ENTRY_REF, so both gather relation evidence on top of their
        // lexical hit. loner answers lexically but has no ENTRY_REF edges at
        // all, so it stays lexical-only. The attribution is independent of
        // the BM25 rank order of the three entries.
        List<KnowledgeResult> results = service().searchKnowledge("calibration", null, null, 10);
        assertEquals(3, results.size(), "All three calibration entries must surface");
        Map<String, Set<String>> lanesByEntry = new HashMap<>();
        for (KnowledgeResult result : results) {
            lanesByEntry.put(result.entryKey(), result.lanes());
        }
        assertEquals(Set.of(KnowledgeService.LANE_LEXICAL, KnowledgeService.LANE_RELATION),
                lanesByEntry.get("bioimage-analysis/patterns/hub"));
        assertEquals(Set.of(KnowledgeService.LANE_LEXICAL, KnowledgeService.LANE_RELATION),
                lanesByEntry.get("bioimage-analysis/patterns/spoke"));
        assertEquals(Set.of(KnowledgeService.LANE_LEXICAL),
                lanesByEntry.get("bioimage-analysis/patterns/loner"));
    }

    @Test
    void domainAndKindFiltersAreExactMatches() {
        KnowledgeService service = service();
        assertTrue(service.searchKnowledge("sorting", "bioimage-analysis", null, 10).isEmpty(),
                "The only sorting entry lives in tables; the wrong domain filter matches nothing");
        assertEquals(List.of("tables/mistakes/string-sorting"),
                service.searchKnowledge("sorting", "tables", null, 10).stream()
                        .map(KnowledgeResult::entryKey).toList());
        assertEquals(List.of("tables/mistakes/string-sorting"),
                service.searchKnowledge("sorting", null, "mistake", 10).stream()
                        .map(KnowledgeResult::entryKey).toList());
        assertTrue(service.searchKnowledge("sorting", null, "pattern", 10).isEmpty(),
                "A kind filter that no hit satisfies matches nothing");
    }

    @Test
    void blankQueriesAndNonPositiveLimitsYieldNothing() {
        KnowledgeService service = service();
        assertTrue(service.searchKnowledge(null, null, null, 10).isEmpty());
        assertTrue(service.searchKnowledge("   ", null, null, 10).isEmpty());
        assertTrue(service.searchKnowledge("calibration", null, null, 0).isEmpty());
    }

    @Test
    void relatedEntriesAreLabeledOneHopNeighbors() {
        KnowledgeService service = service();
        assertEquals(
                List.of(new RelatedEntry("bioimage-analysis/patterns/spoke", "elaborates",
                        RelatedEntry.Direction.OUTGOING)),
                service.relatedEntries("bioimage-analysis/patterns/hub"));
        assertTrue(service.relatedEntries("bioimage-analysis/patterns/missing").isEmpty());
    }

    @Test
    void domainsListTheCorpusWithHumanReadableLabels() {
        // The version-dropped legacy entry still belongs to the corpus and is
        // counted: only its search visibility is dropped.
        assertEquals(
                List.of(new DomainInfo("bioimage-analysis", "Bioimage Analysis", 6),
                        new DomainInfo("tables", "Tables", 1)),
                service().listKnowledgeDomains());
    }
}
