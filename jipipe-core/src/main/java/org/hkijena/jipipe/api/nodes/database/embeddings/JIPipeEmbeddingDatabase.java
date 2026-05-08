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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Manages embedding vectors for node database entries.
 * Supports loading from resource files, disk caching, and JIT computation.
 * All data is keyed by model ID to distinguish embeddings from different models.
 */
public class JIPipeEmbeddingDatabase {

    private static final Logger LOGGER = LoggerFactory.getLogger(JIPipeEmbeddingDatabase.class);

    private static final String MAGIC = "JEMB";
    private static final int VERSION = 1;
    private static final String BUNDLED_RESOURCE_PATH = "ai/embeddings.db";
    private static final long EMBED_TIMEOUT_SECONDS = 30;

    // Inner storage: modelId -> (nodeId -> embedding vector)
    private final Map<String, Map<String, float[]>> embeddings = new ConcurrentHashMap<>();

    // The model ID this database is currently associated with (for convenience)
    private String activeModelId;

    // Dimension of embeddings for each model
    private final Map<String, Integer> dimensions = new ConcurrentHashMap<>();

    // Lock for thread-safe disk operations
    private final ReentrantReadWriteLock diskLock = new ReentrantReadWriteLock();

    /**
     * Creates a new embedding database.
     */
    public JIPipeEmbeddingDatabase() {
    }

    // ===== Resource and Disk I/O =====

    /**
     * Load pre-computed embeddings from a classpath resource (the bundled file).
     * Uses the binary format described in the class documentation.
     * Merges into the embeddings map under the given modelId.
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
            loadFromStream(is, modelId, progressInfo);
        } catch (IOException e) {
            progressInfo.error("Failed to load embeddings from resource: " + resourcePath);
            progressInfo.log(e);
        }
    }

    /**
     * Load embeddings from a file on disk (user cache).
     * Uses the binary format described in the class documentation.
     * Merges into the embeddings map.
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
            loadFromStream(is, modelId, progressInfo);
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
     * Store an embedding, also record the dimension.
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
     *
     * @param modelId the model ID to clear
     */
    public void clearModel(String modelId) {
        embeddings.remove(modelId);
        dimensions.remove(modelId);
    }

    // ===== JIT Computation =====

    /**
     * For each entry that doesn't have an embedding yet, compute one JIT.
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

        for (JIPipeNodeDatabaseEntry entry : entries) {
            String nodeId = entryToId(entry);
            if (hasEmbedding(modelId, nodeId)) {
                continue;
            }

            String text = entryToText(entry);
            try {
                CompletableFuture<float[]> future = aiService.tryEmbed(text);
                if (future == null) {
                    LOGGER.debug("AI service returned null future (AI disabled?). Skipping entry: {}", nodeId);
                    continue;
                }
                float[] embedding = future.get(EMBED_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (embedding != null) {
                    setEmbedding(modelId, nodeId, embedding);
                    LOGGER.debug("Computed embedding for node: {}", nodeId);
                }
            } catch (TimeoutException e) {
                progressInfo.warn("Timeout computing embedding for node: " + nodeId);
            } catch (Exception e) {
                progressInfo.warn("Failed to compute embedding for node: " + nodeId);
                LOGGER.debug("Failed to compute embedding for node: {}", nodeId, e);
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

        // First load the bundled resource (baseline)
        loadFromResource(BUNDLED_RESOURCE_PATH, modelId, progressInfo);

        // Then load from disk cache (overrides resource entries)
        Path diskPath = PathUtils.getJIPipeUserDir().resolve("ai-embeddings").resolve(modelId + ".db");
        loadFromDisk(diskPath, modelId, progressInfo);

        progressInfo.log("Loaded user cache for model " + modelId + ". Total entries: " + getNodeIds(modelId).size());
    }

    /**
     * Save to the user cache directory.
     * Creates the {@code ai-embeddings} directory if it doesn't exist.
     *
     * @param modelId the model ID to save the cache for
     */
    public void saveUserCache(String modelId) {
        Objects.requireNonNull(modelId, "Model ID must not be null");

        Path diskPath = PathUtils.getJIPipeUserDir().resolve("ai-embeddings").resolve(modelId + ".db");
        saveToDisk(diskPath, modelId);
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

    // ===== Internal I/O Methods =====

    /**
     * Load embeddings from an input stream using the binary format.
     * Merges into the embeddings map under the given modelId.
     * Handles empty streams gracefully.
     *
     * @param is           the input stream
     * @param modelId      the model ID
     * @param progressInfo the progress info for logging
     * @throws IOException if an I/O error occurs
     */
    private void loadFromStream(InputStream is, String modelId, JIPipeProgressInfo progressInfo) throws IOException {
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
        String magic = new String(magicBytes, "ASCII");
        if (!MAGIC.equals(magic)) {
            progressInfo.warn("Invalid magic bytes in embedding file. Expected '" + MAGIC + "', got '" + magic + "'");
            return;
        }

        // Read and validate version
        int version = dis.readInt();
        if (version != VERSION) {
            progressInfo.warn("Unsupported embedding file version " + version + ". Expected " + VERSION);
            return;
        }

        // Read model ID from file (for validation/logging)
        int modelIdLength = dis.readInt();
        byte[] modelIdBytes = new byte[modelIdLength];
        dis.readFully(modelIdBytes);
        String fileModelId = new String(modelIdBytes, "UTF-8");
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

        LOGGER.debug("Loading {} embeddings with dimension {} for model {}", entryCount, dimension, modelId);

        // Read entries
        Map<String, float[]> modelEmbeddings = embeddings.computeIfAbsent(modelId, k -> new ConcurrentHashMap<>());
        dimensions.put(modelId, dimension);

        for (int i = 0; i < entryCount; i++) {
            int nodeIdLength = dis.readInt();
            byte[] nodeIdBytes = new byte[nodeIdLength];
            dis.readFully(nodeIdBytes);
            String nodeId = new String(nodeIdBytes, "UTF-8");

            float[] embedding = new float[dimension];
            for (int j = 0; j < dimension; j++) {
                embedding[j] = dis.readFloat();
            }

            modelEmbeddings.put(nodeId, embedding);
        }

        progressInfo.log("Loaded " + entryCount + " embeddings for model " + modelId);
    }

    /**
     * Save embeddings to an output stream using the binary format.
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
        dos.write(MAGIC.getBytes("ASCII"));

        // Write version
        dos.writeInt(VERSION);

        // Write model ID
        byte[] modelIdBytes = modelId.getBytes("UTF-8");
        dos.writeInt(modelIdBytes.length);
        dos.write(modelIdBytes);

        // Write dimension
        dos.writeInt(dimension);

        // Write entry count
        dos.writeInt(modelEmbeddings.size());

        // Write entries
        for (Map.Entry<String, float[]> entry : modelEmbeddings.entrySet()) {
            byte[] nodeIdBytes = entry.getKey().getBytes("UTF-8");
            dos.writeInt(nodeIdBytes.length);
            dos.write(nodeIdBytes);

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
