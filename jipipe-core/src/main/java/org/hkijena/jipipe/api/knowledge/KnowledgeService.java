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
import org.hkijena.jipipe.api.service.components.JIPipeAIServiceComponent;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.function.Function;
import java.util.stream.Stream;

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

    /**
     * The classpath location of the packaged corpus root (design spec 5.1).
     * A constant string literal: it is the packaged resource path.
     */
    static final String PACKAGED_CORPUS_LOCATION = "/org/hkijena/jipipe/ai/kdb/entries";

    /**
     * The classpath root of the unpacked corpus below the temporary directory.
     */
    private static volatile Path unpackedPackagedCorpus;

    /**
     * The process-wide singleton over the packaged corpus (design spec
     * 5.5.1). Volatile plus a synchronized slow path: the corpus index is
     * built exactly once per process, and every reader either sees null
     * (first use) or a fully constructed, immutable service.
     */
    private static volatile KnowledgeService instance;

    private final Map<String, KdbEntry> entriesByKey;
    private final KdbLexicalIndex lexicalIndex;
    private final KdbEntryGraph graph;
    private final KdbVectorLane vectorLane;
    private final KdbRenderer renderer;

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
        // The render time joins are live (design spec 5.5.1): the payload
        // renderer resolves node facts from the registry per lookup and takes
        // the next steps from this service's graph.
        this.renderer = new KdbRenderer(KdbRenderer.liveRegistryResolver(), graph::neighbors);
    }

    /**
     * The process-wide service over the packaged corpus (design spec 5.5.1):
     * constructed lazily on the first call and immutable afterwards. The
     * corpus (entries plus inverted index plus entry graph) is build-time
     * constant and ships inside the jipipe-core resources, so it is loaded
     * from the classpath, never from a file system location; the render-time
     * joins stay live by construction. The optional vector lane is wired when
     * JIPipe is instantiated and an embedding model is configured and ready;
     * without one, the service serves the lexical and relation lanes only.
     * Neither the corpus load nor the lane wiring forces AI initialization.
     *
     * @return the process-wide service
     */
    public static KnowledgeService getInstance() {
        KnowledgeService local = instance;
        if (local == null) {
            synchronized (KnowledgeService.class) {
                local = instance;
                if (local == null) {
                    local = createPackagedService();
                    instance = local;
                }
            }
        }
        return local;
    }

    /**
     * Replaces the process-wide singleton, or restores the lazy packaged
     * singleton by passing {@code null}. Test support only: the automation
     * operations tests install a fixture corpus so they do not depend on the
     * packaged corpus content. Never call this in production code paths:
     * the production lifecycle is construct once, and the corpus index is
     * immutable once loaded (design spec 5.5.1).
     *
     * @param replacement the replacement service, or null to reset
     */
    public static void setInstanceForTests(KnowledgeService replacement) {
        instance = replacement;
    }

    /**
     * Builds the packaged service: the corpus entries from the classpath
     * root plus the optional vector lane.
     */
    private static KnowledgeService createPackagedService() {
        List<KdbEntry> entries = new KdbEntryLoader().loadAll(packagedCorpusRoot()).getEntries();
        return new KnowledgeService(entries, packagedVectorLane(entries));
    }

    /**
     * The packaged corpus root as a file system path, usable with
     * {@link KdbEntryLoader#loadAll(Path)}: the resource directory itself
     * when the classpath carries it as files (development, tests in the
     * reactor), or the tree unpacked once into a temporary directory when it
     * ships inside the jipipe-core jar (the loader walks file system paths).
     * Mirrors the classpath approach of the launcher gate support class
     * {@code KdbProductionCorpus}.
     */
    private static Path packagedCorpusRoot() {
        URL url = KnowledgeService.class.getResource(PACKAGED_CORPUS_LOCATION);
        if (url == null) {
            throw new IllegalStateException("The packaged KDB corpus root is missing from the classpath: "
                    + PACKAGED_CORPUS_LOCATION);
        }
        if ("file".equals(url.getProtocol())) {
            try {
                return Paths.get(url.toURI());
            } catch (URISyntaxException e) {
                throw new IllegalStateException("The packaged KDB corpus root is not a file system path", e);
            }
        }
        return unpackPackagedCorpus(url);
    }

    /**
     * Unpacks the corpus tree from the jipipe-core jar into a temporary
     * directory, exactly once per process.
     */
    private static synchronized Path unpackPackagedCorpus(URL url) {
        if (unpackedPackagedCorpus != null) {
            return unpackedPackagedCorpus;
        }
        try {
            File jarFile = new File(((JarURLConnection) url.openConnection()).getJarFileURL().toURI());
            Path tempRoot = Files.createTempDirectory("jipipe-kdb-corpus");
            tempRoot.toFile().deleteOnExit();
            String prefix = PACKAGED_CORPUS_LOCATION.substring(1) + "/";
            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> jarEntries = jar.entries();
                while (jarEntries.hasMoreElements()) {
                    JarEntry jarEntry = jarEntries.nextElement();
                    String name = jarEntry.getName();
                    if (jarEntry.isDirectory() || !name.startsWith(prefix)) {
                        continue;
                    }
                    Path target = tempRoot.resolve(name.substring(prefix.length()));
                    Files.createDirectories(target.getParent());
                    try (InputStream in = jar.getInputStream(jarEntry)) {
                        Files.copy(in, target);
                    }
                }
            }
            unpackedPackagedCorpus = tempRoot;
            return tempRoot;
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("The packaged KDB corpus root could not be unpacked from " + url, e);
        }
    }

    /**
     * The optional vector lane of the packaged service: wired when JIPipe is
     * instantiated and an embedding model is configured and ready, using the
     * AI service component's synchronous embed operation and a cache
     * directory below the JIPipe user directory. Never called before JIPipe
     * exists, so the service construction never forces AI initialization; a
     * lane whose embed calls fail degrades to an empty ranking by contract,
     * so the fusion keeps the other two lanes.
     */
    private static KdbVectorLane packagedVectorLane(List<KdbEntry> entries) {
        if (!JIPipe.isInstantiated()) {
            return null;
        }
        JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
        if (!aiService.hasConfiguredAndReadyEmbeddingModel()) {
            return null;
        }
        // The synchronized wrapper serializes KDB lane calls against each
        // other: MCP operations run on endpoint threads, so two searches
        // could reach this lane concurrently. embedNow itself is documented
        // queue-thread-only, and concurrent embed-queue users are outside
        // this wrapper's guarantee.
        Function<String, float[]> embedder = text -> {
            synchronized (aiService) {
                return aiService.embedNow(text);
            }
        };
        return new KdbVectorLane(entries, embedder, embeddingCacheDir(aiService.getModelId()));
    }

    /**
     * The vector cache directory: {@code ai-knowledge-embeddings/kdb} below
     * the JIPipe user directory, with the model id as a subdirectory when it
     * is cheaply obtainable (the loaded model reports it; a model that is
     * configured but not yet started does not, so the flat directory is used
     * and documented here). Distinct model directories keep vectors of
     * different embedding models apart.
     */
    private static Path embeddingCacheDir(String modelId) {
        Path base = PathUtils.getJIPipeUserDir().resolve("ai-knowledge-embeddings").resolve("kdb");
        if (modelId == null || modelId.isBlank()) {
            return base;
        }
        return base.resolve(sanitizeModelId(modelId));
    }

    /**
     * Reduces a model id to a single, safe path segment.
     */
    private static String sanitizeModelId(String modelId) {
        StringBuilder sanitized = new StringBuilder();
        for (char character : modelId.trim().toLowerCase(Locale.ROOT).toCharArray()) {
            sanitized.append(Character.isLetterOrDigit(character) || character == '-' || character == '.'
                    ? character : '-');
        }
        String result = sanitized.toString();
        return result.isBlank() ? "default" : result;
    }

    /**
     * Searches the corpus: the lexical lane ranks the query, the optional
     * vector lane ranks it by embedding similarity, the relation lane
     * expands the hits of both retrieval lanes by one hop through ENTRY_REF,
     * and reciprocal rank fusion (k = 60) merges the rankings. The re-rank
     * then drops entries whose version qualifiers exclude the pinned
     * corpus version (an untilVersion below it, or a sinceVersion above
     * it), applies the exact-match filters, and orders by fused
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
     * Resolves an entry by key with its rendered agent facing text: the
     * structured corpus entry plus the render (design spec 5.5). The node
     * facts in the rendered part are resolved from the live registry per
     * lookup, so the payload reflects the current deprecation and successor
     * state, never a cached one. Independent of search visibility: an entry
     * that the version drop hides from search stays resolvable here for
     * provenance.
     *
     * @param entryKey the entry key
     * @return the payload, or empty for an unknown key
     */
    public Optional<KnowledgeEntryPayload> getKnowledgeEntry(String entryKey) {
        KdbEntry entry = entriesByKey.get(entryKey);
        if (entry == null) {
            return Optional.empty();
        }
        return Optional.of(new KnowledgeEntryPayload(entry, renderer.render(entry, KdbRenderer.DEFAULT_MAX_TOKENS)));
    }

    /**
     * Resolves the structured corpus entry by key, without rendering.
     *
     * @param entryKey the entry key
     * @return the entry, or empty for an unknown key
     */
    public Optional<KdbEntry> getKnowledgeEntryRaw(String entryKey) {
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
     * Whether search must hide the entry: one of its version qualifiers
     * excludes the pinned corpus version. The entry is not yet in force
     * when its {@code sinceVersion} is above the corpus version (a future
     * fact), and expired when its {@code untilVersion} is below the corpus
     * version. Both boundaries are inclusive, so an entry qualified at the
     * corpus version itself stays searchable. An unparsable qualifier keeps
     * the entry searchable (fail open): the loader narrows versions at the
     * boundary, so this only guards against hand-built entries.
     */
    private boolean isVersionDropped(KdbEntry entry) {
        int[] corpus = parseVersionCore(CORPUS_VERSION);
        if (entry.getSinceVersion() != null) {
            int[] since = parseVersionCore(entry.getSinceVersion());
            if (since != null && compareVersions(since, corpus) > 0) {
                return true;
            }
        }
        if (entry.getUntilVersion() != null) {
            int[] until = parseVersionCore(entry.getUntilVersion());
            if (until != null && compareVersions(until, corpus) < 0) {
                return true;
            }
        }
        return false;
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
