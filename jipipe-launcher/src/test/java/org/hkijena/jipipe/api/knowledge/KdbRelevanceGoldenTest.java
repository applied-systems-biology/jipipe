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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The golden relevance gate (design spec 5.8): 15 pinned queries over the
 * packaged production corpus that pin the retrieval behavior the oikb
 * failure modes need. Every query pins the exact set of returned entry keys
 * and the set of lanes that contributed, so a corpus edit, a lane change, or
 * a fusion change that moves a relevant entry fails this test.
 * <p>
 * The queries cover: exact node-id lookups, alias vocabulary, plain concept
 * phrases, error-string-shaped queries (the lexical lane keeps
 * {@code error:<node-id>} as one token), cross-domain queries whose
 * vocabulary spans two topics, related-item queries whose expected set
 * contains an ENTRY_REF neighbor that no query token reaches (only the
 * relation lane can return it), and one query that must return nothing.
 * Lanes are pinned for the lexical and relation lanes only: the vector lane
 * is optional, not configured in this test, and therefore not part of the
 * golden contract.
 * <p>
 * The corpus is loaded exactly like {@link KdbCorpusIntegrityTest} (the
 * packaged jipipe-core resource root via {@link KdbProductionCorpus}), and
 * the service is constructed over that corpus without a vector lane, so the
 * results are the two-lane contract.
 */
class KdbRelevanceGoldenTest {

    /**
     * The search limit for every query. Wide enough that no pinned result
     * set is truncated by the limit (the largest pinned set is 10 of the 12
     * pilot entries), so the pins are the complete result sets.
     */
    private static final int LIMIT = 10;

    @BeforeAll
    static void init() {
        // Same bootstrap as the other launcher gates: the launcher session
        // listener preinits the ImageJ1 legacy patcher, ensureInstance() is
        // idempotent.
        JIPipe.ensureInstance();
    }

    private static String patternKey(String name) {
        return "bioimage-analysis/patterns/" + name;
    }

    private static String treeKey(String name) {
        return "bioimage-analysis/decision-trees/" + name;
    }

    /**
     * One pinned query. {@code expectedEntryKeys} must equal the returned
     * entry key set exactly; {@code expectedLanes} must equal the union of
     * lanes over all returned results exactly.
     */
    private record GoldenQuery(String name,
                               String query,
                               Set<String> expectedEntryKeys,
                               Set<String> expectedLanes,
                               String rationale) {
    }

    private static final Set<String> LEXICAL = Set.of(KnowledgeService.LANE_LEXICAL);
    private static final Set<String> LEXICAL_AND_RELATION =
            Set.of(KnowledgeService.LANE_LEXICAL, KnowledgeService.LANE_RELATION);

