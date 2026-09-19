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
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.microservice.MicroserviceState;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeNodeClassification;
import org.hkijena.jipipe.api.nodes.database.embeddings.JIPipeEmbeddingDatabase;
import org.hkijena.jipipe.api.nodes.database.embeddings.JIPipeGlobalEmbeddingSearch;
import org.hkijena.jipipe.api.service.components.JIPipeAIServiceComponent;
import org.hkijena.jipipe.plugins.ai.AIApplicationSettings;
import org.hkijena.jipipe.plugins.ai.environments.EmbeddingModelEnvironment;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AI-powered node database search using embedding vector similarity.
 * <p>
 * This is a separate implementation from legacy/enhanced search.
 * It uses a tiered architecture:
 * <ul>
 *   <li>{@link JIPipeGlobalEmbeddingSearch} singleton for globally-cacheable entries
 *       (stable IDs like {@code create-node-by-info:*})</li>
 *   <li>A local {@link JIPipeEmbeddingDatabase} for project-specific entries
 *       (UUID-based IDs like {@code existing-pipeline-node:*})</li>
 * </ul>
 * <p>
 * Embeddings are computed JIT (just-in-time) similar to how
 * {@link JIPipeEnhancedNodeDatabaseSearch} computes {@code CandidateView} attachments.
 * <p>
 * Returns an empty list when AI is unavailable or the search fails;
 * the caller ({@link JIPipeNodeDatabase}) should NOT fall back to the standard
 * search implementation when AI mode is explicitly requested.
 */
public class JIPipeAINodeDatabaseSearch implements JIPipeNodeDatabaseSearch {

    private static final Logger LOGGER = LoggerFactory.getLogger(JIPipeAINodeDatabaseSearch.class);

    /**
     * Timeout for the entire AI search operation (seconds).
     * This includes model loading, embedding all entries, embedding the query, and scoring.
     */
    private static final long SEARCH_TOTAL_TIMEOUT_SECONDS = 300;

    /**
     * Maximum type distance to include entries when a target slot type is specified.
     * Mirrors the constant in {@link JIPipeEnhancedNodeDatabaseSearch}.
     */
    private static final int MAX_TYPE_DISTANCE_TO_INCLUDE = 64;

    /**
     * In-memory usage tracker (entry ID -> count).
     * Replace with persistent store if desired.
     */
    private static final Map<String, Long> USAGE_COUNTS = new ConcurrentHashMap<>();

    /**
     * Local embedding database for project-specific entries. Not persisted to disk.
     */
    private final JIPipeEmbeddingDatabase localEmbeddingDatabase;

    /**
     * Global embedding search singleton for stable, cacheable entries.
     */
    private final JIPipeGlobalEmbeddingSearch globalEmbeddingSearch;

    private List<JIPipeNodeDatabaseEntry> entries = new ArrayList<>();
    private String currentModelId;

    /**
     * Creates a new AI-powered node database search.
     */
    public JIPipeAINodeDatabaseSearch() {
        this.localEmbeddingDatabase = new JIPipeEmbeddingDatabase(false);
        this.globalEmbeddingSearch = JIPipeGlobalEmbeddingSearch.getInstance();
    }

    /**
     * Gets the progress info for AI embedding operations.
     * Returns the embedding progress info from the AI service if available,
     * otherwise falls back to {@link JIPipeProgressInfo#SILENT}.
     *
     * @return the progress info
     */
    private JIPipeProgressInfo getProgressInfo() {
        try {
            if (JIPipe.isInstantiated()) {
                return JIPipe.getInstance().getAiService().getEmbeddingProgressInfo();
            }
        } catch (Exception ignored) {
        }
        return JIPipeProgressInfo.SILENT;
    }

    /**
     * Sets the list of node database entries to search through.
     * The fallback search receives entries from its own {@code JIPipeNodeDatabase} reference.
     *
     * @param entries the entries
     */
    public void setEntries(List<JIPipeNodeDatabaseEntry> entries) {
        this.entries = entries != null ? new ArrayList<>(entries) : new ArrayList<>();
    }

    /**
     * Gets the current list of entries.
     *
     * @return an unmodifiable view of the entries
     */
    public List<JIPipeNodeDatabaseEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Get the local embedding database for project-specific entries.
     * This database is transient (not persisted to disk).
     *
     * @return the local embedding database
     */
    public JIPipeEmbeddingDatabase getLocalEmbeddingDatabase() {
        return localEmbeddingDatabase;
    }

