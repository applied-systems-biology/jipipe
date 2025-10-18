package org.hkijena.jipipe.api.nodes.database;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ComfyUI-inspired search for JIPipe nodes:
 * - Multi-dimensional scoring (exact / prefix / word / substring / multi-word)
 * - Light length penalty
 * - Usage-frequency aware secondary ranking
 * - Field weighting (name > tokens > description/locations)
 * - Pinned-first ordering
 * - Optional data-type compatibility prioritization
 *
 * confirm* methods intentionally left as stubs per request.
 */
public class JIPipeEnhancedNodeDatabaseSearch implements JIPipeNodeDatabaseSearch {

    private final JIPipeNodeDatabase nodeDatabase;

    // Field weights (roughly Fuse-like bias: name dominates)
    private static final double W_NAME   = 0.65;
    private static final double W_TOKENS = 0.25;
    private static final double W_EXTRA  = 0.10; // description, locations, categories as a weak hint

    // When a target type is provided, soft-cap favored distance
    private static final int MAX_TYPE_DISTANCE_TO_INCLUDE = 64;

    private static final LevenshteinDistance LD = LevenshteinDistance.getDefaultInstance();

    public JIPipeEnhancedNodeDatabaseSearch(JIPipeNodeDatabase nodeDatabase) {
        this.nodeDatabase = nodeDatabase;
    }

    /* ------------------------------- Public API (stubs for confirm*) ------------------------------- */

    @Override
    public void confirmQuery(String text, JIPipeNodeDatabasePipelineVisibility role, boolean allowExisting, boolean allowNew, Set<String> pinnedIds, JIPipeNodeDatabaseEntry userSelected) {
    }

    @Override
    public List<JIPipeNodeDatabaseEntry> query(String text,
                                               JIPipeNodeDatabasePipelineVisibility role,
                                               boolean allowExisting,
                                               boolean allowNew,
                                               Set<String> pinnedIds) {
        return internalQuery(text, role, allowExisting, allowNew, pinnedIds, null, null);
    }

    @Override
    public void confirmQuery(String text, JIPipeNodeDatabasePipelineVisibility role, boolean allowExisting, boolean allowNew, JIPipeSlotType targetSlotType, Class<? extends JIPipeData> targetDataType, JIPipeNodeDatabaseEntry userSelected) {
    }

    @Override
    public List<JIPipeNodeDatabaseEntry> query(String text,
                                               JIPipeNodeDatabasePipelineVisibility role,
                                               boolean allowExisting,
                                               boolean allowNew,
                                               JIPipeSlotType targetSlotType,
                                               Class<? extends JIPipeData> targetDataType) {
        // Note: pinned IDs not wired here yet in the legacy, keep behavior (empty set).
        return internalQuery(text, role, allowExisting, allowNew, Collections.emptySet(), targetSlotType, targetDataType);
    }

    /* ------------------------------- Core Query ------------------------------- */

