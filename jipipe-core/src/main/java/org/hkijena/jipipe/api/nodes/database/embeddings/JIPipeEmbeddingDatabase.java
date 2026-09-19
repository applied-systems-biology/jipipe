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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.service.components.JIPipeAIServiceComponent;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manages embedding vectors for node database entries.
 * Supports loading from resource files, disk caching, and JIT computation.
 * All data is keyed by model ID to distinguish embeddings from different models.
 *
 * <p>The database supports hash verification to detect stale embeddings (e.g., when a node's
 * description changes). Embeddings loaded from cache are marked as {@link VerificationState#UNVERIFIED}
 * and are verified on first access via {@link #getVerifiedEmbedding}. Embeddings from bundled
 * resources are marked as {@link VerificationState#PRE_VERIFIED} and skip verification.</p>
 *
 * <p>The {@code persistent} flag controls whether embeddings are saved to disk. Non-persistent
 * databases (e.g., project-specific ones) are never written to the user cache directory.</p>
 */
public class JIPipeEmbeddingDatabase {

    /**
     * Verification state for cached embeddings.
     */
    public enum VerificationState {
        /** Loaded from cache, needs hash check */
        UNVERIFIED,
        /** Hash checked and matches */
        VERIFIED,
        /** From bundled resource, skip verification */
        PRE_VERIFIED
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(JIPipeEmbeddingDatabase.class);

    private static final String MAGIC = "JEMB";
    private static final int CURRENT_VERSION = 2;
    private static final String BUNDLED_RESOURCE_PATH = "ai/embeddings.db";
    private static final long EMBED_TIMEOUT_SECONDS = 30;

    // Inner storage: modelId -> (nodeId -> embedding vector)
    private final Map<String, Map<String, float[]>> embeddings = new ConcurrentHashMap<>();

    // Hash storage: modelId -> (nodeId -> SHA-256 hash of the text used to generate the embedding)
    private final Map<String, Map<String, String>> hashes = new ConcurrentHashMap<>();

    // Verification state: modelId -> (nodeId -> verification state)
    private final Map<String, Map<String, VerificationState>> verificationStates = new ConcurrentHashMap<>();

    // The model ID this database is currently associated with (for convenience)
    private String activeModelId;

    // Dimension of embeddings for each model
    private final Map<String, Integer> dimensions = new ConcurrentHashMap<>();

    // Tracks which model IDs have had their cache (bundled resource + user disk) loaded.
    // This prevents ensureEmbeddingsForEntries() from computing embeddings that are
    // already available in the cache but haven't been loaded yet.
    private final Set<String> cacheLoadedModels = ConcurrentHashMap.newKeySet();

    // Tracks which model IDs have unsaved (dirty) embeddings that need to be persisted to disk.
    private final Set<String> dirtyModels = ConcurrentHashMap.newKeySet();

    // Lock for thread-safe disk operations
    private final ReentrantReadWriteLock diskLock = new ReentrantReadWriteLock();

    // If false, never save to disk
    private final boolean persistent;

    /**
     * Creates a new persistent embedding database.
     * This is equivalent to {@code JIPipeEmbeddingDatabase(true)}.
     */
    public JIPipeEmbeddingDatabase() {
        this(true);
    }

    /**
     * Creates a new embedding database.
     *
     * @param persistent if false, embeddings are never saved to disk
     */
    public JIPipeEmbeddingDatabase(boolean persistent) {
        this.persistent = persistent;
    }

    // ===== Resource and Disk I/O =====

    /**
     * Load pre-computed embeddings from a classpath resource (the bundled file).
     * Uses the binary format described in the class documentation.
     * Merges into the embeddings map under the given modelId.
     * Entries loaded from resources are marked as {@link VerificationState#PRE_VERIFIED}.
     *
     * @param resourcePath the plugin-internal resource path (e.g., "ai/embeddings.db")
     * @param modelId      the model ID to associate the loaded embeddings with
     * @param progressInfo the progress info for logging
     */
    public void loadFromResource(String resourcePath, String modelId, JIPipeProgressInfo progressInfo) {
        Objects.requireNonNull(resourcePath, "Resource path must not be null");
        Objects.requireNonNull(modelId, "Model ID must not be null");
        Objects.requireNonNull(progressInfo, "Progress info must not be null");

        try (InputStream is = ResourceUtils.getPluginResourceAsStream(resourcePath)) {
            if (is == null) {
                progressInfo.warn("Resource not found: " + resourcePath);
                return;
            }
            loadFromStream(is, modelId, progressInfo, true);
        } catch (IOException e) {
            progressInfo.error("Failed to load embeddings from resource: " + resourcePath);
            progressInfo.log(e);
        }
    }

    /**
     * Load embeddings from a file on disk (user cache).
     * Uses the binary format described in the class documentation.
     * Merges into the embeddings map.
     * Entries loaded from disk are marked as {@link VerificationState#UNVERIFIED}.
     *
     * @param path         the file path to load from
     * @param modelId      the model ID to associate the loaded embeddings with
     * @param progressInfo the progress info for logging
     */
    public void loadFromDisk(Path path, String modelId, JIPipeProgressInfo progressInfo) {
        Objects.requireNonNull(path, "Path must not be null");
        Objects.requireNonNull(modelId, "Model ID must not be null");
        Objects.requireNonNull(progressInfo, "Progress info must not be null");

        if (!Files.exists(path)) {
            LOGGER.debug("Disk cache file does not exist: {}", path);
            return;
        }

        diskLock.readLock().lock();
        try (InputStream is = Files.newInputStream(path)) {
            loadFromStream(is, modelId, progressInfo, false);
        } catch (IOException e) {
            progressInfo.error("Failed to load embeddings from disk: " + path);
            progressInfo.log(e);
        } finally {
            diskLock.readLock().unlock();
        }
    }

    /**
     * Save all embeddings for the given modelId to a file.
     * Uses the binary format described in the class documentation.
     * Creates parent directories if needed.
     *
     * @param path    the file path to save to
     * @param modelId the model ID whose embeddings to save
     */
    public void saveToDisk(Path path, String modelId) {
        Objects.requireNonNull(path, "Path must not be null");
        Objects.requireNonNull(modelId, "Model ID must not be null");

        Map<String, float[]> modelEmbeddings = embeddings.get(modelId);
        if (modelEmbeddings == null || modelEmbeddings.isEmpty()) {
            LOGGER.debug("No embeddings to save for model: {}", modelId);
            return;
        }

        diskLock.writeLock().lock();
        try {
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            try (OutputStream os = Files.newOutputStream(path)) {
                saveToStream(os, modelId);
            }
            dirtyModels.remove(modelId);
            LOGGER.info("Saved {} embeddings for model {} to {}", modelEmbeddings.size(), modelId, path);
        } catch (IOException e) {
            LOGGER.error("Failed to save embeddings to disk: {}", path, e);
        } finally {
            diskLock.writeLock().unlock();
        }
    }

    // ===== Core Access Methods =====

    /**
     * Get the embedding for a node, returns null if not found.
     * Does not perform hash verification; use {@link #getVerifiedEmbedding} for verified access.
     *
     * @param modelId the model ID
     * @param nodeId  the node ID
     * @return the embedding vector, or null if not found
     */
    public float[] getEmbedding(String modelId, String nodeId) {
        Map<String, float[]> modelEmbeddings = embeddings.get(modelId);
        if (modelEmbeddings == null) {
            return null;
        }
        return modelEmbeddings.get(nodeId);
    }

    /**
     * Gets an embedding after verifying its hash against the provided text.
     * Returns null if the embedding doesn't exist or if the hash doesn't match (invalidates stale entry).
     *
     * @param modelId the model ID
     * @param nodeId  the node ID
     * @param text    the current text for the node (used to compute hash for verification)
     * @return the embedding if verified, null if not found or hash mismatch
     */
    public float[] getVerifiedEmbedding(String modelId, String nodeId, String text) {
        Map<String, float[]> modelEmbeddings = embeddings.get(modelId);
        if (modelEmbeddings == null || !modelEmbeddings.containsKey(nodeId)) {
            return null;
        }

        Map<String, VerificationState> modelStates = verificationStates.get(modelId);
        VerificationState state = modelStates != null ? modelStates.get(nodeId) : null;

        // PRE_VERIFIED entries (from bundled resources) skip verification
        if (state == VerificationState.PRE_VERIFIED) {
            return modelEmbeddings.get(nodeId);
        }

        // UNVERIFIED entries need hash check
        if (state == VerificationState.UNVERIFIED) {
            String currentHash = computeHash(text);
            Map<String, String> modelHashes = hashes.get(modelId);
            String cachedHash = modelHashes != null ? modelHashes.get(nodeId) : null;

            if (cachedHash != null && cachedHash.equals(currentHash)) {
                // Hash matches, mark as verified
                modelStates.put(nodeId, VerificationState.VERIFIED);
                return modelEmbeddings.get(nodeId);
            } else {
                // Hash mismatch - invalidate the embedding
                removeEmbedding(modelId, nodeId);
                return null;
            }
        }

        // VERIFIED entries are good
        return modelEmbeddings.get(nodeId);
    }

    /**
     * Store an embedding, also record the dimension.
     * The embedding is marked as {@link VerificationState#VERIFIED}.
     *
     * @param modelId   the model ID
     * @param nodeId    the node ID
     * @param embedding the embedding vector
     */
    public void setEmbedding(String modelId, String nodeId, float[] embedding) {
        Objects.requireNonNull(modelId, "Model ID must not be null");
        Objects.requireNonNull(nodeId, "Node ID must not be null");
        Objects.requireNonNull(embedding, "Embedding must not be null");

        embeddings.computeIfAbsent(modelId, k -> new ConcurrentHashMap<>())
                .put(nodeId, embedding);
        dimensions.put(modelId, embedding.length);
        verificationStates.computeIfAbsent(modelId, k -> new ConcurrentHashMap<>())
                .put(nodeId, VerificationState.VERIFIED);
        dirtyModels.add(modelId);
    }

    /**
     * Store an embedding along with a hash of the source text, and mark as verified.
     *
     * @param modelId   the model ID
     * @param nodeId    the node ID
     * @param embedding the embedding vector
     * @param text      the source text used to generate the embedding (for hash computation)
     */
    public void setEmbeddingWithHash(String modelId, String nodeId, float[] embedding, String text) {
        setEmbedding(modelId, nodeId, embedding);
        String hash = computeHash(text);
        hashes.computeIfAbsent(modelId, k -> new ConcurrentHashMap<>()).put(nodeId, hash);
        verificationStates.computeIfAbsent(modelId, k -> new ConcurrentHashMap<>()).put(nodeId, VerificationState.VERIFIED);
    }

    /**
     * Remove a single embedding entry and its associated hash and verification state.
     *
     * @param modelId the model ID
     * @param nodeId  the node ID
     */
    public void removeEmbedding(String modelId, String nodeId) {
        Map<String, float[]> modelEmbeddings = embeddings.get(modelId);
        if (modelEmbeddings != null) {
            modelEmbeddings.remove(nodeId);
        }
        Map<String, String> modelHashes = hashes.get(modelId);
        if (modelHashes != null) {
            modelHashes.remove(nodeId);
        }
        Map<String, VerificationState> modelStates = verificationStates.get(modelId);
        if (modelStates != null) {
            modelStates.remove(nodeId);
        }
        dirtyModels.add(modelId);
    }

    /**
     * Check if the cache (bundled resource + user disk) has been loaded for a model.
     *
     * @param modelId the model ID to check
     * @return true if the cache has been loaded for this model
     */
    public boolean isCacheLoaded(String modelId) {
        return cacheLoadedModels.contains(modelId);
    }

    /**
     * Check if an embedding exists.
     *
     * @param modelId the model ID
     * @param nodeId  the node ID
     * @return true if an embedding exists for the given model and node
     */
    public boolean hasEmbedding(String modelId, String nodeId) {
        Map<String, float[]> modelEmbeddings = embeddings.get(modelId);
        return modelEmbeddings != null && modelEmbeddings.containsKey(nodeId);
    }

    /**
     * Get the embedding dimension for a model.
     *
     * @param modelId the model ID
     * @return the dimension, or -1 if unknown
     */
    public int getDimension(String modelId) {
        return dimensions.getOrDefault(modelId, -1);
    }

    /**
     * Get all node IDs that have embeddings for a model.
     *
     * @param modelId the model ID
     * @return an unmodifiable set of node IDs, or an empty set if no embeddings exist
     */
    public Set<String> getNodeIds(String modelId) {
        Map<String, float[]> modelEmbeddings = embeddings.get(modelId);
        if (modelEmbeddings == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(modelEmbeddings.keySet());
    }

    /**
     * Remove all embeddings for a model.
     * Also resets the cache-loaded flag so the cache will be re-loaded on next access.
     *
     * @param modelId the model ID to clear
     */
    public void clearModel(String modelId) {
        embeddings.remove(modelId);
        hashes.remove(modelId);
        verificationStates.remove(modelId);
        dimensions.remove(modelId);
        cacheLoadedModels.remove(modelId);
        dirtyModels.remove(modelId);
    }

    /**
     * Marks all current entries for a model as PRE_VERIFIED.
     * Should be called after loading from bundled resources, since resource-provided
     * embeddings are stable and don't need hash verification.
     *
     * @param modelId the model ID whose entries to mark as pre-verified
     */
    public void markPreVerified(String modelId) {
        Map<String, VerificationState> modelStates = verificationStates.computeIfAbsent(modelId, k -> new ConcurrentHashMap<>());
        Map<String, float[]> modelEmbeddings = embeddings.get(modelId);
        if (modelEmbeddings != null) {
            for (String nodeId : modelEmbeddings.keySet()) {
                modelStates.put(nodeId, VerificationState.PRE_VERIFIED);
            }
        }
    }

    // ===== Hash Computation =====

    /**
     * Compute a SHA-256 hash of the given text.
     *
     * @param text the text to hash
     * @return the hex-encoded SHA-256 hash
     */
    public static String computeHash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    // ===== JIT Computation =====

    /**
     * For each entry that doesn't have a verified embedding, compute one JIT.
     * Uses the entry's text representation (see {@link #entryToText(JIPipeNodeDatabaseEntry)})
     * and calls the AI service to generate embeddings.
     * Should be callable from any thread.
     *
     * @param entries      the list of node database entries to ensure embeddings for
     * @param modelId      the model ID to use
     * @param aiService    the AI service component for computing embeddings
     * @param progressInfo the progress info for logging
     */
    public void ensureEmbeddingsForEntries(List<JIPipeNodeDatabaseEntry> entries, String modelId,
                                           JIPipeAIServiceComponent aiService, JIPipeProgressInfo progressInfo) {
        Objects.requireNonNull(entries, "Entries must not be null");
        Objects.requireNonNull(modelId, "Model ID must not be null");
        Objects.requireNonNull(aiService, "AI service must not be null");
        Objects.requireNonNull(progressInfo, "Progress info must not be null");

        // Ensure the cache (bundled resource + user disk) is loaded before computing any embeddings.
        // This prevents JIT embedding of entries that are already in the cache but haven't been loaded yet.
        if (!cacheLoadedModels.contains(modelId)) {
            if (persistent) {
                loadUserCache(modelId, progressInfo);
            } else {
                cacheLoadedModels.add(modelId);
            }
        }

        int computedCount = 0;
        for (JIPipeNodeDatabaseEntry entry : entries) {
            String nodeId = entryToId(entry);
            String text = entryToText(entry);

            // Use verified access to detect and invalidate stale embeddings
            float[] existing = getVerifiedEmbedding(modelId, nodeId, text);
            if (existing != null) {
                continue;
            }

            try {
                CompletableFuture<float[]> future = aiService.tryEmbed(text);
                if (future == null) {
                    LOGGER.debug("AI service returned null future (AI disabled?). Skipping entry: {}", nodeId);
                    continue;
                }
                float[] embedding = future.get(EMBED_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (embedding != null) {
                    setEmbeddingWithHash(modelId, nodeId, embedding, text);
                    computedCount++;
                    LOGGER.debug("Computed embedding for node: {}", nodeId);
                }
            } catch (TimeoutException e) {
                progressInfo.warn("Timeout computing embedding for node: " + nodeId);
            } catch (Exception e) {
                progressInfo.warn("Failed to compute embedding for node: " + nodeId);
                LOGGER.debug("Failed to compute embedding for node: {}", nodeId, e);
            }
        }

        // Auto-save after JIT computation
        if (computedCount > 0) {
            try {
                saveUserCache(modelId);
                LOGGER.debug("Auto-saved {} new embeddings for model {}", computedCount, modelId);
            } catch (Exception e) {
                LOGGER.warn("Failed to auto-save embedding cache for model {}", modelId, e);
            }
        }
    }

    /**
     * For each entry that doesn't have a verified embedding, compute one JIT using the provided
     * embed function. The embed function is called synchronously and should be backed by
     * {@link JIPipeAIServiceComponent#embedNow(String)} on the queue thread.
     * <p>
     * This variant avoids flooding the AI service queue with individual embed tasks, which
     * was the root cause of search freezes when the search was cancelled/superseded.
     *
     * @param entries      the list of node database entries to ensure embeddings for
     * @param modelId      the model ID to use
     * @param embedFn      synchronous embed function (text -> embedding vector)
     * @param progressInfo the progress info for logging
     */
    public void ensureEmbeddingsForEntriesWithEmbedder(List<JIPipeNodeDatabaseEntry> entries, String modelId,
                                                        Function<String, float[]> embedFn, JIPipeProgressInfo progressInfo) {
        Objects.requireNonNull(entries, "Entries must not be null");
        Objects.requireNonNull(modelId, "Model ID must not be null");
        Objects.requireNonNull(embedFn, "Embed function must not be null");
        Objects.requireNonNull(progressInfo, "Progress info must not be null");

        // Ensure the cache (bundled resource + user disk) is loaded before computing any embeddings.
        if (!cacheLoadedModels.contains(modelId)) {
            if (persistent) {
                loadUserCache(modelId, progressInfo);
            } else {
                cacheLoadedModels.add(modelId);
            }
        }

        int computedCount = 0;
        for (JIPipeNodeDatabaseEntry entry : entries) {
            if (Thread.currentThread().isInterrupted()) {
                LOGGER.debug("Interrupted while ensuring embeddings");
                break;
            }

            String nodeId = entryToId(entry);
            String text = entryToText(entry);

            // Use verified access to detect and invalidate stale embeddings
            float[] existing = getVerifiedEmbedding(modelId, nodeId, text);
            if (existing != null) {
                continue;
            }

            try {
                float[] embedding = embedFn.apply(text);
                if (embedding != null) {
                    setEmbeddingWithHash(modelId, nodeId, embedding, text);
                    computedCount++;
                    LOGGER.debug("Computed embedding for node: {}", nodeId);
                }
            } catch (Exception e) {
                progressInfo.warn("Failed to compute embedding for node: " + nodeId);
                LOGGER.debug("Failed to compute embedding for node: {}", nodeId, e);
            }
        }

        // Auto-save after JIT computation
        if (computedCount > 0) {
            try {
                saveUserCache(modelId);
                LOGGER.debug("Auto-saved {} new embeddings for model {}", computedCount, modelId);
            } catch (Exception e) {
                LOGGER.warn("Failed to auto-save embedding cache for model {}", modelId, e);
            }
        }
    }

    // ===== Static Helper Methods =====

    /**
     * Create comprehensive text for embedding from a node database entry.
     * Handles null values gracefully (skips sections with no data).
     *
     * @param entry the node database entry
     * @return a text representation suitable for embedding
     */
    public static String entryToText(JIPipeNodeDatabaseEntry entry) {
        Objects.requireNonNull(entry, "Entry must not be null");

        StringBuilder sb = new StringBuilder();

        // Name
        String name = entry.getName();
        if (name != null && !name.isBlank()) {
            sb.append("Name: ").append(name).append("\n");
        }

        // Description
        HTMLText description = entry.getDescription();
        if (description != null) {
            String plainText = description.toPlainText();
            if (plainText != null && !plainText.isBlank()) {
                sb.append("Description: ").append(plainText).append("\n");
            }
        }

        // Menu location
        List<String> locationInfos = entry.getLocationInfos();
        if (locationInfos != null && !locationInfos.isEmpty()) {
            for (String locationInfo : locationInfos) {
                sb.append("Menu: ").append(locationInfo.replace("\n", " > ")).append("\n");
            }
        }

        // Input slots
        Map<String, JIPipeDataSlotInfo> inputSlots = entry.getInputSlots();
        if (inputSlots != null && !inputSlots.isEmpty()) {
            String inputs = inputSlots.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> {
                        JIPipeDataInfo dataInfo = JIPipeDataInfo.getInstance(e.getValue().getDataClass());
                        return e.getKey() + " (" + dataInfo.getName() + ", " + dataInfo.getDescription() + ")";
                    })
                    .collect(Collectors.joining(", "));
            sb.append("Inputs: ").append(inputs).append("\n");
        }

        // Output slots
        Map<String, JIPipeDataSlotInfo> outputSlots = entry.getOutputSlots();
        if (outputSlots != null && !outputSlots.isEmpty()) {
            String outputs = outputSlots.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> {
                        JIPipeDataInfo dataInfo = JIPipeDataInfo.getInstance(e.getValue().getDataClass());
                        return e.getKey() + " (" + dataInfo.getName() + ", " + dataInfo.getDescription() + ")";
                    })
                    .collect(Collectors.joining(", "));
            sb.append("Outputs: ").append(outputs).append("\n");
        }

        return sb.toString().trim();
    }

    /**
     * Create a stable ID for the entry for caching purposes.
     * Uses {@code entry.getId()} if available, otherwise derives from name+location.
     *
     * @param entry the node database entry
     * @return a stable string ID for the entry
     */
    public static String entryToId(JIPipeNodeDatabaseEntry entry) {
        Objects.requireNonNull(entry, "Entry must not be null");

        String id = entry.getId();
        if (id != null && !id.isBlank()) {
            return id;
        }

        // Fallback: derive from name and location
        StringBuilder sb = new StringBuilder();
        String name = entry.getName();
        if (name != null) {
            sb.append(name);
        }
        List<String> locationInfos = entry.getLocationInfos();
        if (locationInfos != null && !locationInfos.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append("@");
            }
            sb.append(String.join(">", locationInfos));
        }
        return sb.toString();
    }

    // ===== User Cache Methods =====

    /**
     * Sanitize a model ID for use as a filename component.
     * Replaces characters that are invalid on Windows
     * ({@code < > : " / \ | ? * { }}) with underscores to ensure
     * the path can be constructed on all platforms.
     *
     * @param modelId the model ID to sanitize
     * @return a filesystem-safe version of the model ID
     */
    private static String sanitizeModelIdForPath(String modelId) {
        return modelId
                .replace(':', '_')
                .replace('<', '_')
                .replace('>', '_')
                .replace('"', '_')
                .replace('/', '_')
                .replace('\\', '_')
                .replace('|', '_')
                .replace('?', '_')
                .replace('*', '_')
                .replace('{', '_')
                .replace('}', '_');
    }

    /**
     * Load from the user cache directory and the bundled resource.
     * Resource embeddings are the baseline; disk cache overlays on top
     * (disk cache wins for overlapping entries, as it may be newer).
     *
     * @param modelId      the model ID to load caches for
     * @param progressInfo the progress info for logging
     */
    /**
     * Convenience overload for {@link #loadUserCache(String, JIPipeProgressInfo)}
     * that uses {@link JIPipeProgressInfo#SILENT}.
     *
     * @param modelId the model ID to load caches for
     */
    public void loadUserCache(String modelId) {
        loadUserCache(modelId, JIPipeProgressInfo.SILENT);
    }

    /**
     * Load from the user cache directory and the bundled resource.
     * Resource embeddings are the baseline; disk cache overlays on top
     * (disk cache wins for overlapping entries, as it may be newer).
     *
     * @param modelId      the model ID to load caches for
     * @param progressInfo the progress info for logging
     */
    public void loadUserCache(String modelId, JIPipeProgressInfo progressInfo) {
        Objects.requireNonNull(modelId, "Model ID must not be null");
        Objects.requireNonNull(progressInfo, "Progress info must not be null");

        // First load the bundled resource (baseline) - isResource=true, marks as PRE_VERIFIED
        loadFromResource(BUNDLED_RESOURCE_PATH, modelId, progressInfo);

        // Then load from disk cache (overrides resource entries) - isResource=false, marks as UNVERIFIED
        Path diskPath = PathUtils.getJIPipeUserDir().resolve("ai-embeddings").resolve(sanitizeModelIdForPath(modelId) + ".db");
        loadFromDisk(diskPath, modelId, progressInfo);

        // Mark this model's cache as loaded so ensureEmbeddingsForEntries() knows
        // it doesn't need to load the cache again before computing missing embeddings.
        cacheLoadedModels.add(modelId);

        // Freshly loaded data is considered clean
        dirtyModels.remove(modelId);

        progressInfo.log("Loaded user cache for model " + modelId + ". Total entries: " + getNodeIds(modelId).size());
    }

    /**
     * Save to the user cache directory.
     * Creates the {@code ai-embeddings} directory if it doesn't exist.
     * Does nothing if this database is not persistent.
     *
     * @param modelId the model ID to save the cache for
     */
    public void saveUserCache(String modelId) {
        Objects.requireNonNull(modelId, "Model ID must not be null");

        if (!persistent) {
            return;  // Non-persistent databases are never saved to disk
        }

        Path diskPath = PathUtils.getJIPipeUserDir().resolve("ai-embeddings").resolve(sanitizeModelIdForPath(modelId) + ".db");
        saveToDisk(diskPath, modelId);
        dirtyModels.remove(modelId);
    }

    /**
     * Save the user cache for the given model only if there are unsaved (dirty) changes.
     * This avoids unnecessary disk I/O when nothing has changed.
     *
     * @param modelId the model ID to save the cache for
     */
    public void saveIfDirty(String modelId) {
        if (dirtyModels.contains(modelId)) {
            saveUserCache(modelId);
        }
    }

    /**
     * Save all models that have unsaved (dirty) embeddings.
     * Typically called from a shutdown hook to ensure no data is lost on exit.
     * Errors are logged but not thrown, as this is best-effort.
     */
    public void saveAllDirty() {
        for (String modelId : new HashSet<>(dirtyModels)) {
            try {
                saveUserCache(modelId);
            } catch (Exception e) {
                LOGGER.error("Failed to save embedding cache for model {} on shutdown", modelId, e);
            }
        }
    }

    /**
     * Check if a model has unsaved (dirty) embeddings.
     *
     * @param modelId the model ID to check
     * @return true if the model has unsaved changes
     */
    public boolean isDirty(String modelId) {
        return dirtyModels.contains(modelId);
    }

    // ===== Active Model ID =====

    /**
     * Get the active model ID.
     *
     * @return the active model ID, or null if not set
     */
    public String getActiveModelId() {
        return activeModelId;
    }

    /**
     * Set the active model ID.
     *
     * @param activeModelId the active model ID
     */
    public void setActiveModelId(String activeModelId) {
        this.activeModelId = activeModelId;
    }

    // ===== Persistence Flag =====

    /**
     * Check if this database is persistent (saves to disk).
     *
     * @return true if the database saves embeddings to disk
     */
    public boolean isPersistent() {
        return persistent;
    }

    // ===== Internal I/O Methods =====

    /**
     * Load embeddings from an input stream using the binary format.
     * Merges into the embeddings map under the given modelId.
     * Handles empty streams gracefully.
     * Supports both version 1 (no hashes) and version 2 (with hashes) formats.
     *
     * @param is           the input stream
     * @param modelId      the model ID
     * @param progressInfo the progress info for logging
     * @param isResource   if true, entries are marked as PRE_VERIFIED; if false, as UNVERIFIED
     * @throws IOException if an I/O error occurs
     */
    private void loadFromStream(InputStream is, String modelId, JIPipeProgressInfo progressInfo, boolean isResource) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(is);
        DataInputStream dis = new DataInputStream(bis);

        // Check if stream is empty (e.g., placeholder file)
        if (bis.available() == 0) {
            LOGGER.debug("Empty embedding stream for model {}", modelId);
            return;
        }

        // Read and validate magic bytes
        byte[] magicBytes = new byte[4];
        dis.readFully(magicBytes);
        String magic = new String(magicBytes, StandardCharsets.US_ASCII);
        if (!MAGIC.equals(magic)) {
            progressInfo.warn("Invalid magic bytes in embedding file. Expected '" + MAGIC + "', got '" + magic + "'");
            return;
        }

        // Read version
        int version = dis.readInt();
        if (version != 1 && version != 2) {
            progressInfo.warn("Unsupported embedding file version " + version + ". Expected 1 or 2");
            return;
        }

        // Read model ID from file (for validation/logging)
        int modelIdLength = dis.readInt();
        byte[] modelIdBytes = new byte[modelIdLength];
        dis.readFully(modelIdBytes);
        String fileModelId = new String(modelIdBytes, StandardCharsets.UTF_8);
        LOGGER.debug("Loading embeddings for model '{}' from file (file says model '{}')", modelId, fileModelId);

        // Read dimension
        int dimension = dis.readInt();
        if (dimension <= 0) {
            progressInfo.warn("Invalid dimension " + dimension + " in embedding file for model " + modelId);
            return;
        }

        // Read entry count
        int entryCount = dis.readInt();
        if (entryCount < 0) {
            progressInfo.warn("Invalid entry count " + entryCount + " in embedding file for model " + modelId);
            return;
        }

        LOGGER.debug("Loading {} embeddings (v{}) with dimension {} for model {}", entryCount, version, dimension, modelId);

        // Read entries
        Map<String, float[]> modelEmbeddings = embeddings.computeIfAbsent(modelId, k -> new ConcurrentHashMap<>());
        Map<String, String> modelHashes = hashes.computeIfAbsent(modelId, k -> new ConcurrentHashMap<>());
        Map<String, VerificationState> modelStates = verificationStates.computeIfAbsent(modelId, k -> new ConcurrentHashMap<>());
        dimensions.put(modelId, dimension);

        for (int i = 0; i < entryCount; i++) {
            // Read node ID
            int nodeIdLength = dis.readInt();
            byte[] nodeIdBytes = new byte[nodeIdLength];
            dis.readFully(nodeIdBytes);
            String nodeId = new String(nodeIdBytes, StandardCharsets.UTF_8);

            // Read hash (version 2 only)
            String hash = "";
            if (version >= 2) {
                int hashLength = dis.readInt();
                byte[] hashBytes = new byte[hashLength];
                dis.readFully(hashBytes);
                hash = new String(hashBytes, StandardCharsets.UTF_8);
            }

            // Read embedding floats
            float[] embedding = new float[dimension];
            for (int j = 0; j < dimension; j++) {
                embedding[j] = dis.readFloat();
            }

            modelEmbeddings.put(nodeId, embedding);
            if (!hash.isEmpty()) {
                modelHashes.put(nodeId, hash);
            }
            // Only set UNVERIFIED if not already PRE_VERIFIED (from resource loading)
            if (!VerificationState.PRE_VERIFIED.equals(modelStates.get(nodeId))) {
                modelStates.put(nodeId, VerificationState.UNVERIFIED);
            }
        }

        // If loaded from a bundled resource, mark all entries as PRE_VERIFIED
        if (isResource) {
            markPreVerified(modelId);
        }

        progressInfo.log("Loaded " + entryCount + " embeddings (v" + version + ") for model " + modelId);
    }

    /**
     * Save embeddings to an output stream using the binary format (version 2 with hashes).
     *
     * @param os      the output stream
     * @param modelId the model ID
     * @throws IOException if an I/O error occurs
     */
    private void saveToStream(OutputStream os, String modelId) throws IOException {
        Map<String, float[]> modelEmbeddings = embeddings.get(modelId);
        if (modelEmbeddings == null || modelEmbeddings.isEmpty()) {
            LOGGER.debug("No embeddings to write for model {}", modelId);
            return;
        }

        Integer dimension = dimensions.get(modelId);
        if (dimension == null || dimension <= 0) {
            LOGGER.warn("Invalid dimension for model {}, skipping save", modelId);
            return;
        }

        BufferedOutputStream bos = new BufferedOutputStream(os);
        DataOutputStream dos = new DataOutputStream(bos);

        // Write magic bytes
        dos.write(MAGIC.getBytes(StandardCharsets.US_ASCII));

        // Write version 2
        dos.writeInt(CURRENT_VERSION);

        // Write model ID
        byte[] modelIdBytes = modelId.getBytes(StandardCharsets.UTF_8);
        dos.writeInt(modelIdBytes.length);
        dos.write(modelIdBytes);

        // Write dimension
        dos.writeInt(dimension);

        // Write entry count
        dos.writeInt(modelEmbeddings.size());

        // Get hashes for this model
        Map<String, String> modelHashes = hashes.get(modelId);

        // Write entries (version 2 format: nodeId + hash + embedding)
        for (Map.Entry<String, float[]> entry : modelEmbeddings.entrySet()) {
            // Write node ID
            byte[] nodeIdBytes = entry.getKey().getBytes(StandardCharsets.UTF_8);
            dos.writeInt(nodeIdBytes.length);
            dos.write(nodeIdBytes);

            // Write hash (empty string if no hash stored)
            String hash = modelHashes != null ? modelHashes.get(entry.getKey()) : "";
            if (hash == null) hash = "";
            byte[] hashBytes = hash.getBytes(StandardCharsets.UTF_8);
            dos.writeInt(hashBytes.length);
            dos.write(hashBytes);

            // Write embedding floats
            float[] embedding = entry.getValue();
            for (int i = 0; i < dimension && i < embedding.length; i++) {
                dos.writeFloat(embedding[i]);
            }
            // Pad with zeros if embedding is shorter than expected dimension
            for (int i = embedding.length; i < dimension; i++) {
                dos.writeFloat(0f);
            }
        }

        dos.flush();
    }
}
