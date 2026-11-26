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

import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.nodes.JIPipeNodeClassification;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the fusion contract of {@link JIPipeMergedNodeSearchRanking}
 * (the merged node-search ranking of #1409) on synthetic scored lists.
 * <p>
 * Score shapes mirror production: the classic side carries the enhanced
 * search's distance-like scores (lower is better; the exact-name sentinel
 * -1e6, the name-prefix sentinel 0.0, positive coverage scores), the AI
 * side carries cosine similarities (higher is better). The fusion
 * normalizes each side by its own maximum positive score and combines
 * 0.7 classic + 0.3 AI, so the asserted orders pin the 0.7/0.3 property:
 * a strong classic match cannot be outranked by the AI alone, while an
 * AI-only semantic match beats a weak classic match.
 * <p>
 * Pure: no {@link org.hkijena.jipipe.JIPipe} instance, no database, no
 * Swing. The usage tiebreak reads the injected prior supplier (restored
 * in {@code finally}).
 * <p>
 * The degenerate-shape arms (refinement round) pin the two measured
 * regression shapes of the harness: a lone classic entry (its own
 * maximum) and pure sentinel lists (no positive score) keep their classic
 * strength above every AI-only contribution instead of collapsing to 0.
 */
class JIPipeMergedNodeSearchRankingTest {

    // ------------------------------------------------------------------
    // Fixture
    // ------------------------------------------------------------------

    /**
     * A minimal entry: identity, name, and the usage-key inputs
     * ({@link JIPipeNodeDatabaseEntry#getOriginNodeId()} with the entry id
     * fallback). The fusion reads nothing else from the entry.
     */
    private static class TestEntry implements JIPipeNodeDatabaseEntry {

        private final String id;
        private final String name;
        private final String originNodeId;

        private TestEntry(String id, String name, String originNodeId) {
            this.id = id;
            this.name = name;
            this.originNodeId = originNodeId;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginNodeId() {
            return originNodeId;
        }

        @Override
        public WeightedTokens getTokens() {
            return new WeightedTokens();
        }

        @Override
        public boolean exists() {
            return false;
        }

        @Override
        public JIPipeNodeDatabasePipelineVisibility getVisibility() {
            return JIPipeNodeDatabasePipelineVisibility.Pipeline;
        }

        @Override
        public javax.swing.Icon getIcon() {
            return null;
        }

        @Override
        public org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText getDescription() {
            return null;
        }

        @Override
        public List<String> getLocationInfos() {
            return Collections.emptyList();
        }

        @Override
        public Map<String, JIPipeDataSlotInfo> getInputSlots() {
            return Collections.emptyMap();
        }

        @Override
        public Map<String, JIPipeDataSlotInfo> getOutputSlots() {
            return Collections.emptyMap();
        }

        @Override
        public Color getFillColor() {
            return null;
        }

        @Override
        public Color getBorderColor() {
            return null;
        }

        @Override
        public Set<JIPipeDesktopGraphNodeUI> addToGraph(JIPipeDesktopGraphCanvasUI canvasUI) {
            return Collections.emptySet();
        }

        @Override
        public boolean canAddInputSlots() {
            return false;
        }

        @Override
        public boolean canAddOutputSlots() {
            return false;
        }

        @Override
        public boolean isDeprecatedOrUnstable() {
            return false;
        }

        @Override
        public <T> T getAttachment(Class<T> attachmentClass) {
            return null;
        }

        @Override
        public <T> void attach(T attachment) {

        }

        @Override
        public JIPipeNodeClassification getNodeClassification() {
            return JIPipeNodeClassification.Standard;
        }
    }

    private static TestEntry entry(String id, String name) {
        return new TestEntry(id, name, null);
    }

    private static ScoredNodeDatabaseEntry scored(JIPipeNodeDatabaseEntry entry, double score) {
        return new ScoredNodeDatabaseEntry(entry, score, false);
    }

