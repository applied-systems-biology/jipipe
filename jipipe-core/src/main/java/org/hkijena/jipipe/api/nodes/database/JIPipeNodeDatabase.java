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
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeNodeClassification;
import org.hkijena.jipipe.api.nodes.database.relations.*;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyInputProfile;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension;
import org.hkijena.jipipe.api.nodes.taxonomy.NoFramework;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeQueuedRunnableExecutor;
import org.hkijena.jipipe.plugins.settings.application.JIPipeNodeSearchApplicationSettings;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Allows to query nodes
 */
public class JIPipeNodeDatabase implements JIPipeNodeDatabaseSearch {

    private static final Logger LOGGER = LoggerFactory.getLogger(JIPipeNodeDatabase.class);

    private static JIPipeNodeDatabase INSTANCE;
    private final JIPipeQueuedRunnableExecutor queue = new JIPipeQueuedRunnableExecutor("Node database");
    private final JIPipeProject project;
    private final JIPipeNodeDatabaseUpdater updater;
    //    private final JIPipeLuceneNodeDatabaseSearch luceneSearch;
    private final JIPipeLegacyNodeDatabaseSearch legacySearch;
    private final JIPipeEnhancedNodeDatabaseSearch enhancedSearch;
    private final JIPipeAINodeDatabaseSearch aiSearch;
    private List<JIPipeNodeDatabaseEntry> entries = new ArrayList<>();
    /**
     * The cached Similar / Previous / Next workflow-edge index (spec §10.1).
     * A snapshot of the directed edges mined from the pluggable
     * {@link NodeRelationSource}s (family steps + template graphs, all
     * scopes). It drives the {@code previous} / {@code next} directions of
     * {@link #relations(JIPipeNodeDatabaseEntry, int)}.
     * <p>
     * It is rebuilt on the same triggers as the entry list: invalidated by
     * {@link #rebuildImmediately()} and {@link #setEntries(List)}, then
     * rebuilt eagerly by the builder run (off the UI thread, see
     * {@link #rebuildRelationIndex()}) and lazily on the first query after
     * invalidation (see {@link #relationIndex()}).
     * <p>
     * <b>Concurrency contract.</b> A {@code volatile} reference to an
     * immutable index that is never mutated in place: invalidation swaps the
     * reference to {@code null}, and a build publishes a fresh snapshot. A
     * concurrent build may discard another thread's just-published snapshot
     * - acceptable for a best-effort cache (at worst a momentarily stale or
     * recomputed result; correctness never depends on it). Every thread sees
     * either the pre-swap or the post-swap index, never a half-built one.
     */
    private volatile NodeRelationIndex relationIndex;
    /**
     * Cache of {@link #relations(JIPipeNodeDatabaseEntry, int)} results,
     * keyed by the entry's id and the requested {@code maxPerList} (same
     * contract the removed {@code related(entry, max)} had: a second call
     * with the same arguments returns the same {@link NodeRelations}
     * instance). Invalidated whenever the entry list is replaced
     * ({@link #setEntries(List)}) or a rebuild is scheduled
     * ({@link #rebuildImmediately()}).
     * <p>
     * Best-effort, like {@link #relationIndex}: a {@code volatile} reference
     * to an immutable map, never mutated in place. A copy-on-write publish
     * may lose a concurrent entry, which is at worst a recompute.
     */
    private volatile Map<String, NodeRelations> relationsCache = Collections.emptyMap();

    public JIPipeNodeDatabase() {
        this(null);
    }

    public JIPipeNodeDatabase(JIPipeProject project) {
        this.project = project;
        this.updater = new JIPipeNodeDatabaseUpdater(this);
//        this.luceneSearch = new JIPipeLuceneNodeDatabaseSearch(this);
        this.legacySearch = new JIPipeLegacyNodeDatabaseSearch(this);
        this.enhancedSearch = new JIPipeEnhancedNodeDatabaseSearch(this);
        this.aiSearch = new JIPipeAINodeDatabaseSearch();
        rebuildImmediately();
    }