    private List<JIPipeNodeDatabaseEntry> internalQuery(String rawText,
                                                        JIPipeNodeDatabasePipelineVisibility role,
                                                        boolean allowExisting,
                                                        boolean allowNew,
                                                        Set<String> pinnedIds,
                                                        JIPipeSlotType targetSlotType,
                                                        Class<? extends JIPipeData> targetDataType) {

        final String text = normalize(rawText);
        final boolean hasQuery = !text.isEmpty();
        final QueryParts queryParts = splitQuery(text);

        // Pre-compile wildcard (Comfy's extended search feel: '*' allowed)
        final Pattern wildcardPattern = buildWildcardPattern(text);

        // Pre-filter by visibility/existence
        List<JIPipeNodeDatabaseEntry> candidates = nodeDatabase.getEntries().stream()
                .filter(e -> e.getVisibility().matches(role)) // interface-provided visibility filter :contentReference[oaicite:3]{index=3}
                .filter(e -> allowExisting || !e.exists())
                .filter(e -> allowNew || e.exists())
                .collect(Collectors.toList());

        // If type target is provided, compute conversion distance once per entry
        Map<JIPipeNodeDatabaseEntry, Integer> typeDistance = new HashMap<>();
        if (targetSlotType != null && targetDataType != null) {
            for (JIPipeNodeDatabaseEntry e : candidates) {
                int best = bestTypeDistance(e, targetSlotType, targetDataType);
                if (best > MAX_TYPE_DISTANCE_TO_INCLUDE) {
                    // Hard filter nonsensical matches if a query exists;
                    // if there is no textual query, we still allow larger distances to keep recall.
                    if (hasQuery) continue;
                }
                typeDistance.put(e, best);
            }
            candidates = candidates.stream().filter(typeDistance::containsKey).collect(Collectors.toList());
        }

        // Empty query: keep behavior similar to legacy (pinned first, then alphabetical)
        if (!hasQuery) {
            final Map<JIPipeNodeDatabaseEntry, Integer> td = typeDistance; // capture
            candidates.sort(Comparator
                    .comparing((JIPipeNodeDatabaseEntry e) -> !pinnedIds.contains(e.getId()))
                    .thenComparing(e -> td.getOrDefault(e, Integer.MAX_VALUE))
                    .thenComparing(JIPipeNodeDatabaseEntry::getName, String.CASE_INSENSITIVE_ORDER));
            return candidates;
        }

        // Score
        final TObjectDoubleMap<JIPipeNodeDatabaseEntry> primaryScore = new TObjectDoubleHashMap<>(candidates.size());
        final Map<JIPipeNodeDatabaseEntry, SortKey> auxKeys = new HashMap<>(candidates.size());

        for (JIPipeNodeDatabaseEntry e : candidates) {
            // Build fields
            final String name = normalize(e.getName());
            final List<String> tokenStrings = toStrings(e.getTokens()); // interface provides WeightedTokens for indexing :contentReference[oaicite:4]{index=4}
            final String tokensConcat = normalize(String.join(" ", tokenStrings));
            final String extra = normalize(extraBlob(e));

            // Field-wise multi-dimensional scores (lower is better)
            Score nameScore  = scoreItem(queryParts, wildcardPattern, name);
            Score tokenScore = tokensConcat.isEmpty() ? Score.worst() : scoreItem(queryParts, wildcardPattern, tokensConcat);
            Score extraScore = extra.isEmpty() ? Score.worst() : scoreItem(queryParts, wildcardPattern, extra);

            // Weighted combine like Fuse keys with weight
            double fused = nameScore.total() * W_NAME
                    + tokenScore.total() * W_TOKENS
                    + extraScore.total() * W_EXTRA;

            // Type distance as soft bias when present
            int td = typeDistance.getOrDefault(e, Integer.MAX_VALUE);
            double typeBias = td == Integer.MAX_VALUE ? 0.25 : weight(td, 5) * -0.50; // favor closer types (negative pushes up)

            // Deprecated: push down a bit (behaves like Comfy's custom sort tweak)
            double deprecatedPenalty = e.isDeprecated() ? 0.15 : 0.0;

            double finalScore = fused + deprecatedPenalty + typeBias;

            primaryScore.put(e, finalScore);

            // Compose multi-dimensional aux key similar to Comfy:
            // main (category), negative frequency (higher use = better), aux1/aux2, lengthPenalty-aware distance
            long freq = 0;
            SortKey key = new SortKey(
                    Math.min(Math.min(nameScore.main, tokenScore.main), extraScore.main),
                    -freq,
                    Math.min(Math.min(nameScore.aux1, tokenScore.aux1), extraScore.aux1),
                    Math.min(Math.min(nameScore.aux2, tokenScore.aux2), extraScore.aux2),
                    fused
            );
            auxKeys.put(e, key);
        }

        // Filter out poor matches: keep a permissive threshold similar to Comfy’s default "0.3-ish"
        // Our score isn't identical to Fuse, so we derive a percentile cut.
        List<JIPipeNodeDatabaseEntry> filtered = new ArrayList<>(candidates);
        if (!filtered.isEmpty()) {
            double p70 = percentile(primaryScore, filtered, 0.70);
            filtered.removeIf(e -> primaryScore.get(e) > p70 * 1.25); // slightly relaxed tail cut
        }

        // Sort: pinned first, then (main bucket) -> usage -> aux1 -> aux2 -> fused -> name
        filtered.sort(Comparator
                .comparing((JIPipeNodeDatabaseEntry e) -> !pinnedIds.contains(e.getId()))
                .thenComparing(e -> auxKeys.get(e).main)
                .thenComparingLong(e -> auxKeys.get(e).negFrequency)
                .thenComparingDouble(e -> auxKeys.get(e).aux1)
                .thenComparingDouble(e -> auxKeys.get(e).aux2)
                .thenComparingDouble(primaryScore::get)
                .thenComparing(JIPipeNodeDatabaseEntry::getName, String.CASE_INSENSITIVE_ORDER));

        return filtered;
    }

