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

package org.hkijena.jipipe.api.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gate for the embedding model bundle resolution: the fallback chain order,
 * the fail-closed integrity verification (a damaged or tampered bundle must
 * never resolve), and the model id match semantics the service wiring and the
 * capability probe rely on.
 * <p>
 * Pure class: no JIPipe boot, no model, no Swing.
 */
class EmbeddingModelBundleTest {

    private static final String MODEL_ID = "cn.ac.baai.bge_small_en:1.5-any_onnx";

    private static final String OTHER_MODEL_ID = "cn.ac.baai.other_model:2.0-any_onnx";

    @TempDir
    Path tempDir;

    // ===== Fallback chain order =====

    @Test
    void installDirWinsOverSharedAndUserDir() throws IOException {
        Path installBase = Files.createDirectory(tempDir.resolve("install"));
        Path sharedBase = Files.createDirectory(tempDir.resolve("shared"));
        Path userBase = Files.createDirectory(tempDir.resolve("user"));
        createValidBundle(installBase, MODEL_ID);
        createValidBundle(sharedBase, MODEL_ID);
        createValidBundle(userBase, MODEL_ID);

        Optional<Path> resolved = EmbeddingModelBundle.resolveBundleDir(
                () -> installBase, () -> sharedBase, () -> userBase);

        assertTrue(resolved.isPresent());
        assertEquals(installBase.resolve(EmbeddingModelBundle.BUNDLE_DIR_NAME), resolved.get());
    }

    @Test
    void sharedDirWinsOverUserDir() throws IOException {
        Path sharedBase = Files.createDirectory(tempDir.resolve("shared"));
        Path userBase = Files.createDirectory(tempDir.resolve("user"));
        createValidBundle(sharedBase, MODEL_ID);
        createValidBundle(userBase, MODEL_ID);

        Optional<Path> resolved = EmbeddingModelBundle.resolveBundleDir(
                () -> tempDir.resolve("install-does-not-exist"), () -> sharedBase, () -> userBase);

        assertTrue(resolved.isPresent());
        assertEquals(sharedBase.resolve(EmbeddingModelBundle.BUNDLE_DIR_NAME), resolved.get());
    }

    @Test
    void absentBaseDirectoriesAreSkipped() throws IOException {
        Path userBase = Files.createDirectory(tempDir.resolve("user"));
        createValidBundle(userBase, MODEL_ID);

        Optional<Path> resolved = EmbeddingModelBundle.resolveBundleDir(
                () -> tempDir.resolve("install-does-not-exist"),
                () -> tempDir.resolve("shared-does-not-exist"),
                () -> userBase);

        assertTrue(resolved.isPresent());
        assertEquals(userBase.resolve(EmbeddingModelBundle.BUNDLE_DIR_NAME), resolved.get());
    }

    @Test
    void bundleDirNameMustBeADirectory() throws IOException {
        Path installBase = Files.createDirectory(tempDir.resolve("install"));
        // A regular file named like the bundle directory must not win
        Files.write(installBase.resolve(EmbeddingModelBundle.BUNDLE_DIR_NAME),
                "not a directory".getBytes(StandardCharsets.UTF_8));

        Optional<Path> resolved = EmbeddingModelBundle.resolveBundleDir(
                () -> installBase, () -> null, () -> null);

        assertTrue(resolved.isEmpty());
    }

    // ===== Integrity verification (fail-closed) =====

    @Test
    void validBundleResolvesWithExpectedPathsAndModelId() throws IOException {
        Path base = Files.createDirectory(tempDir.resolve("install"));
        Path bundleDir = createValidBundle(base, MODEL_ID);

        Optional<EmbeddingModelBundle.ResolvedBundle> loaded = EmbeddingModelBundle.load(bundleDir);

        assertTrue(loaded.isPresent());
        assertEquals(bundleDir, loaded.get().bundleDir());
        assertEquals(bundleDir.resolve("onnx").resolve("model.onnx"), loaded.get().modelOnnx());
        assertEquals(bundleDir.resolve("tokenizer.json"), loaded.get().tokenizerJson());
        assertEquals(MODEL_ID, loaded.get().modelId());
        assertTrue(Files.isRegularFile(loaded.get().modelOnnx()));
        assertTrue(Files.isRegularFile(loaded.get().tokenizerJson()));
    }

