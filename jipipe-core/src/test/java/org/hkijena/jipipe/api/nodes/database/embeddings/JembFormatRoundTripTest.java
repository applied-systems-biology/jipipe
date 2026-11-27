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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip gate for the JEMB binary format of
 * {@link JIPipeEmbeddingDatabase}: vectors and text hashes written via
 * {@link JIPipeEmbeddingDatabase#saveToStream(java.io.OutputStream, String)}
 * must come back byte-exact through
 * {@link JIPipeEmbeddingDatabase#loadFromStream(java.io.InputStream, String, JIPipeProgressInfo, boolean)},
 * and the file header must record the model id the file was written for
 * (the read-back contract the headless {@code build-embedding-db} command
 * relies on before shipping a database).
 * <p>
 * The version gate is the cache invalidation contract: pre-restructure
 * files (version 1 and version 2) are rejected wholesale with a warning,
 * so a version-mismatched file leaves the database empty for its model
 * (no partial load).
 * <p>
 * Uses synthetic vectors and hashes with a fixed model id; no JIPipe boot
 * and no embedding model are needed.
 */
class JembFormatRoundTripTest {

    private static final String MODEL_ID = "test.model:1.0-any_onnx";

    private static final String OTHER_MODEL_ID = "other.model:2.0-any_onnx";

    private static final String LEGACY_NODE_ID = "legacy-node";

    @TempDir
    Path tempDir;

    @Test
    void roundTripPreservesVectorsAndHashes() throws IOException {
        JIPipeEmbeddingDatabase database = new JIPipeEmbeddingDatabase(false);
        float[] vectorA = {0.1f, -0.25f, 0.75f};
        float[] vectorB = {-1.5f, 2.25f, 0.0f};
        float[] vectorC = {0.125f, 0.875f, -0.625f};
        database.setEmbeddingWithHash(MODEL_ID, "node-a", vectorA, "Text of node A");
        database.setEmbeddingWithHash(MODEL_ID, "node-b", vectorB, "Text of node B");
        database.setEmbeddingWithHash(MODEL_ID, "node-c", vectorC, "Text of node C");

        Path file = tempDir.resolve("round-trip.db");
        try (OutputStream os = Files.newOutputStream(file)) {
            database.saveToStream(os, MODEL_ID);
        }

        // The header records the writer's model id, the dimension, and the entry count
        JIPipeEmbeddingDatabase.JembFileHeader header = JIPipeEmbeddingDatabase.readFileHeader(file);
        assertEquals(MODEL_ID, header.modelId());
        assertEquals(3, header.dimension());
        assertEquals(3, header.entryCount());

        // The written file carries the v3 generation: the first version whose
        // vectors encode the post-#1409 entry text (pinned literal: a version
        // change is a deliberate invalidation event and updates this test)
        assertEquals(3, header.version());

        JIPipeEmbeddingDatabase readBack = new JIPipeEmbeddingDatabase(false);
        try (InputStream is = Files.newInputStream(file)) {
            readBack.loadFromStream(is, MODEL_ID, JIPipeProgressInfo.SILENT, false);
        }

        assertEquals(3, readBack.getNodeIds(MODEL_ID).size());
        assertEquals(3, readBack.getDimension(MODEL_ID));

        assertVectorBytesEqual("node-a", vectorA, readBack.getEmbedding(MODEL_ID, "node-a"));
        assertVectorBytesEqual("node-b", vectorB, readBack.getEmbedding(MODEL_ID, "node-b"));
        assertVectorBytesEqual("node-c", vectorC, readBack.getEmbedding(MODEL_ID, "node-c"));

        assertEquals(JIPipeEmbeddingDatabase.computeHash("Text of node A"), readBack.getHash(MODEL_ID, "node-a"));
        assertEquals(JIPipeEmbeddingDatabase.computeHash("Text of node B"), readBack.getHash(MODEL_ID, "node-b"));
        assertEquals(JIPipeEmbeddingDatabase.computeHash("Text of node C"), readBack.getHash(MODEL_ID, "node-c"));
        assertNotNull(readBack.getHash(MODEL_ID, "node-a"));
        assertFalse(readBack.getHash(MODEL_ID, "node-a").isEmpty());
    }

    @Test
    void fileRecordsTheModelIdItWasWrittenWith() throws IOException {
        // The read-back contract of the DB build command: the header model id
        // of a written file must equal the id the vectors were built with, so
        // a file written for a different model id fails the equality check
        JIPipeEmbeddingDatabase database = new JIPipeEmbeddingDatabase(false);
        database.setEmbeddingWithHash(MODEL_ID, "node-a", new float[]{1.0f, 2.0f, 3.0f}, "Text of node A");

        Path file = tempDir.resolve("model-id.db");
        try (OutputStream os = Files.newOutputStream(file)) {
            database.saveToStream(os, MODEL_ID);
        }

        JIPipeEmbeddingDatabase.JembFileHeader header = JIPipeEmbeddingDatabase.readFileHeader(file);
        assertEquals(MODEL_ID, header.modelId());
        assertNotEquals(OTHER_MODEL_ID, header.modelId());
    }

    @Test
    void legacyV2FileIsRejectedWholesale() throws IOException {
        // The invalidation guarantee: a well-formed pre-restructure v2 file
        // (the format every existing user disk cache and the stale bundled
        // resource were written with) never loads
        Path file = tempDir.resolve("legacy-v2.db");
        writeLegacyVersionFile(file, 2);

        List<String> warns = new ArrayList<>();
        JIPipeEmbeddingDatabase readBack = new JIPipeEmbeddingDatabase(false);
        try (InputStream is = Files.newInputStream(file)) {
            readBack.loadFromStream(is, MODEL_ID, warnCapturingProgressInfo(warns), false);
        }

        // Rejected before any entry is inserted: the database stays empty
        assertTrue(readBack.getNodeIds(MODEL_ID).isEmpty());
        assertFalse(readBack.hasEmbedding(MODEL_ID, LEGACY_NODE_ID));
        assertEquals(-1, readBack.getDimension(MODEL_ID));

        // The warn names the found and the expected version
        assertTrue(warns.stream().anyMatch(w -> w.contains("version 2") && w.contains("Expected 3")),
                "The version rejection must warn with the found and the expected version, got: " + warns);
    }

    @Test
    void legacyV1FileIsRejectedWholesale() throws IOException {
        // Same gate for the oldest format (no per-entry hash field)
        Path file = tempDir.resolve("legacy-v1.db");
        writeLegacyVersionFile(file, 1);

        List<String> warns = new ArrayList<>();
        JIPipeEmbeddingDatabase readBack = new JIPipeEmbeddingDatabase(false);
        try (InputStream is = Files.newInputStream(file)) {
            readBack.loadFromStream(is, MODEL_ID, warnCapturingProgressInfo(warns), false);
        }

        assertTrue(readBack.getNodeIds(MODEL_ID).isEmpty());
        assertFalse(readBack.hasEmbedding(MODEL_ID, LEGACY_NODE_ID));
        assertEquals(-1, readBack.getDimension(MODEL_ID));

        assertTrue(warns.stream().anyMatch(w -> w.contains("version 1") && w.contains("Expected 3")),
                "The version rejection must warn with the found and the expected version, got: " + warns);
    }

    /**
     * Hand-craft a pre-v3 JEMB file with one well-formed entry, following the
     * byte layout of the pre-restructure writers. Deliberately not
     * {@link JIPipeEmbeddingDatabase#saveToStream(java.io.OutputStream, String)},
     * which writes the current version. Version 1 stores no per-entry hash;
     * version 2 and later do.
     */
    private static void writeLegacyVersionFile(Path file, int version) throws IOException {
        try (OutputStream os = Files.newOutputStream(file);
             DataOutputStream dos = new DataOutputStream(os)) {
            dos.write("JEMB".getBytes(StandardCharsets.US_ASCII));
            dos.writeInt(version);
            byte[] modelIdBytes = MODEL_ID.getBytes(StandardCharsets.UTF_8);
            dos.writeInt(modelIdBytes.length);
            dos.write(modelIdBytes);
            dos.writeInt(3); // dimension
            dos.writeInt(1); // entry count
            byte[] nodeIdBytes = LEGACY_NODE_ID.getBytes(StandardCharsets.UTF_8);
            dos.writeInt(nodeIdBytes.length);
            dos.write(nodeIdBytes);
            if (version >= 2) {
                byte[] hashBytes = JIPipeEmbeddingDatabase.computeHash("Pre-restructure text")
                        .getBytes(StandardCharsets.UTF_8);
                dos.writeInt(hashBytes.length);
                dos.write(hashBytes);
            }
            dos.writeFloat(0.5f);
            dos.writeFloat(0.25f);
            dos.writeFloat(-0.75f);
        }
    }

    /**
     * A progress info that collects every warn message (the version gate
     * reports rejections through {@code warn}).
     */
    private static JIPipeProgressInfo warnCapturingProgressInfo(List<String> warns) {
        return new JIPipeProgressInfo() {
            @Override
            public void warn(String message) {
                warns.add(message);
            }
        };
    }

    /**
     * Bit-exact vector comparison: identical raw float bit patterns (this is
     * stricter than {@code Arrays.equals} for the -0.0/NaN corner cases).
     */
    private static void assertVectorBytesEqual(String nodeId, float[] expected, float[] actual) {
        assertNotNull(actual, "Embedding of node '" + nodeId + "' is missing after the round trip");
        assertEquals(expected.length, actual.length, "Embedding dimension of node '" + nodeId + "' changed");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(Float.floatToRawIntBits(expected[i]), Float.floatToRawIntBits(actual[i]),
                    "Embedding component " + i + " of node '" + nodeId + "' changed");
        }
    }
}