    // ------------------------------------------------------------------
    // Contracts (the 7 plan arms + the production-shape sentinel guard)
    // ------------------------------------------------------------------

    /**
     * Arm 1 (degradation): a null AI result leaves the classic order
     * unchanged (same entries, same order). The same holds for an empty
     * (non-null) AI list. Production hits this arm whenever the AI search
     * is unavailable, fails, or times out: the user keeps the classic
     * ranking.
     */
    @Test
    void classicOnlyPreservesClassicOrderExactly() {
        TestEntry a = entry("entry-a", "Alpha");
        TestEntry b = entry("entry-b", "Beta");
        TestEntry c = entry("entry-c", "Gamma");
        List<ScoredNodeDatabaseEntry> classic = List.of(scored(a, 0.3), scored(b, 0.2), scored(c, 0.1));

        assertThat(JIPipeMergedNodeSearchRanking.fuse(classic, null, Set.of()))
                .containsExactly(a, b, c);
        assertThat(JIPipeMergedNodeSearchRanking.fuse(classic, List.of(), Set.of()))
                .containsExactly(a, b, c);
    }

    /**
     * Arm 2: the entry that is strong on both sides stays first: the
     * classic-best entry keeps its lead when the AI agrees.
     */
    @Test
    void entryStrongOnBothSidesStaysFirst() {
        TestEntry x = entry("entry-x", "X ray");
        TestEntry other = entry("entry-other", "Other");
        // Classic scores are distance-like (lower is better): 0.1 is a
        // strong classic match, 0.9 a weak one.
        List<ScoredNodeDatabaseEntry> classic = List.of(scored(x, 0.1), scored(other, 0.9));
        List<ScoredNodeDatabaseEntry> ai = List.of(scored(x, 0.95), scored(other, 0.2));

        List<JIPipeNodeDatabaseEntry> fused = JIPipeMergedNodeSearchRanking.fuse(classic, ai, Set.of());
        assertThat(fused).containsExactly(x, other);
    }

    /**
     * Arm 3 (the 0.7/0.3 property): an AI-only semantic match (no classic
     * contribution) outranks a weak classic match but not a strong one.
     */
    @Test
    void aiOnlyMatchBeatsWeakClassicButNotStrongClassic() {
        TestEntry strong = entry("entry-strong", "Strong match");
        TestEntry weak = entry("entry-weak", "Weak match");
        TestEntry aiOnly = entry("entry-ai-only", "Semantic match");
        // Classic distances: strong 0.1, weak 0.9 (the side maximum).
        List<ScoredNodeDatabaseEntry> classic = List.of(scored(strong, 0.1), scored(weak, 0.9));
        List<ScoredNodeDatabaseEntry> ai = List.of(scored(aiOnly, 1.0));

        List<JIPipeNodeDatabaseEntry> fused = JIPipeMergedNodeSearchRanking.fuse(classic, ai, Set.of());
        assertThat(fused).containsExactly(strong, aiOnly, weak);
    }

    /**
     * Arm 4: an entry present in both lists with equal normalized scores
     * beats an entry that only one list contains: the two contributions
     * add up on the combined scale.
     */
    @Test
    void entryInBothListsBeatsEntryInOnlyOne() {
        TestEntry both = entry("entry-both", "Both");
        TestEntry classicOnly = entry("entry-classic-only", "Classic only");
        TestEntry aiOnly = entry("entry-ai-only", "AI only");
        TestEntry filler = entry("entry-filler", "Filler");
        // Classic distances: both and classicOnly share a mid score; the
        // filler anchors the side maximum (0.6). AI similarities: both and
        // aiOnly share the maximum (0.5).
        List<ScoredNodeDatabaseEntry> classic = List.of(scored(both, 0.3), scored(classicOnly, 0.3), scored(filler, 0.6));
        List<ScoredNodeDatabaseEntry> ai = List.of(scored(both, 0.5), scored(aiOnly, 0.5));

        List<JIPipeNodeDatabaseEntry> fused = JIPipeMergedNodeSearchRanking.fuse(classic, ai, Set.of());
        assertThat(fused).containsExactly(both, classicOnly, aiOnly, filler);
    }