    /* ------------------------------- Scoring (Comfy-like) ------------------------------- */

    private static class Score {
        final int main;     // 0 exact, 1 prefix, 2 word, 3 substring, 4 multi-word, 9 fallback (no hit)
        final double aux1;  // position-based
        final double aux2;  // length-based
        final double base;  // normalized edit distance in [0,1]
        final double lengthPenalty; // small penalty for very different sizes

        Score(int main, double aux1, double aux2, double base, double lengthPenalty) {
            this.main = main;
            this.aux1 = aux1;
            this.aux2 = aux2;
            this.base = base;
            this.lengthPenalty = lengthPenalty;
        }

        static Score worst() { return new Score(9, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0, 0.2); }

        double total() { return base + lengthPenalty + main * 0.02; } // tiny bias to bucket
    }

    private Score scoreItem(QueryParts q, Pattern wildcard, String item) {
        if (item.isEmpty()) return Score.worst();

        String s = item;
        double base = normalizedLD(q.raw, s);
        double lengthPenalty = 0.2 * (1.0 - (Math.min(s.length(), q.raw.length()) / (double) Math.max(s.length(), q.raw.length())));

        // Wildcard full-string match gets priority between exact and prefix
        if (wildcard != null && wildcard.matcher(s).find()) {
            int pos = indexOfRegex(wildcard, s);
            return new Score(1, pos, s.length(), base, lengthPenalty);
        }

        // Exact
        if (s.equals(q.raw)) {
            return new Score(0, 0, s.length(), base * 0.5, 0.0); // exact: waive length penalty
        }

        // Prefix
        if (s.startsWith(q.raw)) {
            return new Score(1, 0, s.length(), base * 0.7, lengthPenalty * 0.3);
        }

        // Word / Substring / Multi-word
        List<String> words = q.splitWords(s);
        if (words.contains(q.raw)) {
            int idx = s.indexOf(q.raw);
            return new Score(2, idx + q.raw.length() * 0.5, s.length(), base * 0.8, lengthPenalty * 0.5);
        }
        if (s.contains(q.raw)) {
            int idx = s.indexOf(q.raw);
            return new Score(3, idx + q.raw.length() * 0.5, s.length(), base * 0.9, lengthPenalty * 0.7);
        }
        if (!q.parts.isEmpty() && q.parts.stream().allMatch(words::contains)) {
            List<Integer> indices = q.parts.stream().map(words::indexOf).sorted().collect(Collectors.toList());
            int min = indices.get(0);
            int max = indices.get(indices.size() - 1);
            double aux1 = (max - min) + max * 0.5 + s.length() * 0.5;
            return new Score(4, aux1, s.length(), base, lengthPenalty);
        }

        return new Score(9, Double.POSITIVE_INFINITY, s.length(), base, lengthPenalty);
    }

    /* ------------------------------- Helpers ------------------------------- */

    private static class SortKey {
        final int main;
        final long negFrequency;
        final double aux1;
        final double aux2;
        final double fused;

        SortKey(int main, long negFrequency, double aux1, double aux2, double fused) {
            this.main = main;
            this.negFrequency = negFrequency;
            this.aux1 = aux1;
            this.aux2 = aux2;
            this.fused = fused;
        }
    }

    private static class QueryParts {
        final String raw;
        final List<String> parts;

        QueryParts(String raw, List<String> parts) {
            this.raw = raw;
            this.parts = parts;
        }

        List<String> splitWords(String s) {
            // Split by space, boundaries, camelCase, digits-letters transitions, underscore and dash
            String[] w = s.split(" |\\b|(?<=[a-z])(?=[A-Z])|(?=[A-Z][a-z])|(?<=\\d)(?=\\D)|(?<=\\D)(?=\\d)|[_\\-\\/\\.]");
            List<String> out = new ArrayList<>();
            for (String x : w) {
                if (!x.isEmpty()) out.add(x);
            }
            return out;
        }
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
        // collapse whitespace
        return n.replaceAll("\\s+", " ");
    }

