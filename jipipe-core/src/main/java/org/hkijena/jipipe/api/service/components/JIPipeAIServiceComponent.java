package org.hkijena.jipipe.api.service.components;

import org.hkijena.jipipe.api.nodes.database.embeddings.JIPipeGlobalEmbeddingSearch;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.ai.JIPipeAIModelRunnerStatus;
import org.hkijena.jipipe.api.ai.JIPipeEmbeddingAIModelRunner;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactRepositoryApplyInstallUninstallRun;
import org.hkijena.jipipe.api.artifacts.JIPipeLocalArtifact;
import org.hkijena.jipipe.api.artifacts.JIPipeRemoteArtifact;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.run.JIPipeQueuedRunnableExecutor;
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
import java.util.function.Function;

/**
 * Service component that manages AI model lifecycle and provides a task queue for serialized execution of AI operations.
 * <p>
 * The component owns a {@link JIPipeQueuedRunnableExecutor} that serializes all AI operations, ensuring:
 * - No concurrent access to model runners (which are not thread-safe)
 * - Natural deferred-unload semantics: an unload task queued while an embed is running will execute after the embed completes
 * - Simple reasoning about state transitions
 */
public class JIPipeAIServiceComponent extends JIPipeServiceComponent {

    private final JIPipeQueuedRunnableExecutor taskQueue = new JIPipeQueuedRunnableExecutor("AI Service");
    private volatile JIPipeEmbeddingAIModelRunner embeddingModelRunner;
    /**
     * Holds the active lease for the LocalOnnx scenario, where embedding is performed
     * via a spawned server process managed by {@link org.hkijena.jipipe.api.service.components.JIPipeServerServiceComponent}.
     */
    private volatile JIPipeServerLease<EmbeddingServerInstance> embeddingLease;
    private volatile JIPipeAIModelRunnerStatus currentStatus = JIPipeAIModelRunnerStatus.Unloaded;
    private String embeddingModelError;
    private final StatusChangedEventEmitter statusChangedEventEmitter = new StatusChangedEventEmitter();

    /**
     * Progress info for embedding-related operations (model loading, embedding computation, cache I/O).
     * The AI monitor window can subscribe to this to display logs in the UI.
     */
    private final JIPipeProgressInfo embeddingProgressInfo = new JIPipeProgressInfo();