    /**
     * Arm 5: entries with equal combined scores order by the shared usage
     * prior (descending) and then by name (case-insensitive). The prior
     * supplier is injected and restored; the usage keys exercise both
     * derivations (origin node id for a, fallback entry id for b and c).
     */
    @Test
    void usageTiebreakThenNameForEqualCombined() {
        TestEntry a = new TestEntry("entry-a", "Alpha", "test-node-a");
        TestEntry b = entry("entry-b", "Zeta");
        TestEntry c = entry("entry-c", "Mu");
        TestEntry d = entry("entry-d", "Delta");
        TestEntry filler = entry("entry-filler", "Filler");
        // Classic distances: a, b, c share a mid score; the filler anchors
        // the side maximum (0.8), so a, b, c tie on the combined score.
        List<ScoredNodeDatabaseEntry> classic = List.of(scored(a, 0.2), scored(c, 0.2), scored(b, 0.2), scored(filler, 0.8));
        List<ScoredNodeDatabaseEntry> ai = List.of(scored(d, 1.0));

        NodeUsage.setPriorSupplier(() -> Map.of("test-node-a", 5, "entry-b", 3, "entry-c", 3));
        try {
            List<JIPipeNodeDatabaseEntry> fused = JIPipeMergedNodeSearchRanking.fuse(classic, ai, Set.of());
            // Usage 5 puts a first; the b/c tie (usage 3 each) falls to the
            // name (Mu < Zeta, case-insensitive); the AI-only d (0.3) and
            // the filler (0.0) follow.
            assertThat(fused).containsExactly(a, c, b, d, filler);
        } finally {
            NodeUsage.setPriorSupplier(null);
        }
    }

    /**
     * Arm 6: pinned entries come first, regardless of their scores.
     */
    @Test
    void pinnedEntriesComeFirstRegardlessOfScores() {
        TestEntry x = entry("entry-x", "X");
        TestEntry p = entry("entry-p", "Pinned");
        // x is the stronger entry on both sides (distance 0.2 vs 0.6,
        // similarity 0.9 vs 0.9 with x at the AI maximum).
        List<ScoredNodeDatabaseEntry> classic = List.of(scored(x, 0.2), scored(p, 0.6));
        List<ScoredNodeDatabaseEntry> ai = List.of(scored(x, 0.9), scored(p, 0.9));

        List<JIPipeNodeDatabaseEntry> fused = JIPipeMergedNodeSearchRanking.fuse(classic, ai, Set.of("entry-p"));
        assertThat(fused).containsExactly(p, x);
    }

    /**
     * Arm 7: an empty classic list yields an empty result (the classic
     * candidate set is the universe of the fusion, even when the AI side
     * has candidates), and so does an empty input on both sides.
     */
    @Test
    void emptyClassicYieldsEmptyResult() {
        TestEntry d = entry("entry-d", "Delta");
        assertThat(JIPipeMergedNodeSearchRanking.fuse(List.of(), List.of(scored(d, 1.0)), Set.of())).isEmpty();
        assertThat(JIPipeMergedNodeSearchRanking.fuse(List.of(), List.of(), Set.of())).isEmpty();
        assertThat(JIPipeMergedNodeSearchRanking.fuse(List.of(), null, Set.of())).isEmpty();
    }