    /**
     * The 15 pinned queries (design spec 5.8: 15-30, deliberately including
     * the oikb failure modes). The rationales document what each pin guards.
     */
    private static final List<GoldenQuery> GOLDEN_QUERIES = List.of(
            new GoldenQuery("node-id-iterative-threshold",
                    "threshold-iterative-by-roi-statistics-2d",
                    Set.of(patternKey("novel-algorithm-patterns"), patternKey("measurement")),
                    LEXICAL_AND_RELATION,
                    "Exact node-id lookup: only novel-algorithm-patterns carries the id in its "
                            + "nodeChain; measurement is its declared crossRef and arrives through the relation lane"),
            new GoldenQuery("node-id-trackmate-measure",
                    "trackmate-measure-tracks",
                    Set.of(patternKey("trackmate-tracking"), patternKey("cellpose-segmentation")),
                    LEXICAL_AND_RELATION,
                    "Exact node-id lookup: only trackmate-tracking carries the id; "
                            + "cellpose-segmentation is its declared crossRef"),
            new GoldenQuery("alias-trackmate",
                    "trackmate",
                    Set.of(patternKey("trackmate-tracking"), patternKey("cellpose-segmentation")),
                    LEXICAL_AND_RELATION,
                    "Alias vocabulary: the alias token trackmate matches only the tracking pattern "
                            + "lexically; the segmentation crossRef follows through the relation lane"),
            new GoldenQuery("alias-z-stack",
                    "z-stack",
                    Set.of(patternKey("z-stack-and-3d"), patternKey("segmentation")),
                    LEXICAL_AND_RELATION,
                    "Alias vocabulary: the alias token z-stack matches only the 3D pattern "
                            + "lexically; the segmentation crossRef follows through the relation lane"),
            new GoldenQuery("concept-otsu",
                    "otsu bimodal histogram",
                    Set.of(patternKey("segmentation"), treeKey("count-cells-2d-fluorescence"),
                            treeKey("choosing-segmentation"), patternKey("z-stack-and-3d"),
                            patternKey("cellpose-segmentation"), patternKey("measurement")),
                    LEXICAL_AND_RELATION,
                    "Concept phrase: the Otsu assumption vocabulary hits the four entries that "
                            + "discuss auto thresholding; cellpose-segmentation and measurement follow "
                            + "as segmentation's declared neighbors"),
            new GoldenQuery("concept-volumetric",
                    "sphericity volume voxels isotropic",
                    Set.of(patternKey("z-stack-and-3d"), patternKey("segmentation")),
                    LEXICAL_AND_RELATION,
                    "Concept phrase: 3D measurement vocabulary matches only the z-stack pattern; "
                            + "segmentation follows as its declared crossRef"),
            new GoldenQuery("concept-screening",
                    "multi-well plate screening z-prime",
                    Set.of(patternKey("high-content-screening"), patternKey("measurement")),
                    LEXICAL_AND_RELATION,
                    "Concept phrase: screening vocabulary matches only the high-content pattern; "
                            + "measurement is its declared crossRef"),
            new GoldenQuery("concept-annotation-management",
                    "rename annotations folder",
                    Set.of(patternKey("import-split-annotate"), patternKey("common-workflow-outline"),
                            patternKey("high-content-screening"), patternKey("measurement")),
                    LEXICAL_AND_RELATION,
                    "Concept phrase: annotation management vocabulary hits import-split-annotate, "
                            + "common-workflow-outline, and high-content-screening; measurement follows "
                            + "as high-content-screening's declared crossRef"),
            new GoldenQuery("error-rename-annotation",
                    "error:rename-annotation",
                    Set.of(patternKey("import-split-annotate"), patternKey("common-workflow-outline")),
                    LEXICAL_AND_RELATION,
                    "Error-string-shaped query: the token error:<node-id> survives as one lexical "
                            + "unit and matches only the entry that documents the failure; the "
                            + "declared crossRef follows through the relation lane"),
            new GoldenQuery("error-trackmate",
                    "error:trackmate-tracking",
                    Set.of(patternKey("trackmate-tracking"), patternKey("cellpose-segmentation")),
                    LEXICAL_AND_RELATION,
                    "Error-string-shaped query: the token error:<node-id> matches only the tracking "
                            + "entry that documents the failure; the crossRef follows"),
            new GoldenQuery("cross-domain-segment-then-track",
                    "cellpose tracking time-lapse pre-segmented",
                    Set.of(patternKey("trackmate-tracking"), patternKey("cellpose-segmentation"),
                            patternKey("cellpose-training"), treeKey("choosing-segmentation"),
                            treeKey("count-cells-2d-fluorescence"), patternKey("z-stack-and-3d"),
                            patternKey("import-split-annotate"), patternKey("segmentation"),
                            patternKey("common-workflow-outline"), patternKey("measurement")),
                    LEXICAL_AND_RELATION,
                    "Cross-domain query: vocabulary from the segmentation and tracking sides of the "
                            + "corpus; the fused result is the tracking pattern plus the connected "
                            + "segmentation-side entries, reached through both lanes"),
            new GoldenQuery("cross-domain-measure-and-export",
                    "export measured intensities results",
                    Set.of(patternKey("common-workflow-outline"), patternKey("import-split-annotate"),
                            patternKey("measurement"), patternKey("high-content-screening"),
                            patternKey("novel-algorithm-patterns"), patternKey("segmentation")),
                    LEXICAL_AND_RELATION,
                    "Cross-domain query: measurement vocabulary plus export vocabulary; the fused "
                            + "result spans the measurement pattern and the export outline with their "
                            + "declared neighbors"),
            new GoldenQuery("relation-colocalization",
                    "colocalization pearson manders coefficients",
                    Set.of(patternKey("measurement"), patternKey("high-content-screening"),
                            patternKey("novel-algorithm-patterns"), patternKey("segmentation")),
                    LEXICAL_AND_RELATION,
                    "Related-item query: only measurement carries the colocalization vocabulary; "
                            + "high-content-screening and novel-algorithm-patterns share no query token "
                            + "and are reachable only through the relation lane"),
            new GoldenQuery("relation-training",
                    "epochs ground truth fine-tuning",
                    Set.of(patternKey("cellpose-training"), patternKey("cellpose-segmentation")),
                    LEXICAL_AND_RELATION,
                    "Related-item query: only the training pattern carries the training-set "
                            + "vocabulary; cellpose-segmentation shares no query token and is reachable "
                            + "only through the relation lane"),
            new GoldenQuery("expected-empty",
                    "mendelian inheritance",
                    Set.of(),
                    Set.of(),
                    "Expected empty: no corpus entry carries these tokens, so nothing may be "
                            + "returned above the fusion"));

    @Test
    void goldenQueriesReturnTheirPinnedResults() {
        Path corpusRoot = KdbProductionCorpus.root();
        KdbLoadResult corpus = new KdbEntryLoader().loadAll(corpusRoot);
        KnowledgeService service = new KnowledgeService(corpus.getEntries());
        for (GoldenQuery goldenQuery : GOLDEN_QUERIES) {
            List<KnowledgeResult> results = service.searchKnowledge(goldenQuery.query(), null, null, LIMIT);
            Set<String> actualEntryKeys = new LinkedHashSet<>();
            Set<String> actualLanes = new LinkedHashSet<>();
            for (KnowledgeResult result : results) {
                actualEntryKeys.add(result.entryKey());
                actualLanes.addAll(result.lanes());
            }
            List<String> actualDescription = new ArrayList<>();
            for (KnowledgeResult result : results) {
                actualDescription.add(result.entryKey() + " " + result.lanes()
                        + " (score " + result.score() + ")");
            }
            assertEquals(goldenQuery.expectedEntryKeys(), actualEntryKeys,
                    "Query '" + goldenQuery.query() + "' (" + goldenQuery.name() + ") returned the wrong "
                            + "entries.\nRationale: " + goldenQuery.rationale()
                            + "\nActual results:\n  " + String.join("\n  ", actualDescription));
            assertEquals(goldenQuery.expectedLanes(), actualLanes,
                    "Query '" + goldenQuery.query() + "' (" + goldenQuery.name() + ") attributed the wrong "
                            + "lanes.\nRationale: " + goldenQuery.rationale()
                            + "\nActual lanes: " + actualLanes);
        }
    }
}
