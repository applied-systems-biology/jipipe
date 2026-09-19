package org.hkijena.jipipe.api.service.components;

import org.hkijena.jipipe.api.nodes.database.embeddings.JIPipeGlobalEmbeddingSearch;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.ai.JIPipeEmbeddingAIModelRunner;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactRepositoryApplyInstallUninstallRun;
import org.hkijena.jipipe.api.artifacts.JIPipeLocalArtifact;
import org.hkijena.jipipe.api.artifacts.JIPipeRemoteArtifact;
import org.hkijena.jipipe.api.microservice.AbstractMicroservice;
import org.hkijena.jipipe.api.microservice.MicroserviceState;
import org.hkijena.jipipe.api.microservice.ServiceAwareEphemeralExecutor;
import org.hkijena.jipipe.api.microservice.ServiceAwareQueuedExecutor;
import org.hkijena.jipipe.api.run.JIPipeRunnableExecutor;
import org.hkijena.jipipe.api.servers.JIPipeServerLease;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;
import org.hkijena.jipipe.plugins.ai.AIApplicationSettings;
import org.hkijena.jipipe.plugins.ai.EmbeddingModelType;
import org.hkijena.jipipe.plugins.ai.environments.EmbeddingModelEnvironment;
import org.hkijena.jipipe.plugins.embeddingserver.EmbeddingServerInstance;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
     */
    private final JIPipeProgressInfo embeddingProgressInfo = new JIPipeProgressInfo();

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
     * Asynchronous embed. Safe to call from UI thread.
     * Enqueues an embed task and returns a CompletableFuture.
     * If no model is loaded, auto-starts it first.
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

        // If model is not ready, start it first (blocking — caller should be off-EDT)
        if (!embeddingModelService.isReady()) {
            embeddingModelService.start();
        }

        EmbedTextRun embedTask = new EmbedTextRun(text);
        embedQueue.enqueue(embedTask);
        return embedTask.getFuture();
    }

    /**
     * Submit a function that can perform multiple synchronous embeddings on the embed queue thread.
     * <p>
     * This is a temporary compatibility method. Task 13 will migrate callers to use the
     * search queue (ephemeral executor) directly.
     *
     * @param fn   the function to execute on the embed queue thread; receives an embed function and returns a result
     * @param <T>  the result type
     * @return a future that completes with the function's result, or null if AI is disabled
     */
    public <T> CompletableFuture<T> submitEmbedFunction(java.util.function.Function<java.util.function.Function<String, float[]>, T> fn) {
        AIApplicationSettings settings = AIApplicationSettings.getInstance();
        if (!settings.isEnableAI()) {
            return null;
        }

        if (!embeddingModelService.isReady()) {
            embeddingModelService.start();
        }

        EmbedFunctionRun<T> task = new EmbedFunctionRun<>(fn);
        embedQueue.enqueue(task);
        return task.getFuture();
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
     *
     * @return true if the model can be started
     */
    public boolean hasConfiguredAndReadyEmbeddingModel() {
        AIApplicationSettings settings = AIApplicationSettings.getInstance();
        EmbeddingModelEnvironment environment = settings.getEmbeddingModelEnvironment();
        if (environment.isLoadFromArtifact()) {
            // Resolve the artifact and check if it's downloaded
            JIPipeArtifact artifact = getService().getArtifacts()
                    .queryPreferredCachedArtifact(environment.getArtifactQuery().getQuery());
            return artifact instanceof JIPipeLocalArtifact;
        } else {
            // Do a local configuration check
            return environment.isValid();
        }
    }

    // ===== Progress Info Access =====

    /**
     * Gets the progress info for embedding-related operations.
     * Subscribe to {@code getEmbeddingProgressInfo().getStatusUpdatedEventEmitter()} to receive log updates.
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
         */
        private void resolveAndApplyArtifactConfiguration(EmbeddingModelEnvironment environment) throws Exception {
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

    /**
     * Task that executes a function on the embed queue thread, providing it with
     * a synchronous embed function backed by {@link #embedNow(String)}.
     * Temporary compatibility class — Task 13 will migrate callers to the ephemeral executor.
     */
    private class EmbedFunctionRun<T> extends DefaultJIPipeRunnable {
        private final CompletableFuture<T> future = new CompletableFuture<>();
        private final java.util.function.Function<java.util.function.Function<String, float[]>, T> fn;

        public EmbedFunctionRun(java.util.function.Function<java.util.function.Function<String, float[]>, T> fn) {
            this.fn = fn;
        }

        public CompletableFuture<T> getFuture() {
            return future;
        }

        @Override
        public String getTaskLabel() {
            return "Batch embedding operation";
        }

        @Override
        public void run() {
            try {
                java.util.function.Function<String, float[]> embedFn = text -> embedNow(text);
                T result = fn.apply(embedFn);
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }
    }
}
