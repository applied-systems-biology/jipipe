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
 * <li>AI strength: {@code score / maxPositive} (unchanged direction);</li>
 * <li>classic strength: {@code 1 - score / maxPositive} (direction
 * inverted). A negative sentinel score maps above 1, so the exact-name
 * match stays strictly above the name-prefix tier (strength 1.0) and every
 * coverage-scored entry (strength below 1); a side whose maximum positive
 * score is 0 or less contributes 0 for all its entries.</li>
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
     * AND a configured, ready embedding model. Exception-safe: any throw
     * means "not active" (the classic ranking alone keeps serving
     * results).
     *
     * @return true if the AI contribution can be applied
     */
    public static boolean isAiContributionActive() {
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

        // Per-side normalized strengths, keyed by ENTRY REFERENCE (see the
        // class documentation; putIfAbsent keeps the first occurrence, the
        // lists carry the same entry objects from the same database).
        Map<JIPipeNodeDatabaseEntry, Double> classicStrength = new IdentityHashMap<>();
        for (ScoredNodeDatabaseEntry scored : classic) {
            classicStrength.putIfAbsent(scored.entry(), normalizeClassicStrength(scored.score(), classicMax));
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
     * is positive: the side then contributes 0 under the normalization).
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
     * coverage scores) into a strength (higher is stronger) normalized by
     * the side's maximum positive score. A side without a positive score
     * contributes 0. Negative sentinel scores map above 1, which keeps the
     * exact-name match strictly above every other entry (the name-prefix
     * sentinel 0.0 normalizes to exactly 1).
     */
    private static double normalizeClassicStrength(double score, double maxPositive) {
        if (maxPositive <= 0) {
            return 0.0;
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
