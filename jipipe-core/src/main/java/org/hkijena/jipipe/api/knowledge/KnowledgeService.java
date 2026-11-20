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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The protocol-neutral retrieval service over the loaded KDB corpus (design
 * spec 5.5): the lexical lane (inverted index with BM25), the optional vector
 * lane (cosine similarity over an injectable embedder), and the relation lane
 * (one-hop ENTRY_REF expansion) fused with reciprocal rank fusion, followed by
 * the deterministic re-rank.
 * <p>
 * The service is immutable once constructed and holds no render-time state:
 * the corpus index does not change, and everything that varies at runtime
 * (node display names, deprecation state) is resolved later at render time.
 * This class is instantiable with an injected corpus; tests construct their
 * own instance, and the process-wide wiring is a separate concern.
 * <p>
 * The re-rank order is deterministic: the version drop, then the domain and
 * kind filters, then the fused score descending with the entry key ascending
 * as the stable tiebreak. The deprecated-node penalty is a later concern
 * (it needs registry access) and is not applied here.
 */
public final class KnowledgeService {

    /**
     * The pinned corpus version that {@code untilVersion} is compared
     * against. The constant is deliberately not the running application
     * version, so search results are time-stable.
     */
    public static final String CORPUS_VERSION = "6.0.0";

    /**
     * The lane id of the lexical lane (inverted index with BM25).
     */
    public static final String LANE_LEXICAL = "lexical";

    /**
     * The lane id of the relation lane (one-hop ENTRY_REF expansion).
     */
    public static final String LANE_RELATION = "relation";

    /**
     * The lane id of the vector lane (cosine similarity over embeddings).
     */
    public static final String LANE_VECTOR = "vector";

    /**
     * The reciprocal rank fusion constant.
     */
    static final double RRF_K = 60.0;

    /**
     * How deep the lexical lane ranks before fusion. Wider than the usual
     * result limit, so fusion and the re-rank filters keep material.
     */
    static final int LANE_TOP_K = 50;

    private final Map<String, KdbEntry> entriesByKey;
    private final KdbLexicalIndex lexicalIndex;
    private final KdbEntryGraph graph;
    private final KdbVectorLane vectorLane;

    /**
     * Creates a service over the loaded corpus without a vector lane. The
     * list is not modified; the derived indexes are immutable once built.
     *
     * @param entries the loaded corpus entries
     */
    public KnowledgeService(List<KdbEntry> entries) {
        this(entries, null);
    }

    /**
     * Creates a service over the loaded corpus with an optional vector lane.
     * The list is not modified; the derived indexes are immutable once built.
     *
     * @param entries    the loaded corpus entries
     * @param vectorLane the vector lane, or null when no embedding model is
     *                   configured (the results are then lexical and relation
     *                   only, indistinguishable from the two-lane service)
     */
    public KnowledgeService(List<KdbEntry> entries, KdbVectorLane vectorLane) {
        this.entriesByKey = new LinkedHashMap<>();
        for (KdbEntry entry : entries) {
            entriesByKey.put(entry.getEntryKey(), entry);
        }
        this.lexicalIndex = KdbLexicalIndex.build(entries);
        this.graph = new KdbEntryGraph(entries);
        this.vectorLane = vectorLane;
    }

