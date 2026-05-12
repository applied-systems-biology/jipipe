package org.hkijena.jipipe.api.nodes.database;

import com.google.common.collect.ImmutableList;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeNodeClassification;
import org.hkijena.jipipe.contrib.libstemmer.ext.EnglishStemmer;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.simmetrics.StringMetric;
import org.simmetrics.metrics.StringMetrics;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ComfyUI-inspired search for JIPipe nodes with improved relevance:
 * - IDF-weighted token coverage (rare query tokens matter more)
 * - Per-token matching ladder: exact > prefix > stem > substring > strong fuzzy
 * - Phrase/order & compact span bonus
 * - Recall floor to prune unrelated items
 * - Usage-frequency-aware secondary sort (in-memory)
 * - Phrase DB normalization (resource-backed, e.g. "8-bit" == "8 bit" == "8bit")
 * - Perfect match (name == query) always found & ranked first
 * <p>
 * Keeps visibility/existence filters, deprecation penalty, type-distance soft bias,
 * and pinned-first ordering. confirm* methods intentionally left minimal per request.
 */
public class JIPipeEnhancedNodeDatabaseSearch implements JIPipeNodeDatabaseSearch {

    // In-memory usage tracker (name -> count). Replace with persistent store if desired.
    private static final Map<String, Long> USAGE_COUNTS = new ConcurrentHashMap<>();
    // Fuzzy similarity metric & settings
    private static final StringMetric JW = StringMetrics.jaroWinkler();
    private static final double MIN_FUZZY_SIM = 0.85; // only reward strong fuzzy matches
    // Coverage threshold to prune junk results
    private static final double MIN_COVERAGE = 0.60;
    // Field blending (like Fuse key weighting)
    private static final double NAME_FIELD_WEIGHT = 0.70;
    private static final double TOKENS_FIELD_WEIGHT = 0.25;
    private static final double EXTRA_FIELD_WEIGHT = 0.05;
    // Type distance gating
    private static final int MAX_TYPE_DISTANCE_TO_INCLUDE = 64;
    private static final LevenshteinDistance LD = LevenshteinDistance.getDefaultInstance();
    private static final Pattern SPLIT_WORD_PATTERN = Pattern.compile(
            "[\\s_\\-/\\.]+|(?<=[a-z])(?=[A-Z])|(?=[A-Z][a-z])");
    // ---------- Phrase DB (resource-backed) ----------
    // Canonicalization map: variant -> canonical
    private static final Map<String, String> PHRASE_CANON = loadPhraseCanon();
    private final JIPipeNodeDatabase nodeDatabase;

    public JIPipeEnhancedNodeDatabaseSearch(JIPipeNodeDatabase nodeDatabase) {
        this.nodeDatabase = nodeDatabase;
    }

    /* ------------------------------- Public API (stubs for confirm*) ------------------------------- */

    private static CoverageScore coverageAgainst(QueryRep q, List<String> candTokens, List<String> candStems, Idf idf) {
        if (q.parts.isEmpty() || candTokens.isEmpty()) return new CoverageScore(0.0);

        double totalIdf = 0.0;
        double matchedIdf = 0.0;

        Set<String> tokenSet = new HashSet<>(candTokens);
        Set<String> stemSet = new HashSet<>(candStems);

        for (int i = 0; i < q.parts.size(); i++) {
            String qt = q.parts.get(i);
            String qStem = q.stems.get(i);
            double w = idf.get(qt) * (1.0 + 0.15 * (qt.length() >= 5 ? 1 : 0)); // tiny bias for longer rare tokens
            totalIdf += w;

            double m = 0.0;

            // Exact token
            if (tokenSet.contains(qt)) {
                m = 1.00;
            } else {
                // Prefix on any token
                String bestToken = null;
                for (String tok : candTokens) {
                    if (tok.startsWith(qt)) {
                        bestToken = tok;
                        break;
                    }
                }
                if (bestToken != null) {
                    m = 0.92;
                } else if (stemSet.contains(qStem)) {
                    m = 0.88; // stemmed match
                } else {
                    // Substring inside a token?
                    for (String tok : candTokens) {
                        if (tok.contains(qt)) {
                            bestToken = tok;
                            break;
                        }
                    }
                    if (bestToken != null) {
                        m = 0.80;
                    } else {
                        // Strong fuzzy
                        double sim = 0.0;
                        for (String tok : candTokens) {
                            sim = Math.max(sim, JW.compare(qt, tok));
                            if (sim >= 0.99) break;
                        }
                        if (sim >= MIN_FUZZY_SIM) {
                            // map [0.85..1.0] -> [0.65..0.80]
                            m = 0.65 + (sim - MIN_FUZZY_SIM) * (0.80 - 0.65) / (1.0 - MIN_FUZZY_SIM);
                        }
                    }
                }
            }

            matchedIdf += w * m;
        }

        double coverage = (totalIdf <= 0) ? 0.0 : (matchedIdf / totalIdf);
        return new CoverageScore(coverage);
    }