    @Test
    void malformedManifestIsRejected() throws IOException {
        Path base = Files.createDirectory(tempDir.resolve("install"));
        Path bundleDir = createBundlePayload(base);
        Files.write(bundleDir.resolve(EmbeddingModelBundle.MANIFEST_NAME),
                "{ this is not json".getBytes(StandardCharsets.UTF_8));

        assertTrue(EmbeddingModelBundle.load(bundleDir).isEmpty());
    }

    @Test
    void missingManifestIsRejected() throws IOException {
        Path base = Files.createDirectory(tempDir.resolve("install"));
        Path bundleDir = createBundlePayload(base);

        assertTrue(EmbeddingModelBundle.load(bundleDir).isEmpty());
    }

    @Test
    void blankModelIdIsRejected() throws IOException {
        Path base = Files.createDirectory(tempDir.resolve("install"));
        Path bundleDir = createBundlePayload(base);
        writeManifest(bundleDir, manifestWithFiles("   ", filesJsonObject(bundleDir)));

        assertTrue(EmbeddingModelBundle.load(bundleDir).isEmpty());
    }

    @Test
    void missingModelIdIsRejected() throws IOException {
        Path base = Files.createDirectory(tempDir.resolve("install"));
        Path bundleDir = createBundlePayload(base);
        writeManifest(bundleDir, """
                {
                    "version": 1,
                    "files": %s
                }""".formatted(filesJsonObject(bundleDir)));

        assertTrue(EmbeddingModelBundle.load(bundleDir).isEmpty());
    }

    @Test
    void unsupportedManifestVersionIsRejected() throws IOException {
        Path base = Files.createDirectory(tempDir.resolve("install"));
        Path bundleDir = createBundlePayload(base);
        // A future manifest schema version must not be parsed by this runtime
        writeManifest(bundleDir, """
                {
                    "modelId": "%s",
                    "version": 2,
                    "files": %s
                }""".formatted(MODEL_ID, filesJsonObject(bundleDir)));

        assertTrue(EmbeddingModelBundle.load(bundleDir).isEmpty());
    }

    @Test
    void missingListedFileIsRejected() throws IOException {
        Path base = Files.createDirectory(tempDir.resolve("install"));
        Path bundleDir = createBundlePayload(base);
        writeManifest(bundleDir, manifestWithFiles(MODEL_ID, filesJsonObject(bundleDir)));
        Files.delete(bundleDir.resolve("onnx").resolve("model.onnx_data"));

        assertTrue(EmbeddingModelBundle.load(bundleDir).isEmpty());
    }

    @Test
    void tamperedFileFailsTheHashCheck() throws IOException {
        Path base = Files.createDirectory(tempDir.resolve("install"));
        Path bundleDir = createBundlePayload(base);
        writeManifest(bundleDir, manifestWithFiles(MODEL_ID, filesJsonObject(bundleDir)));
        // Overwrite the tokenizer with different content after the manifest was written
        Files.write(bundleDir.resolve("tokenizer.json"), "tampered".getBytes(StandardCharsets.UTF_8));

        assertTrue(EmbeddingModelBundle.load(bundleDir).isEmpty());
    }

    @Test
    void truncatedExternalDataFileFailsTheHashCheck() throws IOException {
        Path base = Files.createDirectory(tempDir.resolve("install"));
        Path bundleDir = createBundlePayload(base);
        writeManifest(bundleDir, manifestWithFiles(MODEL_ID, filesJsonObject(bundleDir)));
        // A truncated payload: same kind of file, fewer bytes
        Files.write(bundleDir.resolve("onnx").resolve("model.onnx_data"), new byte[4]);

        assertTrue(EmbeddingModelBundle.load(bundleDir).isEmpty());
    }

