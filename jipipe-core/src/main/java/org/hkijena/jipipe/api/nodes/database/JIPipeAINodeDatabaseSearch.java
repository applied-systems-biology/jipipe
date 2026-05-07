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
import org.hkijena.jipipe.api.ai.JIPipeAIModelRunnerStatus;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.database.embeddings.JIPipeEmbeddingDatabase;
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
import java.util.stream.Collectors;

/**
 * AI-powered node database search using embedding vector similarity.
 * <p>
 * This is a separate implementation from legacy/enhanced search.
 * It uses {@link JIPipeEmbeddingDatabase} for cached embeddings and
 * {@link JIPipeAIServiceComponent} for computing new embeddings.
 * <p>
 * Embeddings are computed JIT (just-in-time) similar to how
 * {@link JIPipeEnhancedNodeDatabaseSearch} computes {@code CandidateView} attachments.
 * <p>
 * Returns {@code null} when AI is unavailable or the search fails;
 * the caller ({@link JIPipeNodeDatabase}) is responsible for falling back
 * to the standard search implementation.
 */
public class JIPipeAINodeDatabaseSearch implements JIPipeNodeDatabaseSearch {

    private static final Logger LOGGER = LoggerFactory.getLogger(JIPipeAINodeDatabaseSearch.class);

    /**
     * Timeout for embedding the search query text (seconds).
     * This is the time allowed for the embedding computation itself,
     * after the model is already loaded and idle.
     */
    private static final long QUERY_EMBED_TIMEOUT_SECONDS = 30;

    /**
     * Timeout for waiting for the embedding model to load (seconds).
     * ONNX models can take significant time to load, so this is generous.
     */
    private static final long MODEL_LOAD_TIMEOUT_SECONDS = 120;

    /**
     * Polling interval when waiting for the model to load (milliseconds).
     */
    private static final long MODEL_LOAD_POLL_INTERVAL_MS = 500;

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

    private final JIPipeEmbeddingDatabase embeddingDatabase;

    private List<JIPipeNodeDatabaseEntry> entries = new ArrayList<>();
    private String currentModelId;

