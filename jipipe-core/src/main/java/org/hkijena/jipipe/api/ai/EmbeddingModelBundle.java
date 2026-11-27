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

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Resolves the well-known {@code embedding-model-bundle} directory that release
 * packages ship next to the application: the pre-seeded embedding model files
 * plus the regenerated embedding database, so a fresh install needs no artifact
 * download and no recompute.
 * <p>
 * The resolution follows the jcef-bundle fallback chain (see {@code CefPlugin}):
 * install dir -&gt; shared dir -&gt; user dir. The first base directory that contains
 * a {@link #BUNDLE_DIR_NAME} subdirectory wins. Resolution is read-only: this
 * class never creates a bundle on the user's behalf.
 * <p>
 * The bundle directory is the artifact root, so the file layout inside
 * {@code embedding-model-bundle/} mirrors the artifact layout without the
 * {@code embedding/} prefix:
 * <ul>
 *     <li>{@code manifest.json} - the integrity manifest (schema below)</li>
 *     <li>{@code onnx/model.onnx} - the ONNX model (plus its external-data
 *     sibling {@code onnx/model.onnx_data}, which the ONNX runtime resolves
 *     relative to the model file)</li>
 *     <li>{@code tokenizer.json} - the tokenizer configuration</li>
 *     <li>{@code embeddings.db} - the regenerated embedding database</li>
 * </ul>
 * <p>
 * The manifest schema is:
 * <pre>{@code
 * {
 *     "modelId": "<full artifact id, e.g. cn.ac.baai.bge_small_en:1.5-any_onnx>",
 *     "version": 1,
 *     "files": {
 *         "onnx/model.onnx": "<sha256>",
 *         "onnx/model.onnx_data": "<sha256>",
 *         "tokenizer.json": "<sha256>"
 *     }
 * }
 * }</pre>
 * {@code version} is the manifest schema version, independent of the JEMB
 * format version of {@code embeddings.db}. Unknown extra keys are ignored.
 * Every listed file must exist and hash-match, otherwise the bundle is
 * rejected: verification is fail-closed, and a damaged bundle is never used
 * (the caller falls back to the artifact download). Hash results are cached
 * per bundle identity (paths, sizes, modification times), so repeated probes
 * (capability status, search availability) do not re-hash the model payload.
 */
public final class EmbeddingModelBundle {

    public static final String BUNDLE_DIR_NAME = "embedding-model-bundle";

    public static final String MANIFEST_NAME = "manifest.json";

    public static final String DB_FILE_NAME = "embeddings.db";

    /**
     * The manifest schema version this class parses. Manifests with a
     * different version are rejected (forward compatibility is fail-closed).
     */
    private static final int MANIFEST_SCHEMA_VERSION = 1;

    /**
     * The relative path of the ONNX model inside the bundle directory.
     */
    private static final String MODEL_ONNX_PATH = "onnx/model.onnx";

    /**
     * The relative path of the ONNX external-data file inside the bundle
     * directory (resolved by the ONNX runtime relative to the model file).
     */
    private static final String MODEL_ONNX_DATA_PATH = "onnx/model.onnx_data";

    /**
     * The relative path of the tokenizer inside the bundle directory.
     */
    private static final String TOKENIZER_JSON_PATH = "tokenizer.json";

    /**
     * The manifest entries a bundle must list: the files the runner consumes.
     * Only listed files are hash-verified, so a manifest that omits one of
     * these would serve an unverified model file and is rejected outright.
     */
    private static final List<String> REQUIRED_MANIFEST_FILES =
            List.of(MODEL_ONNX_PATH, MODEL_ONNX_DATA_PATH, TOKENIZER_JSON_PATH);

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddingModelBundle.class);

    /**
     * The hash buffer size for streaming SHA-256 (64 KB).
     */
    private static final int HASH_BUFFER_SIZE = 64 * 1024;

    /**
     * The last successfully validated bundle (guarded by its fingerprint).
     * Volatile fields with a benign race: concurrent validations of the same
     * bundle both hash and both store the same result.
     */
    private static volatile ResolvedBundle cachedValidBundle;
    private static volatile String cachedValidFingerprint;

    private EmbeddingModelBundle() {
    }

    /**
     * A validated bundle: the directory it was resolved from, the model and
     * tokenizer files the runner consumes, and the manifest model id.
     *
     * @param bundleDir     the bundle directory
     * @param modelOnnx     the ONNX model file ({@code onnx/model.onnx} inside the bundle)
     * @param tokenizerJson the tokenizer file ({@code tokenizer.json} inside the bundle)
     * @param modelId       the manifest model id (the full artifact id)
     */
    public record ResolvedBundle(Path bundleDir, Path modelOnnx, Path tokenizerJson, String modelId) {
    }

    /**
     * Resolves the bundle directory from the fallback chain. The first base
     * directory that contains a {@link #BUNDLE_DIR_NAME} subdirectory wins.
     *
     * @param installDir supplier of the installation directory (e.g. the ImageJ dir)
     * @param sharedDir  supplier of the shared directory
     * @param userDir    supplier of the user directory
     * @return the bundle directory, or empty when no base directory contains one
     */
    public static Optional<Path> resolveBundleDir(Supplier<Path> installDir, Supplier<Path> sharedDir, Supplier<Path> userDir) {
        for (Supplier<Path> supplier : List.of(installDir, sharedDir, userDir)) {
            Path base = supplier.get();
            if (base == null) {
                continue;
            }
            Path candidate = base.resolve(BUNDLE_DIR_NAME);
            if (Files.isDirectory(candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /**
     * Resolves the bundle directory from the production fallback chain:
     * ImageJ install dir -&gt; JIPipe shared dir -&gt; JIPipe user dir.
     *
     * @return the bundle directory, or empty when no location contains one
     */
    public static Optional<Path> resolveBundleDir() {
        return resolveBundleDir(PathUtils::getImageJDir, PathUtils::getJIPipeSharedDir, PathUtils::getJIPipeUserDir);
    }

    /**
     * Loads and verifies the bundle in the given directory. The manifest must
     * parse, carry a non-blank {@code modelId} and the supported schema
     * {@code version}, list all three runtime-critical files (the model, its
     * external data, and the tokenizer), and every listed file must exist and
     * hash-match.
     * <p>
     * Never throws: any problem is logged (malformed manifest at debug level,
     * integrity and completeness failures as warnings naming the file or
     * entry) and reported as an empty optional, so a damaged bundle can never
     * break the caller.
     *
     * @param bundleDir the bundle directory
     * @return the verified bundle, or empty when the bundle is absent or damaged
     */
    public static Optional<ResolvedBundle> load(Path bundleDir) {
        if (bundleDir == null || !Files.isDirectory(bundleDir)) {
            return Optional.empty();
        }
        Path manifestFile = bundleDir.resolve(MANIFEST_NAME);
        if (!Files.isRegularFile(manifestFile)) {
            LOGGER.debug("Embedding model bundle manifest not found: {}", manifestFile);
            return Optional.empty();
        }
        JsonNode manifest;
        try (InputStream is = Files.newInputStream(manifestFile)) {
            manifest = JsonUtils.getObjectMapper().readTree(is);
        } catch (Exception e) {
            LOGGER.debug("Embedding model bundle manifest is malformed: {}", manifestFile, e);
            return Optional.empty();
        }
        if (manifest == null || !manifest.isObject()) {
            LOGGER.debug("Embedding model bundle manifest is not a JSON object: {}", manifestFile);
            return Optional.empty();
        }
        String modelId = manifest.path("modelId").asText("");
        if (StringUtils.isNullOrEmpty(modelId) || modelId.isBlank()) {
            LOGGER.debug("Embedding model bundle manifest has no model id: {}", manifestFile);
            return Optional.empty();
        }
        int version = manifest.path("version").asInt(-1);
        if (version != MANIFEST_SCHEMA_VERSION) {
            LOGGER.debug("Embedding model bundle manifest has unsupported version {} (expected {}): {}",
                    version, MANIFEST_SCHEMA_VERSION, manifestFile);
            return Optional.empty();
        }
        JsonNode filesNode = manifest.path("files");
        if (!filesNode.isObject() || filesNode.isEmpty()) {
            LOGGER.debug("Embedding model bundle manifest lists no files: {}", manifestFile);
            return Optional.empty();
        }
        Map<String, String> listedFiles = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> entry : filesNode.properties()) {
            String hash = entry.getValue() == null ? "" : entry.getValue().asText("");
            listedFiles.put(entry.getKey(), hash);
        }

        // Completeness guard: only listed files are hash-verified, so a
        // manifest that omits one of the runtime-critical entries would serve
        // an unverified model. Reject it before anything is verified or cached.
        for (String requiredPath : REQUIRED_MANIFEST_FILES) {
            if (!listedFiles.containsKey(requiredPath)) {
                LOGGER.warn("Embedding model bundle manifest does not list the required file: {} ({})",
                        requiredPath, bundleDir);
                return Optional.empty();
            }
        }

        // Cheap identity fingerprint (paths, sizes, modification times). When it
        // matches the last successful validation, the hashes are re-used, so
        // repeated probes do not re-hash the model payload.
        String fingerprint = fingerprint(bundleDir, manifestFile, listedFiles.keySet());
        if (fingerprint != null && cachedValidBundle != null && fingerprint.equals(cachedValidFingerprint)) {
            return Optional.of(cachedValidBundle);
        }

        for (Map.Entry<String, String> entry : listedFiles.entrySet()) {
            String relativePath = entry.getKey();
            String expectedHash = entry.getValue();
            Path file = bundleDir.resolve(relativePath).normalize();
            if (!file.startsWith(bundleDir.normalize())) {
                LOGGER.warn("Embedding model bundle manifest lists a file outside the bundle directory: {} ({})",
                        relativePath, bundleDir);
                return Optional.empty();
            }
            if (StringUtils.isNullOrEmpty(expectedHash)) {
                LOGGER.warn("Embedding model bundle manifest has no hash for file: {} ({})", relativePath, bundleDir);
                return Optional.empty();
            }
            if (!Files.isRegularFile(file)) {
                LOGGER.warn("Embedding model bundle file is missing: {} ({})", file, bundleDir);
                return Optional.empty();
            }
            String actualHash = sha256(file);
            if (actualHash == null || !actualHash.equalsIgnoreCase(expectedHash)) {
                LOGGER.warn("Embedding model bundle file failed the integrity check: {} ({})", file, bundleDir);
                return Optional.empty();
            }
        }

        Path modelOnnx = bundleDir.resolve(MODEL_ONNX_PATH);
        Path tokenizerJson = bundleDir.resolve(TOKENIZER_JSON_PATH);
        if (!Files.isRegularFile(modelOnnx) || !Files.isRegularFile(tokenizerJson)) {
            LOGGER.warn("Embedding model bundle is missing the model or tokenizer file: {} ({})", bundleDir, manifestFile);
            return Optional.empty();
        }
        ResolvedBundle bundle = new ResolvedBundle(bundleDir, modelOnnx, tokenizerJson, modelId);
        if (fingerprint != null) {
            cachedValidBundle = bundle;
            cachedValidFingerprint = fingerprint;
        }
        return Optional.of(bundle);
    }

    /**
     * Resolves and verifies the bundle from the production fallback chain.
     *
     * @return the verified bundle, or empty when no bundle is present or the
     * bundle failed the verification
     */
    public static Optional<ResolvedBundle> resolveValidated() {
        Optional<Path> bundleDir = resolveBundleDir();
        if (bundleDir.isEmpty()) {
            return Optional.empty();
        }
        return load(bundleDir.get());
    }

    /**
     * Checks whether the bundle satisfies the expected model id.
     * <p>
     * When {@code expectedModelIdOrPrefix} contains a colon, it is a full
     * artifact id and the match is exact equality against the manifest model
     * id. Without a colon it is a coordinate prefix (group and artifact id,
     * e.g. {@code cn.ac.baai.bge_small_en}) and the manifest model id must
     * start with {@code expected + ":"}, so every version and classifier of
     * the coordinate matches but a distinct coordinate that merely shares the
     * prefix does not.
     * <p>
     * Callers in this codebase pass the coordinate prefix form
     * ({@code AIPlugin.DEFAULT_EMBEDDING_ARTIFACT_PREFIX}): on a fresh install
     * no full artifact id is derivable before the environment is configured
     * from the bundle itself, so the prefix form is the only meaningful
     * expectation for both the service wiring and the capability probe.
     *
     * @param bundle                  the verified bundle
     * @param expectedModelIdOrPrefix the full artifact id or the coordinate prefix
     * @return true when the bundle's model id matches the expectation
     */
    public static boolean isUsableFor(ResolvedBundle bundle, String expectedModelIdOrPrefix) {
        if (bundle == null || StringUtils.isNullOrEmpty(expectedModelIdOrPrefix)) {
            return false;
        }
        if (expectedModelIdOrPrefix.indexOf(':') < 0) {
            return bundle.modelId().startsWith(expectedModelIdOrPrefix + ":");
        }
        return bundle.modelId().equals(expectedModelIdOrPrefix);
    }

    /**
     * Resolves the regenerated embedding database inside the bundle directory
     * from the production fallback chain. The database ships as a package file
     * next to the model (not as a jar resource); the classpath resource remains
     * the dev-build source and the fallback.
     * <p>
     * Presence is checked, not content: the JEMB format version gate is the
     * load path's business (the database class rejects unsupported versions).
     *
     * @return the database file, or empty when no bundle directory contains one
     */
    public static Optional<Path> resolveDatabaseFile() {
        return resolveDatabaseFile(PathUtils::getImageJDir, PathUtils::getJIPipeSharedDir, PathUtils::getJIPipeUserDir);
    }

    /**
     * Resolves the regenerated embedding database inside the bundle directory
     * from the given fallback chain.
     *
     * @param installDir supplier of the installation directory (e.g. the ImageJ dir)
     * @param sharedDir  supplier of the shared directory
     * @param userDir    supplier of the user directory
     * @return the database file, or empty when no base directory provides one
     */
    public static Optional<Path> resolveDatabaseFile(Supplier<Path> installDir, Supplier<Path> sharedDir, Supplier<Path> userDir) {
        return resolveBundleDir(installDir, sharedDir, userDir).flatMap(bundleDir -> {
            Path databaseFile = bundleDir.resolve(DB_FILE_NAME);
            if (Files.isRegularFile(databaseFile)) {
                return Optional.of(databaseFile);
            }
            return Optional.empty();
        });
    }

    /**
     * Builds the cheap identity fingerprint of a bundle: the bundle directory,
     * the manifest identity (size and modification time) and the identity of
     * every listed file (size and modification time).
     *
     * @return the fingerprint, or null when a file could not be inspected
     * (the caller then verifies fully instead of using the cache)
     */
    private static String fingerprint(Path bundleDir, Path manifestFile, Iterable<String> listedPaths) {
        try {
            StringBuilder builder = new StringBuilder();
            builder.append(bundleDir.toAbsolutePath().normalize());
            appendFileIdentity(builder, manifestFile);
            for (String listedPath : listedPaths) {
                builder.append('|').append(listedPath);
                appendFileIdentity(builder, bundleDir.resolve(listedPath));
            }
            return builder.toString();
        } catch (Exception e) {
            LOGGER.debug("Could not fingerprint the embedding model bundle at {}", bundleDir, e);
            return null;
        }
    }

    /**
     * Appends the size and modification time of one file to the fingerprint.
     */
    private static void appendFileIdentity(StringBuilder builder, Path file) throws IOException {
        builder.append('|');
        if (!Files.isRegularFile(file)) {
            builder.append("missing");
            return;
        }
        builder.append(Files.size(file)).append(':').append(Files.getLastModifiedTime(file).toMillis());
    }

    /**
     * Computes the SHA-256 hash of a file, streaming with 64 KB buffers.
     *
     * @return the lowercase hex hash, or null when the file could not be read
     */
    private static String sha256(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(file)) {
                byte[] buffer = new byte[HASH_BUFFER_SIZE];
                int read;
                while ((read = is.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            LOGGER.warn("Could not hash the embedding model bundle file: {}", file, e);
            return null;
        }
    }
}