    public JIPipeAIServiceComponent(JIPipeService service) {
        super(service);
        taskQueue.setSilent(true);

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

    private void fireStatusChanged(JIPipeAIModelRunnerStatus oldStatus, JIPipeAIModelRunnerStatus newStatus) {
        currentStatus = newStatus;
        statusChangedEventEmitter.emit(new StatusChangedEvent(this, oldStatus, newStatus));
    }

    // ===== Lifecycle Methods =====

    /**
     * Blocking start of the embedding model.
     * <p>
     * Branches on the configured {@link EmbeddingModelType}:
     * <ul>
     *     <li>{@link EmbeddingModelType#LocalOnnx} → starts a spawned server process via the server service</li>
     *     <li>{@link EmbeddingModelType#OpenAIAPI} → starts a direct in-process runner</li>
     * </ul>
     * Must only be called from the queue thread.
     *
     * @param progressInfo the progress info
     */
    public void startEmbeddingModelNow(JIPipeProgressInfo progressInfo) {
        progressInfo.log("Starting embedding model ...");

        AIApplicationSettings settings = AIApplicationSettings.getInstance();
        EmbeddingModelEnvironment environment = settings.getEmbeddingModelEnvironment();
        embeddingModelError = null;

        if (environment.getModelType() == EmbeddingModelType.LocalOnnx) {
            startEmbeddingServer(environment, progressInfo);
        } else {
            startEmbeddingDirectRunner(environment, progressInfo);
        }
    }

    /**
     * Starts the embedding model via a spawned server process (LocalOnnx scenario).
     * Acquires a lease from the server service, which spawns and waits for the
     * embedding server to become healthy.
     *
     * @param env          the embedding model environment
     * @param progressInfo the progress info
     */
    private void startEmbeddingServer(EmbeddingModelEnvironment env, JIPipeProgressInfo progressInfo) {
        releaseExistingEmbeddingResources(progressInfo);

        JIPipeAIModelRunnerStatus oldStatus = getEmbeddingModelStatus();
        fireStatusChanged(oldStatus, JIPipeAIModelRunnerStatus.Loading);

        try {
            // If configured from an artifact, resolve/download it and apply the local model/tokenizer paths
            if (env.isLoadFromArtifact()) {
                resolveAndApplyArtifactConfiguration(env, progressInfo);
            } else {
                if (!env.isValid()) {
                    throw new IllegalStateException("Embedding model environment is not valid. " +
                            "Please configure a local model or remote API in the AI settings.");
                }
            }

            // Acquire a lease for a spawned embedding server instance
            JIPipeServerServiceComponent serverService = getService().getServerService();
            embeddingLease = serverService.acquireLease(
                    EmbeddingServerInstance.FACTORY_ID, EmbeddingServerInstance.class, env);

            // The server instance is Busy while a lease is held, but at the server level
            // "Busy" means "lease held", not "actively processing". The model is now loaded
            // and ready, so report Idle from the AI service's perspective.
            fireStatusChanged(JIPipeAIModelRunnerStatus.Loading,
                    JIPipeAIModelRunnerStatus.Idle);
        } catch (Exception e) {
            e.printStackTrace();
            progressInfo.log(e);
            embeddingModelError = ExceptionUtils.getMessage(e);
            embeddingLease = null;
            fireStatusChanged(JIPipeAIModelRunnerStatus.Loading, JIPipeAIModelRunnerStatus.Failed);
        }
    }

    /**
     * Starts the embedding model via a direct in-process runner (OpenAIAPI scenario).
     * This preserves the legacy behavior where a runner is created from the environment
     * and started directly within this process.
     *
     * @param env          the embedding model environment
     * @param progressInfo the progress info
     */
    private void startEmbeddingDirectRunner(EmbeddingModelEnvironment env, JIPipeProgressInfo progressInfo) {
        releaseExistingEmbeddingResources(progressInfo);

        JIPipeAIModelRunnerStatus oldStatus = getEmbeddingModelStatus();
        fireStatusChanged(oldStatus, JIPipeAIModelRunnerStatus.Loading);

        try {
            if (env.isLoadFromArtifact()) {
                resolveAndApplyArtifactConfiguration(env, progressInfo);
            } else {
                if (!env.isValid()) {
                    throw new IllegalStateException("Embedding model environment is not valid. " +
                            "Please configure a local model or remote API in the AI settings.");
                }
            }

            JIPipeEmbeddingAIModelRunner runner = env.toRunner();
            embeddingModelRunner = runner;  // Assign BEFORE start() so UI can see Loading state
            runner.start();
            fireStatusChanged(JIPipeAIModelRunnerStatus.Loading, runner.getStatus());
        } catch (Exception e) {
            e.printStackTrace();
            progressInfo.log(e);
            embeddingModelError = ExceptionUtils.getMessage(e);
            embeddingModelRunner = null;
            fireStatusChanged(JIPipeAIModelRunnerStatus.Loading, JIPipeAIModelRunnerStatus.Failed);
        }
    }

    /**
     * Best-effort cleanup of any currently held embedding resources.
     * Releases an active server lease and/or shuts down a direct runner,
     * swallowing exceptions so that a failed cleanup does not abort a start/stop sequence.
     *
     * @param progressInfo the progress info
     */
    private void releaseExistingEmbeddingResources(JIPipeProgressInfo progressInfo) {
        if (embeddingLease != null) {
            try {
                embeddingLease.close();
            } catch (Exception e) {
                e.printStackTrace();
                progressInfo.log("Error during existing embedding server lease release: " + e.getMessage());
            }
            embeddingLease = null;
        }
        if (embeddingModelRunner != null) {
            try {
                embeddingModelRunner.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
                progressInfo.log("Error during existing model shutdown: " + e.getMessage());
            }
            embeddingModelRunner = null;
        }
    }

    /**
     * Resolves the artifact for the given environment, downloads it if necessary, and applies
     * the configuration so that model/tokenizer paths are populated.
     * Follows the same pattern as {@link org.hkijena.jipipe.api.environments.JIPipeEnvironmentConfigurator#configure}.
     *
     * @param environment  the artifact-based embedding model environment
     * @param progressInfo the progress info
     */
    private void resolveAndApplyArtifactConfiguration(EmbeddingModelEnvironment environment, JIPipeProgressInfo progressInfo) {
        JIPipeArtifactsServiceComponent artifacts = getService().getArtifacts();
        String query = environment.getArtifactQuery().getQuery();

        // Step 1: Resolve the artifact from the cache
        JIPipeArtifact artifact = artifacts.searchClosestCompatibleArtifactFromQuery(query);
        if (artifact == null) {
            throw new IllegalStateException("Unable to find a compatible artifact for the embedding model. " +
                    "Query: " + query + ". Please check your internet connection and artifact configuration.");
        }
        progressInfo.log("Matched artifact: " + artifact.getFullId());

        // Step 2: Download if the artifact is remote (not yet installed locally)
        if (artifact instanceof JIPipeRemoteArtifact remoteArtifact) {
            progressInfo.log("Downloading artifact " + remoteArtifact.getFullId());
            JIPipeArtifactRepositoryApplyInstallUninstallRun run = new JIPipeArtifactRepositoryApplyInstallUninstallRun(
                    List.of(remoteArtifact), Collections.emptyList());
            run.setProgressInfo(progressInfo.resolve("Download artifact"));
            run.run();

            // Refresh the cache and re-query to get the local artifact
            artifacts.updateCachedArtifacts(progressInfo.resolve("Refresh artifact cache"));
            artifact = artifacts.searchClosestCompatibleArtifactFromQuery(query);
            if (artifact == null) {
                throw new IllegalStateException("Artifact download was unsuccessful. " +
                        "Unable to find the artifact after download. Query: " + query);
            }
        }

        // Step 3: Apply configuration from the local artifact
        if (artifact instanceof JIPipeLocalArtifact localArtifact) {
            environment.applyConfigurationFromArtifactAndSetLastArtifact(localArtifact,
                    progressInfo.resolve("Configure environment from artifact"));
        } else {
            throw new IllegalStateException("Artifact download was unsuccessful! " +
                    "The artifact " + artifact.getFullId() + " is still not available locally. " +
                    "Check your internet connection and if your firewall does not block remote repositories.");
        }
    }

    /**
     * Blocking shutdown of the embedding model.
     * Releases both the spawned server lease (if any) and the direct runner (if any).
     * Must only be called from the queue thread.
     *
     * @param progressInfo the progress info
     */
    public void stopEmbeddingModelNow(JIPipeProgressInfo progressInfo) {
        if (embeddingLease == null && embeddingModelRunner == null) {
            return;
        }

        progressInfo.log("Stopping embedding model ...");

        JIPipeAIModelRunnerStatus oldStatus = getEmbeddingModelStatus();
        fireStatusChanged(oldStatus, JIPipeAIModelRunnerStatus.Unloading);

        try {
            if (embeddingLease != null) {
                try {
                    embeddingLease.close();
                } catch (Exception e) {
                    progressInfo.log("Error during embedding server lease release: " + e.getMessage());
                }
                embeddingLease = null;
            }
            if (embeddingModelRunner != null) {
                try {
                    embeddingModelRunner.shutdown();
                } catch (Exception e) {
                    progressInfo.log("Error during model shutdown: " + e.getMessage());
                }
                embeddingModelRunner = null;
            }
            embeddingModelError = null;
            fireStatusChanged(JIPipeAIModelRunnerStatus.Unloading, JIPipeAIModelRunnerStatus.Unloaded);
        } catch (Exception e) {
            progressInfo.log("Error during model shutdown: " + e.getMessage());
            embeddingModelError = ExceptionUtils.getMessage(e);
            fireStatusChanged(JIPipeAIModelRunnerStatus.Unloading, JIPipeAIModelRunnerStatus.Failed);
        }
    }

    /**
     * Enqueue a start operation. Safe to call from UI thread.
     * If a model is currently loaded, it will be stopped first (unload+load = replace).
     */
    public void tryStartEmbeddingModel() {
        JIPipeAIModelRunnerStatus status = getEmbeddingModelStatus();

        // Already loading — nothing to do
        if (status == JIPipeAIModelRunnerStatus.Loading) {
            return;
        }

        // Already idle or busy — nothing to do
        if (status == JIPipeAIModelRunnerStatus.Idle || status == JIPipeAIModelRunnerStatus.Busy) {
            return;
        }

        if (hasEmbeddingModel()) {
            taskQueue.enqueue(new UnloadEmbeddingModelTask());
        }
        taskQueue.enqueue(new LoadEmbeddingModelTask());
    }

    /**
     * Enqueue a stop operation. Safe to call from UI thread.
     * If the model is busy, the unload will happen after the current task completes
     * (because the queue serializes operations).
     */
    public void tryStopEmbeddingModel() {
        taskQueue.enqueue(new UnloadEmbeddingModelTask());
    }

    /**
     * Synchronous embed. Must only be called from the queue thread.
     * <p>
     * Branches on the active resource:
     * <ul>
     *     <li>If a server lease is held, performs an HTTP POST against the spawned server.</li>
     *     <li>Otherwise, if a direct runner is loaded, uses it.</li>
     *     <li>Otherwise throws {@link IllegalStateException}.</li>
     * </ul>
     *
     * @param text the text to embed
     * @return the embedding vector
     * @throws IllegalStateException if no embedding model is loaded
     */
    public float[] embedNow(String text) {
        if (embeddingLease == null && embeddingModelRunner == null) {
            throw new IllegalStateException("No embedding model loaded");
        }

        embeddingProgressInfo.log("Embedding: " + text);

        JIPipeAIModelRunnerStatus oldStatus = getEmbeddingModelStatus();
        fireStatusChanged(oldStatus, JIPipeAIModelRunnerStatus.Busy);

        try {
            if (embeddingLease != null && embeddingLease.isOpen()) {
                float[] result = embeddingLease.getInstance().embed(text);
                // The embed finished. The server instance stays Busy because the lease is still
                // held (the model remains loaded), but the AI is no longer processing an embed.
                fireStatusChanged(JIPipeAIModelRunnerStatus.Busy,
                        JIPipeAIModelRunnerStatus.Idle);
                return result;
            } else if (embeddingModelRunner != null) {
                float[] result = embeddingModelRunner.embed(text);
                fireStatusChanged(JIPipeAIModelRunnerStatus.Busy, embeddingModelRunner.getStatus());
                return result;
            } else {
                throw new IllegalStateException("No embedding model loaded");
            }
        } catch (RuntimeException e) {
            embeddingModelError = ExceptionUtils.getMessage(e);
            embeddingProgressInfo.error(embeddingModelError);
            fireStatusChanged(JIPipeAIModelRunnerStatus.Busy, JIPipeAIModelRunnerStatus.Failed);
            throw e;
        } catch (Exception e) {
            // Wrap checked exceptions (e.g., IOException from the spawned server) so the
            // public signature stays unchanged (no checked throws clause).
            embeddingModelError = ExceptionUtils.getMessage(e);
            embeddingProgressInfo.error(embeddingModelError);
            fireStatusChanged(JIPipeAIModelRunnerStatus.Busy, JIPipeAIModelRunnerStatus.Failed);
            throw new RuntimeException(e);
        }
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

        // If model is not loaded, enqueue a load first.
        // Use hasEmbeddingModel() so both the server-lease path and the direct-runner path
        // are covered; the status checks use currentStatus, which is maintained for both.
        JIPipeAIModelRunnerStatus status = getEmbeddingModelStatus();
        if (!hasEmbeddingModel()
                || status == JIPipeAIModelRunnerStatus.Unloaded
                || status == JIPipeAIModelRunnerStatus.Failed) {
            // Only enqueue if not already loading (avoid duplicate load tasks)
            if (status != JIPipeAIModelRunnerStatus.Loading) {
                taskQueue.enqueue(new LoadEmbeddingModelTask());
            }
        }

        EmbedTextTask embedTask = new EmbedTextTask(text);
        taskQueue.enqueue(embedTask);
        return embedTask.getFuture();
    }

    /**
     * Submit a function that can perform multiple synchronous embeddings on the queue thread.
     * <p>
     * This is the preferred API for callers that need to embed multiple texts (e.g., the AI search
     * which needs to embed all node entries plus the query). Instead of enqueuing one task per
     * text (which floods the serial queue and prevents cancellation from propagating), a single
     * task is enqueued that receives a {@link Function} backed by {@link #embedNow(String)}.
     * <p>
     * The function will be called on the queue worker thread, where {@code embedNow} is safe.
     * The caller receives a {@link CompletableFuture} that completes with whatever the function
     * returns.
     * <p>
     * If the model is not loaded, a load task is enqueued first.
     *
     * @param fn   the function to execute on the queue thread; receives an embed function and returns a result
     * @param <T>  the result type
     * @return a future that completes with the function's result, or null if AI is disabled
     */
    public <T> CompletableFuture<T> submitEmbedFunction(Function<java.util.function.Function<String, float[]>, T> fn) {
        AIApplicationSettings settings = AIApplicationSettings.getInstance();
        if (!settings.isEnableAI()) {
            return null;
        }

        JIPipeAIModelRunnerStatus status = getEmbeddingModelStatus();
        if (!hasEmbeddingModel()
                || status == JIPipeAIModelRunnerStatus.Unloaded
                || status == JIPipeAIModelRunnerStatus.Failed) {
            if (status != JIPipeAIModelRunnerStatus.Loading) {
                taskQueue.enqueue(new LoadEmbeddingModelTask());
            }
        }

        EmbedFunctionTask<T> task = new EmbedFunctionTask<>(fn);
        taskQueue.enqueue(task);
        return task.getFuture();
    }

    // ===== Status Query Methods =====

    public boolean hasEmbeddingModel() {
        return (embeddingLease != null && embeddingLease.isOpen()) || embeddingModelRunner != null;
    }

    public JIPipeAIModelRunnerStatus getEmbeddingModelStatus() {
        return currentStatus;
    }

    public String getEmbeddingModelError() {
        if (embeddingLease != null) {
            return embeddingModelError;
        }
        if (embeddingModelRunner != null) {
            return embeddingModelRunner.getLastError();
        }
        return embeddingModelError;
    }

    /**
     * Get the model ID of the currently loaded embedding model.
     * Returns null if no model is loaded.
     *
     * @return the model ID, or null if no model is loaded
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

    public JIPipeQueuedRunnableExecutor getQueue() {
        return taskQueue;
    }

    // ===== Event System =====

    public StatusChangedEventEmitter getStatusChangedEventEmitter() {
        return statusChangedEventEmitter;
    }

    /**
     * Event emitted when the embedding model status changes.
     */
    public static class StatusChangedEvent extends AbstractJIPipeEvent {
        private final JIPipeAIModelRunnerStatus oldStatus;
        private final JIPipeAIModelRunnerStatus newStatus;

        public StatusChangedEvent(Object source,
                                  JIPipeAIModelRunnerStatus oldStatus,
                                  JIPipeAIModelRunnerStatus newStatus) {
            super(source);
            this.oldStatus = oldStatus;
            this.newStatus = newStatus;
        }

        public JIPipeAIModelRunnerStatus getOldStatus() {
            return oldStatus;
        }

        public JIPipeAIModelRunnerStatus getNewStatus() {
            return newStatus;
        }
    }

    /**
     * Listener interface for embedding model status changes.
     */
    public interface StatusChangedEventListener {
        void onAIStatusChanged(StatusChangedEvent event);
    }

    /**
     * Event emitter for embedding model status changes.
     */
    public static class StatusChangedEventEmitter
            extends JIPipeEventEmitter<StatusChangedEvent, StatusChangedEventListener> {
        @Override
        protected void call(StatusChangedEventListener listener, StatusChangedEvent event) {
            listener.onAIStatusChanged(event);
        }
    }

    // ===== Task Classes (inner classes) =====

    /**
     * Task to load the embedding model. Runs on the queue worker thread.
     */
    private class LoadEmbeddingModelTask extends DefaultJIPipeRunnable {
        @Override
        public String getTaskLabel() {
            return "Load embedding model";
        }

        @Override
        public void run() {
            startEmbeddingModelNow(getProgressInfo());
        }
    }

    /**
     * Task to unload the embedding model. Runs on the queue worker thread.
     */
    private class UnloadEmbeddingModelTask extends DefaultJIPipeRunnable {
        @Override
        public String getTaskLabel() {
            return "Unload embedding model";
        }

        @Override
        public void run() {
            stopEmbeddingModelNow(getProgressInfo());
        }
    }

    /**
     * Task to embed text. Runs on the queue worker thread.
     */
    private class EmbedTextTask extends DefaultJIPipeRunnable {
        private final CompletableFuture<float[]> future = new CompletableFuture<>();
        private final String text;

        public EmbedTextTask(String text) {
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
     * Task that executes a function on the queue thread, providing it with
     * a synchronous embed function backed by {@link #embedNow(String)}.
     * This allows batch embedding without flooding the queue with individual tasks.
     */
    private class EmbedFunctionTask<T> extends DefaultJIPipeRunnable {
        private final CompletableFuture<T> future = new CompletableFuture<>();
        private final Function<java.util.function.Function<String, float[]>, T> fn;

        public EmbedFunctionTask(Function<java.util.function.Function<String, float[]>, T> fn) {
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