    /**
     * Creates a new AI-powered node database search.
     */
    public JIPipeAINodeDatabaseSearch() {
        this.embeddingDatabase = new JIPipeEmbeddingDatabase();
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
     * Gets the embedding database used by this search instance.
     *
     * @return the embedding database
     */
    public JIPipeEmbeddingDatabase getEmbeddingDatabase() {
        return embeddingDatabase;
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
        return internalQuery(text, role, allowExisting, allowNew, pinnedIds, null, null);
    }

    @Override
    public void confirmQuery(String text, JIPipeNodeDatabasePipelineVisibility role, boolean allowExisting, boolean allowNew, JIPipeSlotType targetSlotType, Class<? extends JIPipeData> targetDataType, JIPipeNodeDatabaseEntry userSelected) {
        updateUsageCount(userSelected);
    }

    @Override
    public List<JIPipeNodeDatabaseEntry> query(String text, JIPipeNodeDatabasePipelineVisibility role, boolean allowExisting, boolean allowNew, JIPipeSlotType targetSlotType, Class<? extends JIPipeData> targetDataType, Object... flags) {
        return internalQuery(text, role, allowExisting, allowNew, Collections.emptySet(), targetSlotType, targetDataType);
    }

    @Override
    public void buildIndex() {
        // Check if AI is available
        if (!isAIAvailable()) {
            LOGGER.debug("AI not available, skipping embedding index build");
            return;
        }

        try {
            String modelId = resolveCurrentModelId();
            if (modelId == null) {
                LOGGER.warn("Could not resolve model ID, skipping embedding index build");
                return;
            }

            // Check if model changed since last build
            if (!Objects.equals(currentModelId, modelId)) {
                LOGGER.info("Model ID changed from {} to {}, clearing old embeddings", currentModelId, modelId);
                if (currentModelId != null) {
                    embeddingDatabase.clearModel(currentModelId);
                }
                currentModelId = modelId;
            }

            // Only load cached embeddings (bundled resource + user disk cache).
            // Do NOT compute new embeddings here — that is done JIT in internalQuery()
            // or on demand via the manual BuildAIEmbeddingIndexTool.
            embeddingDatabase.loadUserCache(modelId);

            LOGGER.info("Loaded AI search index with {} cached entries for model {}", entries.size(), modelId);
        } catch (Exception e) {
            LOGGER.warn("Failed to build AI search index, falling back to enhanced search", e);
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

    // ===== Internal Methods =====

    /**
     * Internal query implementation that handles both query overloads.
     * <p>
     * Returns {@code null} if AI is unavailable or the search fails,
     * allowing the caller to fall back to the standard search.
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
        try {
            String modelId = resolveCurrentModelId();
            if (modelId == null) {
                LOGGER.warn("Could not resolve model ID, returning null");
                return null;
            }

            // Check if model changed since last query
            if (!Objects.equals(currentModelId, modelId)) {
                LOGGER.info("Model ID changed from {} to {}, reloading embeddings", currentModelId, modelId);
                if (currentModelId != null) {
                    embeddingDatabase.clearModel(currentModelId);
                }
                currentModelId = modelId;
                embeddingDatabase.loadUserCache(modelId);
            }

            // Get the AI service and ensure the embedding model is loaded before computing anything.
            // This is critical: tryEmbed() auto-starts the model, but the CompletableFuture it returns
            // won't complete until the model finishes loading AND the embedding is computed.
            // If the model isn't loaded yet, the 30s embed timeout expires before loading finishes,
            // causing InterruptedException. We must wait for the model to be ready first.
            JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
            if (!waitForEmbeddingModelReady(aiService)) {
                LOGGER.warn("AI model failed to load within timeout, falling back to standard search");
                return null;
            }

            // Ensure candidates have embeddings (JIT computation — only for filtered entries, not all)
            embeddingDatabase.ensureEmbeddingsForEntries(candidates, modelId, aiService);

            // Embed the search query text
            CompletableFuture<float[]> queryFuture = aiService.tryEmbed(text);
            if (queryFuture == null) {
                LOGGER.warn("AI service returned null future, returning null");
                return null;
            }

            float[] queryEmbedding = queryFuture.get(QUERY_EMBED_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (queryEmbedding == null) {
                LOGGER.warn("Query embedding is null, returning null");
                return null;
            }

            // Score each candidate by cosine similarity
            List<ScoredEntry> scored = new ArrayList<>(candidates.size());
            for (JIPipeNodeDatabaseEntry entry : candidates) {
                String nodeId = JIPipeEmbeddingDatabase.entryToId(entry);
                float[] entryEmbedding = embeddingDatabase.getEmbedding(modelId, nodeId);

                double similarity;
                if (entryEmbedding != null) {
                    similarity = cosineSimilarity(queryEmbedding, entryEmbedding);
                } else {
                    // No embedding available for this entry, assign low score
                    similarity = -1.0;
                }

                boolean isPinned = pinnedIds != null && pinnedIds.contains(entry.getId());
                scored.add(new ScoredEntry(entry, similarity, isPinned));
            }

            // Sort: pinned first, then by similarity (descending), then by usage, then by name
            scored.sort(Comparator
                    .comparing((ScoredEntry se) -> !se.pinned)
                    .thenComparing((ScoredEntry se) -> -se.similarity)
                    .thenComparing((ScoredEntry se) -> -USAGE_COUNTS.getOrDefault(se.entry.getId(), 0L))
                    .thenComparing(se -> se.entry.getName(), String.CASE_INSENSITIVE_ORDER));

            // Extract entries from scored results
            return scored.stream()
                    .map(se -> se.entry)
                    .collect(Collectors.toList());

        } catch (TimeoutException e) {
            LOGGER.warn("Timeout embedding search query, returning null");
            return null;
        } catch (Exception e) {
            LOGGER.warn("AI search failed, returning null", e);
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
     * Wait for the embedding model to be ready (Idle or Busy status).
     * <p>
     * If the model is not currently loaded, this method triggers the model start
     * via {@link JIPipeAIServiceComponent#tryStartEmbeddingModel()} and then polls
     * the model status until it reaches {@link JIPipeAIModelRunnerStatus#Idle} or
     * {@link JIPipeAIModelRunnerStatus#Busy}, or until the timeout expires.
     * <p>
     * This is safe to call from a background thread (which is where
     * {@link #internalQuery} runs via {@code JIPipeRunnableWorker}).
     *
     * @param aiService the AI service component
     * @return true if the model is ready, false if it failed to load or timed out
     */
    private boolean waitForEmbeddingModelReady(JIPipeAIServiceComponent aiService) {
        JIPipeAIModelRunnerStatus status = aiService.getEmbeddingModelStatus();

        // Already ready?
        if (status == JIPipeAIModelRunnerStatus.Idle || status == JIPipeAIModelRunnerStatus.Busy) {
            return true;
        }

        // Model needs to be started — trigger the load
        LOGGER.info("Embedding model not loaded (status={}), starting it now", status);
        aiService.tryStartEmbeddingModel();

        // Poll until ready, failed, or timeout
        long deadline = System.currentTimeMillis() + MODEL_LOAD_TIMEOUT_SECONDS * 1000;
        while (System.currentTimeMillis() < deadline) {
            status = aiService.getEmbeddingModelStatus();

            if (status == JIPipeAIModelRunnerStatus.Idle || status == JIPipeAIModelRunnerStatus.Busy) {
                LOGGER.info("Embedding model is now ready (status={})", status);
                return true;
            }

            if (status == JIPipeAIModelRunnerStatus.Failed) {
                String error = aiService.getEmbeddingModelError();
                LOGGER.warn("Embedding model failed to load: {}", error);
                return false;
            }

            // Still loading or unloading — wait and retry
            try {
                Thread.sleep(MODEL_LOAD_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warn("Interrupted while waiting for embedding model to load");
                return false;
            }
        }

        LOGGER.warn("Embedding model did not become ready within {} seconds (status={})",
                MODEL_LOAD_TIMEOUT_SECONDS, aiService.getEmbeddingModelStatus());
        return false;
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