    public static synchronized JIPipeNodeDatabase getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JIPipeNodeDatabase();
        }
        return INSTANCE;
    }

    /**
     * Stably partitions the given tier so that the down-ranked
     * (non-{@link JIPipeNodeClassification#Standard}) entries come after
     * the Standard ones, preserving the candidate order within both
     * groups.
     */
    private static List<JIPipeNodeDatabaseEntry> standardFirst(List<JIPipeNodeDatabaseEntry> tier) {
        List<JIPipeNodeDatabaseEntry> standard = new ArrayList<>(tier.size());
        List<JIPipeNodeDatabaseEntry> downRanked = new ArrayList<>();
        for (JIPipeNodeDatabaseEntry entry : tier) {
            if (entry.getNodeClassification() == JIPipeNodeClassification.Standard) {
                standard.add(entry);
            } else {
                downRanked.add(entry);
            }
        }
        standard.addAll(downRanked);
        return standard;
    }

    public void rebuildImmediately() {
        queue.cancelAll();
        // Invalidate both caches by swapping the references (see the fields'
        // concurrency contracts); never mutate the current values in place.
        relationIndex = null;
        relationsCache = Collections.emptyMap();
        queue.enqueue(new JIPipeNodeDatabaseBuilderRun(this));
    }

    /**
     * The generalized workflow relations of the given entry (spec §10.1):
     * the replacement for the removed {@code followUps(entry)} /
     * {@code related(entry, max)} API pair.
     * <p>
     * The result bundles three disjoint, per-list-capped lists:
     * <ul>
     * <li><b>{@link NodeRelations#next next}</b> - the entries that run
     * <em>after</em> this one in a workflow (family successors ∪ template
     * edges pointing out of it), from the cached {@link NodeRelationIndex};</li>
     * <li><b>{@link NodeRelations#previous previous}</b> - the entries that
     * run <em>before</em> this one (family predecessors ∪ template edges
     * pointing into it), from the index; and</li>
     * <li><b>{@link NodeRelations#similar similar}</b> - entries that do the
     * same kind of thing as this one, ranked by taxonomy tiers
     * (same function → same framework → shared tags), with any entry that is
     * also a {@code previous}/{@code next} neighbor <em>excluded</em> so the
     * three lists stay disjoint (and the entry itself / its alias entries
     * excluded, as before).</li>
     * </ul>
     * The previous/next node ids are mapped back to database entries (the
     * first entry in entry order whose origin node id matches wins; ids with
     * no entry are dropped). Each list is capped at {@code maxPerList}.
     * <p>
     * The result is cached per entry and {@code maxPerList}: a second call
     * with the same arguments returns the same {@link NodeRelations}
     * instance. The cache is invalidated whenever the entries are replaced
     * ({@link #setEntries(List)}) or a rebuild is scheduled
     * ({@link #rebuildImmediately()}).
     *
     * @param e          the entry to compute relations for
     * @param maxPerList the maximum number of entries per list
     * @return the three relation lists (never {@code null}, each possibly
     * empty); all lists empty if {@code e} wraps no node type
     */
    public NodeRelations relations(JIPipeNodeDatabaseEntry e, int maxPerList) {
        int max = Math.max(maxPerList, 0);
        String cacheKey = e.getId() + ":" + max;
        // Read the volatile cache reference once: this thread then works
        // with a single immutable map snapshot.
        NodeRelations cached = relationsCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String originNodeId = e.getOriginNodeId();
        NodeRelations result;
        if (originNodeId == null) {
            result = new NodeRelations(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        } else {
            // Previous / next come from the cached relation index (family
            // steps + template graphs), ranked by edge support then node id.
            NodeRelationIndex index = relationIndex();
            List<JIPipeNodeDatabaseEntry> next = toEntries(index.next(originNodeId), max);
            List<JIPipeNodeDatabaseEntry> previous = toEntries(index.previous(originNodeId), max);

            // Similar: taxonomy tiers (function → framework → tags) with the
            // workflow-neighbor nodes excluded so the three lists stay
            // disjoint.
            Set<String> neighborIds = new HashSet<>();
            neighborIds.addAll(index.next(originNodeId));
            neighborIds.addAll(index.previous(originNodeId));
            List<JIPipeNodeDatabaseEntry> similar = computeSimilar(e, originNodeId, neighborIds, max);

            result = new NodeRelations(
                    Collections.unmodifiableList(similar),
                    Collections.unmodifiableList(previous),
                    Collections.unmodifiableList(next));
        }
        cacheRelations(cacheKey, result);
        return result;
    }

    /**
     * Publishes a computed {@link #relations(JIPipeNodeDatabaseEntry, int)}
     * result into the per-entry cache by swapping in a copy of the current
     * map plus the new entry (copy-on-write; see the {@link #relationsCache}
     * concurrency contract - a concurrent publish may lose one of the two
     * entries, which is harmless for a best-effort cache).
     */
    private void cacheRelations(String cacheKey, NodeRelations result) {
        Map<String, NodeRelations> current = relationsCache;
        Map<String, NodeRelations> updated = new LinkedHashMap<>(current);
        updated.put(cacheKey, result);
        relationsCache = Collections.unmodifiableMap(updated);
    }

    /**
     * Maps an ordered list of node ids (as returned by the relation index)
     * back to database entries and caps it at {@code max}. The first entry
     * (in entry order) whose origin node id matches wins; ids with no
     * database entry are dropped.
     */
    private List<JIPipeNodeDatabaseEntry> toEntries(List<String> nodeIds, int max) {
        List<JIPipeNodeDatabaseEntry> result = new ArrayList<>(Math.min(nodeIds.size(), max));
        Set<String> seen = new LinkedHashSet<>();
        for (String nodeId : nodeIds) {
            if (result.size() >= max) {
                break;
            }
            JIPipeNodeDatabaseEntry entry = entryByOriginNodeId(nodeId);
            if (entry != null && seen.add(entry.getId())) {
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * The taxonomy-tier similarity query refactored from the removed
     * {@code related(entry, max)}: same function → same framework → shared
     * tags (≥2 shared before exactly 1), de-duplicated, down-ranking
     * non-Standard entries to the end of their tier, and capped at
     * {@code max}.
     * <p>
     * Unlike the old {@code related}, the first tier (same-family siblings
     * minus follow-ups - the workflow neighborhood) is gone: those nodes are
     * now surfaced by {@link NodeRelations#previous()}/{@link NodeRelations#next()}.
     * Candidates that are also in {@code neighborIds} (i.e. already a
     * previous/next neighbor) are excluded so the three lists stay disjoint.
     */
    private List<JIPipeNodeDatabaseEntry> computeSimilar(JIPipeNodeDatabaseEntry e, String originNodeId,
                                                         Set<String> neighborIds, int max) {
        if (max <= 0) {
            return Collections.emptyList();
        }
        String selfId = e.getId();
        // Entries representing the same node (e.g. alias entries of a
        // "create node by info" entry) are never "similar": a node is not
        // similar to itself under a different menu path.
        Predicate<JIPipeNodeDatabaseEntry> sameNode =
                entry -> selfId.equals(entry.getId()) || originNodeId.equals(entry.getOriginNodeId());
        // A candidate is excluded if it is the entry's own node, already
        // surfaced as a previous/next neighbor, or has no database entry.
        Predicate<JIPipeNodeDatabaseEntry> excluded =
                entry -> sameNode.test(entry)
                        || (entry.getOriginNodeId() != null && neighborIds.contains(entry.getOriginNodeId()));
        Set<String> added = new LinkedHashSet<>();

        // Tier 1: same function - entries that appear under at least one of
        // the entry's function leaves (an exact shared function id). Ranking
        // shared functionality above framework is what keeps the suggestions
        // distinct for functionally different nodes that share a large
        // framework (e.g. the many ImageJ 1 nodes): two "Filtering" nodes are
        // a better "similar" pair than two arbitrary ImageJ 1 nodes.
        List<JIPipeNodeDatabaseEntry> tierFunction = new ArrayList<>();
        Set<String> myFunctionIds = e.getFunctionIds();
        if (!myFunctionIds.isEmpty()) {
            for (JIPipeNodeDatabaseEntry entry : entries) {
                if (excluded.test(entry)) {
                    continue;
                }
                for (String fid : entry.getFunctionIds()) {
                    if (myFunctionIds.contains(fid) && added.add(entry.getId())) {
                        tierFunction.add(entry);
                        break;
                    }
                }
            }
        }

        // Tier 2: same framework (the NoFramework sentinel never matches:
        // it would make every framework-less entry a "sibling")
        List<JIPipeNodeDatabaseEntry> tierFramework = new ArrayList<>();
        String frameworkId = e.getFramework().getId();
        if (!NoFramework.ID.equals(frameworkId)) {
            for (JIPipeNodeDatabaseEntry entry : entries) {
                if (frameworkId.equals(entry.getFramework().getId())
                        && !excluded.test(entry) && added.add(entry.getId())) {
                    tierFramework.add(entry);
                }
            }
        }

        // Tier 3: shared tags (>=2 shared before exactly 1)
        List<JIPipeNodeDatabaseEntry> tierTags = new ArrayList<>();
        Set<String> myTags = e.getTags();
        if (!myTags.isEmpty()) {
            List<JIPipeNodeDatabaseEntry> twoShared = new ArrayList<>();
            List<JIPipeNodeDatabaseEntry> oneShared = new ArrayList<>();
            for (JIPipeNodeDatabaseEntry entry : entries) {
                if (excluded.test(entry)) {
                    continue;
                }
                int shared = 0;
                for (String tag : entry.getTags()) {
                    if (myTags.contains(tag)) {
                        shared++;
                    }
                }
                if (shared >= 2) {
                    twoShared.add(entry);
                } else if (shared == 1) {
                    oneShared.add(entry);
                }
            }
            for (JIPipeNodeDatabaseEntry entry : twoShared) {
                if (added.add(entry.getId())) {
                    tierTags.add(entry);
                }
            }
            for (JIPipeNodeDatabaseEntry entry : oneShared) {
                if (added.add(entry.getId())) {
                    tierTags.add(entry);
                }
            }
        }

        // Merge the tiers in priority order; within each tier,
        // down-ranked (non-Standard) entries are sorted to the end.
        List<JIPipeNodeDatabaseEntry> result = new ArrayList<>(max);
        for (List<JIPipeNodeDatabaseEntry> tier : List.of(tierFunction, tierFramework, tierTags)) {
            if (result.size() >= max) {
                break;
            }
            for (JIPipeNodeDatabaseEntry entry : standardFirst(tier)) {
                if (result.size() >= max) {
                    break;
                }
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * Returns the current cached {@link NodeRelationIndex}, building it
     * eagerly on the first query after the cache was invalidated (i.e. after
     * {@link #rebuildImmediately()} or {@link #setEntries(List)}). The build
     * is guarded so that concurrent first queries do not each re-mine the
     * sources (a lost build is harmless - it is a best-effort cache; see the
     * {@link #relationIndex} concurrency contract).
     */
    private NodeRelationIndex relationIndex() {
        NodeRelationIndex index = relationIndex;
        if (index == null) {
            synchronized (this) {
                index = relationIndex;
                if (index == null) {
                    index = buildRelationIndex();
                    relationIndex = index;
                }
            }
        }
        return index;
    }

    /**
     * Mines all {@link NodeRelationSource}s (family steps + every template
     * graph, all scopes) into a fresh, immutable {@link NodeRelationIndex}.
     * The sources resolve and warn about stale edges themselves, so this
     * never throws on a stale template edge (Review Focus #2).
     * <p>
     * The source list is the pluggable seam (spec §10.2): a future source
     * (starter patterns, #1333 repository templates, ...) attaches by adding
     * one entry here - no change to the index or to {@link #relations}.
     */
    private NodeRelationIndex buildRelationIndex() {
        List<NodeRelationSource> sources = List.of(
                new FamilyRelationSource(),
                new TemplateRelationSource(project));
        NodeRelationIndex index = new NodeRelationIndex();
        for (NodeRelationSource source : sources) {
            source.contribute(index::addEdge);
        }
        return index;
    }

    /**
     * The first entry (in entry order) whose origin node id equals the
     * given node id, or {@code null} if no such entry exists.
     */
    private JIPipeNodeDatabaseEntry entryByOriginNodeId(String originNodeId) {
        for (JIPipeNodeDatabaseEntry entry : entries) {
            if (originNodeId.equals(entry.getOriginNodeId())) {
                return entry;
            }
        }
        return null;
    }

    /**
     * The current relation index snapshot, rebuilt immediately on the
     * calling thread. Invoked from the builder run so that after every
     * registry/template refresh the Similar/Previous/Next data is current
     * before the first UI query (the lazy path in
     * {@link #relationIndex()} still covers the window between invalidation
     * and the builder run finishing).
     */
    public void rebuildRelationIndex() {
        relationIndex = buildRelationIndex();
    }

    public List<JIPipeNodeDatabaseEntry> getEntries() {
        return entries;
    }

    public synchronized void setEntries(List<JIPipeNodeDatabaseEntry> entries) {
        this.entries = entries;
        // Invalidate both caches by swapping the references (see the fields'
        // concurrency contracts); never mutate the current values in place.
        relationIndex = null;
        relationsCache = Collections.emptyMap();
        aiSearch.setEntries(entries);
    }

    public JIPipeQueuedRunnableExecutor getQueue() {
        return queue;
    }

    public JIPipeProject getProject() {
        return project;
    }

    public JIPipeNodeDatabaseUpdater getUpdater() {
        return updater;
    }

    public JIPipeNodeDatabaseSearchImplementation getSearchImplementation() {
        if (JIPipe.isInstantiated()) {
            return JIPipeNodeSearchApplicationSettings.getInstance().getSearchImplementation();
        }
        return JIPipeNodeDatabaseSearchImplementation.Legacy;
    }

    public JIPipeNodeDatabaseSearch getSearch() {
        if (getSearchImplementation() == JIPipeNodeDatabaseSearchImplementation.Legacy) {
            return legacySearch;
        }
        if (getSearchImplementation() == JIPipeNodeDatabaseSearchImplementation.Enhanced) {
            return enhancedSearch;
        }
        return legacySearch;
    }

    @Override
    public void confirmQuery(String text, JIPipeNodeDatabasePipelineVisibility role, boolean allowExisting, boolean allowNew, Set<String> pinnedIds, JIPipeNodeDatabaseEntry userSelected) {
        getSearch().confirmQuery(text, role, allowExisting, allowNew, pinnedIds, userSelected);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The merged ranking of #1409: for a non-blank query, when the AI
     * contribution is active ({@link JIPipeMergedNodeSearchRanking#isAiContributionActive()})
     * and the configured search implementation is {@link JIPipeNodeDatabaseSearchImplementation#Enhanced},
     * the classic (enhanced) per-entry scores are fused with the AI
     * search's per-entry cosine similarities via
     * {@link JIPipeMergedNodeSearchRanking#fuse}. The AI side blocks this
     * call until the search future completes or the AI search's inherited
     * timeout ({@link JIPipeAINodeDatabaseSearch#SEARCH_TIMEOUT_SECONDS})
     * elapses; a timeout, failure, or interruption degrades to the classic
     * order (the fusion's null-AI arm).
     * <p>
     * A null or whitespace-only query never reaches the fusion: it carries
     * no relevance signal (both searches' empty-query arms expose neutral
     * 0.0 scores), so it returns the classic result directly without
     * consulting the AI. A non-blank query whose enhanced classic result is
     * empty keeps the fused list empty although the AI side IS consulted
     * and its results are discarded (the classic candidate set is the
     * fusion's universe): a punctuation-only string such as "!!!" passes
     * the blank check, but the enhanced search scores no candidate into
     * the result (every coverage stays below the recall floor). A non-blank
     * query that normalizes and canonicalizes to no relevance tokens
     * (for example a query made only of combining marks) gets the enhanced
     * search's neutral empty-query arm (every entry exposed at 0.0); the
     * classic side is then all-neutral (strength 1.0 for every classic
     * entry), so the fusion keeps the classic entries above AI-only
     * candidates and the AI reorders within that tie.
     * <p>
     * The varargs {@code flags} parameter is ignored (kept for interface
     * compatibility): the former {@code FLAG_USE_AI_SEARCH} flag was
     * removed together with the AI-search toggle buttons (#1409); the
     * merged ranking replaces it.
     */
    @Override
    public List<JIPipeNodeDatabaseEntry> query(String text, JIPipeNodeDatabasePipelineVisibility role, boolean allowExisting, boolean allowNew, Set<String> pinnedIds, Object... flags) {
        if (text == null || text.isBlank()) {
            return getSearch().query(text, role, allowExisting, allowNew, pinnedIds);
        }
        if (JIPipeMergedNodeSearchRanking.isAiContributionActive()
                && getSearchImplementation() == JIPipeNodeDatabaseSearchImplementation.Enhanced) {
            List<ScoredNodeDatabaseEntry> classic = enhancedSearch.queryScored(text, role, allowExisting, allowNew, pinnedIds);
            List<ScoredNodeDatabaseEntry> ai = awaitAiScored(text, role, allowExisting, allowNew, pinnedIds);
            return JIPipeMergedNodeSearchRanking.fuse(classic, ai, pinnedIds);
        }
        return getSearch().query(text, role, allowExisting, allowNew, pinnedIds);
    }

    @Override
    public void confirmQuery(String text, JIPipeNodeDatabasePipelineVisibility role, boolean allowExisting, boolean allowNew, JIPipeSlotType targetSlotType, Class<? extends JIPipeData> targetDataType, JIPipeNodeDatabaseEntry userSelected) {
        getSearch().confirmQuery(text, role, allowExisting, allowNew, targetSlotType, targetDataType, userSelected);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The varargs {@code flags} parameter is ignored (kept for interface
     * compatibility; the former {@code FLAG_USE_AI_SEARCH} flag was removed
     * in #1409). Scope ruling of the merged ranking (#1409): this overload
     * keeps classic-only behavior - the removed flag had no caller here,
     * and type distance dominates slot-targeted queries.
     */
    @Override
    public List<JIPipeNodeDatabaseEntry> query(String text, JIPipeNodeDatabasePipelineVisibility role, boolean allowExisting, boolean allowNew, JIPipeSlotType targetSlotType, Class<? extends JIPipeData> targetDataType, Object... flags) {
        return getSearch().query(text, role, allowExisting, allowNew, targetSlotType, targetDataType);
    }

    /**
     * The blocking AI contribution of the merged ranking: waits for the
     * {@link JIPipeAINodeDatabaseSearch#queryScoredAsync} future with the
     * AI search's inherited timeout and catch semantics (identical to its
     * own blocking wait). Every failure kind (unavailable model, timeout,
     * cancellation, error) maps to {@code null}, which the fusion treats
     * as "no AI contribution" and returns the classic order.
     */
    private List<ScoredNodeDatabaseEntry> awaitAiScored(String text, JIPipeNodeDatabasePipelineVisibility role, boolean allowExisting, boolean allowNew, Set<String> pinnedIds) {
        CompletableFuture<List<ScoredNodeDatabaseEntry>> future = aiSearch.queryScoredAsync(text, role, allowExisting, allowNew, pinnedIds);
        if (future == null) {
            return null;
        }
        try {
            return future.get(JIPipeAINodeDatabaseSearch.SEARCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.debug("AI search was interrupted/cancelled");
            return null;
        } catch (TimeoutException e) {
            LOGGER.warn("Timeout during AI search, returning null");
            return null;
        } catch (Exception e) {
            LOGGER.warn("AI search failed, returning null");
            LOGGER.debug("AI search failed", e);
            return null;
        }
    }

    /**
     * Queries the database with the given taxonomy facets applied as a
     * <em>post-filter</em> on the regular query result.
     * <p>
     * This overload is intentionally implemented on this concrete class
     * (not on the {@link JIPipeNodeDatabaseSearch} interface): the three
     * search implementations are untouched, and the facets are applied by
     * filtering the entries the underlying search returned, in the same
     * order.
     * <p>
     * An empty facet (a {@code null} framework, or an empty spatial / input /
     * tags set) imposes no filter on that dimension. The facet predicates
     * mirror the node browser model's {@code matches(entry)} helper, so the
     * list render and the database query agree on what "matches" means.
     *
     * @param text          the search text
     * @param role          the pipeline visibility role
     * @param allowExisting whether existing nodes may be returned
     * @param allowNew      whether new nodes may be returned
     * @param pinnedIds     the pinned entry ids (used for ranking)
     * @param framework     the framework to filter by, or {@code null} for no filter
     * @param spatial       the spatial dimensions to filter by; empty = no filter
     * @param input         the input profiles to filter by; empty = no filter
     * @param tags          the tags to filter by; empty = no filter
     * @param flags         ignored (kept for source compatibility; the
     *                      former AI-search flag was removed in #1409)
     * @return the query result filtered by the active facets, in the
     * underlying search's order
     */
    public List<JIPipeNodeDatabaseEntry> queryWithFacets(String text, JIPipeNodeDatabasePipelineVisibility role,
                                                         boolean allowExisting, boolean allowNew, Set<String> pinnedIds,
                                                         @Nullable JIPipeNodeTaxonomyFramework framework,
                                                         Set<JIPipeNodeTaxonomySpatialDimension> spatial,
                                                         Set<JIPipeNodeTaxonomyInputProfile> input,
                                                         Set<String> tags, Object... flags) {
        List<JIPipeNodeDatabaseEntry> result = query(text, role, allowExisting, allowNew, pinnedIds);
        return result.stream()
                .filter(e -> (framework == null || framework.getId().equals(e.getFramework().getId()))
                        && (spatial == null || spatial.isEmpty() || !Collections.disjoint(spatial, e.getSpatialDimensions()))
                        && (input == null || input.isEmpty() || input.contains(e.getInputProfile()))
                        && (tags == null || tags.isEmpty() || !Collections.disjoint(tags, e.getTags())))
                .collect(Collectors.toList());
    }

    public void buildIndex() {
        getSearch().buildIndex();
        aiSearch.buildIndex();
    }

    /**
     * Gets the AI-powered search instance.
     *
     * @return the AI search
     */
    public JIPipeAINodeDatabaseSearch getAiSearch() {
        return aiSearch;
    }
}