    private static double phraseBonus(QueryRep q, List<String> candTokens) {
        if (q.parts.size() < 2 || candTokens.isEmpty()) return 0.0;

        int lastPos = -1;
        int firstPos = Integer.MAX_VALUE;
        int matched = 0;

        List<String> candStems = candTokens.stream().map(JIPipeEnhancedNodeDatabaseSearch::stem).collect(Collectors.toList());

        for (int i = 0; i < q.parts.size(); i++) {
            String qt = q.parts.get(i);
            String qStem = q.stems.get(i);
            int pos = indexOfBest(candTokens, candStems, qt, qStem, lastPos + 1);
            if (pos >= 0) {
                matched++;
                firstPos = Math.min(firstPos, pos);
                lastPos = pos;
            }
        }
        if (matched < Math.max(2, (int) Math.ceil(q.parts.size() * 0.6))) return 0.0;

        int span = (lastPos - firstPos) + 1;
        double compactness = Math.max(0.0, Math.min(1.0, (double) matched / span));
        return 0.02 + compactness * 0.06; // [0.02 … 0.08]
    }

    private static int indexOfBest(List<String> tokens, List<String> stems, String q, String qStem, int start) {
        int best = -1;
        for (int i = Math.max(0, start); i < tokens.size(); i++) {
            String t = tokens.get(i);
            if (t.equals(q) || t.startsWith(q) || stems.get(i).equals(qStem)) {
                best = i;
                break;
            }
        }
        return best;
    }

    private static double spanOfQueryInTokens(QueryRep q, List<String> tokens) {
        if (q.parts.isEmpty() || tokens.isEmpty()) return Double.POSITIVE_INFINITY;
        List<Integer> positions = new ArrayList<>();
        List<String> stems = tokens.stream().map(JIPipeEnhancedNodeDatabaseSearch::stem).collect(Collectors.toList());
        for (int i = 0; i < q.parts.size(); i++) {
            String qt = q.parts.get(i);
            String qs = q.stems.get(i);
            int best = indexOfBest(tokens, stems, qt, qs, 0);
            if (best >= 0) positions.add(best);
        }
        if (positions.size() < 2) return Double.POSITIVE_INFINITY;
        int min = positions.stream().min(Integer::compareTo).get();
        int max = positions.stream().max(Integer::compareTo).get();
        return (max - min) + 1 - positions.size(); // gaps inside span; smaller is better
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
        return n.replaceAll("\\s+", " ");
    }