    /**
     * The production score shapes: the enhanced search's exact-name
     * sentinel (-1e6, distance-like) must survive the fusion - an exact
     * name match stays first even when another entry carries a perfect AI
     * similarity. (The AI list ordering here is the AI search's own:
     * similarity descending puts the non-exact entry first.)
     */
    @Test
    void exactNameSentinelSurvivesFusion() {
        TestEntry exact = entry("entry-exact", "Gaussian blur");
        TestEntry weak = entry("entry-weak", "Weak coverage match");
        List<ScoredNodeDatabaseEntry> classic = List.of(scored(exact, -1e6), scored(weak, 0.6));
        List<ScoredNodeDatabaseEntry> ai = List.of(scored(weak, 1.0), scored(exact, 0.1));

        List<JIPipeNodeDatabaseEntry> fused = JIPipeMergedNodeSearchRanking.fuse(classic, ai, Set.of());
        assertThat(fused).containsExactly(exact, weak);
    }

    // ------------------------------------------------------------------
    // Degenerate-shape arms (refinement round: the two measured fusion
    // regression shapes of the harness)
    // ------------------------------------------------------------------

    /**
     * Arm B: a classic list whose only entry carries a positive score (no
     * sentinel entries). The entry is its own maximum, so the plain
     * formula would normalize it to strength 0 and let every AI-only
     * entry outrank the classic search's only hit; it keeps strength 1.0
     * instead, so its combined score (at least 0.7) beats every AI-only
     * contribution (at most 0.3), regardless of how poor its cosine is.
     */
    @Test
    void lonePositiveClassicEntryBeatsEveryAiOnlyEntry() {
        TestEntry lone = entry("entry-lone", "Lone hit");
        TestEntry aiOnly1 = entry("entry-ai-only-1", "Semantic match 1");
        TestEntry aiOnly2 = entry("entry-ai-only-2", "Semantic match 2");
        List<ScoredNodeDatabaseEntry> classic = List.of(scored(lone, 0.5));
        List<ScoredNodeDatabaseEntry> ai = List.of(scored(aiOnly1, 1.0), scored(aiOnly2, 0.5), scored(lone, 0.1));

        List<JIPipeNodeDatabaseEntry> fused = JIPipeMergedNodeSearchRanking.fuse(classic, ai, Set.of());
        assertThat(fused).containsExactly(lone, aiOnly1, aiOnly2);
    }

    /**
     * Arm A (exact tier): a classic list of only the exact-name sentinel
     * (-1e6) has no positive score, so the side must not zero out: the
     * unit denominator maps the sentinel above 1, and the exact match
     * beats every AI-only entry (whose combined contribution is at most
     * 0.3). This is the flaw class behind the measured typo-dseckle-05 /
     * typo-ipaining-08 regressions (the Task 5 review's Minor 1).
     */
    @Test
    void pureExactSentinelListBeatsEveryAiOnlyEntry() {
        TestEntry exact = entry("entry-exact", "Gaussian blur");
        TestEntry aiOnly = entry("entry-ai-only", "Semantic match");
        List<ScoredNodeDatabaseEntry> classic = List.of(scored(exact, -1e6));
        List<ScoredNodeDatabaseEntry> ai = List.of(scored(aiOnly, 1.0), scored(exact, 0.1));

        List<JIPipeNodeDatabaseEntry> fused = JIPipeMergedNodeSearchRanking.fuse(classic, ai, Set.of());
        assertThat(fused).containsExactly(exact, aiOnly);
    }

    /**
     * Arm A (prefix tier): a classic list of only name-prefix sentinels
     * (0.0) has no positive score; the unit denominator maps each prefix
     * entry to exactly 1.0 (their order inside the tier follows the AI
     * similarity), above every AI-only entry.
     */
    @Test
    void purePrefixSentinelListBeatsEveryAiOnlyEntry() {
        TestEntry prefix1 = entry("entry-prefix-1", "Alpha opening");
        TestEntry prefix2 = entry("entry-prefix-2", "Alpha open workbench");
        TestEntry aiOnly = entry("entry-ai-only", "Semantic match");
        List<ScoredNodeDatabaseEntry> classic = List.of(scored(prefix1, 0.0), scored(prefix2, 0.0));
        List<ScoredNodeDatabaseEntry> ai = List.of(scored(prefix1, 0.8), scored(prefix2, 0.6), scored(aiOnly, 1.0));

        List<JIPipeNodeDatabaseEntry> fused = JIPipeMergedNodeSearchRanking.fuse(classic, ai, Set.of());
        assertThat(fused).containsExactly(prefix1, prefix2, aiOnly);
    }

