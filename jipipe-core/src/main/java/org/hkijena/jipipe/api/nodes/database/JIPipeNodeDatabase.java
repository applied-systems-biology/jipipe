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
import org.hkijena.jipipe.api.nodes.taxonomy.FamilyAssembler;
import org.hkijena.jipipe.api.nodes.taxonomy.NoFramework;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeQueuedRunnableExecutor;
import org.hkijena.jipipe.plugins.ai.AIApplicationSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeGeneralUIApplicationSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Allows to query nodes
 */
public class JIPipeNodeDatabase implements JIPipeNodeDatabaseSearch {

    /**
     * A query flag that indicates that the search algorithm to be used is the AI-based one (embedding-based)
     */
    public static final Object FLAG_USE_AI_SEARCH = new Object();

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
     * Cache of {@link #related(JIPipeNodeDatabaseEntry, int)} results,
     * keyed by the entry's id and the requested {@code max}. Invalidated
     * whenever the entry list is replaced ({@link #setEntries(List)}) or
     * a rebuild is scheduled ({@link #rebuildImmediately()}), i.e. on
     * every rebuild.
     * <p>
     * <b>Concurrency contract.</b> This is a best-effort cache read from
     * the UI thread and written/invalidated from several threads (UI,
     * the SwingWorker builder, the updater's Swing timer). It is
     * therefore a {@code volatile} reference to an <em>immutable</em>
     * map that is never mutated in place: invalidation swaps in a fresh
     * empty map, and a cache miss publishes its result via a
     * copy-on-write swap of a new map (see
     * {@link #cacheRelated(String, List)}). A concurrent miss may thus
     * lose another thread's just-published entry — that is acceptable:
     * a lost or momentarily stale entry is at worst recomputed, and
     * correctness never depends on the cache. What matters is
     * visibility: every thread observes either the pre-swap or the
     * post-swap map, never a half-built one.
     */
    private volatile Map<String, List<JIPipeNodeDatabaseEntry>> relatedCache = Collections.emptyMap();

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

    public void rebuildImmediately() {
        queue.cancelAll();
        // Invalidate by swapping in a fresh empty map (see the field's
        // concurrency contract); never mutate the current map in place.
        relatedCache = Collections.emptyMap();
        queue.enqueue(new JIPipeNodeDatabaseBuilderRun(this));
    }

    /**
     * The direct workflow successors (follow-ups) of the given entry:
     * the entries whose origin node id is a direct successor of the
     * entry's origin node in the taxonomy family workflow graphs
     * (see {@link org.hkijena.jipipe.api.nodes.taxonomy.FamilyAssembler#followUps(String)}).
     * <p>
     * Successor node ids are mapped back to database entries; the first
     * entry (in entry order) whose origin node id matches wins. The
     * result preserves the assembler's order and is never {@code null}.
     *
     * @param e the entry whose follow-ups are requested
     * @return an unmodifiable list of the follow-up entries, ordered;
     * empty if the entry does not wrap a node type or the node has no
     * successors (or the successor is not present in the database)
     */
    public List<JIPipeNodeDatabaseEntry> followUps(JIPipeNodeDatabaseEntry e) {
        String originNodeId = e.getOriginNodeId();
        if (originNodeId == null) {
            return Collections.emptyList();
        }
        List<String> successorNodeIds = JIPipe.getNodes().getFamilyAssembler().followUps(originNodeId);
        if (successorNodeIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<JIPipeNodeDatabaseEntry> result = new ArrayList<>(successorNodeIds.size());
        for (String successorNodeId : successorNodeIds) {
            JIPipeNodeDatabaseEntry entry = entryByOriginNodeId(successorNodeId);
            if (entry != null) {
                result.add(entry);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Entries related to the given entry, ranked by how strongly they
     * belong to its workflow neighborhood:
     * <ol>
     * <li><b>same-family siblings</b> — entries whose origin node is a
     * step in any family that the entry's origin node is a step in,
     * <i>excluding the entry's follow-ups</i> (those are surfaced by
     * {@link #followUps(JIPipeNodeDatabaseEntry)});</li>
     * <li><b>same framework</b> — entries whose
     * {@link JIPipeNodeDatabaseEntry#getFramework() framework id} equals
     * the entry's (the {@code NoFramework} sentinel never matches);</li>
     * <li><b>shared tags</b> — entries sharing taxonomy tags with the
     * entry; candidates sharing at least two tags rank before those
     * sharing exactly one.</li>
     * </ol>
     * The entry itself, and other entries representing the same node
     * (e.g. alias entries of a "create node by info" entry), are
     * excluded from the candidates. Candidates are de-duplicated (an
     * entry qualifies for several tiers at once and appears in the
     * highest one), and capped at {@code max}.
     * <p>
     * <b>Down-ranking.</b> Within each tier, entries whose
     * {@link JIPipeNodeClassification} is not {@link JIPipeNodeClassification#Standard}
     * (i.e. {@link JIPipeNodeClassification#AutoImport AutoImport} and
     * {@link JIPipeNodeClassification#EdgeCase EdgeCase}, mirroring the
     * search ranking) are sorted to the end of their tier, preserving
     * entry order otherwise.
     * <p>
     * The result is cached per entry and {@code max}: a second call with
     * the same arguments returns the same list instance. The cache is
     * invalidated whenever the entries are replaced
     * ({@link #setEntries(List)}) or a rebuild is scheduled
     * ({@link #rebuildImmediately()}).
     *
     * @param e   the entry to find related entries for
     * @param max the maximum number of results to return
     * @return the related entries, ordered by rank; empty if {@code e}
     * wraps no node type or no entry qualifies
     */
    public List<JIPipeNodeDatabaseEntry> related(JIPipeNodeDatabaseEntry e, int max) {
        if (max <= 0) {
            return Collections.emptyList();
        }
        String cacheKey = e.getId() + ":" + max;
        // Read the volatile cache reference once: this thread then works
        // with a single immutable map snapshot.
        Map<String, List<JIPipeNodeDatabaseEntry>> cache = relatedCache;
        List<JIPipeNodeDatabaseEntry> cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String originNodeId = e.getOriginNodeId();
        List<JIPipeNodeDatabaseEntry> result = new ArrayList<>();
        if (originNodeId == null) {
            List<JIPipeNodeDatabaseEntry> immutable = Collections.unmodifiableList(result);
            cacheRelated(cacheKey, immutable);
            return immutable;
        }

        FamilyAssembler assembler = JIPipe.getNodes().getFamilyAssembler();
        String selfId = e.getId();
        // Entries representing the same node (e.g. alias entries of
        // "create node by info") are never "related": a node is not
        // related to itself under a different menu path
        Predicate<JIPipeNodeDatabaseEntry> sameNode =
            entry -> selfId.equals(entry.getId()) || originNodeId.equals(entry.getOriginNodeId());
        Set<String> added = new LinkedHashSet<>();

        // Tier 1: same-family siblings, excluding follow-ups (a follow-up
        // is also a sibling, so it is removed here and surfaced by
        // followUps() instead)
        Set<String> siblingNodeIds = new LinkedHashSet<>(assembler.siblingNodeIds(originNodeId));
        siblingNodeIds.removeAll(assembler.followUps(originNodeId));
        List<JIPipeNodeDatabaseEntry> tier1 = new ArrayList<>();
        for (String siblingNodeId : siblingNodeIds) {
            JIPipeNodeDatabaseEntry entry = entryByOriginNodeId(siblingNodeId);
            if (entry != null && !sameNode.test(entry) && added.add(entry.getId())) {
                tier1.add(entry);
            }
        }

        // Tier 2: same framework (the NoFramework sentinel never matches:
        // it would make every framework-less entry a "sibling")
        List<JIPipeNodeDatabaseEntry> tier2 = new ArrayList<>();
        String frameworkId = e.getFramework().getId();
        if (!NoFramework.ID.equals(frameworkId)) {
            for (JIPipeNodeDatabaseEntry entry : entries) {
                if (frameworkId.equals(entry.getFramework().getId())
                    && !sameNode.test(entry) && added.add(entry.getId())) {
                    tier2.add(entry);
                }
            }
        }

        // Tier 3: shared tags (>=2 shared before exactly 1)
        List<JIPipeNodeDatabaseEntry> tier3 = new ArrayList<>();
        Set<String> myTags = e.getTags();
        if (!myTags.isEmpty()) {
            List<JIPipeNodeDatabaseEntry> twoShared = new ArrayList<>();
            List<JIPipeNodeDatabaseEntry> oneShared = new ArrayList<>();
            for (JIPipeNodeDatabaseEntry entry : entries) {
                if (sameNode.test(entry)) {
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
                    tier3.add(entry);
                }
            }
            for (JIPipeNodeDatabaseEntry entry : oneShared) {
                if (added.add(entry.getId())) {
                    tier3.add(entry);
                }
            }
        }

        // Merge the tiers in priority order; within each tier,
        // down-ranked (non-Standard) entries are sorted to the end
        for (List<JIPipeNodeDatabaseEntry> tier : List.of(tier1, tier2, tier3)) {
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

        List<JIPipeNodeDatabaseEntry> immutable = Collections.unmodifiableList(result);
        cacheRelated(cacheKey, immutable);
        return immutable;
    }

    /**
     * Publishes a computed {@link #related(JIPipeNodeDatabaseEntry, int)}
     * result into the cache by swapping in a copy of the current map plus
     * the new entry (copy-on-write; see the {@link #relatedCache}
     * concurrency contract — a concurrent miss may lose one of the two
     * entries, which is harmless for a best-effort cache).
     */
    private void cacheRelated(String cacheKey, List<JIPipeNodeDatabaseEntry> result) {
        Map<String, List<JIPipeNodeDatabaseEntry>> current = relatedCache;
        Map<String, List<JIPipeNodeDatabaseEntry>> updated = new LinkedHashMap<>(current);
        updated.put(cacheKey, result);
        relatedCache = Collections.unmodifiableMap(updated);
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

    public List<JIPipeNodeDatabaseEntry> getEntries() {
        return entries;
    }

    public synchronized void setEntries(List<JIPipeNodeDatabaseEntry> entries) {
        this.entries = entries;
        // Invalidate by swapping in a fresh empty map (see the field's
        // concurrency contract); never mutate the current map in place.
        relatedCache = Collections.emptyMap();
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
            return JIPipeGeneralUIApplicationSettings.getInstance().getSearchImplementation();
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

    @Override
    public List<JIPipeNodeDatabaseEntry> query(String text, JIPipeNodeDatabasePipelineVisibility role, boolean allowExisting, boolean allowNew, Set<String> pinnedIds, Object... flags) {
        if (containsFlag(flags, FLAG_USE_AI_SEARCH) && AIApplicationSettings.getInstance().isEnableAI()) {
            return aiSearch.query(text, role, allowExisting, allowNew, pinnedIds, flags);
        }
        return getSearch().query(text, role, allowExisting, allowNew, pinnedIds);
    }

    @Override
    public void confirmQuery(String text, JIPipeNodeDatabasePipelineVisibility role, boolean allowExisting, boolean allowNew, JIPipeSlotType targetSlotType, Class<? extends JIPipeData> targetDataType, JIPipeNodeDatabaseEntry userSelected) {
        getSearch().confirmQuery(text, role, allowExisting, allowNew, targetSlotType, targetDataType, userSelected);
    }

    @Override
    public List<JIPipeNodeDatabaseEntry> query(String text, JIPipeNodeDatabasePipelineVisibility role, boolean allowExisting, boolean allowNew, JIPipeSlotType targetSlotType, Class<? extends JIPipeData> targetDataType, Object... flags) {
        if (containsFlag(flags, FLAG_USE_AI_SEARCH) && AIApplicationSettings.getInstance().isEnableAI()) {
            return aiSearch.query(text, role, allowExisting, allowNew, targetSlotType, targetDataType, flags);
        }
        return getSearch().query(text, role, allowExisting, allowNew, targetSlotType, targetDataType);
    }

    public void buildIndex() {
        getSearch().buildIndex();
        aiSearch.buildIndex();
    }

    /**
     * Checks if the given flags array contains the specified flag using identity comparison.
     *
     * @param flags the flags array (may be null)
     * @param flag  the flag to search for
     * @return true if the flag is found
     */
    private boolean containsFlag(Object[] flags, Object flag) {
        if (flags == null) return false;
        for (Object f : flags) {
            if (f == flag) return true;  // Identity check for flag constants
        }
        return false;
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
