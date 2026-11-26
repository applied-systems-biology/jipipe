package org.hkijena.jipipe.api.nodes.database;

import com.google.common.collect.ImmutableList;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeNodeClassification;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension;
import org.hkijena.jipipe.contrib.libstemmer.ext.EnglishStemmer;
import org.hkijena.jipipe.plugins.settings.application.JIPipeNodeSearchApplicationSettings;
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
 * - Name-match ladder: exact name > name prefix (#1404) > IDF-weighted token coverage (rare query tokens matter more)
 * - Per-token matching ladder: exact > prefix > joined/bigram > stem > substring (either direction, reverse
 *   guarded at 3 chars) > strong fuzzy > weak fuzzy
 * - Phrase/order & compact span bonus
 * - Recall floor to prune unrelated items
 * - Spatial-dimension bias: a query that constrains a dimension ("watershed
 *   3d") rewards nodes declaring it and penalizes nodes declaring a
 *   conflicting one (#1408)
 * - Usage-frequency-aware secondary sort (in-memory)
 * - Phrase DB normalization (resource-backed, e.g. "8-bit" == "8 bit" == "8bit")
 * - Perfect match (name == query) always found & ranked first
 * - Per-entry scores exposed through {@link #queryScored} (the merged
 *   ranking of #1409 consumes them)
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
    // Weak fuzzy tier (#1408): below the strong band, a near-match of a
    // typo grades flat, but only when BOTH tokens carry at least 5
    // characters (short tokens are too noisy for typo matching).
    private static final double MIN_WEAK_FUZZY_SIM = 0.78;
    private static final double WEAK_FUZZY_SCORE = 0.62;
    // Spatial-dimension bias (#1408): when the query constrains a spatial
    // dimension ("2d", "3d", ...), a coverage-scored candidate that declares
    // the dimension is rewarded and one that declares a DIFFERENT set is
    // penalized; a candidate that declares no dimensions is untouched. The
    // bonus is the smaller nudge (a matching node must not jump over a
    // better text match); the penalty is what pushes a hard dimension
    // constraint ("watershed 3d") past dimension-blind competitors of
    // equal text quality.
    private static final double DIMENSION_MATCH_BONUS = 0.03;
    private static final double DIMENSION_MISMATCH_PENALTY = 0.10;
    // Coverage threshold to prune junk results
    private static final double MIN_COVERAGE = 0.60;
    // Field blending (like Fuse key weighting)
    private static final double NAME_FIELD_WEIGHT = 0.70;
    private static final double TOKENS_FIELD_WEIGHT = 0.25;
    private static final double EXTRA_FIELD_WEIGHT = 0.05;
    // Name-exact bonus (#1408): the simplicity preference. When every
    // query token appears as an exact token of the candidate's name, the
    // blended coverage gains this bonus before the recall floor. The check
    // is exact token equality (containsAll of the post-canonicalization
    // query tokens against the canonicalized name tokens): stems,
    // prefixes, and joined compound forms do not count, and the query
    // tokens include dimension tokens when present ("gaussian blur 3d"
    // needs the name tokens [gaussian, blur, 3d]) - name-exact means the
    // name says it all. The bonus writes coverage only: it is independent
    // of the spatial-dimension bias (which shifts finalScore) and of the
    // phrase/order bonus (its own clamped line above).
    private static final double NAME_EXACT_BONUS = 0.05;
    // Type distance gating
    private static final int MAX_TYPE_DISTANCE_TO_INCLUDE = 64;
    private static final LevenshteinDistance LD = LevenshteinDistance.getDefaultInstance();
    // Usage bonus (#1404): the maximum effect of usage on the fused score
    // (a near-tie flip, never overriding text-match quality) and the
    // saturation point (half the bonus at this many uses).
    private static final double USAGE_BONUS_WEIGHT = 0.05;
    private static final double USAGE_BONUS_K = 10.0;
    // ---------- Phrase DB (resource-backed) ----------
    // Canonicalization map: variant -> canonical
    private static final Map<String, String> PHRASE_CANON = loadPhraseCanon();
    // Query-side spatial-dimension mapping (#1408): a post-canonicalization
    // query token that names a dimension constrains the search. "4d"/"5d"
    // fold into 3D (JIPipe has no 4D/5D spatial data), the "+t" compound
    // forms map like their digit (canon "2d+t"/"3d+t", see phrases.txt),
    // and "1d" maps to nothing (not a spatial dimension).
    private static final Map<String, JIPipeNodeTaxonomySpatialDimension> QUERY_DIMENSION_TOKENS = Map.of(
            "2d", JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D,
            "2d+t", JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D,
            "3d", JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D,
            "3d+t", JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D,
            "4d", JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D,
            "5d", JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D);
    private final JIPipeNodeDatabase nodeDatabase;

    public JIPipeEnhancedNodeDatabaseSearch(JIPipeNodeDatabase nodeDatabase) {
        this.nodeDatabase = nodeDatabase;
    }

    /* ------------------------------- Public API (stubs for confirm*) ------------------------------- */

    /**
     * Whether the weak fuzzy band (the typo tier) may fire (#1409):
     * reads the "Typo tolerance" setting of the Node search settings
     * page. Fails open: when JIPipe is not instantiated (tests,
     * headless runs), the current default-on behavior is kept. The
     * strong fuzzy band is unaffected and always active.
     */
    private static boolean isTypoToleranceEnabled() {
        return JIPipe.isInstantiated() ? JIPipeNodeSearchApplicationSettings.getInstance().isTypoTolerance() : true;
    }

    private static CoverageScore coverageAgainst(QueryRep q, List<String> candTokens, List<String> candStems,
                                                 List<String> candJoined, Idf idf) {
        if (q.parts.isEmpty() || candTokens.isEmpty()) return new CoverageScore(0.0);

        double totalIdf = 0.0;
        double matchedIdf = 0.0;

        Set<String> tokenSet = new HashSet<>(candTokens);
        Set<String> stemSet = new HashSet<>(candStems);
        Set<String> joinedSet = new HashSet<>(candJoined);

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
                } else if (joinedSet.contains(qt)) {
                    // Compound bridging (#1408): the word is the joined form
                    // of a separator compound of the candidate ("kmeans" ->
                    // "k-means")
                    m = 0.92;
                } else if (i + 1 < q.parts.size() && matchesAdjacentBigram(q, i, joinedSet)) {
                    // Adjacent-query bigram (#1408): "k" + "means" is the
                    // joined form of the next query word as well; both
                    // words are covered by this match, so the next iteration
                    // skips i + 1.
                    String nextQt = q.parts.get(i + 1);
                    double wNext = idf.get(nextQt) * (1.0 + 0.15 * (nextQt.length() >= 5 ? 1 : 0));
                    matchedIdf += w * 0.92;
                    matchedIdf += wNext * 0.92;
                    totalIdf += wNext;
                    i++;
                    continue;
                } else if (stemSet.contains(qStem)) {
                    m = 0.88; // stemmed match
                } else {
                    // Substring inside a token, or the reverse direction:
                    // the query token extends a candidate token (#1408).
                    // The reverse direction fires only for a candidate
                    // token of at least 3 characters, so a 1-2 char
                    // haystack token cannot prefix-match every longer
                    // query token; for the reverse arm the query token is
                    // the longer string, so the guard also implies the
                    // query token carries at least 3 characters. Grade
                    // stays 0.80.
                    for (String tok : candTokens) {
                        if (tok.contains(qt) || (qt.startsWith(tok) && tok.length() >= 3)) {
                            bestToken = tok;
                            break;
                        }
                    }
                    if (bestToken != null) {
                        m = 0.80;
                    } else {
                        // Fuzzy: one pass computes the max Jaro-Winkler
                        // similarity of the query token against the
                        // candidate tokens, then grades from two bands
                        // (#1408). Strong band: >= MIN_FUZZY_SIM maps to
                        // the gradient [0.65..0.80]. Weak band (typo
                        // tier): >= MIN_WEAK_FUZZY_SIM grades the flat
                        // WEAK_FUZZY_SCORE, but only when BOTH the query
                        // token and the matched candidate token carry at
                        // least 5 characters, and only when typo
                        // tolerance is enabled (#1409). Below both
                        // bands: no match.
                        double sim = 0.0;
                        String matchedToken = null;
                        for (String tok : candTokens) {
                            double s = JW.compare(qt, tok);
                            if (s > sim) {
                                sim = s;
                                matchedToken = tok;
                            }
                            if (sim >= 0.99) break;
                        }
                        if (sim >= MIN_FUZZY_SIM) {
                            // map [0.85..1.0] -> [0.65..0.80]
                            m = 0.65 + (sim - MIN_FUZZY_SIM) * (0.80 - 0.65) / (1.0 - MIN_FUZZY_SIM);
                        } else if (isTypoToleranceEnabled()
                                && sim >= MIN_WEAK_FUZZY_SIM
                                && qt.length() >= 5
                                && matchedToken != null
                                && matchedToken.length() >= 5) {
                            m = WEAK_FUZZY_SCORE;
                        }
                    }
                }
            }

            matchedIdf += w * m;
        }

        double coverage = (totalIdf <= 0) ? 0.0 : (matchedIdf / totalIdf);
        return new CoverageScore(coverage);
    }

    /**
     * Whether the adjacent query words {@code q.parts[i]} +
     * {@code q.parts[i + 1]} (lowercase concatenation) is the joined form
     * of one of the candidate's separator compounds (#1408): the query
     * typed with a space where the text has a separator ("k means"
     * against "k-means"). Only the joined-compound set can answer this:
     * a candidate token equal to the two-word concatenation would start
     * with {@code q.parts[i]}, so the prefix rung above always fires
     * first for it. A camelCase name needs no
     * bigram rung either: normalization already folds it into its
     * joined form as a single token ("AbCde" -> "abcde"), which the
     * query words reach through the ordinary exact/prefix/substring
     * rungs above.
     */
    private static boolean matchesAdjacentBigram(QueryRep q, int i, Set<String> joinedSet) {
        return joinedSet.contains(q.parts.get(i) + q.parts.get(i + 1));
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

    /**
     * The canonical lowercase id of a spatial dimension (#1408):
     * {@code SPATIAL_2D} -> "2d", {@code SPATIAL_3D} -> "3d". The enum is a
     * closed set without an id accessor, so the id derives from the constant
     * name; it is a comparison key inside this class only, never persisted.
     */
    private static String spatialDimensionId(JIPipeNodeTaxonomySpatialDimension dimension) {
        String name = dimension.name().toLowerCase(Locale.ROOT);
        return name.startsWith("spatial_") ? name.substring("spatial_".length()) : name;
    }

    /**
     * The spatial dimension the query constrains (#1408), or null: the first
     * post-canonicalization query token that {@link #QUERY_DIMENSION_TOKENS}
     * maps. A query naming several dimensions ("2d 3d") constrains by its
     * first mapped token.
     */
    private static JIPipeNodeTaxonomySpatialDimension querySpatialDimension(QueryRep q) {
        for (String part : q.parts) {
            JIPipeNodeTaxonomySpatialDimension dimension = QUERY_DIMENSION_TOKENS.get(part);
            if (dimension != null) {
                return dimension;
            }
        }
        return null;
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
        // Taxonomy function ids (Task 21b): every node-info-carrying entry is
        // searchable by the ids of the taxonomy functions its node appears
        // under. Uses the Task-15 functionIds facet already carried by the
        // entry (no TaxonomyView needed here). Entries that do not wrap a node
        // type (compartments, templates) legitimately contribute no id tokens;
        // their name/description/location tokens remain.
        Set<String> functionIds = e.getFunctionIds();
        if (functionIds != null && !functionIds.isEmpty()) {
            extra.addAll(functionIds);
        }
        return String.join(" ", extra);
    }

    private static List<String> splitWords(String s) {
        return NodeSearchText.splitWords(s);
    }

    private static String stem(String token) {
        return NodeSearchText.stem(token);
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

    /**
     * The usage bonus of the enhanced search (#1404): a small, bounded,
     * saturating value subtracted from the fused score of COVERAGE-SCORED
     * entries only - the exact and name-prefix ladder tiers never receive
     * it, so usage can flip near-ties but can never cross a text-quality
     * tier. Saturates at {@link #USAGE_BONUS_WEIGHT} (half at
     * {@link #USAGE_BONUS_K} uses). {@code null} ids (templates,
     * compartments, existing nodes) yield 0.
     */
    static double usageBonus(String nodeId) {
        int uses = NodeUsage.usage(nodeId);
        return uses <= 0 ? 0.0 : USAGE_BONUS_WEIGHT * uses / (uses + USAGE_BONUS_K);
    }

    /**
     * The main sort bucket for a blended coverage (#1404): rescaled by the
     * name+token field maximum ({@code NAME_FIELD_WEIGHT +
     * TOKENS_FIELD_WEIGHT = 0.95}) so a node that matches the query
     * completely in its name and taxonomy tokens reaches the top bucket
     * even when its description never echoes the query words. The bucket-0
     * boundary is exactly that maximum (coverage >= 0.95).
     */
    static int bucketFor(double coverage) {
        double c = Math.min(1.0, coverage / (NAME_FIELD_WEIGHT + TOKENS_FIELD_WEIGHT));
        return (c >= 1.0) ? 0 :
                (c >= 0.90) ? 1 :
                        (c >= 0.80) ? 2 :
                                (c >= 0.70) ? 3 : 4;
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
        return entriesOf(queryScored(text, role, allowExisting, allowNew, pinnedIds));
    }

    /**
     * The scored variant of the pinned-ids query: the same candidate set as
     * {@link #query(String, JIPipeNodeDatabasePipelineVisibility, boolean, boolean, Set, Object...)}
     * in the same order, with the per-entry relevance score retained. The
     * score is the fused distance-like final score (lower is better) that
     * the ranking sorts by: coverage-scored entries carry their computed
     * final score, the exact-name and name-prefix ladder tiers keep their
     * sentinel scores (whatever the scoring assigns them), and the
     * empty-query arm (which computes no relevance) exposes the neutral
     * score 0.0 for every entry.
     */
    public List<ScoredNodeDatabaseEntry> queryScored(String text,
                                                     JIPipeNodeDatabasePipelineVisibility role,
                                                     boolean allowExisting,
                                                     boolean allowNew,
                                                     Set<String> pinnedIds) {
        return internalQueryScored(text, role, allowExisting, allowNew, pinnedIds, null, null);
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
        return entriesOf(internalQueryScored(text, role, allowExisting, allowNew, Collections.emptySet(), targetSlotType, targetDataType));
    }

    /**
     * Maps a scored result list back to its entries (the plain query view).
     */
    private static List<JIPipeNodeDatabaseEntry> entriesOf(List<ScoredNodeDatabaseEntry> scored) {
        return scored.stream().map(ScoredNodeDatabaseEntry::entry).collect(Collectors.toList());
    }

    @Override
    public void buildIndex() {
        for (JIPipeNodeDatabaseEntry entry : ImmutableList.copyOf(nodeDatabase.getEntries())) {
            // Create a candidate view which will attach the indexed info
            CandidateView.from(entry);
        }
    }

    /**
     * The shared query path of both overloads: ranks the candidates and
     * returns them with the final score each entry was ranked by.
     */
    private List<ScoredNodeDatabaseEntry> internalQueryScored(String rawText,
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
            // The empty-query arm computes no relevance score: every entry
            // is exposed with the neutral score 0.0 (the order above is
            // determined by pinned status, type distance, and name).
            return scoredResults(candidates, pinnedIds, Collections.emptyMap(), 0.0);
        }

        // ---------- Build candidate views + corpus DF/IDF ----------
        final List<CandidateView> views = new ArrayList<>(candidates.size());
        final Map<String, Integer> df = new HashMap<>();
        final List<JIPipeNodeDatabaseEntry> exactNameMatches = new ArrayList<>();
        final List<JIPipeNodeDatabaseEntry> namePrefixMatches = new ArrayList<>();
        final Map<JIPipeNodeDatabaseEntry, String> canonicalNames = new HashMap<>();

        for (JIPipeNodeDatabaseEntry e : candidates) {
            CandidateView v = CandidateView.from(e);
            views.add(v);

            // Name match ladder (after normalization + canonicalization):
            // the name equals the query (perfect match), or the query is a
            // PREFIX of the name (#1404, e.g. "find part" -> "Find
            // particles 2D") - the progressive-typing case that pure token
            // coverage ranks inconsistently.
            String nameNormCanon = canonicalize(normalize(e.getName()));
            canonicalNames.put(e, nameNormCanon);
            if (nameNormCanon.equals(text)) {
                exactNameMatches.add(e);
            } else if (nameNormCanon.startsWith(text)) {
                namePrefixMatches.add(e);
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

        // The query-side spatial dimension is the same for every candidate:
        // resolve it once here, not once per candidate (#1408).
        final JIPipeNodeTaxonomySpatialDimension queryDimension = querySpatialDimension(Q);

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

            // Name-prefix match (#1404): second-highest precedence, before
            // every coverage-scored entry. A name that starts with what the
            // user typed is never junk, so the entry skips scoring (and the
            // recall floor / tail cut) and is ordered within its bucket by
            // name length - "Find particles 2D" before a long pattern whose
            // name merely continues the same prefix.
            if (namePrefixMatches.contains(e)) {
                primaryScore.put(e, 0.0);
                auxKeys.put(e, new SortKey(0, 0L, canonicalNames.get(e).length(), -1.0, -1.0));
                continue;
            }

            // Field-wise coverage scores in [0..1]
            CoverageScore covName = coverageAgainst(Q, v.nameTokens, v.nameStems, v.nameJoined, idf);
            CoverageScore covTokens = coverageAgainst(Q, v.tokenTokens, v.tokenStems, v.tokenJoined, idf);
            CoverageScore covExtra = coverageAgainst(Q, v.extraTokens, v.extraStems, v.extraJoined, idf);

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

            // Name-exact bonus (#1408): a candidate whose name carries
            // every query token is what the user named. The exact and
            // name-prefix ladder tiers never reach this line (they skip
            // scoring above), so only coverage-scored entries are graded.
            coverage = Math.min(1.0, coverage + (v.nameTokens.containsAll(Q.parts) ? NAME_EXACT_BONUS : 0.0));

            // Enforce recall floor to remove unrelated items
            if (coverage < MIN_COVERAGE) {
                primaryScore.put(e, Double.POSITIVE_INFINITY);
                auxKeys.put(e, new SortKey(9, 0L, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 1e9));
                continue;
            }

            // Small tie-breaks: normalized LD to name, and length penalty
            String nameNormCanon = canonicalNames.get(e);
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

            // Convert "higher is better" coverage to distance-like (lower is
            // better). The usage bonus (#1404) applies to coverage-scored
            // entries only - the exact and name-prefix tiers continue above
            // and never receive it.
            double fused = (1.0 - coverage) * 0.9 + 0.1 * ldName;

            // Spatial-dimension bias (#1408): see the constants. The
            // dimension token itself still scores normally in the coverage
            // ladder above; the bias only re-orders dimension-bearing
            // candidates of otherwise equal coverage. The exact and
            // name-prefix ladder tiers never reach this line.
            double dimensionBias = 0.0;
            if (queryDimension != null) {
                String queryDimensionId = spatialDimensionId(queryDimension);
                Set<String> declaredDimensions = v.spatialDimensions;
                if (declaredDimensions.contains(queryDimensionId)) {
                    dimensionBias = -DIMENSION_MATCH_BONUS;
                } else if (!declaredDimensions.isEmpty()) {
                    dimensionBias = DIMENSION_MISMATCH_PENALTY;
                }
            }

            double finalScore = fused + lengthPenalty + deprecatedPenalty + classificationPenalty + typeBias
                    - usageBonus(e.getOriginNodeId())
                    + dimensionBias;

            primaryScore.put(e, finalScore);

            // Aux sort key (the main bucket is rescaled by the name+token
            // field maximum - see bucketFor)
            int mainBucket = bucketFor(coverage);

            // aux1: shorter index span of matched tokens in name is better
            double span = spanOfQueryInTokens(Q, v.nameTokens);

            long freq = USAGE_COUNTS.getOrDefault(e.getName(), 0L);
            auxKeys.put(e, new SortKey(mainBucket, -freq, span, -coverage, fused));
        }

        // Tail cut now that coverage floor exists (never drop perfect or
        // name-prefix matches). The percentile is computed over the scored
        // entries only, so the sentinel scores of the exact/prefix matches
        // do not skew the cut.
        List<JIPipeNodeDatabaseEntry> filtered = new ArrayList<>(candidates);
        filtered.removeIf(e -> !exactNameMatches.contains(e) && !namePrefixMatches.contains(e)
                && Double.isInfinite(primaryScore.getOrDefault(e, Double.POSITIVE_INFINITY)));
        List<JIPipeNodeDatabaseEntry> scored = new ArrayList<>(filtered);
        scored.removeIf(e -> exactNameMatches.contains(e) || namePrefixMatches.contains(e));
        if (!scored.isEmpty()) {
            double p90 = percentile(primaryScore, scored, 0.90);
            filtered.removeIf(e -> !exactNameMatches.contains(e) && !namePrefixMatches.contains(e)
                    && primaryScore.get(e) > p90 * 1.25);
        }

        // Final sort: pinned first, then the name-match ladder (exact >
        // name-prefix > rest), then bucket → primary score → usage (as a
        // deterministic tie-break AFTER the score) → name
        Comparator<JIPipeNodeDatabaseEntry> baseComparator = Comparator
                .comparing((JIPipeNodeDatabaseEntry e) -> exactNameMatches.contains(e) ? 0
                        : namePrefixMatches.contains(e) ? 1 : 2)
                .thenComparing((JIPipeNodeDatabaseEntry e) -> auxKeys.getOrDefault(e, new SortKey(9, 0, 0, 0, 0)).main)
                .thenComparingDouble(e -> primaryScore.getOrDefault(e, Double.POSITIVE_INFINITY))
                .thenComparingLong(e -> -NodeUsage.usage(e.getOriginNodeId()))
                .thenComparing(JIPipeNodeDatabaseEntry::getName, String.CASE_INSENSITIVE_ORDER);

        filtered.sort(Comparator
                .comparing((JIPipeNodeDatabaseEntry e) -> !pinnedIds.contains(e.getId()))
                .thenComparing(baseComparator));

        return scoredResults(filtered, pinnedIds, primaryScore, Double.POSITIVE_INFINITY);
    }

    /**
     * Wraps a ranked result list into scored entries: each entry carries
     * the score the ranking assigned (the comparator default when absent)
     * and whether its entry id is pinned.
     */
    private static List<ScoredNodeDatabaseEntry> scoredResults(List<JIPipeNodeDatabaseEntry> entries,
                                                               Set<String> pinnedIds,
                                                               Map<JIPipeNodeDatabaseEntry, Double> scores,
                                                               double defaultScore) {
        List<ScoredNodeDatabaseEntry> result = new ArrayList<>(entries.size());
        for (JIPipeNodeDatabaseEntry entry : entries) {
            result.add(new ScoredNodeDatabaseEntry(entry, scores.getOrDefault(entry, defaultScore), pinnedIds.contains(entry.getId())));
        }
        return result;
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
        final List<String> nameJoined, tokenJoined, extraJoined;
        // Canonical lowercase ids of the node's declared spatial dimensions
        // (#1408); empty when the entry wraps no node type (templates,
        // compartments) or declares none.
        final Set<String> spatialDimensions;

        private CandidateView(List<String> n, List<String> t, List<String> x,
                              List<String> nj, List<String> tj, List<String> xj,
                              Set<String> spatialDimensions) {
            this.nameTokens = n;
            this.tokenTokens = t;
            this.extraTokens = x;
            this.nameStems = n.stream().map(JIPipeEnhancedNodeDatabaseSearch::stem).collect(Collectors.toList());
            this.tokenStems = t.stream().map(JIPipeEnhancedNodeDatabaseSearch::stem).collect(Collectors.toList());
            this.extraStems = x.stream().map(JIPipeEnhancedNodeDatabaseSearch::stem).collect(Collectors.toList());
            this.nameJoined = nj;
            this.tokenJoined = tj;
            this.extraJoined = xj;
            this.spatialDimensions = spatialDimensions;
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
                String tokenText = String.join(" ", tokens);
                String extra = canonicalize(normalize(extraBlob(e)));

                candidateView = new CandidateView(
                        splitWords(name),
                        splitWords(tokenText),
                        splitWords(extra),
                        // Compound bridging (#1408): the joined forms are
                        // additive comparison targets; the token streams
                        // above stay unchanged.
                        NodeSearchText.compoundJoinedTokens(name),
                        NodeSearchText.compoundJoinedTokens(tokenText),
                        NodeSearchText.compoundJoinedTokens(extra),
                        resolveSpatialDimensions(e)
                );
                e.attach(candidateView);
            }
            return candidateView;
        }

        /**
         * The canonical lowercase ids of the entry's declared spatial
         * dimensions (#1408). The entry carries the facet already: the
         * database copies the node info's declared set
         * ({@link org.hkijena.jipipe.api.nodes.JIPipeNodeInfo#getSpatialDimensions()})
         * into every node-type-wrapping entry at build time, which is the
         * same set a node type registry lookup by
         * {@code entry.getOriginNodeId()} would return, without the throwing
         * lookup and the per-view registry walk. Entries that wrap no node
         * type (templates, compartments) keep the interface default (empty).
         * Resolved once per candidate view: the view is cached on the entry.
         */
        static Set<String> resolveSpatialDimensions(JIPipeNodeDatabaseEntry e) {
            Set<JIPipeNodeTaxonomySpatialDimension> declared = e.getSpatialDimensions();
            if (declared == null || declared.isEmpty()) {
                return Collections.emptySet();
            }
            Set<String> ids = new LinkedHashSet<>();
            for (JIPipeNodeTaxonomySpatialDimension dimension : declared) {
                ids.add(spatialDimensionId(dimension));
            }
            return Collections.unmodifiableSet(ids);
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
