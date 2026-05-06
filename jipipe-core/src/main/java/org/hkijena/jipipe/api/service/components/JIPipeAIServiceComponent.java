package org.hkijena.jipipe.api.service.components;

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
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;
import org.hkijena.jipipe.plugins.ai.AIApplicationSettings;
import org.hkijena.jipipe.plugins.ai.environments.EmbeddingModelEnvironment;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service component that manages AI model lifecycle and provides a task queue for serialized execution of AI operations.
 * <p>
 * The component owns a {@link JIPipeRunnableQueue} that serializes all AI operations, ensuring:
 * - No concurrent access to model runners (which are not thread-safe)
 * - Natural deferred-unload semantics: an unload task queued while an embed is running will execute after the embed completes
 * - Simple reasoning about state transitions
 */
public class JIPipeAIServiceComponent extends JIPipeServiceComponent {

    private final JIPipeRunnableQueue taskQueue = new JIPipeRunnableQueue("AI Service");
    private volatile JIPipeEmbeddingAIModelRunner embeddingModelRunner;
    private volatile JIPipeAIModelRunnerStatus currentStatus = JIPipeAIModelRunnerStatus.Unloaded;
    private String embeddingModelError;
    private final StatusChangedEventEmitter statusChangedEventEmitter = new StatusChangedEventEmitter();

    public JIPipeAIServiceComponent(JIPipeService service) {
        super(service);
        taskQueue.setSilent(true);
    }

    private void fireStatusChanged(JIPipeAIModelRunnerStatus oldStatus, JIPipeAIModelRunnerStatus newStatus) {
        currentStatus = newStatus;
        statusChangedEventEmitter.emit(new StatusChangedEvent(this, oldStatus, newStatus));
    }

    // ===== Lifecycle Methods =====

    /**
     * Blocking start of the embedding model.
     * Creates a runner from current settings and starts it.
     * Must only be called from the queue thread.
     *
     * @param progressInfo the progress info
     */
    public void startEmbeddingModelNow(JIPipeProgressInfo progressInfo) {
        // If already loaded and idle, skip
        if (embeddingModelRunner != null && embeddingModelRunner.getStatus() == JIPipeAIModelRunnerStatus.Idle) {
            return;
        }

        // If a runner exists in any state, shut it down first
        if (embeddingModelRunner != null) {
            try {
                embeddingModelRunner.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
                progressInfo.log("Error during existing model shutdown: " + e.getMessage());
            }
            embeddingModelRunner = null;
        }

        // Create new runner from settings
        AIApplicationSettings settings = AIApplicationSettings.getInstance();
        EmbeddingModelEnvironment environment = settings.getEmbeddingModelEnvironment();
        embeddingModelError = null;

        // Emit status change to Loading
        JIPipeAIModelRunnerStatus oldStatus = getEmbeddingModelStatus();
        fireStatusChanged(oldStatus, JIPipeAIModelRunnerStatus.Loading);

        try {
            // Ensure artifact is downloaded and configuration is applied before creating the runner
            if (environment.isLoadFromArtifact()) {
                resolveAndApplyArtifactConfiguration(environment, progressInfo);
            } else {
                if (!environment.isValid()) {
                    throw new IllegalStateException("Embedding model environment is not valid. " +
                            "Please configure a local model or remote API in the AI settings.");
                }
            }

            JIPipeEmbeddingAIModelRunner runner = environment.toRunner();
            embeddingModelRunner = runner;  // Assign BEFORE start() so UI can see Loading state
            runner.start();
            fireStatusChanged(JIPipeAIModelRunnerStatus.Loading, runner.getStatus());
        } catch (Exception e) {
            e.printStackTrace();
            progressInfo.log(e);
            embeddingModelError = ExceptionUtils.getMessage(e);
            embeddingModelRunner = null;
            fireStatusChanged(JIPipeAIModelRunnerStatus.Loading, JIPipeAIModelRunnerStatus.Failed);
            return;
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
     * Must only be called from the queue thread.
     *
     * @param progressInfo the progress info
     */
    public void stopEmbeddingModelNow(JIPipeProgressInfo progressInfo) {
        if (embeddingModelRunner == null) {
            return;
        }

        JIPipeAIModelRunnerStatus oldStatus = getEmbeddingModelStatus();
        fireStatusChanged(oldStatus, JIPipeAIModelRunnerStatus.Unloading);

        try {
            embeddingModelRunner.shutdown();
            embeddingModelRunner = null;
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
        if (embeddingModelRunner != null) {
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
     * Synchronous embed. Must only be called when model is Idle.
     * Must only be called from the queue thread.
     *
     * @param text the text to embed
     * @return the embedding vector
     * @throws IllegalStateException if no model is loaded or model is not idle
     */
    public float[] embedNow(String text) {
        if (embeddingModelRunner == null) {
            throw new IllegalStateException("No embedding model loaded");
        }
        if (embeddingModelRunner.getStatus() != JIPipeAIModelRunnerStatus.Idle) {
            throw new IllegalStateException("Embedding model is not idle (status: "
                    + embeddingModelRunner.getStatus() + ")");
        }

        JIPipeAIModelRunnerStatus oldStatus = getEmbeddingModelStatus();
        fireStatusChanged(oldStatus, JIPipeAIModelRunnerStatus.Busy);

        try {
            float[] result = embeddingModelRunner.embed(text);
            fireStatusChanged(JIPipeAIModelRunnerStatus.Busy, embeddingModelRunner.getStatus());
            return result;
        } catch (Exception e) {
            embeddingModelError = ExceptionUtils.getMessage(e);
            fireStatusChanged(JIPipeAIModelRunnerStatus.Busy, JIPipeAIModelRunnerStatus.Failed);
            throw e;
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

        // If model is not loaded, enqueue a load first
        if (embeddingModelRunner == null
                || embeddingModelRunner.getStatus() == JIPipeAIModelRunnerStatus.Unloaded
                || embeddingModelRunner.getStatus() == JIPipeAIModelRunnerStatus.Failed) {
            taskQueue.enqueue(new LoadEmbeddingModelTask());
        }

        EmbedTextTask embedTask = new EmbedTextTask(text);
        taskQueue.enqueue(embedTask);
        return embedTask.getFuture();
    }

    // ===== Status Query Methods =====

    public boolean hasEmbeddingModel() {
        return embeddingModelRunner != null;
    }

    public JIPipeAIModelRunnerStatus getEmbeddingModelStatus() {
        return currentStatus;
    }

    public String getEmbeddingModelError() {
        if (embeddingModelRunner != null) {
            return embeddingModelRunner.getLastError();
        }
        return embeddingModelError;
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

    // ===== Queue Access =====

    public JIPipeRunnableQueue getQueue() {
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
}
