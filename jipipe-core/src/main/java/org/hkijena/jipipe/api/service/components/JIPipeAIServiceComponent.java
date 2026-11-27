package org.hkijena.jipipe.api.service.components;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.ai.EmbeddingModelBundle;
import org.hkijena.jipipe.api.ai.JIPipeEmbeddingAIModelRunner;
import org.hkijena.jipipe.api.ai.llm.activity.LLMActivityPhase;
import org.hkijena.jipipe.api.ai.llm.activity.LLMActivityTracker;
import org.hkijena.jipipe.api.progress.JIPipeProgressLog;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactRepositoryApplyInstallUninstallRun;
import org.hkijena.jipipe.api.artifacts.JIPipeLocalArtifact;
import org.hkijena.jipipe.api.artifacts.JIPipeRemoteArtifact;
import org.hkijena.jipipe.api.microservice.AbstractMicroservice;
import org.hkijena.jipipe.api.microservice.MicroserviceState;
import org.hkijena.jipipe.api.microservice.ServiceAwareEphemeralExecutor;
import org.hkijena.jipipe.api.microservice.ServiceAwareQueuedExecutor;
import org.hkijena.jipipe.api.nodes.database.embeddings.JIPipeGlobalEmbeddingSearch;
import org.hkijena.jipipe.api.progress.JIPipeProgressLog;
import org.hkijena.jipipe.api.run.JIPipeRunnableExecutor;
import org.hkijena.jipipe.api.servers.JIPipeServerLease;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;
import org.hkijena.jipipe.plugins.ai.AIApplicationSettings;
import org.hkijena.jipipe.plugins.ai.AIPlugin;
import org.hkijena.jipipe.plugins.ai.EmbeddingModelType;
import org.hkijena.jipipe.plugins.ai.environments.EmbeddingModelEnvironment;
import org.hkijena.jipipe.plugins.embeddingserver.EmbeddingServerInstance;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service component that manages AI model lifecycle and provides service-aware executors
 * for serialized execution of AI operations.
 * <p>
 * The component owns an {@link EmbeddingModelService} (a {@link AbstractMicroservice})
 * that encapsulates the embedding model lifecycle, and two executors that gate on it:
 * <ul>
 *     <li>{@link ServiceAwareQueuedExecutor} for batch embed operations (FIFO)</li>
 *     <li>{@link ServiceAwareEphemeralExecutor} for AI search operations (latest-wins)</li>
 * </ul>
 */
public class JIPipeAIServiceComponent extends JIPipeServiceComponent {

    private final EmbeddingModelService embeddingModelService;
    private final ServiceAwareQueuedExecutor embedQueue;
    private final ServiceAwareEphemeralExecutor searchQueue;

    /**
     * Progress info for embedding-related operations (model loading, embedding computation, cache I/O).
     * The AI monitor window can subscribe to this to display logs in the UI.
     * <p>
     * Built on a file-backed store in the global temporary base
     * ({@link JIPipeProgressLog#resolveStorageBase()}; the store's NDJSON file is created lazily by
     * the writer thread on the first flushed record - no disk cost until records flow), so the
     * monitor's embedding log page receives coalesced batch events and can re-read the record tail
     * on every open. A silent store would be dead UI: it never emits batches. The handle is created
     * directly on the store (not via {@code setStorageBase}), which is for adopting into an
     * existing handle; this handle owns its store for the service's lifetime.
     */
    private final JIPipeProgressInfo embeddingProgressInfo = createEmbeddingProgressInfo();

    /**
     * How long the embedding activity phase stays open after the last progress batch
     * before it is finalized (milliseconds). Batches are coalesced to at most one per
     * 100 ms, so 1500 ms of silence reliably means the current embedding-related work
     * has stopped writing records.
     */
    private static final long EMBEDDING_ACTIVITY_QUIET_MS = 1500;

    /**
     * The rolling LLM activity history rendered by the AI status graph
     * ({@code JIPipeDesktopAIStatusGraph}). Producers: the embedding activity feed
     * below (Embedding phase) and the MCP dispatch in
     * {@code JIPipeDesktopMCPIntegration} (ToolCall phase).
     */
    private final LLMActivityTracker llmActivityTracker = new LLMActivityTracker();

