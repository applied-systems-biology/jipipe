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

package org.hkijena.jipipe.api.nodes.database.embeddings;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.service.components.JIPipeAIServiceComponent;

import java.util.List;
import java.util.Objects;

/**
 * Application-wide singleton that holds one {@link JIPipeEmbeddingDatabase} for
 * globally-cacheable node entries (those with stable, non-UUID IDs).
 * <p>
 * Initialized lazily on first access. Loads bundled resource + user disk cache.
 * Thread-safe via synchronized initialization.
 * <p>
 * Project-local entries (UUID-based IDs, template entries) are NOT stored here;
 * they belong in the per-project transient {@link JIPipeEmbeddingDatabase}.
 */
public class JIPipeGlobalEmbeddingSearch {

    private static JIPipeGlobalEmbeddingSearch INSTANCE;

    private final JIPipeEmbeddingDatabase embeddingDatabase;
    private volatile String currentModelId;
    private volatile boolean initialized = false;

    private JIPipeGlobalEmbeddingSearch() {
        this.embeddingDatabase = new JIPipeEmbeddingDatabase();
        // Global database is persistent (saves to disk)
    }

    /**
     * Get the singleton instance. Does not trigger initialization.
     *
     * @return the singleton instance
     */
    public static synchronized JIPipeGlobalEmbeddingSearch getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JIPipeGlobalEmbeddingSearch();
        }
        return INSTANCE;
    }

    /**
     * Initialize the global embedding database for the given model ID.
     * Loads bundled resource + user disk cache if not already loaded.
     * Handles model ID changes by clearing old model data.
     * <p>
     * Thread-safe: synchronized to prevent concurrent initialization.
     *
     * @param modelId      the model ID to initialize for
     * @param progressInfo the progress info for logging
     */
    public synchronized void initialize(String modelId, JIPipeProgressInfo progressInfo) {
        if (initialized && Objects.equals(currentModelId, modelId)) {
            return;  // Already initialized for this model
        }

        // Handle model change
        if (currentModelId != null && !Objects.equals(currentModelId, modelId)) {
            progressInfo.log("Global embedding search: model changed from " + currentModelId
                    + " to " + modelId + ", clearing old data");
            embeddingDatabase.clearModel(currentModelId);
        }

        currentModelId = modelId;
        embeddingDatabase.loadUserCache(modelId, progressInfo);
        initialized = true;

        progressInfo.log("Global embedding search initialized for model " + modelId
                + " with " + embeddingDatabase.getNodeIds(modelId).size() + " cached entries");
    }

    /**
     * Ensure embeddings exist for the given global entries, computing them JIT if needed.
     * Initializes the database if not already done.
     *
     * @param entries      global entries to ensure embeddings for
     * @param modelId      the model ID
     * @param aiService    the AI service for computing embeddings
     * @param progressInfo the progress info for logging
     */
    public void ensureEmbeddingsForEntries(List<JIPipeNodeDatabaseEntry> entries, String modelId,
                                           JIPipeAIServiceComponent aiService,
                                           JIPipeProgressInfo progressInfo) {
        initialize(modelId, progressInfo);
        embeddingDatabase.ensureEmbeddingsForEntries(entries, modelId, aiService, progressInfo);
    }

    /**
     * Get an embedding from the global database.
     * Does NOT perform hash verification; use {@link #getVerifiedEmbedding} for that.
     *
     * @param modelId the model ID
     * @param nodeId  the node ID
     * @return the embedding vector, or null if not found
     */
    public float[] getEmbedding(String modelId, String nodeId) {
        return embeddingDatabase.getEmbedding(modelId, nodeId);
    }

    /**
     * Get a verified embedding from the global database.
     * Performs hash verification if the entry is unverified.
     *
     * @param modelId the model ID
     * @param nodeId  the node ID
     * @param entry   the entry for hash computation
     * @return the embedding vector, or null if stale or missing
     */
    public float[] getVerifiedEmbedding(String modelId, String nodeId,
                                         JIPipeNodeDatabaseEntry entry) {
        String text = JIPipeEmbeddingDatabase.entryToText(entry);
        return embeddingDatabase.getVerifiedEmbedding(modelId, nodeId, text);
    }

    /**
     * Check if an embedding exists in the global database.
     *
     * @param modelId the model ID
     * @param nodeId  the node ID
     * @return true if an embedding exists
     */
    public boolean hasEmbedding(String modelId, String nodeId) {
        return embeddingDatabase.hasEmbedding(modelId, nodeId);
    }

    /**
     * Save all dirty embeddings to disk. Called from shutdown hook.
     */
    public void saveAllDirty() {
        embeddingDatabase.saveAllDirty();
    }

    /**
     * Get the underlying embedding database.
     * Used by tools like {@link org.hkijena.jipipe.plugins.ai.tools.BuildAIEmbeddingIndexTool}
     * and {@link org.hkijena.jipipe.plugins.ai.tools.ExportGlobalEmbeddingDatabaseTool}.
     *
     * @return the embedding database
     */
    public JIPipeEmbeddingDatabase getEmbeddingDatabase() {
        return embeddingDatabase;
    }

    /**
     * Get the current model ID.
     *
     * @return the current model ID, or null if not initialized
     */
    public String getCurrentModelId() {
        return currentModelId;
    }

    /**
     * Check if the global database has been initialized.
     *
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
}