    private static QueryParts splitQuery(String q) {
        if (q.isEmpty()) return new QueryParts("", Collections.emptyList());
        String[] parts = q.split("\\s+");
        return new QueryParts(q, Arrays.stream(parts).filter(p -> !p.isEmpty()).collect(Collectors.toList()));
    }

    private static Pattern buildWildcardPattern(String q) {
        if (q == null || q.isEmpty() || q.indexOf('*') < 0) return null;
        // Escape regex meta, then expand '*' to '.*?'
        StringBuilder sb = new StringBuilder();
        for (char c : q.toCharArray()) {
            if (c == '*') sb.append(".*?");
            else {
                // Quote literal char
                if ("\\.[]{}()+-^$|?".indexOf(c) >= 0) sb.append("\\");
                sb.append(c);
            }
        }
        try {
            return Pattern.compile(sb.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int indexOfRegex(Pattern p, String s) {
        var m = p.matcher(s);
        if (m.find()) return m.start();
        return Integer.MAX_VALUE / 2;
    }

    private static List<String> toStrings(WeightedTokens tokens) {
        if (tokens == null || tokens.size() == 0) return Collections.emptyList();
        List<String> out = new ArrayList<>(tokens.size());
        for (int i = 0; i < tokens.size(); i++) {
            String t = tokens.getToken(i);
            if (!StringUtils.isNullOrEmpty(t)) out.add(t);
        }
        return out;
    }

    private static String extraBlob(JIPipeNodeDatabaseEntry e) {
        // very light, optional signals; kept weakly weighted (W_EXTRA)
        List<String> extra = new ArrayList<>();
        if (e.getDescription() != null && e.getDescription().getHtml() != null) {
            // Remove tags as a cheap normalization, the main normalize() will lowercase/strip accents
            extra.add(e.getDescription().getHtml().replaceAll("<[^>]*>", " "));
        }
        if (e.getLocationInfos() != null) extra.addAll(e.getLocationInfos());
        if (e.getCategoryIds() != null) extra.addAll(e.getCategoryIds());
        return String.join(" ", extra);
    }

    private static double normalizedLD(String a, String b) {
        int max = Math.max(a.length(), b.length());
        if (max == 0) return 0.0;
        int d = LD.apply(a, b);
        if (d < 0) return 1.0;
        return d / (double) max;
    }

    // Gaussian-like weight; taken from legacy idea to bias distances
    private static double weight(double x, double xMax) {
        return Math.exp(-Math.pow(x / xMax, 2));
    }

    // Data type distance adapted from legacy search (keeps semantics consistent)
    private static int dataTypeDistance(Class<? extends JIPipeData> from, Class<? extends JIPipeData> to) {
        if (from == to) {
            return 0;
        } else if (to.isAssignableFrom(from)) {
            return ReflectionUtils.getClassDistance(to, from);
        } else {
            return JIPipe.getDataTypes().getConversionDistance(from, to) * 5; // weight conversions higher
        }
    }

    private static int bestTypeDistance(JIPipeNodeDatabaseEntry entry, JIPipeSlotType targetSlotType, Class<? extends JIPipeData> targetDataType) {
        int best = Integer.MAX_VALUE;
        Map<String, JIPipeDataSlotInfo> map = (targetSlotType == JIPipeSlotType.Input) ? entry.getOutputSlots() : entry.getInputSlots();
        for (Map.Entry<String, JIPipeDataSlotInfo> si : map.entrySet()) {
            int d = (targetSlotType == JIPipeSlotType.Input)
                    ? dataTypeDistance(si.getValue().getDataClass(), targetDataType)
                    : dataTypeDistance(targetDataType, si.getValue().getDataClass());
            if (d >= 0 && d < best) best = d;
        }
        return best;
        // Entry slot access via interface fields here. :contentReference[oaicite:5]{index=5}
    }

    private static double percentile(TObjectDoubleMap<JIPipeNodeDatabaseEntry> score,
                                     List<JIPipeNodeDatabaseEntry> items,
                                     double p) {
        double[] arr = new double[items.size()];
        for (int i = 0; i < items.size(); i++) arr[i] = score.get(items.get(i));
        Arrays.sort(arr);
        int idx = (int) Math.floor(Math.max(0, Math.min(arr.length - 1, p * (arr.length - 1))));
        return arr[idx];
    }
}