    /**
     * Searches the corpus: the lexical lane ranks the query, the optional
     * vector lane ranks it by embedding similarity, the relation lane
     * expands the hits of both retrieval lanes by one hop through ENTRY_REF,
     * and reciprocal rank fusion (k = 60) merges the rankings. The re-rank
     * then drops entries whose {@code untilVersion} is below the pinned
     * corpus version, applies the exact-match filters, and orders by fused
     * score descending with the entry key ascending as the stable tiebreak.
     *
     * @param query       the search query (null or blank yields nothing)
     * @param domainFilter exact match against the {@link KdbDomain} id, or
     *                    null or blank for no filter
     * @param kindFilter  exact match against the {@link KdbEntryKind} id, or
     *                    null or blank for no filter
     * @param limit       the maximum number of results; non-positive yields
     *                    nothing
     * @return the fused, re-ranked results
     */
    public List<KnowledgeResult> searchKnowledge(String query, String domainFilter, String kindFilter, int limit) {
        if (query == null || query.isBlank() || limit <= 0) {
            return List.of();
        }
        // Lane 1: the lexical ranking. Lane 2: the vector ranking (empty
        // when no lane is configured; the lane itself degrades to empty when
        // its embedder fails). Lane 3: the one-hop ENTRY_REF expansion of the
        // lane 1 and lane 2 hits, ranked in discovery order (lexical rank of
        // the source, then neighbor order, then vector rank of the source).
        // Both retrieval lanes run over the full corpus; the version drop is
        // a re-rank stage and happens after fusion.
        List<KdbLexicalIndex.ScoredEntry> lexical = lexicalIndex.search(query, LANE_TOP_K);
        List<KdbVectorLane.ScoredEntry> vector = vectorLane == null
                ? List.of()
                : vectorLane.search(query, LANE_TOP_K);
        List<String> relationRanked = new ArrayList<>();
        Set<String> relationSeen = new LinkedHashSet<>();
        for (KdbLexicalIndex.ScoredEntry hit : lexical) {
            for (RelatedEntry neighbor : graph.neighbors(hit.entryKey())) {
                if (relationSeen.add(neighbor.entryKey())) {
                    relationRanked.add(neighbor.entryKey());
                }
            }
        }
        for (KdbVectorLane.ScoredEntry hit : vector) {
            for (RelatedEntry neighbor : graph.neighbors(hit.entryKey())) {
                if (relationSeen.add(neighbor.entryKey())) {
                    relationRanked.add(neighbor.entryKey());
                }
            }
        }
        // Reciprocal rank fusion with 1-based ranks: an entry in several
        // lanes collects every contribution.
        Map<String, Double> fusedScores = new LinkedHashMap<>();
        Map<String, Set<String>> lanesByEntry = new LinkedHashMap<>();
        for (int rank = 0; rank < lexical.size(); rank++) {
            String entryKey = lexical.get(rank).entryKey();
            fusedScores.merge(entryKey, 1.0 / (RRF_K + rank + 1), Double::sum);
            lanesByEntry.computeIfAbsent(entryKey, key -> new LinkedHashSet<>()).add(LANE_LEXICAL);
        }
        for (int rank = 0; rank < relationRanked.size(); rank++) {
            String entryKey = relationRanked.get(rank);
            fusedScores.merge(entryKey, 1.0 / (RRF_K + rank + 1), Double::sum);
            lanesByEntry.computeIfAbsent(entryKey, key -> new LinkedHashSet<>()).add(LANE_RELATION);
        }
        for (int rank = 0; rank < vector.size(); rank++) {
            String entryKey = vector.get(rank).entryKey();
            fusedScores.merge(entryKey, 1.0 / (RRF_K + rank + 1), Double::sum);
            lanesByEntry.computeIfAbsent(entryKey, key -> new LinkedHashSet<>()).add(LANE_VECTOR);
        }
        // Deterministic re-rank: version drop, filters, score, stable
        // tiebreak.
        List<KnowledgeResult> results = new ArrayList<>();
        for (Map.Entry<String, Double> fused : fusedScores.entrySet()) {
            KdbEntry entry = entriesByKey.get(fused.getKey());
            if (entry == null || isVersionDropped(entry)) {
                continue;
            }
            if (domainFilter != null && !domainFilter.isBlank()
                    && !entry.getDomain().getId().equals(domainFilter)) {
                continue;
            }
            if (kindFilter != null && !kindFilter.isBlank()
                    && !entry.getKind().getId().equals(kindFilter)) {
                continue;
            }
            results.add(new KnowledgeResult(fused.getKey(), fused.getValue(), Set.copyOf(lanesByEntry.get(fused.getKey()))));
        }
        results.sort(Comparator.comparingDouble(KnowledgeResult::score).reversed()
                .thenComparing(KnowledgeResult::entryKey));
        if (results.size() <= limit) {
            return results;
        }
        return new ArrayList<>(results.subList(0, limit));
    }