    @Test
    void manifestEntryOutsideTheBundleDirIsRejected() throws IOException {
        Path base = Files.createDirectory(tempDir.resolve("install"));
        Path bundleDir = createBundlePayload(base);
        Files.write(tempDir.resolve("outside.txt"), "outside".getBytes(StandardCharsets.UTF_8));
        // A path-traversal entry must be refused, never resolved
        writeManifest(bundleDir, """
                {
                    "modelId": "%s",
                    "version": 1,
                    "files": {
                        "onnx/model.onnx": "%s",
                        "onnx/model.onnx_data": "%s",
                        "tokenizer.json": "%s",
                        "../outside.txt": "%s"
                    }
                }""".formatted(
                MODEL_ID,
                sha256Of(bundleDir.resolve("onnx").resolve("model.onnx")),
                sha256Of(bundleDir.resolve("onnx").resolve("model.onnx_data")),
                sha256Of(bundleDir.resolve("tokenizer.json")),
                sha256Of(tempDir.resolve("outside.txt"))));

        assertTrue(EmbeddingModelBundle.load(bundleDir).isEmpty());
    }

    @Test
    void unknownManifestKeysAreIgnored() throws IOException {
        Path base = Files.createDirectory(tempDir.resolve("install"));
        Path bundleDir = createBundlePayload(base);
        writeManifest(bundleDir, """
                {
                    "modelId": "%s",
                    "version": 1,
                    "comment": "some future key",
                    "files": %s,
                    "extra": {"nested": true}
                }""".formatted(MODEL_ID, filesJsonObject(bundleDir)));

        Optional<EmbeddingModelBundle.ResolvedBundle> loaded = EmbeddingModelBundle.load(bundleDir);
        assertTrue(loaded.isPresent());
        assertEquals(MODEL_ID, loaded.get().modelId());
    }

    // ===== Model id match semantics =====

    @Test
    void fullIdMatchesExactly() throws IOException {
        EmbeddingModelBundle.ResolvedBundle bundle = loadValidBundle(MODEL_ID);
        assertTrue(EmbeddingModelBundle.isUsableFor(bundle, MODEL_ID));
    }

    @Test
    void differentFullIdDoesNotMatch() throws IOException {
        EmbeddingModelBundle.ResolvedBundle bundle = loadValidBundle(MODEL_ID);
        assertFalse(EmbeddingModelBundle.isUsableFor(bundle, OTHER_MODEL_ID));
    }

    @Test
    void coordinatePrefixMatchesSameCoordinate() throws IOException {
        EmbeddingModelBundle.ResolvedBundle bundle = loadValidBundle(MODEL_ID);
        assertTrue(EmbeddingModelBundle.isUsableFor(bundle, "cn.ac.baai.bge_small_en"));
    }

    @Test
    void coordinatePrefixDoesNotMatchADistinctCoordinate() throws IOException {
        EmbeddingModelBundle.ResolvedBundle bundle = loadValidBundle(MODEL_ID);
        assertFalse(EmbeddingModelBundle.isUsableFor(bundle, "cn.ac.baai.other_model"));
    }

    @Test
    void nullBundleAndBlankExpectationDoNotMatch() {
        assertFalse(EmbeddingModelBundle.isUsableFor(null, MODEL_ID));
        assertFalse(EmbeddingModelBundle.isUsableFor(null, "cn.ac.baai.bge_small_en"));
    }

    // ===== Database file resolution =====

    @Test
    void databaseFileResolvesOnlyWhenPresent() throws IOException {
        Path installBase = Files.createDirectory(tempDir.resolve("install"));
        Path bundleDir = createValidBundle(installBase, MODEL_ID);

        // No database file yet
        assertTrue(EmbeddingModelBundle.resolveDatabaseFile(
                () -> installBase, () -> null, () -> null).isEmpty());

        Files.write(bundleDir.resolve(EmbeddingModelBundle.DB_FILE_NAME), new byte[]{1, 2, 3});
        Optional<Path> databaseFile = EmbeddingModelBundle.resolveDatabaseFile(
                () -> installBase, () -> null, () -> null);
        assertTrue(databaseFile.isPresent());
        assertEquals(bundleDir.resolve(EmbeddingModelBundle.DB_FILE_NAME), databaseFile.get());
    }

