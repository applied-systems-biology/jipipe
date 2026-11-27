package org.hkijena.jipipe.launcher.commands;

import net.imagej.ImageJ;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.ai.JIPipeOnnxEmbeddingAIModelRunner;
import org.hkijena.jipipe.api.microservice.MicroserviceState;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabase;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.embeddings.JIPipeEmbeddingDatabase;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceInitializationSettings;
import org.hkijena.jipipe.api.service.JIPipeServiceMode;
import org.hkijena.jipipe.plugins.ai.AIApplicationSettings;
import org.hkijena.jipipe.plugins.ai.environments.EmbeddingModelEnvironment;
import org.hkijena.jipipe.plugins.settings.application.JIPipeExtensionApplicationSettings;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The headless {@code build-embedding-db} command: computes embeddings for
 * every node database entry with an ONNX embedding model runner constructed
 * directly from the given files (no AI service is started) and writes a JEMB
 * file, reading it back before exiting. Part of the distribution embedding
 * DB regeneration (#1410).
 *
 * <p>Model file layout: {@code --model} must point at the ONNX model file
 * (e.g. {@code onnx/model.onnx}); if the model was exported with external
 * data, the external data file ({@code model.onnx_data}) must sit next to
 * it, because the ONNX Runtime resolves external data relative to the model
 * file. {@code --tokenizer} points at the HuggingFace tokenizer file
 * ({@code tokenizer.json}); the tokenizer reader consumes only that file.</p>
 *
 * <p>The command boots the full JIPipe registry (never {@code --fast-init},
 * so the database covers the complete node set), then embeds
 * {@link JIPipeEmbeddingDatabase#entryToText(JIPipeNodeDatabaseEntry)} of each
 * entry keyed by {@link JIPipeEmbeddingDatabase#entryToId(JIPipeNodeDatabaseEntry)}
 * under the model id. The default model id is derived the same way the AI
 * service derives it for the default embedding model artifact, so a database
 * built with the default is consumed by the runtime unchanged; the
 * {@code --model-id} override skips the derivation.</p>
 *
 * <p>Registry scope caveat: "complete node set" means complete for the
 * environment the command boots in. A Maven/dev classpath boot yields a
 * PARTIAL registry - the JIPipe-authored nodes only; the runtime
 * auto-imported algorithm families that exist only in the packaged Fiji
 * plugin jars (ij2, clij, trackmate, ...) are absent. The distribution
 * pipeline therefore regenerates the shipped database through the packaged
 * Fiji launcher, not through a Maven classpath (#1410).</p>
 *
 * <p>The {@code --threads} flag parallelizes the embed calls. Values above 1
 * assume the embedding model runner tolerates concurrent embed calls (the
 * ONNX session is documented thread-safe; the shared tokenizer carries no
 * documented thread-safety guarantee), so 1 is the safe choice.</p>
 *
 * <p>Failure handling is fail-closed: any entry whose embedding throws or is
 * empty aborts the run with exit code 1 and the database is NOT written (no
 * partial file is ever shipped). Before reporting success, the written file
 * is validated by a built-in read-back: the file header's format version,
 * model id, and entry count and a seeded 10 percent sample of vectors and
 * hashes are compared against the in-memory data; any mismatch or exception
 * aborts with exit code 1 and removes the written output file and any
 * {@code .tmp} residue (once the file is moved into place it is this run's
 * product, so a failed verification must not leave a half-verified database
 * behind; an output file that existed before the run and pre-move failure
 * paths are never touched).</p>
 */
public class BuildEmbeddingDbCommand {

    /**
     * How long to wait for the asynchronously built node database entries
     * before failing (the full registry is large; the rebuild runs on a
     * background queue - the {@code NodeSearchGoldenSetTest} await pattern
     * with a wider bound for cold starts).
     */
    private static final long DATABASE_AWAIT_TIMEOUT_MS = 300_000;

    /**
     * The modulo of the deterministic read-back sample: every 10th entry id
     * is compared against the in-memory database (about 10 percent).
     */
    private static final int READ_BACK_SAMPLE_STRIDE = 10;

    /**
     * Side file next to {@code --output} that a run writes before the atomic
     * move; deleted on every failure path so a failed run leaves no residue.
     */
    private static final String TEMP_FILE_SUFFIX = ".tmp";

    /**
     * Maximum number of per-entry failures printed in the failure summary.
     */
    private static final int MAX_PRINTED_FAILURES = 20;

    /**
     * Runs the {@code build-embedding-db} command.
     * Fatal states exit the JVM with status 1 (the
     * {@link JIPipe#abortIfInitializationFailed} pattern); a successful run
     * returns and the launcher exits with status 0.
     *
     * @param argsList the command line arguments (the subcommand token removed)
     */
    public static void doBuildEmbeddingDb(List<String> argsList) {
        try {
            run(argsList);
        } catch (Throwable t) {
            System.err.println("build-embedding-db failed: " + t);
            System.err.println(ExceptionUtils.getStackTrace(t));
            System.err.flush();
            System.exit(1);
        }
    }

    private static void run(List<String> argsList) throws IOException {
        long startTime = System.currentTimeMillis();

        Path modelPath = null;
        Path tokenizerPath = null;
        Path outputPath = null;
        String modelId = null;
        int numThreads = 1;
        boolean verbose = false;

        // Parse flags
        for (int i = 0; i < argsList.size(); i++) {
            String arg = argsList.get(i);
            boolean success = false;
            if (arg.equals("--verbose")) {
                verbose = true;
                success = true;
            }

            // Delete the flag
            if (success) {
                argsList.remove(i);
                --i;
            }
        }

        // Parse key value pairs
        for (int i = 0; i < argsList.size(); i += 2) {
            String arg = argsList.get(i);
            if (i + 1 >= argsList.size()) {
                System.err.println("Missing value for argument: " + arg);
                HelpCommand.showHelp();
                System.exit(1);
            }
            String value = argsList.get(i + 1);
            if (arg.equals("--model")) {
                modelPath = Paths.get(value);
            } else if (arg.equals("--tokenizer")) {
                tokenizerPath = Paths.get(value);
            } else if (arg.equals("--output")) {
                outputPath = Paths.get(value);
            } else if (arg.equals("--model-id")) {
                modelId = value;
            } else if (arg.equals("--threads")) {
                try {
                    numThreads = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid number of threads: " + value);
                    System.exit(1);
                }
            } else {
                System.err.println("Unknown argument: " + arg);
                HelpCommand.showHelp();
                System.exit(1);
            }
        }

        // Validate arguments before any boot
        if (modelPath == null || tokenizerPath == null || outputPath == null) {
            System.err.println("Missing required arguments: --model, --tokenizer, and --output are required");
            HelpCommand.showHelp();
            System.exit(1);
        }
        if (numThreads < 1) {
            System.err.println("Invalid number of threads: " + numThreads);
            System.exit(1);
        }
        if (!Files.isRegularFile(modelPath)) {
            System.err.println("Model file does not exist: " + modelPath);
            System.exit(1);
        }
        if (!Files.isRegularFile(tokenizerPath)) {
            System.err.println("Tokenizer file does not exist: " + tokenizerPath);
            System.exit(1);
        }

        // The ONNX external data file must sit next to the model file (the
        // ONNX Runtime resolves external data relative to the model file).
        // Only a warning: a model without external data is also valid.
        Path externalDataPath = modelPath.resolveSibling(modelPath.getFileName().toString() + "_data");
        if (!Files.isRegularFile(externalDataPath)) {
            System.err.println("Warning: ONNX external data file not found: " + externalDataPath);
            System.err.println("If the model was exported with external data (model.onnx_data), it must sit next to the model file.");
        }

        // Probe that the output location is writable before any boot, so an
        // unwritable destination fails fast instead of after the embedding run
        try {
            Path outputParent = outputPath.toAbsolutePath().getParent();
            if (outputParent != null && !Files.exists(outputParent)) {
                Files.createDirectories(outputParent);
            }
            Path probePath = outputPath.resolveSibling(outputPath.getFileName().toString() + ".write-probe");
            Files.write(probePath, new byte[0]);
            Files.deleteIfExists(probePath);
        } catch (IOException e) {
            System.err.println("Cannot write the output file '" + outputPath + "': " + e);
            System.err.println("Check that the output directory exists and is writable.");
            System.exit(1);
        }

        // Boot JIPipe headless with the full registry init (never fast-init:
        // the database must cover the complete node set)
        JIPipeServiceInitializationSettings initializationSettings = new JIPipeServiceInitializationSettings();
        initializationSettings.setMode(JIPipeServiceMode.Headless);
        initializationSettings.setVerbose(verbose);

        System.out.println("Initializing JIPipe (headless, full registry) ...");
        final ImageJ ij = new ImageJ();
        JIPipeService service = JIPipe.createInstance(ij.context(), initializationSettings);
        JIPipeExtensionApplicationSettings extensionSettings = JIPipeExtensionApplicationSettings.getInstanceFromRaw();
        extensionSettings.setSilent(true);
        service.ensureInitialized();
        // Fail-loud startup: a failed initialization (e.g. an aborted settings
        // migration) leaves the service in Error state; continuing would build
        // a database against a broken registry.
        JIPipe.abortIfInitializationFailed(service);

        // Default model id: derived exactly the way the AI service derives it
        // for the default embedding model artifact (the settings sheet builds
        // the artifact-loaded environment with the default artifact query), so
        // the runtime consumes the built database unchanged. Must run after
        // the boot: the derivation needs the artifact registry.
        if (StringUtils.isNullOrEmpty(modelId)) {
            EmbeddingModelEnvironment environment = AIApplicationSettings.getInstance().getEmbeddingModelEnvironment();
            modelId = environment.deriveModelId();
        }
        if (StringUtils.isNullOrEmpty(modelId)) {
            System.err.println("Could not derive the model id for the default embedding model artifact.");
            System.err.println("Pass --model-id <id> explicitly.");
            System.exit(1);
        }
        System.out.println("Model id: " + modelId);

        // Await the node database entries (bounded, the golden gate's shape)
        JIPipeNodeDatabase database = JIPipeNodeDatabase.getInstance();
        awaitDatabaseEntries(database);

        // Construct the runner directly from the given files: no AI service,
        // no queues - embed() calls go straight to the ONNX session
        System.out.println("Loading embedding model ...");
        JIPipeOnnxEmbeddingAIModelRunner runner = new JIPipeOnnxEmbeddingAIModelRunner(modelPath, tokenizerPath, modelId);
        runner.start();
        if (runner.getStatus() != MicroserviceState.Ready) {
            System.err.println("Could not load the embedding model (state: " + runner.getStatus() + ")");
            if (!StringUtils.isNullOrEmpty(runner.getLastError())) {
                System.err.println("Last error: " + runner.getLastError());
            }
            System.err.println("The model file must be a valid ONNX model; if it was exported with external data, the data file must sit next to it.");
            System.exit(1);
        }

        // Render the embedding texts on this thread (registry reads), keyed by
        // the entry id. Duplicate ids (e.g. alias entries) keep the first text.
        List<JIPipeNodeDatabaseEntry> entries = new ArrayList<>(database.getEntries());
        LinkedHashMap<String, String> texts = new LinkedHashMap<>();
        int duplicateIds = 0;
        for (JIPipeNodeDatabaseEntry entry : entries) {
            String nodeId = JIPipeEmbeddingDatabase.entryToId(entry);
            String text = JIPipeEmbeddingDatabase.entryToText(entry);
            if (texts.putIfAbsent(nodeId, text) != null) {
                ++duplicateIds;
            }
        }

        // Embed every entry. Failures are recorded per entry; the run is
        // fail-closed: any failure aborts before the database is written.
        JIPipeEmbeddingDatabase db = new JIPipeEmbeddingDatabase(false);
        List<String> nodeIds = new ArrayList<>(texts.keySet());
        int total = nodeIds.size();
        System.out.println("Embedding " + total + " entries with " + numThreads + " thread(s) ...");
        Map<String, String> failures = new ConcurrentHashMap<>();
        AtomicInteger completed = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        // Effectively final capture for the worker lambda (modelId may have
        // been reassigned by the default derivation above)
        String finalModelId = modelId;
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (String nodeId : nodeIds) {
                futures.add(executor.submit(() -> {
                    try {
                        String text = texts.get(nodeId);
                        float[] embedding = runner.embed(text);
                        if (embedding == null || embedding.length == 0) {
                            failures.put(nodeId, "The model returned no embedding vector");
                            return;
                        }
                        db.setEmbeddingWithHash(finalModelId, nodeId, embedding, text);
                    } catch (Throwable t) {
                        failures.put(nodeId, t.getMessage() != null ? t.getMessage() : t.toString());
                    } finally {
                        int done = completed.incrementAndGet();
                        if (done % 100 == 0 || done == total) {
                            System.out.println("Embedded " + done + "/" + total + " entries (" + failures.size() + " failed)");
                        }
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupted while embedding entries. The database was NOT saved.");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Embedding run failed: " + e.getMessage());
            System.exit(1);
        } finally {
            executor.shutdownNow();
            runner.shutdown();
        }

        if (!failures.isEmpty()) {
            System.err.println("Failed to embed " + failures.size() + " of " + total + " entries. The database was NOT saved (fail-closed: no partial database is shipped).");
            Map<String, String> sortedFailures = new TreeMap<>(failures);
            int printed = 0;
            for (Map.Entry<String, String> failure : sortedFailures.entrySet()) {
                if (printed >= MAX_PRINTED_FAILURES) {
                    System.err.println("  ... and " + (failures.size() - printed) + " more");
                    break;
                }
                System.err.println("  " + failure.getKey() + ": " + failure.getValue());
                ++printed;
            }
            System.exit(1);
        }

        int storedCount = db.getNodeIds(modelId).size();
        if (storedCount == 0) {
            System.err.println("No embeddings were computed. The database was NOT saved.");
            System.exit(1);
        }
        if (duplicateIds > 0) {
            System.out.println("Merged " + duplicateIds + " duplicate entry ids (first occurrence wins).");
        }

        // Write to a temporary file, then move into place (no partial output).
        // A failed write deletes the temporary file (best effort): the
        // fail-closed contract is that a failed run leaves neither the output
        // file nor a leftover side file.
        Path tmpPath = outputPath.resolveSibling(outputPath.getFileName().toString() + TEMP_FILE_SUFFIX);
        try (OutputStream os = Files.newOutputStream(tmpPath)) {
            db.saveToStream(os, modelId);
        } catch (IOException | RuntimeException e) {
            System.err.println("Failed to write the database to '" + tmpPath + "': " + e);
            try {
                Files.deleteIfExists(tmpPath);
            } catch (IOException ignored) {
                // Best effort: the output file is still never created
            }
            System.err.println("The database was NOT saved.");
            System.exit(1);
        }
        if (!Files.exists(tmpPath) || Files.size(tmpPath) == 0) {
            System.err.println("The written database file is empty. The output was NOT moved to " + outputPath);
            Files.deleteIfExists(tmpPath);
            System.exit(1);
        }
        try {
            Files.move(tmpPath, outputPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmpPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // Built-in read-back: verify the written file before reporting success.
        // Fail-closed: any verification failure (mismatch or exception) removes
        // this run's output and any .tmp residue before the non-zero exit, so a
        // failed run never leaves a half-verified database behind.
        JIPipeEmbeddingDatabase.JembFileHeader header;
        List<String> sampleIds = new ArrayList<>();
        try {
            header = JIPipeEmbeddingDatabase.readFileHeader(outputPath);
            if (header.version() != JIPipeEmbeddingDatabase.getFormatVersion()) {
                System.err.println("Read-back failed: the written file has JEMB version " + header.version()
                        + " but version " + JIPipeEmbeddingDatabase.getFormatVersion() + " was expected.");
                deleteWrittenOutput(outputPath);
                System.exit(1);
            }
            System.out.println("Read-back: JEMB format version " + header.version() + " re-checked (matches the version the file was written with)");
            if (!Objects.equals(modelId, header.modelId())) {
                System.err.println("Read-back failed: the written file records model id '" + header.modelId() + "' but '" + modelId + "' was expected.");
                deleteWrittenOutput(outputPath);
                System.exit(1);
            }
            if (header.entryCount() != storedCount) {
                System.err.println("Read-back failed: the written file contains " + header.entryCount() + " entries but " + storedCount + " were expected.");
                deleteWrittenOutput(outputPath);
                System.exit(1);
            }

            JIPipeEmbeddingDatabase readBack = new JIPipeEmbeddingDatabase(false);
            try (InputStream is = Files.newInputStream(outputPath)) {
                readBack.loadFromStream(is, modelId, JIPipeProgressInfo.SILENT, false);
            }
            if (readBack.getNodeIds(modelId).size() != storedCount) {
                System.err.println("Read-back failed: the loaded database contains " + readBack.getNodeIds(modelId).size()
                        + " entries but " + storedCount + " were expected.");
                deleteWrittenOutput(outputPath);
                System.exit(1);
            }

            // Seeded sample: every 10th id in the deterministic (sorted) id space,
            // vectors byte-compared and hashes compared against the in-memory data
            List<String> storedIds = new ArrayList<>(readBack.getNodeIds(modelId));
            Collections.sort(storedIds);
            for (int i = 0; i < storedIds.size(); i += READ_BACK_SAMPLE_STRIDE) {
                sampleIds.add(storedIds.get(i));
            }
            for (String nodeId : sampleIds) {
                float[] expectedEmbedding = db.getEmbedding(modelId, nodeId);
                float[] actualEmbedding = readBack.getEmbedding(modelId, nodeId);
                if (expectedEmbedding == null || actualEmbedding == null || !vectorsByteEqual(expectedEmbedding, actualEmbedding)) {
                    System.err.println("Read-back failed: the embedding vector of entry '" + nodeId + "' differs after the round trip.");
                    deleteWrittenOutput(outputPath);
                    System.exit(1);
                }
                String expectedHash = db.getHash(modelId, nodeId);
                String actualHash = readBack.getHash(modelId, nodeId);
                if (!Objects.equals(expectedHash, actualHash)) {
                    System.err.println("Read-back failed: the text hash of entry '" + nodeId + "' differs after the round trip.");
                    deleteWrittenOutput(outputPath);
                    System.exit(1);
                }
            }
        } catch (IOException | RuntimeException e) {
            // An exception during the read-back (header read, stream load, ...)
            // is also a failed verification: remove this run's output, then let
            // the launcher's catch-all render the error and exit non-zero.
            deleteWrittenOutput(outputPath);
            throw e;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("Embedding database built: entries=" + storedCount
                + ", model id=" + modelId
                + ", format=JEMB v" + header.version()
                + ", output=" + outputPath
                + ", read-back sample=" + sampleIds.size() + " entries verified"
                + (duplicateIds > 0 ? ", duplicate entry ids merged=" + duplicateIds : "")
                + ", elapsed=" + (elapsed / 1000) + " s");
    }

    /**
     * Best-effort removal of the written output file and any {@code .tmp}
     * residue after a failed read-back verification: the fail-closed contract
     * is that a failed run ships nothing. Only called after the file was moved
     * into place, so the file at the output path is this run's product; an
     * output file that existed before the run (and every pre-move failure
     * path) is never touched by this.
     *
     * @param outputPath the output file path
     */
    private static void deleteWrittenOutput(Path outputPath) {
        try {
            if (Files.deleteIfExists(outputPath)) {
                System.err.println("Removed the unverified output file '" + outputPath + "' (fail-closed: a failed run ships nothing).");
            }
        } catch (IOException e) {
            System.err.println("Warning: could not remove the unverified output file '" + outputPath + "': " + e);
        }
        Path tmpPath = outputPath.resolveSibling(outputPath.getFileName().toString() + TEMP_FILE_SUFFIX);
        try {
            if (Files.deleteIfExists(tmpPath)) {
                System.err.println("Removed the residue file '" + tmpPath + "'.");
            }
        } catch (IOException e) {
            System.err.println("Warning: could not remove the residue file '" + tmpPath + "': " + e);
        }
    }

    /**
     * Waits until the database has built its entries and the build queue has
     * drained (the {@code NodeSearchGoldenSetTest} await shape), bounded at
     * {@link #DATABASE_AWAIT_TIMEOUT_MS}. Exits with status 1 on timeout.
     *
     * @param database the node database
     */
    private static void awaitDatabaseEntries(JIPipeNodeDatabase database) {
        long deadline = System.currentTimeMillis() + DATABASE_AWAIT_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (!database.getEntries().isEmpty() && database.getQueue().isEmpty()) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.err.println("The node database did not finish building its entries within " + DATABASE_AWAIT_TIMEOUT_MS + " ms"
                + " (entries built: " + !database.getEntries().isEmpty()
                + ", build queue drained: " + database.getQueue().isEmpty() + ")");
        System.exit(1);
    }

    /**
     * Bit-exact vector comparison: identical raw float bit patterns.
     *
     * @param a the first vector
     * @param b the second vector
     * @return true if both vectors have identical lengths and raw bit patterns
     */
    private static boolean vectorsByteEqual(float[] a, float[] b) {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (Float.floatToRawIntBits(a[i]) != Float.floatToRawIntBits(b[i])) {
                return false;
            }
        }
        return true;
    }
}