    /**
     * Get the global embedding search singleton.
     *
     * @return the global embedding search
     */
    public JIPipeGlobalEmbeddingSearch getGlobalEmbeddingSearch() {
        return globalEmbeddingSearch;
    }


    /**
     * Gets the current model ID used by this search instance.
     * This may be non-null even if the AI service model is not currently loaded,
     * if embeddings were previously computed.
     *
     * @return the current model ID, or null if not set
     */
    public String getCurrentModelId() {
        return currentModelId;
    }

    @Override
    public void confirmQuery(String text, JIPipeNodeDatabasePipelineVisibility role, boolean allowExisting, boolean allowNew, Set<String> pinnedIds, JIPipeNodeDatabaseEntry userSelected) {
        updateUsageCount(userSelected);
    }

    @Override
    public List<JIPipeNodeDatabaseEntry> query(String text, JIPipeNodeDatabasePipelineVisibility role, boolean allowExisting, boolean allowNew, Set<String> pinnedIds, Object... flags) {
        List<JIPipeNodeDatabaseEntry> result = internalQuery(text, role, allowExisting, allowNew, pinnedIds, null, null);
        return result != null ? result : Collections.emptyList();
    }

    @Override
    public void confirmQuery(String text, JIPipeNodeDatabasePipelineVisibility role, boolean allowExisting, boolean allowNew, JIPipeSlotType targetSlotType, Class<? extends JIPipeData> targetDataType, JIPipeNodeDatabaseEntry userSelected) {
        updateUsageCount(userSelected);
    }

    @Override
    public List<JIPipeNodeDatabaseEntry> query(String text, JIPipeNodeDatabasePipelineVisibility role, boolean allowExisting, boolean allowNew, JIPipeSlotType targetSlotType, Class<? extends JIPipeData> targetDataType, Object... flags) {
        List<JIPipeNodeDatabaseEntry> result = internalQuery(text, role, allowExisting, allowNew, Collections.emptySet(), targetSlotType, targetDataType);
        return result != null ? result : Collections.emptyList();
    }

    @Override
    public void buildIndex() {
        // Check if AI is available
        if (!isAIAvailable()) {
            LOGGER.debug("AI not available, skipping embedding index build");
            return;
        }

        JIPipeProgressInfo progressInfo = getProgressInfo();
        try {
            String modelId = resolveCurrentModelId();
            if (modelId == null) {
                progressInfo.warn("Could not resolve model ID, skipping embedding index build");
                return;
            }

            // Handle model change for local database
            if (!Objects.equals(currentModelId, modelId)) {
                if (currentModelId != null) {
                    localEmbeddingDatabase.clearModel(currentModelId);
                }
                currentModelId = modelId;
            }

            // Initialize the global database (loads resource + user cache)
            globalEmbeddingSearch.initialize(modelId, progressInfo);

            // No cache loading needed for local database (transient)
            EntrySplit split = splitEntries(entries);
            progressInfo.log("AI search index built. Model: " + modelId
                    + ", Global entries: " + split.global.size()
                    + ", Local entries: " + split.local.size());
        } catch (Exception e) {
            progressInfo.warn("Failed to build AI search index");
            LOGGER.debug("Failed to build AI search index", e);
        }
    }

    /**
     * Compute cosine similarity between two vectors.
     * Handles edge cases (null vectors, zero vectors, different lengths) by returning 0.
     *
     * @param a the first vector
     * @param b the second vector
     * @return the cosine similarity in range [-1, 1], or 0 if inputs are invalid
     */
    public static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // ===== Entry Classification =====

    /**
     * Determine if an entry is globally cacheable (stable ID, not project-specific).
     * Global entries are stored in the application-wide singleton database.
     * Local entries are stored in the per-project transient database.
     *
     * @param entry the node database entry
     * @return true if the entry should be stored in the global database
     */
    public static boolean isGlobalEntry(JIPipeNodeDatabaseEntry entry) {
        String id = entry.getId();
        if (id == null) return false;
        return id.startsWith("create-node-by-info:") ||
               id.startsWith("create-node-by-example:") ||
               id.startsWith("create-node-custom:");
    }

