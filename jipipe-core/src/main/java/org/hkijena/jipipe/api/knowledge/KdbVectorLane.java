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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * The vector retrieval lane (design spec 5.5, lane 2): cosine similarity
 * between the query embedding and one embedding per corpus entry, ranked best
 * first.
 * <p>
 * The embedder is injectable ({@code Function<String, float[]>}): production
 * passes the AI service component's synchronous embed operation, tests pass a
 * deterministic stub, so the lane is fully testable without a running
 * embedding model. Until the rendered payload exists (design spec 5.6), the
 * embedded text is the title, the TEXT slot values, and the body; switching
 * to the rendered text is a one method change
 * ({@link #embeddingText(KdbEntry)}), and the cache key follows automatically
 * because it is the hash of exactly that text.
 * <p>
 * Per entry vectors are cached on disk keyed by the content hash of the
 * embedded text, so an unchanged corpus never re-embeds. A cache directory of
 * null disables persistence: every search computes the vectors. The cache is
 * best effort: a missing, corrupt, or dimension mismatched file (for example
 * left by a different embedder) is a miss, the vector is recomputed, and the
 * file is replaced. Cache I/O errors and embedder failures never propagate:
 * {@link #search(String, int)} degrades to an empty ranking, so the fusion in
 * {@link KnowledgeService} always keeps the other lanes (spec 5.5: graceful
 * when no embedding model is available).
 * <p>
 * The lane is immutable once constructed: it is constructed from the corpus,
 * and every later call is a read.
 */
public final class KdbVectorLane {

    /**
     * The magic word of the cache file format (the bytes "KVL1").
     */
    private static final int CACHE_MAGIC = 0x4B564C31;

    /**
     * The file name suffix of the cache files.
     */
    private static final String CACHE_SUFFIX = ".bin";

    /**
     * The largest vector dimension a cache file may declare. A corrupt
     * header must not allocate an unbounded array: the value is far above
     * every known embedding dimension and small enough to be harmless.
     */
    private static final int MAX_CACHED_DIMENSION = 65536;

    private final List<KdbEntry> entries;
    private final Function<String, float[]> embedder;
    private final Path cacheDir;

    /**
     * Creates the lane over the given corpus.
     *
     * @param entries  the loaded corpus entries; the list is copied and not
     *                 modified
     * @param embedder the embedder that turns a text into a vector; a null
     *                 embedder or a null or empty vector result counts as an
     *                 embedding failure (the lane serves nothing)
     * @param cacheDir the directory for the per entry vector cache, or null
     *                 for no persistence (every search computes the vectors)
     */
    public KdbVectorLane(List<KdbEntry> entries, Function<String, float[]> embedder, Path cacheDir) {
        this.entries = List.copyOf(entries);
        this.embedder = embedder;
        this.cacheDir = cacheDir;
    }

    /**
     * Searches the lane: the query is embedded, every entry vector is taken
     * from the cache or computed, and the entries with a positive cosine
     * similarity are ordered by similarity descending with the entry key
     * ascending as the stable tiebreak, capped at {@code topK}. The method
     * never throws: an embedding failure degrades to an empty ranking.
     *
     * @param query the query, or null or blank for an empty result
     * @param topK  the maximum number of results; non-positive yields nothing
     * @return the scored entries, best first
     */
    public List<ScoredEntry> search(String query, int topK) {
        if (query == null || query.isBlank() || topK <= 0 || embedder == null || entries.isEmpty()) {
            return List.of();
        }
        try {
            float[] queryVector = embedSafely(query);
            if (queryVector == null) {
                return List.of();
            }
            List<ScoredEntry> scored = new ArrayList<>();
            for (KdbEntry entry : entries) {
                float[] vector = entryVector(entry, queryVector.length);
                if (vector == null) {
                    continue;
                }
                double score = cosine(queryVector, vector);
                if (Double.isNaN(score) || score <= 0.0) {
                    continue;
                }
                scored.add(new ScoredEntry(entry.getEntryKey(), score));
            }
            scored.sort(Comparator.comparingDouble(ScoredEntry::score).reversed()
                    .thenComparing(ScoredEntry::entryKey));
            if (scored.size() <= topK) {
                return scored;
            }
            return new ArrayList<>(scored.subList(0, topK));
        } catch (Exception e) {
            // The lane never fails the fusion: an unexpected failure degrades
            // to an empty ranking.
            return List.of();
        }
    }

    /**
     * The vector of an entry, from the cache or freshly computed. A cached
     * vector whose dimension differs from the expected one (for example left
     * by a different embedder) is a miss: the vector is recomputed and the
     * cache file replaced. A null result means the entry is skipped.
     */
    private float[] entryVector(KdbEntry entry, int expectedDimension) {
        String text = embeddingText(entry);
        String hash = contentHash(text);
        float[] vector = readCache(hash);
        if (vector == null || vector.length != expectedDimension) {
            vector = embedSafely(text);
            if (vector == null) {
                return null;
            }
            writeCache(hash, vector);
        }
        return vector;
    }

    /**
     * The text that is embedded for an entry: the title, the TEXT slot
     * values, and the body. Until the rendered payload exists (design spec
     * 5.6) this is the embedding text; switching to the rendered text changes
     * this method only, and the cache key follows because it hashes exactly
     * this text. Non-string TEXT values cannot occur (the loader narrows TEXT
     * to strings) and are skipped defensively.
     */
    private String embeddingText(KdbEntry entry) {
        StringBuilder text = new StringBuilder();
        if (entry.getTitle() != null) {
            text.append(entry.getTitle());
        }
        for (Map.Entry<String, List<Object>> slot : entry.getSlotValues().entrySet()) {
            if (entry.getKind().getSlot(slot.getKey())
                    .map(def -> def.type() != KdbValueType.TEXT)
                    .orElse(true)) {
                // Unknown slots are loader findings, not embedding material;
                // other value types carry no prose surface.
                continue;
            }
            for (Object value : slot.getValue()) {
                if (value instanceof String string) {
                    if (text.length() > 0) {
                        text.append(' ');
                    }
                    text.append(string);
                }
            }
        }
        if (entry.getBody() != null) {
            if (text.length() > 0) {
                text.append(' ');
            }
            text.append(entry.getBody());
        }
        return text.toString();
    }

    /**
     * The SHA-256 hash of the text, hex encoded: the cache key of the
     * embedding.
     */
    private static String contentHash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The cached vector of a hash, or null on a miss: a missing file, a
     * corrupt or truncated file, an out of range dimension header, or a
     * non-finite vector value all count as a miss and never throw.
     */
    private float[] readCache(String hash) {
        if (cacheDir == null) {
            return null;
        }
        Path file = cacheDir.resolve(hash + CACHE_SUFFIX);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            if (in.readInt() != CACHE_MAGIC) {
                return null;
            }
            int dimension = in.readInt();
            if (dimension < 0 || dimension > MAX_CACHED_DIMENSION) {
                return null;
            }
            float[] vector = new float[dimension];
            for (int i = 0; i < dimension; i++) {
                vector[i] = in.readFloat();
            }
            for (float value : vector) {
                if (!Float.isFinite(value)) {
                    return null;
                }
            }
            return vector;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /**
     * Writes a vector to the cache, best effort: a cache that cannot be
     * written only costs recomputation. The file is written to a temporary
     * name and moved into place, so a reader never sees a half written file.
     * The format is deterministic: the magic word, the dimension, then the
     * component floats.
     */
    private void writeCache(String hash, float[] vector) {
        if (cacheDir == null) {
            return;
        }
        Path file = cacheDir.resolve(hash + CACHE_SUFFIX);
        Path temp = cacheDir.resolve(hash + CACHE_SUFFIX + ".tmp");
        try {
            Files.createDirectories(cacheDir);
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temp)))) {
                out.writeInt(CACHE_MAGIC);
                out.writeInt(vector.length);
                for (float value : vector) {
                    out.writeFloat(value);
                }
            }
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException e) {
            // Best effort: the vector is recomputed on the next search.
        }
    }

    /**
     * Embeds a text, mapping every failure (an exception, a null result, an
     * empty vector) to null.
     */
    private float[] embedSafely(String text) {
        try {
            float[] vector = embedder.apply(text);
            return vector == null || vector.length == 0 ? null : vector;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * The cosine similarity of two vectors, or NaN when the similarity is
     * undefined: a dimension mismatch or a zero vector (no direction).
     */
    private static double cosine(float[] left, float[] right) {
        if (left.length != right.length) {
            return Double.NaN;
        }
        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int i = 0; i < left.length; i++) {
            dot += (double) left[i] * right[i];
            leftNorm += (double) left[i] * left[i];
            rightNorm += (double) right[i] * right[i];
        }
        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return Double.NaN;
        }
        return dot / Math.sqrt(leftNorm * rightNorm);
    }

    /**
     * One vector hit.
     *
     * @param entryKey the entry key of the matching entry
     * @param score    the cosine similarity in (0, 1]
     */
    public record ScoredEntry(String entryKey, double score) {
    }
}
