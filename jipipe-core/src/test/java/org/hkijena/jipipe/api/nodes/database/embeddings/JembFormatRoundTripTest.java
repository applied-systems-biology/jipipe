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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
 * Uses synthetic vectors and hashes with a fixed model id; no JIPipe boot
 * and no embedding model are needed.
 */
class JembFormatRoundTripTest {

    private static final String MODEL_ID = "test.model:1.0-any_onnx";

    private static final String OTHER_MODEL_ID = "other.model:2.0-any_onnx";

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