    /**
     * The mixed shape (sentinels AND a positive score) keeps the plain
     * formula: exact above the prefix tier, the prefix tier above the
     * coverage entry (whose strength is 0: its own side maximum), and the
     * coverage entry above a zero-cosine AI-only entry via its own
     * cosine. Guard for the refinement: arms A and B must not apply here.
     */
    @Test
    void mixedSentinelsAndSinglePositiveKeepFormulaLadder() {
        TestEntry exact = entry("entry-exact", "Gaussian blur");
        TestEntry prefix = entry("entry-prefix", "Gaussian blur 3d");
        TestEntry positive = entry("entry-positive", "Coverage match");
        TestEntry aiOnly = entry("entry-ai-only", "Zero cosine match");
        List<ScoredNodeDatabaseEntry> classic = List.of(scored(exact, -1e6), scored(prefix, 0.0), scored(positive, 0.5));
        List<ScoredNodeDatabaseEntry> ai = List.of(scored(exact, 0.9), scored(prefix, 0.7), scored(positive, 0.4), scored(aiOnly, 0.0));

        List<JIPipeNodeDatabaseEntry> fused = JIPipeMergedNodeSearchRanking.fuse(classic, ai, Set.of());
        assertThat(fused).containsExactly(exact, prefix, positive, aiOnly);
    }

    /**
     * Regression reproduction (typo-dseckle-05, measured live at merged
     * rank 35 from classic rank 0): a lone classic entry with the
     * exact-name sentinel and a poor cosine stays at fused rank 0 - the
     * classic search's only hit may not be buried under AI-only entries
     * with better cosines.
     */
    @Test
    void regressionLoneExactSentinelStaysAtFusedRank0() {
        TestEntry lone = entry("entry-lone", "Dseckle");
        TestEntry aiOnly1 = entry("entry-ai-only-1", "Semantic match 1");
        TestEntry aiOnly2 = entry("entry-ai-only-2", "Semantic match 2");
        List<ScoredNodeDatabaseEntry> classic = List.of(scored(lone, -1e6));
        List<ScoredNodeDatabaseEntry> ai = List.of(scored(aiOnly1, 1.0), scored(aiOnly2, 0.5), scored(lone, 0.05));

        List<JIPipeNodeDatabaseEntry> fused = JIPipeMergedNodeSearchRanking.fuse(classic, ai, Set.of());
        assertThat(fused).containsExactly(lone, aiOnly1, aiOnly2);
    }

    /**
     * Regression reproduction (typo-ipaining-08, measured live at merged
     * rank 40 from classic rank 0): a lone classic entry with the
     * name-prefix sentinel and a poor cosine stays at fused rank 0.
     */
    @Test
    void regressionLonePrefixSentinelStaysAtFusedRank0() {
        TestEntry lone = entry("entry-lone", "Ipaining");
        TestEntry aiOnly1 = entry("entry-ai-only-1", "Semantic match 1");
        TestEntry aiOnly2 = entry("entry-ai-only-2", "Semantic match 2");
        List<ScoredNodeDatabaseEntry> classic = List.of(scored(lone, 0.0));
        List<ScoredNodeDatabaseEntry> ai = List.of(scored(aiOnly1, 1.0), scored(aiOnly2, 0.5), scored(lone, 0.05));

        List<JIPipeNodeDatabaseEntry> fused = JIPipeMergedNodeSearchRanking.fuse(classic, ai, Set.of());
        assertThat(fused).containsExactly(lone, aiOnly1, aiOnly2);
    }
}