    /**
     * Apply phrase canonicalization using resource-backed DB
     */
    private static String canonicalize(String normalized) {
        if (normalized.isEmpty() || PHRASE_CANON.isEmpty()) return normalized;
        // Simple pass: replace longer variants first to avoid cascading micro-replacements
        // Build a list of variants sorted by length desc
        List<Map.Entry<String, String>> entries = new ArrayList<>(PHRASE_CANON.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
        String s = normalized;
        for (Map.Entry<String, String> e : entries) {
            String variant = e.getKey();
            String canon = e.getValue();
            // Replace exact substring occurrences; both already normalized
            s = s.replace(variant, canon);
        }
        return s;
    }

    /* ------------------------------- Helpers & scoring ------------------------------- */

    private static QueryParts splitQuery(String q) {
        if (q == null) q = "";
        q = normalize(q);
        q = canonicalize(q);
        if (q.isEmpty()) return new QueryParts("", Collections.emptyList());
        String[] parts = q.split("\\s+");
        return new QueryParts(q, Arrays.stream(parts).filter(p -> !p.isEmpty()).collect(Collectors.toList()));
    }

    private static Pattern buildWildcardPattern(String q) {
        if (q == null || q.isEmpty() || q.indexOf('*') < 0) return null;
        StringBuilder sb = new StringBuilder();
        for (char c : q.toCharArray()) {
            if (c == '*') sb.append(".*?");
            else {
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
        List<String> extra = new ArrayList<>();
        if (e.getDescription() != null && e.getDescription().getHtml() != null) {
            extra.add(e.getDescription().getHtml().replaceAll("<[^>]*>", " "));
        }
        if (e.getLocationInfos() != null) extra.addAll(e.getLocationInfos());
        if (e.getCategoryIds() != null) extra.addAll(e.getCategoryIds());
        return String.join(" ", extra);
    }

    private static List<String> splitWords(String s) {
        String[] w = SPLIT_WORD_PATTERN.split(s);
        List<String> out = new ArrayList<>(w.length);
        for (String x : w) if (!x.isEmpty()) out.add(x);
        return out;
    }

    private static String stem(String token) {
        if (token == null || token.isEmpty()) return token;
        EnglishStemmer s = new EnglishStemmer();
        s.setCurrent(token);
        if (s.stem()) return s.getCurrent();
        return token;
    }

    private static double normalizedLD(String a, String b) {
        int max = Math.max(a.length(), b.length());
        if (max == 0) return 0.0;
        int d = LD.apply(a, b);
        if (d < 0) return 1.0;
        return d / (double) max;
    }

    private static double weight(double x, double xMax) {
        return Math.exp(-Math.pow(x / xMax, 2));
    }

    private static int dataTypeDistance(Class<? extends JIPipeData> from, Class<? extends JIPipeData> to) {
        if (from == to) {
            return 0;
        } else if (to.isAssignableFrom(from)) {
            return ReflectionUtils.getClassDistance(to, from);
        } else {
            return JIPipe.getDataTypes().getConversionDistance(from, to) * 5;
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
    }

    private static double percentile(Map<JIPipeNodeDatabaseEntry, Double> score,
                                     List<JIPipeNodeDatabaseEntry> items,
                                     double p) {
        double[] arr = new double[items.size()];
        for (int i = 0; i < items.size(); i++) arr[i] = score.get(items.get(i));
        Arrays.sort(arr);
        int idx = (int) Math.floor(Math.max(0, Math.min(arr.length - 1, p * (arr.length - 1))));
        return arr[idx];
    }

    private static Map<String, String> loadPhraseCanon() {
        // Try resource first
        final String resourcePath = "/org/hkijena/jipipe/search/phrases.txt";
        Map<String, String> canon = new HashMap<>();
        boolean loaded = false;
        try (InputStream is = JIPipeEnhancedNodeDatabaseSearch.class.getResourceAsStream(resourcePath)) {
            if (is != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        // allow comma or semicolon separators
                        String[] parts = line.split("[,;]");
                        List<String> forms = new ArrayList<>();
                        for (String p : parts) {
                            String f = canonicalNormalize(p);
                            if (!f.isEmpty()) forms.add(f);
                        }
                        if (forms.size() >= 2) {
                            String canonical = forms.get(0);
                            for (String f : forms) {
                                canon.put(f, canonical);
                            }
                        } else if (forms.size() == 1) {
                            String f = forms.get(0);
                            canon.put(f, f);
                        }
                    }
                    loaded = true;
                }
            }
        } catch (Exception ignore) {
            // fall back
        }

        if (!loaded) {
            // Built-in minimal defaults so your examples work even without the resource present
            String[][] defaults = new String[][]{
                    {"2d", "2 d", "2-d"},
                    {"3d", "3 d", "3-d"},
                    {"8-bit", "8 bit", "8bit"},
                    {"16-bit", "16 bit", "16bit"},
                    {"roi", "region of interest"}
            };
            for (String[] group : defaults) {
                String canonical = canonicalNormalize(group[0]);
                for (String v : group) {
                    canon.put(canonicalNormalize(v), canonical);
                }
            }
        }
        return canon;
    }

    private static String canonicalNormalize(String s) {
        return normalize(s);
    }

    @Override
    public void confirmQuery(String text,
                             JIPipeNodeDatabasePipelineVisibility role,
                             boolean allowExisting,
                             boolean allowNew,
                             Set<String> pinnedIds,
                             JIPipeNodeDatabaseEntry userSelected) {
        // Stub: update usage frequency (optional)
        if (userSelected != null && !StringUtils.isNullOrEmpty(userSelected.getName())) {
            USAGE_COUNTS.merge(userSelected.getName(), 1L, Long::sum);
        }
    }

    @Override
    public List<JIPipeNodeDatabaseEntry> query(String text,
                                               JIPipeNodeDatabasePipelineVisibility role,
                                               boolean allowExisting,
                                               boolean allowNew,
                                               Set<String> pinnedIds, Object... flags) {
        return internalQuery(text, role, allowExisting, allowNew, pinnedIds, null, null);
    }

    @Override
    public void confirmQuery(String text,
                             JIPipeNodeDatabasePipelineVisibility role,
                             boolean allowExisting,
                             boolean allowNew,
                             JIPipeSlotType targetSlotType,
                             Class<? extends JIPipeData> targetDataType,
                             JIPipeNodeDatabaseEntry userSelected) {
        // Stub: update usage frequency (optional)
        if (userSelected != null && !StringUtils.isNullOrEmpty(userSelected.getName())) {
            USAGE_COUNTS.merge(userSelected.getName(), 1L, Long::sum);
        }
    }

    @Override
    public List<JIPipeNodeDatabaseEntry> query(String text,
                                               JIPipeNodeDatabasePipelineVisibility role,
                                               boolean allowExisting,
                                               boolean allowNew,
                                               JIPipeSlotType targetSlotType,
                                               Class<? extends JIPipeData> targetDataType, Object... flags) {
        return internalQuery(text, role, allowExisting, allowNew, Collections.emptySet(), targetSlotType, targetDataType);
    }

    @Override
    public void buildIndex() {
        for (JIPipeNodeDatabaseEntry entry : ImmutableList.copyOf(nodeDatabase.getEntries())) {
            // Create a candidate view which will attach the indexed info
            CandidateView.from(entry);
        }
    }

    private List<JIPipeNodeDatabaseEntry> internalQuery(String rawText,
                                                        JIPipeNodeDatabasePipelineVisibility role,
                                                        boolean allowExisting,
                                                        boolean allowNew,
                                                        Set<String> pinnedIds,
                                                        JIPipeSlotType targetSlotType,
                                                        Class<? extends JIPipeData> targetDataType) {

        // Normalize + phrase-canonicalize the query before tokenization
        final String text = canonicalize(normalize(rawText));
        final boolean hasQuery = !text.isEmpty();
        final QueryParts queryParts = splitQuery(text);
        final QueryRep Q = QueryRep.from(queryParts);

        // Optional wildcard regex (Comfy extended-search-like)
        final Pattern wildcardPattern = buildWildcardPattern(text);

        // Pre-filter by visibility and existence
        List<JIPipeNodeDatabaseEntry> candidates = nodeDatabase.getEntries().stream()
                .filter(e -> e.getVisibility().matches(role))
                .filter(e -> allowExisting || !e.exists())
                .filter(e -> allowNew || e.exists())
                .collect(Collectors.toList());

        // Type distance pre-computation / filter if a target is specified
        final Map<JIPipeNodeDatabaseEntry, Integer> typeDistance = new HashMap<>();
        if (targetSlotType != null && targetDataType != null) {
            for (JIPipeNodeDatabaseEntry e : candidates) {
                int best = bestTypeDistance(e, targetSlotType, targetDataType);
                if (best > MAX_TYPE_DISTANCE_TO_INCLUDE) {
                    // If a textual query exists, we can discard far-away types to keep precision;
                    // otherwise keep them for recall.
                    if (hasQuery) continue;
                }
                typeDistance.put(e, best);
            }
            candidates = candidates.stream().filter(typeDistance::containsKey).collect(Collectors.toList());
        }

        // Empty query: predictable order (pinned → type distance → alphabetical)
        if (!hasQuery) {
            final Map<JIPipeNodeDatabaseEntry, Integer> td = typeDistance;
            candidates.sort(Comparator
                    .comparing((JIPipeNodeDatabaseEntry e) -> !pinnedIds.contains(e.getId()))
                    .thenComparing(e -> td.getOrDefault(e, Integer.MAX_VALUE))
                    .thenComparing(JIPipeNodeDatabaseEntry::getName, String.CASE_INSENSITIVE_ORDER));
            return candidates;
        }

        // ---------- Build candidate views + corpus DF/IDF ----------
        final List<CandidateView> views = new ArrayList<>(candidates.size());
        final Map<String, Integer> df = new HashMap<>();
        final List<JIPipeNodeDatabaseEntry> exactNameMatches = new ArrayList<>();

        for (JIPipeNodeDatabaseEntry e : candidates) {
            CandidateView v = CandidateView.from(e);
            views.add(v);

            // Perfect name match (after normalization + canonicalization)
            if (canonicalize(normalize(e.getName())).equals(text)) {
                exactNameMatches.add(e);
            }

            Set<String> uniq = new HashSet<>();
            uniq.addAll(v.nameTokens);
            uniq.addAll(v.tokenTokens);
            uniq.addAll(v.nameStems);
            uniq.addAll(v.tokenStems);
            for (String t : uniq) df.merge(t, 1, Integer::sum);
        }
        final int N = Math.max(1, views.size());
        final Idf idf = new Idf(df, N);

        // ---------- Score each candidate ----------
        final Map<JIPipeNodeDatabaseEntry, Double> primaryScore = new HashMap<>(candidates.size());
        final Map<JIPipeNodeDatabaseEntry, SortKey> auxKeys = new HashMap<>(candidates.size());

        for (int i = 0; i < candidates.size(); i++) {
            JIPipeNodeDatabaseEntry e = candidates.get(i);
            CandidateView v = views.get(i);

            // Perfect match takes absolute precedence:
            final boolean isExact = exactNameMatches.contains(e);
            if (isExact) {
                // Force the smallest possible score and best bucket
                primaryScore.put(e, -1e6);
                long freq = USAGE_COUNTS.getOrDefault(e.getName(), 0L);
                auxKeys.put(e, new SortKey(-1, -freq, 0.0, -1.0, -1.0));
                continue;
            }

            // Field-wise coverage scores in [0..1]
            CoverageScore covName = coverageAgainst(Q, v.nameTokens, v.nameStems, idf);
            CoverageScore covTokens = coverageAgainst(Q, v.tokenTokens, v.tokenStems, idf);
            CoverageScore covExtra = coverageAgainst(Q, v.extraTokens, v.extraStems, idf);

            // Phrase/order bonus (primarily from name)
            double orderBonus = phraseBonus(Q, v.nameTokens) * 0.8
                    + phraseBonus(Q, v.tokenTokens) * 0.2;

            // Blend like Fuse key-weights
            double coverage = covName.coverage * NAME_FIELD_WEIGHT
                    + covTokens.coverage * TOKENS_FIELD_WEIGHT
                    + covExtra.coverage * EXTRA_FIELD_WEIGHT;

            // Wildcard: if provided and matches the normalized+canonicalized name, nudge coverage upward
            if (wildcardPattern != null && wildcardPattern.matcher(canonicalize(normalize(e.getName()))).find()) {
                coverage = Math.min(1.0, coverage + 0.05);
            }

            coverage = Math.min(1.0, coverage + orderBonus);

            // Enforce recall floor to remove unrelated items
            if (coverage < MIN_COVERAGE) {
                primaryScore.put(e, Double.POSITIVE_INFINITY);
                auxKeys.put(e, new SortKey(9, 0L, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 1e9));
                continue;
            }

            // Small tie-breaks: normalized LD to name, and length penalty
            String nameNormCanon = canonicalize(normalize(e.getName()));
            double ldName = normalizedLD(Q.raw, nameNormCanon);
            double lengthPenalty = 0.15 * (1.0 - (Math.min(Q.raw.length(), nameNormCanon.length()) / (double) Math.max(Q.raw.length(), nameNormCanon.length())));

            // Type distance soft bias
            int td = (targetSlotType != null && targetDataType != null)
                    ? typeDistance.getOrDefault(e, Integer.MAX_VALUE)
                    : Integer.MAX_VALUE;
            double typeBias = td == Integer.MAX_VALUE ? 0.20 : weight(td, 5) * -0.50;

            // Deprecated penalty
            double deprecatedPenalty = e.isDeprecatedOrUnstable() ? 0.12 : 0.0;

            // Classification penalty
            double classificationPenalty = 0.0;
            JIPipeNodeClassification classification = e.getNodeClassification();
            if (classification == JIPipeNodeClassification.AutoImport) {
                classificationPenalty = 0.12;
            } else if (classification == JIPipeNodeClassification.EdgeCase) {
                classificationPenalty = 0.25;
            }

            // Convert "higher is better" coverage to distance-like (lower is better)
            double fused = (1.0 - coverage) * 0.9 + 0.1 * ldName;
            double finalScore = fused + lengthPenalty + deprecatedPenalty + classificationPenalty + typeBias;

            primaryScore.put(e, finalScore);

            // Aux sort key
            int mainBucket = (coverage >= 0.98) ? 0 :
                    (coverage >= 0.90) ? 1 :
                            (coverage >= 0.80) ? 2 :
                                    (coverage >= 0.70) ? 3 : 4;

            // aux1: shorter index span of matched tokens in name is better
            double span = spanOfQueryInTokens(Q, v.nameTokens);

            long freq = USAGE_COUNTS.getOrDefault(e.getName(), 0L);
            auxKeys.put(e, new SortKey(mainBucket, -freq, span, -coverage, fused));
        }

        // Tail cut now that coverage floor exists (never drop perfect matches)
        List<JIPipeNodeDatabaseEntry> filtered = new ArrayList<>(candidates);
        filtered.removeIf(e -> !exactNameMatches.contains(e) && Double.isInfinite(primaryScore.getOrDefault(e, Double.POSITIVE_INFINITY)));
        if (!filtered.isEmpty()) {
            double p90 = percentile(primaryScore, filtered, 0.90);
            filtered.removeIf(e -> !exactNameMatches.contains(e) && primaryScore.get(e) > p90 * 1.25);
        }

        // Final sort: pinned first, then exact-match bucket, then bucket → primary score → name
        Comparator<JIPipeNodeDatabaseEntry> baseComparator = Comparator
                .comparing((JIPipeNodeDatabaseEntry e) -> exactNameMatches.contains(e) ? 0 : 1)
                .thenComparing((JIPipeNodeDatabaseEntry e) -> auxKeys.getOrDefault(e, new SortKey(9, 0, 0, 0, 0)).main)
                .thenComparingDouble(e -> primaryScore.getOrDefault(e, Double.POSITIVE_INFINITY))
                .thenComparing(JIPipeNodeDatabaseEntry::getName, String.CASE_INSENSITIVE_ORDER);

        filtered.sort(Comparator
                .comparing((JIPipeNodeDatabaseEntry e) -> !pinnedIds.contains(e.getId()))
                .thenComparing(baseComparator));

        return filtered;
    }

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
    }

    private static class QueryRep {
        final String raw;
        final List<String> parts;
        final List<String> stems;

        private QueryRep(String raw, List<String> parts) {
            this.raw = raw;
            this.parts = parts;
            this.stems = parts.stream().map(JIPipeEnhancedNodeDatabaseSearch::stem).collect(Collectors.toList());
        }

        static QueryRep from(QueryParts qp) {
            return new QueryRep(qp.raw, qp.parts);
        }
    }

    private static class CandidateView {
        final List<String> nameTokens, tokenTokens, extraTokens;
        final List<String> nameStems, tokenStems, extraStems;

        private CandidateView(List<String> n, List<String> t, List<String> x) {
            this.nameTokens = n;
            this.tokenTokens = t;
            this.extraTokens = x;
            this.nameStems = n.stream().map(JIPipeEnhancedNodeDatabaseSearch::stem).collect(Collectors.toList());
            this.tokenStems = t.stream().map(JIPipeEnhancedNodeDatabaseSearch::stem).collect(Collectors.toList());
            this.extraStems = x.stream().map(JIPipeEnhancedNodeDatabaseSearch::stem).collect(Collectors.toList());
        }

        static CandidateView from(JIPipeNodeDatabaseEntry e) {
            CandidateView candidateView = e.getAttachment(CandidateView.class);
            if (candidateView == null) {

                // Normalize + phrase-canonicalize before tokenization
                String name = canonicalize(normalize(e.getName()));
                List<String> tokens = toStrings(e.getTokens()).stream()
                        .map(JIPipeEnhancedNodeDatabaseSearch::normalize)
                        .map(JIPipeEnhancedNodeDatabaseSearch::canonicalize)
                        .collect(Collectors.toList());
                String extra = canonicalize(normalize(extraBlob(e)));

                candidateView = new CandidateView(
                        splitWords(name),
                        splitWords(String.join(" ", tokens)),
                        splitWords(extra)
                );
                e.attach(candidateView);
            }
            return candidateView;
        }
    }

    /* ------------------------------- Phrase DB loader ------------------------------- */

    private static class Idf {
        final Map<String, Double> idf;
        final double defaultIdf;

        Idf(Map<String, Integer> df, int N) {
            this.idf = new HashMap<>(df.size() * 2);
            for (Map.Entry<String, Integer> e : df.entrySet()) {
                double val = Math.log(1.0 + (N / (double) (1 + e.getValue())));
                idf.put(e.getKey(), val);
            }
            this.defaultIdf = Math.log(1.0 + N);
        }

        double get(String token) {
            return idf.getOrDefault(token, defaultIdf);
        }
    }

    private static class CoverageScore {
        final double coverage; // [0..1], IDF-weighted

        CoverageScore(double c) {
            this.coverage = c;
        }
    }
}