    @Test
    void databaseFileMustBeARegularFile() throws IOException {
        Path installBase = Files.createDirectory(tempDir.resolve("install"));
        Path bundleDir = createValidBundle(installBase, MODEL_ID);
        // A directory named like the database file is not a database
        Files.createDirectories(bundleDir.resolve(EmbeddingModelBundle.DB_FILE_NAME));

        assertTrue(EmbeddingModelBundle.resolveDatabaseFile(
                () -> installBase, () -> null, () -> null).isEmpty());
    }

    // ===== Helpers =====

    /**
     * Creates a valid bundle in {@code <base>/embedding-model-bundle} with a
     * small synthetic payload and a correct manifest.
     *
     * @return the bundle directory
     */
    private Path createValidBundle(Path base, String modelId) throws IOException {
        Path bundleDir = createBundlePayload(base);
        writeManifest(bundleDir, manifestWithFiles(modelId, filesJsonObject(bundleDir)));
        return bundleDir;
    }

    /**
     * Loads a valid bundle (and fails the test when it does not resolve).
     */
    private EmbeddingModelBundle.ResolvedBundle loadValidBundle(String modelId) throws IOException {
        Path base = Files.createDirectory(tempDir.resolve("bundle-" + modelId.replace(':', '_')));
        Path bundleDir = createValidBundle(base, modelId);
        Optional<EmbeddingModelBundle.ResolvedBundle> loaded = EmbeddingModelBundle.load(bundleDir);
        assertTrue(loaded.isPresent(), "The test bundle must be valid");
        return loaded.get();
    }

    /**
     * Creates the bundle payload (model, external data, tokenizer) without a manifest.
     *
     * @return the bundle directory
     */
    private Path createBundlePayload(Path base) throws IOException {
        Path bundleDir = Files.createDirectories(base.resolve(EmbeddingModelBundle.BUNDLE_DIR_NAME));
        Path onnxDir = Files.createDirectories(bundleDir.resolve("onnx"));
        Files.write(onnxDir.resolve("model.onnx"), new byte[]{1, 2, 3, 4, 5});
        Files.write(onnxDir.resolve("model.onnx_data"), new byte[]{6, 7, 8, 9, 10, 11, 12});
        Files.write(bundleDir.resolve("tokenizer.json"), "{\"vocab\": []}".getBytes(StandardCharsets.UTF_8));
        return bundleDir;
    }

    /**
     * @return the files JSON object with the current payload hashes
     */
    private String filesJsonObject(Path bundleDir) throws IOException {
        return """
                {
                    "onnx/model.onnx": "%s",
                    "onnx/model.onnx_data": "%s",
                    "tokenizer.json": "%s"
                }""".formatted(
                sha256Of(bundleDir.resolve("onnx").resolve("model.onnx")),
                sha256Of(bundleDir.resolve("onnx").resolve("model.onnx_data")),
                sha256Of(bundleDir.resolve("tokenizer.json")));
    }

    /**
     * @return a manifest JSON with the given model id and files section
     */
    private String manifestWithFiles(String modelId, String filesJson) {
        return """
                {
                    "modelId": "%s",
                    "version": 1,
                    "files": %s
                }""".formatted(modelId, filesJson);
    }

    /**
     * Writes the manifest.
     */
    private void writeManifest(Path bundleDir, String json) throws IOException {
        Files.write(bundleDir.resolve(EmbeddingModelBundle.MANIFEST_NAME),
                json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @return the SHA-256 hash of a file as lowercase hex
     */
    private String sha256Of(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new IOException("SHA-256 is unavailable", e);
        }
        try (InputStream is = Files.newInputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = is.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
