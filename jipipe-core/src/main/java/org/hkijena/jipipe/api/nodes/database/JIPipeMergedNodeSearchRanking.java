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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.plugins.ai.AIApplicationSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeNodeSearchApplicationSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The merged ("blended") node-search ranking of #1409: the classic
 * (lexicon) search's per-entry scores and the AI search's per-entry cosine
 * similarities are each normalized onto a comparable strength scale and
 * combined into one ordering. Pure (no I/O, no JIPipe state beyond the
 * availability probe and the shared {@link NodeUsage} prior).
 * <p>
 * <b>Score semantics and normalization.</b> The two sides report different
 * kinds of scores ({@link ScoredNodeDatabaseEntry}): the enhanced search's
 * score is distance-like (lower is better; the exact-name sentinel -1e6,
 * the name-prefix sentinel 0.0, positive coverage scores), while the AI
 * search's score is a cosine similarity (higher is better). Each side is
 * normalized by its own maximum positive score, and the classic side is
 * additionally converted from distance to strength so that higher always
 * means "stronger match":
 * <ul>
 * <li>AI strength: {@code score / maxPositive} (unchanged direction; a
 * side without a positive score contributes 0);</li>
 * <li>classic strength (direction inverted, distance to strength). With
 * positive coverage scores in the list: {@code 1 - score / maxPositive} -
 * a negative sentinel maps above 1, so the exact-name match stays
 * strictly above the name-prefix tier (strength 1.0) and every
 * coverage-scored entry (strength below 1). A pure sentinel list (no
 * positive score) keeps the ladder on a unit denominator instead of
 * contributing 0: the exact-name sentinel maps above every other
 * contribution, the name-prefix tier to 1.0, both above every AI-only
 * contribution (which is at most 0.3). A list whose only entry carries a
 * positive score (no sentinels) gives that entry strength 1.0: the plain
 * formula would normalize it to 0 (it is its own maximum) and let
 * AI-only entries bury the classic search's only hit.</li>
 * </ul>
 * The conversion direction is what makes the weight property below hold
 * for real enhanced scores; the unit tests pin it on both synthetic and
 * production-shaped inputs.
 * <p>
 * <b>Weights.</b> Combined = {@link #FUSION_CLASSIC_WEIGHT} * classic
 * strength + {@link #FUSION_AI_WEIGHT} * AI strength: a strong classic
 * match cannot be outranked by the AI alone (the AI-only maximum
 * contribution is 0.3), and an AI-only semantic match beats a weak classic
 * match. An entry present in only one list gets 0 for the missing side.
 * <p>
 * <b>Ordering.</b> Pinned entries first, then the combined score
 * descending, then the shared {@link NodeUsage} prior descending (keyed by
 * the origin node id, falling back to the entry id - the same derivation
 * both searches use), then the name case-insensitively.
 * <p>
 * <b>Identity keying.</b> All per-entry maps are keyed by the
 * {@link JIPipeNodeDatabaseEntry} reference (an {@link IdentityHashMap},
 * so the mapping stays an identity map even if an entry implementation
 * ever overrode {@code equals}). Keying by the
 * {@link ScoredNodeDatabaseEntry} record would be wrong: the classic side
 * and the AI side wrap the same entry objects, and two records of the same
 * entry with equal score and pinned flag would collide.
 * <p>
 * <b>Degradation.</b> A null or empty AI list returns the classic order
 * unchanged (production: AI unavailable, failed, or timed out). An empty
 * classic list returns an empty result: the classic candidate set is the
 * universe of the fusion (this holds even when the AI side has
 * candidates). A blank query never reaches the fusion at all
 * ({@code JIPipeNodeDatabase.query} returns the classic result directly:
 * the empty-query arms expose neutral 0.0 scores, which carry no relevance
 * signal and would wash to an all-tie under the normalization).
 */
public final class JIPipeMergedNodeSearchRanking {

    /**
     * The weight of the normalized classic strength in the combined score
     * (see the class documentation for the property this pins: a strong
     * classic match cannot be outranked by the AI alone).
     */
    public static final double FUSION_CLASSIC_WEIGHT = 0.7;

    /**
     * The weight of the normalized AI strength in the combined score (an
     * AI-only semantic match beats a weak classic match, but never a
     * strong one).
     */
    public static final double FUSION_AI_WEIGHT = 0.3;

    private static final Logger LOGGER = LoggerFactory.getLogger(JIPipeMergedNodeSearchRanking.class);

    private JIPipeMergedNodeSearchRanking() {
    }

    /**
     * Whether the AI contribution may join the merged ranking: the node
     * search settings' AI-contribution switch AND the general AI switch
     * AND a configured embedding model AND the embedding model service
     * being actually Ready (started and serving). "Configured" alone is
     * not enough: the state after every app start is configured-but-
     * Stopped, and fusing there would block the caller for the AI
     * search's whole timeout before degrading (#1409 final review I-1).
     * Exception-safe: any throw means "not active" (the classic ranking
     * alone keeps serving results).
     *
     * @return true if the AI contribution can be applied
     */
    public static boolean isAiContributionActive() {
        if (!isAiContributionConfigured()) {
            return false;
        }
        try {
            // The same accessor the merged-ranking harness gates on:
            // the embedding model microservice's Ready state (started and
            // serving), not merely a resolvable configuration.
            return JIPipe.getInstance().getAiService().hasEmbeddingModel();
        } catch (Exception e) {
            LOGGER.debug("AI contribution readiness check failed", e);
            return false;
        }
    }

    /**
     * Whether the AI contribution is switched on and a model is
     * configured, WITHOUT the readiness requirement of
     * {@link #isAiContributionActive()}: the production probe for "the
     * model should be running for the merged ranking but is not". The
     * merged path uses it to fire the background model start (a
     * configured-but-Stopped model is startable; a Ready model makes this
     * true and {@link #isAiContributionActive()} true with it).
     * Exception-safe like {@link #isAiContributionActive()}.
     *
     * @return true if the AI contribution is enabled and a model is configured
     */
    public static boolean isAiContributionConfigured() {
        try {
            if (!JIPipe.isInstantiated()) {
                return false;
            }
            if (!JIPipeNodeSearchApplicationSettings.getInstance().isAiContribution()) {
                return false;
            }
            if (!AIApplicationSettings.getInstance().isEnableAI()) {
                return false;
            }
            return JIPipe.getInstance().getAiService().hasConfiguredAndReadyEmbeddingModel();
        } catch (Exception e) {
            LOGGER.debug("AI contribution availability check failed", e);
            return false;
        }
    }

    /**
     * Fuses the classic and AI scored result lists into one ordering (see
     * the class documentation). The inputs are never mutated.
     *
     * @param classic    the classic search's scored results (never mutated)
     * @param ai         the AI search's scored results, or null/empty when
     *                   no AI contribution is available (degradation arm)
     * @param pinnedIds  the pinned entry ids of the query (null counts as
     *                   empty)
     * @return the fused entry list; empty when the classic list is null or
     * empty
     */
    public static List<JIPipeNodeDatabaseEntry> fuse(List<ScoredNodeDatabaseEntry> classic,
                                                     List<ScoredNodeDatabaseEntry> ai,
                                                     Set<String> pinnedIds) {
        // No classic candidates: the classic candidate set is the universe
        // of the fusion (also covers a null classic list defensively).
        if (classic == null || classic.isEmpty()) {
            return Collections.emptyList();
        }
        // Degradation arm: no AI contribution keeps the classic order.
        if (ai == null || ai.isEmpty()) {
            return classic.stream().map(ScoredNodeDatabaseEntry::entry).collect(Collectors.toList());
        }
        Set<String> pins = pinnedIds != null ? pinnedIds : Collections.emptySet();

        double classicMax = maxPositiveScore(classic);
        double aiMax = maxPositiveScore(ai);
        // The classic normalization arm is decided by the list's
        // composition (see normalizeClassicStrength): how many entries
        // carry a positive score, and whether sentinel entries (score
        // <= 0) are present.
        int classicPositiveCount = 0;
        boolean classicHasSentinel = false;
        for (ScoredNodeDatabaseEntry scored : classic) {
            if (scored.score() > 0) {
                classicPositiveCount++;
            } else {
                classicHasSentinel = true;
            }
        }

        // Per-side normalized strengths, keyed by ENTRY REFERENCE (see the
        // class documentation; putIfAbsent keeps the first occurrence, the
        // lists carry the same entry objects from the same database).
        Map<JIPipeNodeDatabaseEntry, Double> classicStrength = new IdentityHashMap<>();
        for (ScoredNodeDatabaseEntry scored : classic) {
            classicStrength.putIfAbsent(scored.entry(),
                    normalizeClassicStrength(scored.score(), classicMax, classicPositiveCount, classicHasSentinel));
        }
        Map<JIPipeNodeDatabaseEntry, Double> aiStrength = new IdentityHashMap<>();
        for (ScoredNodeDatabaseEntry scored : ai) {
            aiStrength.putIfAbsent(scored.entry(), normalizeAiStrength(scored.score(), aiMax));
        }

        // Union in a stable order: the classic list first (its order is the
        // one the degradation arm preserves), then AI-only entries in AI
        // list order. The sort below is stable, so full ties keep this
        // order.
        List<FusedEntry> fused = new ArrayList<>(classic.size() + ai.size());
        Set<JIPipeNodeDatabaseEntry> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ScoredNodeDatabaseEntry scored : classic) {
            if (seen.add(scored.entry())) {
                double combined = FUSION_CLASSIC_WEIGHT * classicStrength.get(scored.entry())
                        + FUSION_AI_WEIGHT * aiStrength.getOrDefault(scored.entry(), 0.0);
                fused.add(new FusedEntry(scored.entry(), combined));
            }
        }
        for (ScoredNodeDatabaseEntry scored : ai) {
            if (seen.add(scored.entry())) {
                double combined = FUSION_CLASSIC_WEIGHT * classicStrength.getOrDefault(scored.entry(), 0.0)
                        + FUSION_AI_WEIGHT * aiStrength.get(scored.entry());
                fused.add(new FusedEntry(scored.entry(), combined));
            }
        }

        // Pinned first, then combined descending, then usage descending,
        // then name (case-insensitive): the comparator shape both searches
        // use today.
        fused.sort(Comparator
                .comparing((FusedEntry f) -> !pins.contains(f.entry().getId()))
                .thenComparingDouble(f -> -f.combined())
                .thenComparingLong(f -> -NodeUsage.usage(usageNodeId(f.entry())))
                .thenComparing(f -> f.entry().getName(), String.CASE_INSENSITIVE_ORDER));
        return fused.stream().map(FusedEntry::entry).collect(Collectors.toList());
    }

    /**
     * The maximum strictly positive score of the side (0.0 when no score
     * is positive: the AI side then contributes 0, and the classic side
     * switches to its unit-denominator sentinel ladder).
     */
    private static double maxPositiveScore(List<ScoredNodeDatabaseEntry> scored) {
        double max = 0.0;
        for (ScoredNodeDatabaseEntry entry : scored) {
            if (entry.score() > max) {
                max = entry.score();
            }
        }
        return max;
    }

    /**
     * Normalizes the AI search's cosine similarity (higher is better) by
     * the side's maximum positive score; a side without a positive score
     * contributes 0.
     */
    private static double normalizeAiStrength(double score, double maxPositive) {
        if (maxPositive <= 0) {
            return 0.0;
        }
        return score / maxPositive;
    }

    /**
     * Converts the enhanced search's distance-like score (lower is better;
     * exact-name sentinel -1e6, name-prefix sentinel 0.0, positive
     * coverage scores) into a strength (higher is stronger). The arm is
     * decided by the classic list's composition, not by the single score:
     * <ul>
     * <li><b>Pure sentinel list</b> ({@code maxPositive <= 0}: no
     * positive-scored entry): a unit denominator keeps the sentinel
     * ladder instead of zeroing the side. The exact-name sentinel (-1e6)
     * maps to {@code 1 + |score|} (far above 1, so the exact match stays
     * above every other entry), the name-prefix sentinel (0.0) maps to
     * exactly 1.0, and any other non-positive score maps monotonically in
     * between. The ladder tiers sit above every AI-only contribution (at
     * most 0.3 combined) by construction.</li>
     * <li><b>Lone positive entry</b> (exactly one positive-scored entry
     * and no sentinel entries): the entry is its own maximum, so the
     * plain formula would normalize it to 0 and bury the classic search's
     * only hit under AI-only entries; it keeps strength 1.0 (the classic
     * search judged it the only relevant hit). When sentinels ARE present
     * the precondition fails deliberately: they already rank above the
     * positive entry via the formula, so its true strength relative to
     * them is preserved by leaving the formula alone.</li>
     * <li><b>Otherwise</b> (a multi-entry positive list, or sentinels
     * coexisting with positive scores): {@code 1 - score / maxPositive}.
     * Negative sentinel scores map above 1 (the exact-name match strictly
     * above the name-prefix tier at 1.0, coverage entries below 1).</li>
     * </ul>
     */
    private static double normalizeClassicStrength(double score,
                                                   double maxPositive,
                                                   int positiveCount,
                                                   boolean sentinelPresent) {
        if (maxPositive <= 0) {
            // Arm A: pure sentinel list. Unit denominator: the ladder
            // (exact > prefix) survives above every AI-only contribution.
            return 1.0 - score;
        }
        if (positiveCount == 1 && !sentinelPresent) {
            // Arm B: the lone positive entry is its own maximum; keep it
            // at full strength instead of normalizing it to 0.
            return 1.0;
        }
        return 1.0 - score / maxPositive;
    }

    /**
     * The usage-signal id of an entry: the origin node type id when the
     * entry wraps a node type, else the entry id (the same derivation the
     * AI search's ranking uses; {@link NodeUsage} is keyed by node ids).
     */
    private static String usageNodeId(JIPipeNodeDatabaseEntry entry) {
        return entry.getOriginNodeId() != null ? entry.getOriginNodeId() : entry.getId();
    }

    /**
     * One fused candidate: the entry and its combined score.
     */
    private record FusedEntry(JIPipeNodeDatabaseEntry entry, double combined) {
    }
}