    /**
     * Guards the embedding activity phase bookkeeping between the batch listener
     * (store writer thread) and the quiet-period finalizer (scheduler thread).
     */
    private final ReentrantLock embeddingActivityLock = new ReentrantLock();

    /**
     * Wall-clock time of the last embedding progress batch.
     */
    private volatile long lastEmbeddingActivityMs;

    /**
     * Serializes the quiet-period checks for the embedding activity phase.
     */
    private final ScheduledExecutorService embeddingActivityFinalizer = Executors.newSingleThreadScheduledExecutor(
            runnable -> {
                Thread thread = new Thread(runnable, "JIPipe-AI-Embedding-Activity");
                thread.setDaemon(true);
                return thread;
            });

    /**
     * Creates the embedding progress handle on its file-backed store.
     */
    private static JIPipeProgressInfo createEmbeddingProgressInfo() {
        String runUuid = UUID.randomUUID().toString();
        Path file = JIPipeProgressLog.resolveStorageBase().resolve(runUuid + ".ndjson");
        return new JIPipeProgressInfo(JIPipeProgressLog.create(file, runUuid, "AI embedding operations"));
    }

    public JIPipeAIServiceComponent(JIPipeService service) {
        super(service);

        this.embeddingModelService = new EmbeddingModelService();
        this.embedQueue = new ServiceAwareQueuedExecutor("AI embed queue", embeddingModelService);
        this.embedQueue.setSilent(true);
        this.searchQueue = new ServiceAwareEphemeralExecutor("AI search queue", embeddingModelService);
        this.searchQueue.setSilent(true);

        // Register a shutdown hook to save any dirty embedding caches before the JVM exits.
        // This is a safety net; the primary auto-save happens in ensureEmbeddingsForEntries().
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                // Save the global embedding database (the only persistent one)
                JIPipeGlobalEmbeddingSearch.getInstance().saveAllDirty();
            } catch (Exception e) {
                // Best effort on shutdown
            }
        }, "JIPipe-AI-Embedding-Shutdown"));

        // Feed the AI status graph's embedding row from the embedding progress store: every
        // batch proves actual embedding-related activity (model load, embed calls, cache I/O).
        // The subscription lives for the component's lifetime (application lifetime).
        embeddingProgressInfo.subscribeToBatchEvents(this::onEmbeddingActivityBatch);
        embeddingActivityFinalizer.scheduleAtFixedRate(this::finalizeEmbeddingActivityIfQuiet,
                500, 500, TimeUnit.MILLISECONDS);
    }

    // ===== LLM activity tracking (AI status graph) =====

    /**
     * Gets the rolling LLM activity tracker rendered by the AI status graph. Producers
     * record phases into it (embedding activity here, MCP tool calls in the desktop
     * integration); the graph only reads.
     */
    public LLMActivityTracker getLLMActivityTracker() {
        return llmActivityTracker;
    }

    /**
     * Embedding activity feed: a progress batch proves that embedding-related work is
     * (still) writing records, so it opens (or extends) an Embedding phase in the
     * tracker. Phase bookkeeping is serialized against the quiet-period finalizer.
     * Never throws: the status graph must not be able to break embedding operations.
     */
    private void onEmbeddingActivityBatch(JIPipeProgressLog.ProgressBatchEvent event) {
        try {
            embeddingActivityLock.lock();
            try {
                lastEmbeddingActivityMs = System.currentTimeMillis();
                if (llmActivityTracker.getCurrentPhase() != LLMActivityPhase.Embedding) {
                    llmActivityTracker.beginPhase(LLMActivityPhase.Embedding, null);
                }
            } finally {
                embeddingActivityLock.unlock();
            }
        } catch (Throwable ignored) {
            // The status graph must never break embedding operations.
        }
    }

    /**
     * Finalizes an open Embedding phase once no progress batch arrived for
     * {@link #EMBEDDING_ACTIVITY_QUIET_MS}. Never throws.
     */
    private void finalizeEmbeddingActivityIfQuiet() {
        try {
            embeddingActivityLock.lock();
            try {
                if (llmActivityTracker.getCurrentPhase() != LLMActivityPhase.Embedding) {
                    return;
                }
                if (System.currentTimeMillis() - lastEmbeddingActivityMs < EMBEDDING_ACTIVITY_QUIET_MS) {
                    return;
                }
                llmActivityTracker.finalizeCurrentPhase();
            } finally {
                embeddingActivityLock.unlock();
            }
        } catch (Throwable ignored) {
            // Best effort: the next batch reopens the phase.
        }
    }

    // ===== Public API (delegating to EmbeddingModelService) =====

    /**
     * Enqueue a start operation. Safe to call from UI thread.
     * If a model is currently loaded, it will be stopped first (unload+load = replace).
     */
    public void tryStartEmbeddingModel() {
        new Thread(() -> embeddingModelService.start(), "AI-Embedding-Model-Start").start();
    }

    /**
     * Enqueue a stop operation. Safe to call from UI thread.
     * If the model is busy, the unload will happen after the current task completes
     * (because the embed queue serializes operations).
     */
    public void tryStopEmbeddingModel() {
        new Thread(() -> embeddingModelService.stop(), "AI-Embedding-Model-Stop").start();
    }

    /**
     * Synchronous embed. Must only be called from the embed queue thread.
     *
     * @param text the text to embed
     * @return the embedding vector
     * @throws IllegalStateException if no embedding model is loaded
     */
    public float[] embedNow(String text) {
        return embeddingModelService.embedNow(text);
    }

    /**
     * Asynchronous embed. Must not be called from the EDT - blocks until the model is ready.
     * Enqueues an embed task and returns a CompletableFuture.
     * If no model is loaded, auto-starts it first (blocking).
     * Returns null if AI is disabled in settings.
     *
     * @param text the text to embed
     * @return a future that completes with the embedding vector, or null if AI is disabled
     */
    public CompletableFuture<float[]> tryEmbed(String text) {
        AIApplicationSettings settings = AIApplicationSettings.getInstance();
        if (!settings.isEnableAI()) {
            return null;
        }

        // If model is not ready, start it first (blocking - caller should be off-EDT)
        if (!embeddingModelService.isReady()) {
            embeddingModelService.start();
        }

        EmbedTextRun embedTask = new EmbedTextRun(text);
        embedQueue.enqueue(embedTask);
        return embedTask.getFuture();
    }

    // ===== Status Query Methods =====

    public boolean hasEmbeddingModel() {
        return embeddingModelService.isReady();
    }

    public MicroserviceState getEmbeddingModelStatus() {
        return embeddingModelService.getState();
    }

    public String getEmbeddingModelError() {
        if (embeddingModelService.getState() == MicroserviceState.Failed) {
            return embeddingModelService.getStateDetail();
        }
        return null;
    }

    /**
     * Get the model ID of the currently loaded embedding model.
     * Returns null if no model is loaded.
     *
     * @return the model ID, or null if no model is loaded
     */
    public String getModelId() {
        return embeddingModelService.getModelId();
    }

    /**
     * Checks if the embedding model is configured and ready to be started (not if it is currently running).
     * This checks if the model CAN be started, not if it IS started.
     * <p>
     * For the default embedding artifact query, a validated embedding model
     * bundle (the model files release packages ship inside
     * {@code embedding-model-bundle/}) also satisfies this check: the model can
     * be started from the bundle without any artifact download. Any other
     * artifact query is unaffected.
     *
     * @return true if the model can be started
     */
    public boolean hasConfiguredAndReadyEmbeddingModel() {
        AIApplicationSettings settings = AIApplicationSettings.getInstance();
        EmbeddingModelEnvironment environment = settings.getEmbeddingModelEnvironment();
        if (environment.isLoadFromArtifact()) {
            String query = environment.getArtifactQuery().getQuery();
            if (AIPlugin.DEFAULT_EMBEDDING_ARTIFACT.equals(query)
                    && isDefaultEmbeddingModelBundleAvailable()) {
                return true;
            }
            // Resolve the artifact and check if it's downloaded
            JIPipeArtifact artifact = getService().getArtifacts()
                    .queryPreferredCachedArtifact(query);
            return artifact instanceof JIPipeLocalArtifact;
        } else {
            // Do a local configuration check
            return environment.isValid();
        }
    }

    /**
     * Checks if a verified embedding model bundle is available for the default
     * embedding artifact: the bundle resolves and passes the integrity
     * verification, and its manifest model id matches the default artifact
     * coordinate prefix. Read-only and cached per bundle identity, so callers
     * may probe repeatedly.
     *
     * @return true when the bundle can serve the default embedding model
     */
    private static boolean isDefaultEmbeddingModelBundleAvailable() {
        Optional<EmbeddingModelBundle.ResolvedBundle> bundle = EmbeddingModelBundle.resolveValidated();
        return bundle.isPresent()
                && EmbeddingModelBundle.isUsableFor(bundle.get(), AIPlugin.DEFAULT_EMBEDDING_ARTIFACT_PREFIX);
    }

    // ===== Progress Info Access =====

    /**
     * Gets the progress info for embedding-related operations.
     * Subscribe to the embedding progress info's store batch emitter
     * ({@link JIPipeProgressInfo#subscribeToBatchEvents(JIPipeProgressLog.ProgressBatchEventListener)})
     * to receive log updates as coalesced batches. The handle is file-backed (global temporary
     * base), so batches flow and the record tail can be re-read on every UI open.
     *
     * @return the embedding progress info
     */
    public JIPipeProgressInfo getEmbeddingProgressInfo() {
        return embeddingProgressInfo;
    }

    // ===== Queue Access =====

    /**
     * Returns the embed queue for UI monitoring.
     */
    public JIPipeRunnableExecutor getQueue() {
        return embedQueue;
    }

    /**
     * Returns the search queue for AI search operations.
     */
    public ServiceAwareEphemeralExecutor getSearchQueue() {
        return searchQueue;
    }

    /**
     * Returns the embedding model service, allowing UI components to subscribe
     * to its {@link org.hkijena.jipipe.api.microservice.MicroserviceStateChangeEventEmitter}.
     */
    public EmbeddingModelService getEmbeddingModelService() {
        return embeddingModelService;
    }

    // ===== EmbeddingModelService inner class =====

    /**
     * Microservice that encapsulates the embedding model lifecycle.
     * <p>
     * Non-static inner class so it has access to {@link JIPipeAIServiceComponent}'s
     * fields (embeddingProgressInfo, getService(), etc.).
     */
    public class EmbeddingModelService extends AbstractMicroservice {

        private volatile JIPipeEmbeddingAIModelRunner embeddingModelRunner;
        /**
         * Holds the active lease for the LocalOnnx scenario, where embedding is performed
         * via a spawned server process managed by {@link JIPipeServerServiceComponent}.
         */
        private volatile JIPipeServerLease<EmbeddingServerInstance> embeddingLease;
        private volatile String embeddingModelError;
        /**
         * The manifest model id when the currently-started model was configured
         * from the embedding model bundle, else null. Set by
         * {@link #tryApplyEmbeddingModelBundleConfiguration(EmbeddingModelEnvironment)}
         * and verified after the model start, so a diverging derivation cannot
         * silently key the embedding database under a different model id.
         */
        private volatile String appliedBundleModelId;

        public EmbeddingModelService() {
            super("Embedding model service");
        }

        @Override
        protected void onStart() throws Exception {
            embeddingProgressInfo.log("Starting embedding model ...");

            AIApplicationSettings settings = AIApplicationSettings.getInstance();
            EmbeddingModelEnvironment environment = settings.getEmbeddingModelEnvironment();
            embeddingModelError = null;

            if (environment.getModelType() == EmbeddingModelType.LocalOnnx) {
                startEmbeddingServer(environment);
            } else {
                startEmbeddingDirectRunner(environment);
            }
            verifyAppliedBundleModelId();
        }

        @Override
        protected void onStop() throws Exception {
            if (embeddingLease == null && embeddingModelRunner == null) {
                return;
            }

            embeddingProgressInfo.log("Stopping embedding model ...");

            try {
                if (embeddingLease != null) {
                    try {
                        embeddingLease.close();
                    } catch (Exception e) {
                        embeddingProgressInfo.log("Error during embedding server lease release: " + e.getMessage());
                    }
                    embeddingLease = null;
                }
                if (embeddingModelRunner != null) {
                    try {
                        embeddingModelRunner.shutdown();
                    } catch (Exception e) {
                        embeddingProgressInfo.log("Error during model shutdown: " + e.getMessage());
                    }
                    embeddingModelRunner = null;
                }
                appliedBundleModelId = null;
                embeddingModelError = null;
            } catch (Exception e) {
                embeddingProgressInfo.log("Error during model shutdown: " + e.getMessage());
                embeddingModelError = ExceptionUtils.getMessage(e);
                throw e;
            }
        }

        /**
         * Starts the embedding model via a spawned server process (LocalOnnx scenario).
         */
        private void startEmbeddingServer(EmbeddingModelEnvironment env) throws Exception {
            releaseExistingEmbeddingResources();

            try {
                if (env.isLoadFromArtifact()) {
                    resolveAndApplyArtifactConfiguration(env);
                } else {
                    if (!env.isValid()) {
                        throw new IllegalStateException("Embedding model environment is not valid. " +
                                "Please configure a local model or remote API in the AI settings.");
                    }
                }

                JIPipeServerServiceComponent serverService = getService().getServerService();
                embeddingLease = serverService.acquireLease(
                        EmbeddingServerInstance.FACTORY_ID, EmbeddingServerInstance.class, env);

                setStateDetail("Idle");
            } catch (Exception e) {
                e.printStackTrace();
                embeddingProgressInfo.log(e);
                embeddingModelError = ExceptionUtils.getMessage(e);
                embeddingLease = null;
                throw e;
            }
        }

        /**
         * Starts the embedding model via a direct in-process runner (OpenAIAPI scenario).
         */
        private void startEmbeddingDirectRunner(EmbeddingModelEnvironment env) throws Exception {
            releaseExistingEmbeddingResources();

            try {
                if (env.isLoadFromArtifact()) {
                    resolveAndApplyArtifactConfiguration(env);
                } else {
                    if (!env.isValid()) {
                        throw new IllegalStateException("Embedding model environment is not valid. " +
                                "Please configure a local model or remote API in the AI settings.");
                    }
                }

                JIPipeEmbeddingAIModelRunner runner = env.toRunner();
                embeddingModelRunner = runner;
                runner.start();
                setStateDetail("Idle");
            } catch (Exception e) {
                e.printStackTrace();
                embeddingProgressInfo.log(e);
                embeddingModelError = ExceptionUtils.getMessage(e);
                embeddingModelRunner = null;
                throw e;
            }
        }

        /**
         * Best-effort cleanup of any currently held embedding resources.
         */
        private void releaseExistingEmbeddingResources() {
            appliedBundleModelId = null;
            if (embeddingLease != null) {
                try {
                    embeddingLease.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    embeddingProgressInfo.log("Error during existing embedding server lease release: " + e.getMessage());
                }
                embeddingLease = null;
            }
            if (embeddingModelRunner != null) {
                try {
                    embeddingModelRunner.shutdown();
                } catch (Exception e) {
                    e.printStackTrace();
                    embeddingProgressInfo.log("Error during existing model shutdown: " + e.getMessage());
                }
                embeddingModelRunner = null;
            }
        }

        /**
         * Resolves the artifact for the given environment, downloads it if necessary, and applies
         * the configuration so that model/tokenizer paths are populated.
         * <p>
         * When a verified embedding model bundle is available for the default
         * embedding artifact, the environment is configured from the bundle and
         * the artifact repository is not touched at all (the bundle is the
         * release-package source of the model; the artifact path is the fallback).
         */
        private void resolveAndApplyArtifactConfiguration(EmbeddingModelEnvironment environment) throws Exception {
            if (tryApplyEmbeddingModelBundleConfiguration(environment)) {
                return;
            }
            JIPipeArtifactsServiceComponent artifacts = getService().getArtifacts();
            String query = environment.getArtifactQuery().getQuery();

            JIPipeArtifact artifact = artifacts.searchClosestCompatibleArtifactFromQuery(query);
            if (artifact == null) {
                throw new IllegalStateException("Unable to find a compatible artifact for the embedding model. " +
                        "Query: " + query + ". Please check your internet connection and artifact configuration.");
            }
            embeddingProgressInfo.log("Matched artifact: " + artifact.getFullId());

            if (artifact instanceof JIPipeRemoteArtifact remoteArtifact) {
                embeddingProgressInfo.log("Downloading artifact " + remoteArtifact.getFullId());
                JIPipeArtifactRepositoryApplyInstallUninstallRun run = new JIPipeArtifactRepositoryApplyInstallUninstallRun(
                        List.of(remoteArtifact), Collections.emptyList());
                run.setProgressInfo(embeddingProgressInfo.resolve("Download artifact"));
                run.run();

                artifacts.updateCachedArtifacts(embeddingProgressInfo.resolve("Refresh artifact cache"));
                artifact = artifacts.searchClosestCompatibleArtifactFromQuery(query);
                if (artifact == null) {
                    throw new IllegalStateException("Artifact download was unsuccessful. " +
                            "Unable to find the artifact after download. Query: " + query);
                }
            }

            if (artifact instanceof JIPipeLocalArtifact localArtifact) {
                environment.applyConfigurationFromArtifactAndSetLastArtifact(localArtifact,
                        embeddingProgressInfo.resolve("Configure environment from artifact"));
            } else {
                throw new IllegalStateException("Artifact download was unsuccessful! " +
                        "The artifact " + artifact.getFullId() + " is still not available locally. " +
                        "Check your internet connection and if your firewall does not block remote repositories.");
            }
        }

        /**
         * Configures the environment from the embedding model bundle when one is
         * available for the default embedding artifact. The bundle ships with
         * release packages (the model files plus the regenerated embedding
         * database inside {@code embedding-model-bundle/}), so it is preferred
         * over any artifact download.
         * <p>
         * Only the default embedding artifact query is bundle-served: any other
         * query keeps the artifact path untouched. The bundle path does not
         * consult the artifact repository at all.
         * <p>
         * The environment is configured from the bundle files directly (not via
         * {@link EmbeddingModelEnvironment#applyConfigurationFromArtifact},
         * which assumes the artifact layout's {@code embedding/} prefix that
         * the bundle layout does not have), and a synthetic local artifact
         * carries the manifest model id as the last configured artifact, so
         * {@link EmbeddingModelEnvironment#toRunner()},
         * {@link EmbeddingModelEnvironment#deriveModelId()} and
         * {@link #getModelId()} all return the manifest model id and the
         * artifact-query fallback can never trigger on the bundle path.
         *
         * @param environment the environment to configure
         * @return true when the environment was configured from the bundle
         * (the caller must skip the artifact resolution entirely)
         */
        private boolean tryApplyEmbeddingModelBundleConfiguration(EmbeddingModelEnvironment environment) {
            String query = environment.getArtifactQuery().getQuery();
            if (!AIPlugin.DEFAULT_EMBEDDING_ARTIFACT.equals(query)) {
                return false;
            }
            Optional<EmbeddingModelBundle.ResolvedBundle> bundle = EmbeddingModelBundle.resolveValidated();
            if (bundle.isEmpty()) {
                embeddingProgressInfo.log("Embedding model bundle not found, falling back to the artifact");
                return false;
            }
            EmbeddingModelBundle.ResolvedBundle resolved = bundle.get();
            if (!EmbeddingModelBundle.isUsableFor(resolved, AIPlugin.DEFAULT_EMBEDDING_ARTIFACT_PREFIX)) {
                embeddingProgressInfo.log("Embedding model bundle at " + resolved.bundleDir()
                        + " does not match the default embedding model (manifest model id "
                        + resolved.modelId() + "), falling back to the artifact");
                return false;
            }
            environment.setModelType(EmbeddingModelType.LocalOnnx);
            environment.setLocalModelFile(resolved.modelOnnx());
            environment.setLocalTokenizerFile(resolved.tokenizerJson());
            JIPipeLocalArtifact bundleArtifact = new JIPipeLocalArtifact();
            bundleArtifact.setFullId(resolved.modelId());
            bundleArtifact.setLocalPath(resolved.bundleDir());
            environment.setLastConfiguredArtifact(bundleArtifact);
            appliedBundleModelId = resolved.modelId();
            embeddingProgressInfo.log("Embedding model bundle found at " + resolved.bundleDir()
                    + " (model id " + resolved.modelId() + ")");
            return true;
        }

        /**
         * Verifies after a model start that the bundle-configured environment
         * actually reports the manifest model id. A mismatch means the
         * embedding database would be keyed under a different model id than
         * the shipped database uses, so it is a hard start-time failure
         * instead of a silent divergence.
         */
        private void verifyAppliedBundleModelId() {
            String expected = appliedBundleModelId;
            if (expected == null) {
                return;
            }
            String derived = getModelId();
            if (!expected.equals(derived)) {
                throw new IllegalStateException("The embedding model bundle model id '" + expected
                        + "' does not match the id reported by the started model ('" + derived
                        + "'). The embedding database keys would diverge.");
            }
            embeddingProgressInfo.log("Embedding model bundle model id verified: " + derived);
        }

        /**
         * Synchronous embed. Must only be called from the embed queue thread.
         */
        public float[] embedNow(String text) {
            if (embeddingLease == null && embeddingModelRunner == null) {
                throw new IllegalStateException("No embedding model loaded");
            }

            embeddingProgressInfo.log("Embedding: " + text);
            setStateDetail("Busy");

            try {
                if (embeddingLease != null && embeddingLease.isOpen()) {
                    float[] result = embeddingLease.getInstance().embed(text);
                    setStateDetail("Idle");
                    return result;
                } else if (embeddingModelRunner != null) {
                    float[] result = embeddingModelRunner.embed(text);
                    setStateDetail("Idle");
                    return result;
                } else {
                    throw new IllegalStateException("No embedding model loaded");
                }
            } catch (RuntimeException e) {
                embeddingModelError = ExceptionUtils.getMessage(e);
                embeddingProgressInfo.error(embeddingModelError);
                setStateDetail("Idle");
                throw e;
            } catch (Exception e) {
                embeddingModelError = ExceptionUtils.getMessage(e);
                embeddingProgressInfo.error(embeddingModelError);
                setStateDetail("Idle");
                throw new RuntimeException(e);
            }
        }

        /**
         * Get the model ID of the currently loaded embedding model.
         * Returns null if no model is loaded.
         */
        public String getModelId() {
            if (embeddingLease != null && embeddingLease.isOpen()) {
                return embeddingLease.getInstance().getEnvironment().deriveModelId();
            }
            if (embeddingModelRunner != null) {
                return embeddingModelRunner.getModelId();
            }
            return null;
        }

        /**
         * Returns the last error message, or null if no error.
         */
        public String getEmbeddingModelError() {
            if (embeddingLease != null) {
                return embeddingModelError;
            }
            if (embeddingModelRunner != null) {
                return embeddingModelRunner.getLastError();
            }
            return embeddingModelError;
        }
    }

    // ===== Task Classes =====

    /**
     * Task to embed text. Runs on the embed queue worker thread.
     */
    private class EmbedTextRun extends DefaultJIPipeRunnable {
        private final CompletableFuture<float[]> future = new CompletableFuture<>();
        private final String text;

        public EmbedTextRun(String text) {
            this.text = text;
        }

        public CompletableFuture<float[]> getFuture() {
            return future;
        }

        @Override
        public String getTaskLabel() {
            return "Generate embedding";
        }

        @Override
        public void run() {
            try {
                float[] result = embedNow(text);
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }
    }
}