    /**
     * Split a list of entries into global and local groups.
     *
     * @param entries the entries to split
     * @return an {@link EntrySplit} containing the two groups
     */
    private static EntrySplit splitEntries(List<JIPipeNodeDatabaseEntry> entries) {
        List<JIPipeNodeDatabaseEntry> global = new ArrayList<>();
        List<JIPipeNodeDatabaseEntry> local = new ArrayList<>();
        for (JIPipeNodeDatabaseEntry entry : entries) {
            if (isGlobalEntry(entry)) {
                global.add(entry);
            } else {
                local.add(entry);
            }
        }
        return new EntrySplit(global, local);
    }

    // ===== Internal Methods =====

    /**
     * Internal query implementation that handles both query overloads.
     * <p>
     * Returns {@code null} if AI is unavailable or the search fails,
     * allowing the caller to fall back to the standard search.
     * <p>
     * The entire AI search operation (ensure embeddings + query embedding + scoring)
     * is submitted as a single task on the AI service queue via
     * {@link JIPipeAIServiceComponent#submitEmbedFunction}. This avoids flooding
     * the serial AI queue with individual embed tasks (which caused freezes when
     * the search was cancelled/superseded but the enqueued tasks remained).
     */
    private List<JIPipeNodeDatabaseEntry> internalQuery(String text,
                                                         JIPipeNodeDatabasePipelineVisibility role,
                                                         boolean allowExisting,
                                                         boolean allowNew,
                                                         Set<String> pinnedIds,
                                                         JIPipeSlotType targetSlotType,
                                                         Class<? extends JIPipeData> targetDataType) {
        // Check if AI is available
        if (!isAIAvailable()) {
            return null;
        }

        // Pre-filter by visibility and existence (same logic as enhanced search)
        List<JIPipeNodeDatabaseEntry> candidates = entries.stream()
                .filter(e -> e.getVisibility().matches(role))
                .filter(e -> allowExisting || !e.exists())
                .filter(e -> allowNew || e.exists())
                .collect(Collectors.toList());

        // Type distance filter if a target slot type is specified
        if (targetSlotType != null && targetDataType != null) {
            candidates = candidates.stream()
                    .filter(e -> bestTypeDistance(e, targetSlotType, targetDataType) <= MAX_TYPE_DISTANCE_TO_INCLUDE)
                    .collect(Collectors.toList());
        }

        // Empty query: return all filtered candidates sorted by pinned status, usage, and name
        if (StringUtils.isNullOrEmpty(text) || text.isBlank()) {
            candidates.sort(Comparator
                    .comparing((JIPipeNodeDatabaseEntry e) -> !(pinnedIds != null && pinnedIds.contains(e.getId())))
                    .thenComparing((JIPipeNodeDatabaseEntry e) -> -USAGE_COUNTS.getOrDefault(e.getId(), 0L))
                    .thenComparing(JIPipeNodeDatabaseEntry::getName, String.CASE_INSENSITIVE_ORDER));
            return candidates;
        }

        // AI search path
        JIPipeProgressInfo progressInfo = getProgressInfo();
        try {
            String modelId = resolveCurrentModelId();
            if (modelId == null) {
                progressInfo.warn("Could not resolve model ID, returning null");
                return null;
            }

            // Handle model change for local database
            if (!Objects.equals(currentModelId, modelId)) {
                if (currentModelId != null) {
                    localEmbeddingDatabase.clearModel(currentModelId);
                }
                currentModelId = modelId;
            }

            // Capture the search text for use inside the queue task
            final String searchText = text;
            final List<JIPipeNodeDatabaseEntry> candidatesFinal = candidates;
            final String modelIdFinal = modelId;
            final JIPipeProgressInfo progressInfoFinal = progressInfo;
            final Set<String> pinnedIdsFinal = pinnedIds;

            // Submit the entire embedding + scoring operation as a single task on the AI service queue.
            // This runs on the queue thread where embedNow() is safe, and avoids enqueuing
            // hundreds of individual embed tasks that would clog the serial queue.
            JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();

            CompletableFuture<List<JIPipeNodeDatabaseEntry>> searchFuture = aiService.submitEmbedFunction(embedFn -> {
                // === This code runs on the AI service queue thread ===

                // The model should already be loaded at this point, because submitEmbedFunction
                // enqueues a LoadEmbeddingModelTask before this task (if needed), and the queue
                // is serial, so the load completes before this task starts.
                // We still check the status for safety, but do NOT call tryStartEmbeddingModel()
                // from here (that would enqueue a load task after this task on the serial queue,
                // creating a deadlock).
                MicroserviceState status = aiService.getEmbeddingModelStatus();
                if (status != MicroserviceState.Ready) {
                    progressInfoFinal.warn("Embedding model not ready (status=" + status + ") when embed function started");
                    return null;
                }

                if (Thread.currentThread().isInterrupted()) {
                    LOGGER.debug("AI search was interrupted/cancelled inside embed function");
                    return null;
                }

                // Split entries into global and local tiers
                EntrySplit split = splitEntries(candidatesFinal);

                // Ensure embeddings for global entries via the singleton
                globalEmbeddingSearch.ensureEmbeddingsForEntriesWithEmbedder(
                        split.global, modelIdFinal, embedFn, progressInfoFinal);

                if (Thread.currentThread().isInterrupted()) {
                    LOGGER.debug("AI search was interrupted/cancelled after global embeddings");
                    return null;
                }

                // Ensure embeddings for local entries via the local database
                localEmbeddingDatabase.ensureEmbeddingsForEntriesWithEmbedder(
                        split.local, modelIdFinal, embedFn, progressInfoFinal);

                if (Thread.currentThread().isInterrupted()) {
                    LOGGER.debug("AI search was interrupted/cancelled after local embeddings");
                    return null;
                }

                // Embed the search query text synchronously
                float[] queryEmbedding = embedFn.apply(searchText);
                if (queryEmbedding == null) {
                    progressInfoFinal.warn("Query embedding is null, returning null");
                    return null;
                }

                // Score each candidate by cosine similarity using the appropriate tier
                List<ScoredEntry> scored = new ArrayList<>(candidatesFinal.size());
                for (JIPipeNodeDatabaseEntry entry : candidatesFinal) {
                    String nodeId = JIPipeEmbeddingDatabase.entryToId(entry);
                    float[] entryEmbedding;

                    if (isGlobalEntry(entry)) {
                        entryEmbedding = globalEmbeddingSearch.getVerifiedEmbedding(
                                modelIdFinal, nodeId, entry);
                    } else {
                        String entryText = JIPipeEmbeddingDatabase.entryToText(entry);
                        entryEmbedding = localEmbeddingDatabase.getVerifiedEmbedding(
                                modelIdFinal, nodeId, entryText);
                    }

                    double similarity;
                    if (entryEmbedding != null) {
                        similarity = cosineSimilarity(queryEmbedding, entryEmbedding);
                    } else {
                        similarity = -1.0;
                    }

                    // Apply classification penalty
                    JIPipeNodeClassification classification = entry.getNodeClassification();
                    if (classification == JIPipeNodeClassification.AutoImport) {
                        similarity *= 0.85;
                    } else if (classification == JIPipeNodeClassification.EdgeCase) {
                        similarity *= 0.7;
                    }

                    boolean isPinned = pinnedIdsFinal != null && pinnedIdsFinal.contains(entry.getId());
                    scored.add(new ScoredEntry(entry, similarity, isPinned));
                }

                // Sort: pinned first, then by similarity (descending), then by usage, then by name
                scored.sort(Comparator
                        .comparing((ScoredEntry se) -> !se.pinned)
                        .thenComparing((ScoredEntry se) -> -se.similarity)
                        .thenComparing((ScoredEntry se) -> -USAGE_COUNTS.getOrDefault(se.entry.getId(), 0L))
                        .thenComparing(se -> se.entry.getName(), String.CASE_INSENSITIVE_ORDER));

                return scored.stream()
                        .map(se -> se.entry)
                        .collect(Collectors.toList());
            });

            if (searchFuture == null) {
                progressInfo.warn("AI service returned null future (AI disabled?), returning null");
                return null;
            }

            // Block on the result with a generous timeout (model load + all embeddings + query)
            List<JIPipeNodeDatabaseEntry> result = searchFuture.get(SEARCH_TOTAL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.debug("AI search was interrupted/cancelled");
            return null;
        } catch (TimeoutException e) {
            progressInfo.warn("Timeout during AI search, returning null");
            return null;
        } catch (Exception e) {
            progressInfo.warn("AI search failed, returning null");
            LOGGER.debug("AI search failed", e);
            return null;
        }
    }

    /**
     * Check if AI search is available (enabled in settings and model is configured).
     *
     * @return true if AI search can be used
     */
    private boolean isAIAvailable() {
        try {
            if (!JIPipe.isInstantiated()) {
                return false;
            }
            AIApplicationSettings settings = AIApplicationSettings.getInstance();
            if (!settings.isEnableAI()) {
                return false;
            }
            JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
            return aiService.hasConfiguredAndReadyEmbeddingModel();
        } catch (Exception e) {
            LOGGER.debug("Error checking AI availability", e);
            return false;
        }
    }

    /**
     * Resolve the current model ID without starting the model.
     * <p>
     * If the model is already loaded/running, returns its ID directly.
     * Otherwise, derives the model ID from the current settings configuration
     * via {@link EmbeddingModelEnvironment#deriveModelId()}.
     * <p>
     * This method does NOT start the embedding model. The model is only started
     * JIT when an actual embedding is needed (via {@link JIPipeAIServiceComponent#tryEmbed(String)}).
     *
     * @return the model ID, or null if it cannot be determined
     */
    private String resolveCurrentModelId() {
        try {
            // If the model is already loaded, use its ID directly
            JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
            String modelId = aiService.getModelId();
            if (modelId != null) {
                return modelId;
            }

            // Model not loaded — derive the ID from settings without starting the model
            AIApplicationSettings settings = AIApplicationSettings.getInstance();
            EmbeddingModelEnvironment environment = settings.getEmbeddingModelEnvironment();
            return environment.deriveModelId();
        } catch (Exception e) {
            LOGGER.debug("Error resolving model ID", e);
            return null;
        }
    }

    /**
     * Update the usage count for a selected entry.
     *
     * @param userSelected the entry selected by the user
     */
    private void updateUsageCount(JIPipeNodeDatabaseEntry userSelected) {
        if (userSelected != null && !StringUtils.isNullOrEmpty(userSelected.getId())) {
            USAGE_COUNTS.merge(userSelected.getId(), 1L, Long::sum);
        }
    }

    /**
     * Compute the best (minimum) type distance between an entry's slots and a target data type.
     * <p>
     * When looking for an input slot match, we check the entry's output slots
     * (because the entry's output connects to the target input).
     * When looking for an output slot match, we check the entry's input slots
     * (because the target output connects to the entry's input).
     *
     * @param entry          the node database entry
     * @param targetSlotType the target slot type
     * @param targetDataType the target data type
     * @return the best (minimum) type distance, or {@link Integer#MAX_VALUE} if no compatible slots
     */
    private static int bestTypeDistance(JIPipeNodeDatabaseEntry entry, JIPipeSlotType targetSlotType, Class<? extends JIPipeData> targetDataType) {
        int best = Integer.MAX_VALUE;
        Map<String, JIPipeDataSlotInfo> map = (targetSlotType == JIPipeSlotType.Input) ? entry.getOutputSlots() : entry.getInputSlots();
        if (map == null) {
            return Integer.MAX_VALUE;
        }
        for (Map.Entry<String, JIPipeDataSlotInfo> si : map.entrySet()) {
            int d = dataTypeDistance(si.getValue().getDataClass(), targetDataType);
            if (d >= 0 && d < best) best = d;
        }
        return best;
    }

    /**
     * Compute the type distance between two data types.
     * Mirrors the logic in {@link JIPipeEnhancedNodeDatabaseSearch}.
     *
     * @param from the source data type
     * @param to   the target data type
     * @return the type distance (0 = same type, higher = further away)
     */
    private static int dataTypeDistance(Class<? extends JIPipeData> from, Class<? extends JIPipeData> to) {
        if (from == to) {
            return 0;
        } else if (to.isAssignableFrom(from)) {
            return ReflectionUtils.getClassDistance(to, from);
        } else {
            return JIPipe.getDataTypes().getConversionDistance(from, to) * 5;
        }
    }

    // ===== Inner Classes =====

    /**
     * Helper to hold split entry lists.
     */
    private static class EntrySplit {
        final List<JIPipeNodeDatabaseEntry> global;
        final List<JIPipeNodeDatabaseEntry> local;

        EntrySplit(List<JIPipeNodeDatabaseEntry> global, List<JIPipeNodeDatabaseEntry> local) {
            this.global = global;
            this.local = local;
        }
    }

    /**
     * Helper class to hold a scored entry during ranking.
     */
    private static class ScoredEntry {
        final JIPipeNodeDatabaseEntry entry;
        final double similarity;
        final boolean pinned;

        ScoredEntry(JIPipeNodeDatabaseEntry entry, double similarity, boolean pinned) {
            this.entry = entry;
            this.similarity = similarity;
            this.pinned = pinned;
        }
    }
}