    /**
     * Resolves an entry by key, independent of search visibility: an entry
     * that the version drop hides from search stays resolvable here for
     * provenance.
     *
     * @param entryKey the entry key
     * @return the entry, or empty for an unknown key
     */
    public Optional<KdbEntry> getKnowledgeEntry(String entryKey) {
        return Optional.ofNullable(entriesByKey.get(entryKey));
    }

    /**
     * Lists the domains of the corpus with their entry counts, in the
     * declaration order of the closed domain set. Domains without entries
     * are not listed.
     *
     * @return the domain infos
     */
    public List<DomainInfo> listKnowledgeDomains() {
        List<DomainInfo> domains = new ArrayList<>();
        for (KdbDomain domain : KdbDomain.values()) {
            long entryCount = entriesByKey.values().stream()
                    .filter(entry -> entry.getDomain() == domain)
                    .count();
            if (entryCount > 0) {
                domains.add(new DomainInfo(domain.getId(), displayLabel(domain.getId()), entryCount));
            }
        }
        return domains;
    }

    /**
     * The one-hop, labeled ENTRY_REF neighbors of an entry.
     *
     * @param entryKey the entry key
     * @return the neighbors; empty for an unknown entry key
     */
    public List<RelatedEntry> relatedEntries(String entryKey) {
        return graph.neighbors(entryKey);
    }

    /**
     * Whether search must hide the entry: its {@code untilVersion} is below
     * the pinned corpus version. An {@code untilVersion} is inclusive, so
     * the corpus version itself keeps the entry searchable. An
     * unparsable version keeps the entry searchable (fail open): the loader
     * narrows versions at the boundary, so this only guards against
     * hand-built entries.
     */
    private boolean isVersionDropped(KdbEntry entry) {
        if (entry.getUntilVersion() == null) {
            return false;
        }
        int[] until = parseVersionCore(entry.getUntilVersion());
        if (until == null) {
            return false;
        }
        return compareVersions(until, parseVersionCore(CORPUS_VERSION)) < 0;
    }

    /**
     * Parses the numeric core of a version string: the qualifier after the
     * first dash or plus is cut, the rest splits on dots into integer
     * segments. Returns null if the core is not numeric.
     */
    private static int[] parseVersionCore(String version) {
        if (version == null || version.isBlank()) {
            return null;
        }
        String core = version;
        int dash = core.indexOf('-');
        int plus = core.indexOf('+');
        int cut = dash >= 0 && (plus < 0 || dash < plus) ? dash : plus;
        if (cut >= 0) {
            core = core.substring(0, cut);
        }
        String[] segments = core.split("\\.");
        int[] parts = new int[segments.length];
        for (int i = 0; i < segments.length; i++) {
            try {
                parts[i] = Integer.parseInt(segments[i].trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return parts;
    }

    /**
     * Compares two parsed versions segment by segment (numerically), with
     * missing segments counting as zero.
     */
    private static int compareVersions(int[] left, int[] right) {
        int length = Math.max(left.length, right.length);
        for (int i = 0; i < length; i++) {
            int leftSegment = i < left.length ? left[i] : 0;
            int rightSegment = i < right.length ? right[i] : 0;
            if (leftSegment != rightSegment) {
                return Integer.compare(leftSegment, rightSegment);
            }
        }
        return 0;
    }

    /**
     * Derives the human readable domain label from the id: the hyphens
     * become spaces and every word starts capitalized.
     */
    private static String displayLabel(String id) {
        String[] words = id.split("-");
        StringBuilder label = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (label.length() > 0) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1));
        }
        return label.toString();
    }
}
