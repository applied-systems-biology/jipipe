package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.automation.AsyncAutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationSchemaSpec;
import org.hkijena.jipipe.api.automation.AutomationToolScope;
import org.hkijena.jipipe.api.automation.InstrumentationJob;
import org.hkijena.jipipe.api.automation.InstrumentationJobStatus;
import org.hkijena.jipipe.api.automation.events.CapabilityChangedEvent;
import org.hkijena.jipipe.api.run.JIPipeQueuedRunnableExecutor;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifactRepositoryApplyInstallUninstallRun;
import org.hkijena.jipipe.api.artifacts.JIPipeRemoteArtifact;
import org.hkijena.jipipe.api.capabilities.JIPipeArtifactCapabilityProvider;
import org.hkijena.jipipe.api.capabilities.JIPipeCapability;
import org.hkijena.jipipe.api.capabilities.JIPipeCapabilityCandidate;
import org.hkijena.jipipe.api.capabilities.JIPipeCapabilityProvider;
import org.hkijena.jipipe.api.capabilities.JIPipeCapabilityQuery;
import org.hkijena.jipipe.api.capabilities.JIPipeCapabilityRegistry;
import org.hkijena.jipipe.api.capabilities.JIPipeCapabilityStatus;
import org.hkijena.jipipe.api.capabilities.JIPipeInstallHint;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Built-in capability operations: status query, listing, and the async artifact install.
 * <p>
 * The answers mirror the capability query shape from the design document:
 * {@code {"id", "state", "blockedBy", "install", "setup", "candidates"}}, with
 * {@code state} in lowercase snake case ({@code available}, {@code needs_setup},
 * {@code blocked}, {@code missing}) and {@code setup} always null in v1.
 * All three operations are instance-global: the capability registry is not
 * project data.
 * <p>
 * The install enqueues {@link JIPipeArtifactRepositoryApplyInstallUninstallRun}
 * on the shared {@link JIPipeQueuedRunnableExecutor} (the same mechanism the
 * desktop cache actions use, wired by {@code CacheOperations.wireJobToExecutorLifecycle}),
 * so {@code cancel_job} cancels the run: queued runs are dropped, running runs
 * are interrupted. An in-flight archive download may still finish its current
 * transfer before the run checks for cancellation; the half-written target is
 * removed again, so a cancelled install leaves nothing marked installed.
 */
public class CapabilityOperations {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * The capability ids whose install job is currently running. A second
     * install request for the same capability is refused (fail-closed) instead
     * of queued, so two downloads can never fight over the same target path.
     */
    private static final Set<String> IN_FLIGHT_INSTALLS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static String textOrNull(JsonNode params, String field) {
        return params.has(field) && !params.get(field).isNull() ? params.get(field).asText() : null;
    }

    private static ObjectNode error(String message) {
        return mapper.createObjectNode().put("error", message);
    }

    /**
     * The lowercase snake-case spelling of a resolved capability state, e.g.
     * {@code needs_setup} for {@link JIPipeCapabilityStatus.State#NEEDS_SETUP}.
     */
    private static String stateName(JIPipeCapabilityStatus.State state) {
        return state.name().toLowerCase(Locale.ROOT);
    }

    /**
     * A capability id is known when it is a registered capability definition or
     * when at least one registered provider can satisfy it (a plain provider
     * level such as {@code cellpose4} that no definition declares is still
     * answerable). Unknown ids are refused with the invalid-params error.
     */
    private static boolean isKnown(JIPipeCapabilityRegistry registry, String capabilityId) {
        return registry.find(capabilityId).isPresent()
                || !registry.query(capabilityId).candidates().isEmpty();
    }

    /**
     * Renders one capability query in the MCP answer shape: the resolved state,
     * the install hint to act on (serialized, never null), {@code setup} null in
     * v1, and the ranked candidates serialized verbatim.
     */
    static ObjectNode queryToJson(JIPipeCapabilityQuery query) {
        ObjectNode data = mapper.createObjectNode();
        data.put("id", query.id());
        data.put("state", stateName(query.status().state()));
        ArrayNode blockedBy = data.putArray("blockedBy");
        for (String blocked : query.status().blockedBy()) {
            blockedBy.add(blocked);
        }
        data.set("install", mapper.valueToTree(query.install()));
        data.putNull("setup");
        ArrayNode candidates = data.putArray("candidates");
        for (JIPipeCapabilityCandidate candidate : query.candidates()) {
            candidates.add(mapper.valueToTree(candidate));
        }
        return data;
    }

    /**
     * Get the status of one capability: its resolved state, the missing
     * requirements, the install hint to act on, and the ranked candidate providers.
     */
    public static class GetStatus implements AutomationOperation {
        @Override
        public String getId() {
            return "get_capability_status";
        }

        @Override
        public String getDescription() {
            return "Get the status of one capability: resolved state (available, needs_setup, blocked, missing), "
                    + "the missing requirements, the install hint to act on, and the ranked candidate providers. "
                    + "Unknown capability ids return an error naming the id";
        }

        private static final AutomationSchemaSpec SPEC = AutomationSchemaSpec.object()
                .string("capability", "The capability id to query, e.g. ai:embedding, ai:mcp, or a plain id like cellpose4").required()
                .build();

        @Override
        public AutomationSchemaSpec getSchemaSpec() {
            return SPEC;
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Query;
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        public AutomationToolScope getToolScope() {
            return AutomationToolScope.INSTANCE_GLOBAL;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            String capability = textOrNull(params, "capability");
            if (capability == null) {
                return error("capability is required");
            }
            JIPipeCapabilityRegistry registry = JIPipe.getCapabilities();
            if (!isKnown(registry, capability)) {
                return error("Unknown capability: " + capability);
            }
            return queryToJson(registry.query(capability));
        }
    }

    /**
     * List the registered capabilities with their resolved status; each entry
     * has the same shape as get_capability_status.
     */
    public static class ListCapabilities implements AutomationOperation {
        @Override
        public String getId() {
            return "list_capabilities";
        }

        @Override
        public String getDescription() {
            return "List the registered capabilities with their resolved status; each entry has the same shape "
                    + "as get_capability_status (id, state, blockedBy, install, setup, candidates)";
        }

        private static final AutomationSchemaSpec SPEC = AutomationSchemaSpec.object()
                .string("domain", "Optional domain id filter, e.g. ai or acp; omit to list all capabilities")
                .build();

        @Override
        public AutomationSchemaSpec getSchemaSpec() {
            return SPEC;
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Query;
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        public AutomationToolScope getToolScope() {
            return AutomationToolScope.INSTANCE_GLOBAL;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            String domain = textOrNull(params, "domain");
            JIPipeCapabilityRegistry registry = JIPipe.getCapabilities();
            ObjectNode data = mapper.createObjectNode();
            ArrayNode capabilities = data.putArray("capabilities");
            for (JIPipeCapability capability : registry.getCapabilities()) {
                if (domain != null && !domain.equals(capability.getDomain())) {
                    continue;
                }
                capabilities.add(queryToJson(registry.query(capability.getId())));
            }
            return data;
        }
    }

    /**
     * Install one capability through its artifact provider. Resolves the chosen
     * provider from the capability's ranked candidates, downloads the artifact
     * with the same machinery as the desktop AI setup dialog
     * ({@link JIPipeArtifactRepositoryApplyInstallUninstallRun}), and reports the
     * result through the job.
     * <p>
     * The install is refused when the provider has no artifact install hint
     * (method none, manual, or auto_on_demand are not installable through this
     * operation) or when an install for the same capability is already running
     * (fail-closed: no queueing). When the job finishes (success, failure, or
     * cancellation) a {@link CapabilityChangedEvent} is published on the context
     * event bus so clients can re-query the status.
     * <p>
     * The run is enqueued on the shared {@link JIPipeQueuedRunnableExecutor} and
     * wired to the job like {@code CacheOperations.UpdateCache}, so
     * {@code cancel_job} cancels it (queued runs are dropped, running runs are
     * interrupted; an in-flight download may finish its current transfer, the
     * half-written target is removed again).
     */
    public static class Install implements AsyncAutomationOperation {
        @Override
        public String getId() {
            return "install_capability";
        }

        @Override
        public String getDescription() {
            return "Install one capability through its artifact provider: downloads the artifact from the "
                    + "configured artifact repositories, returns a job id, and publishes a capability-changed event "
                    + "when the job finishes. Track the job with get_job_status. Only providers with an artifact "
                    + "install hint are installable; providers with method none, manual, or auto_on_demand return "
                    + "an error. A second install for the same capability is refused while the first is running";
        }

        private static final AutomationSchemaSpec SPEC = AutomationSchemaSpec.object()
                .string("capability", "The capability id to install, e.g. ai:embedding").required()
                .string("provider", "The provider id to install, as returned in the candidates list of get_capability_status or list_capabilities").required()
                .build();

        @Override
        public AutomationSchemaSpec getSchemaSpec() {
            return SPEC;
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Execute;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public AutomationToolScope getToolScope() {
            return AutomationToolScope.INSTANCE_GLOBAL;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) throws Exception {
            try {
                InstrumentationJob job = executeAsync(ctx, params);
                // Plan-pinned envelope: {"job-id": ...} is the key get_job_status and cancel_job consume
                return mapper.createObjectNode().put("job-id", job.getId());
            } catch (IllegalArgumentException e) {
                return error(e.getMessage());
            }
        }

        @Override
        public InstrumentationJob executeAsync(AutomationContext ctx, JsonNode params) {
            String capability = textOrNull(params, "capability");
            String providerId = textOrNull(params, "provider");
            if (capability == null || providerId == null) {
                throw new IllegalArgumentException("capability and provider are required");
            }
            JIPipeCapabilityRegistry registry = JIPipe.getCapabilities();
            if (!isKnown(registry, capability)) {
                throw new IllegalArgumentException("Unknown capability: " + capability);
            }
            JIPipeCapabilityCandidate candidate = null;
            for (JIPipeCapabilityCandidate c : registry.query(capability).candidates()) {
                if (c.providerId().equals(providerId)) {
                    candidate = c;
                    break;
                }
            }
            if (candidate == null) {
                throw new IllegalArgumentException("Unknown provider for capability '" + capability + "': " + providerId);
            }
            if (candidate.hint() == null || !JIPipeInstallHint.METHOD_ARTIFACT.equals(candidate.hint().method())) {
                throw new IllegalArgumentException("Capability '" + capability + "' cannot be installed through JIPipe: "
                        + "provider '" + providerId + "' has no artifact install hint");
            }
            // Resolve the provider once here (the hint-method check above already passed, so a
            // provider that is not an artifact provider is refused up front): the install run
            // carries the resolved provider and does not look it up again.
            JIPipeCapabilityProvider provider = registry.findProvider(providerId).orElse(null);
            if (!(provider instanceof JIPipeArtifactCapabilityProvider artifactProvider)) {
                throw new IllegalArgumentException("Capability '" + capability + "' cannot be installed through JIPipe: "
                        + "provider '" + providerId + "' is not an artifact provider");
            }
            // Resolve the remote artifact BEFORE the job is created (the shared artifact cache
            // is refreshed at service init), so a missing artifact is refused with the
            // invalid-params error instead of being installed and failing at the end of the job.
            JIPipeArtifact artifact = JIPipe.getArtifacts()
                    .queryPreferredCachedArtifact(artifactProvider.getArtifactQueryPrefix() + ":*");
            if (!(artifact instanceof JIPipeRemoteArtifact remoteArtifact)) {
                throw new IllegalArgumentException("No installable artifact found for capability '" + capability + "': "
                        + artifactProvider.getArtifactQueryPrefix()
                        + " (check the configured artifact repositories and your internet connection)");
            }
            // Fail-closed guard, taken BEFORE the job exists: the terminal listeners release
            // the slot, so a slot is only ever held by a job that will release it (if
            // createJob fails below, the slot is released immediately, so it is never held
            // by an abandoned job).
            if (!IN_FLIGHT_INSTALLS.add(capability)) {
                throw new IllegalArgumentException("Install already running for capability: " + capability);
            }
            InstrumentationJob job;
            try {
                job = ctx.getJobManager().createJob("install_capability",
                        "Install capability " + capability, ctx.getProgressInfo());
            } catch (RuntimeException e) {
                IN_FLIGHT_INSTALLS.remove(capability);
                throw e;
            }
            InstallRun run = new InstallRun(remoteArtifact, artifactProvider, capability, providerId);
            // The run records its progress on the job's handle (the automation layer's shared
            // per-project chain), so get_log and get_errors see the install records and the
            // worker's store adoption moves the chain with the run (the run engine's contract).
            // Set before enqueue(): the worker reads the run's progress info when it attaches
            // a file-backed store.
            run.setProgressInfo(job.getProgressInfo());
            JIPipeQueuedRunnableExecutor executor = JIPipeQueuedRunnableExecutor.getInstance();
            // Must be wired before enqueue(): the executor starts an enqueued run
            // synchronously when its queue is empty, so the started event can fire
            // while enqueue() is still on the stack - a later subscription would miss it.
            wireJobToExecutorLifecycle(ctx, job, capability, run, executor);
            executor.enqueue(run);
            return job;
        }

        /**
         * Registers the job with the executor lifecycle of the given run and arms the job
         * cancellation handler ({@link JIPipeQueuedRunnableExecutor#cancel} supports queued
         * and running runs). Must be called BEFORE the run is enqueued (see {@link #executeAsync}):
         * the executor re-emits the worker events of every run unconditionally, so the
         * listeners filter by run identity (the shared queue also carries unrelated runs).
         * <p>
         * The job mirrors the run lifecycle: PENDING while the run is queued, RUNNING once
         * the executor starts it, COMPLETED (with the install result) or FAILED when it
         * finishes or is interrupted, and CANCELLED when {@code cancel_job} cancels it
         * (either from the queue or from a running download). The capability-changed event
         * is published on the context event bus on every terminal outcome, and the in-flight
         * slot is released on completion, failure, and cancellation alike.
         */
        static void wireJobToExecutorLifecycle(AutomationContext ctx, InstrumentationJob job,
                                               String capability, InstallRun run,
                                               JIPipeQueuedRunnableExecutor executor) {
            job.setCancellationHandler(() -> executor.cancel(run));
            final JIPipeRunnable.StartedEventListener[] started = new JIPipeRunnable.StartedEventListener[1];
            final JIPipeRunnable.FinishedEventListener[] finished = new JIPipeRunnable.FinishedEventListener[1];
            final JIPipeRunnable.InterruptedEventListener[] interrupted = new JIPipeRunnable.InterruptedEventListener[1];
            started[0] = event -> {
                if (event.getRun() == run) {
                    job.setStatus(InstrumentationJobStatus.RUNNING);
                    job.updateProgress(0.05, "Installing " + run.artifactId());
                }
            };
            finished[0] = event -> {
                if (event.getRun() != run) {
                    return;
                }
                unsubscribeRunLifecycle(executor, started[0], finished[0], interrupted[0]);
                if (isActive(job)) {
                    ObjectNode result = run.verifyInstalled();
                    if (result != null) {
                        job.complete(result);
                        finishInstall(ctx, job, capability, true, null);
                    } else {
                        String message = "Install finished but provider '" + run.providerId
                                + "' is still not present for capability '" + capability + "'";
                        job.getProgressInfo().log(message);
                        job.fail(message);
                        finishInstall(ctx, job, capability, false, message);
                    }
                } else {
                    // The run finished but the job was already terminal (a cancel_job race):
                    // the job is not re-terminalized, but the slot is released and the
                    // capability-changed event is still published.
                    finishInstall(ctx, job, capability, false, "Cancelled by user");
                }
            };
            interrupted[0] = event -> {
                if (event.getRun() != run) {
                    return;
                }
                unsubscribeRunLifecycle(executor, started[0], finished[0], interrupted[0]);
                if (isActive(job)) {
                    // Surface the failure in the job's progress store (the automation layer's
                    // shared chain): get_errors reports it from there.
                    Throwable exception = event.getException();
                    String message = exception == null ? "Capability install failed"
                            : exception.getMessage() != null ? exception.getMessage() : exception.toString();
                    job.getProgressInfo().log(exception != null ? exception : new RuntimeException(message));
                    job.fail(message);
                    finishInstall(ctx, job, capability, false, message);
                } else {
                    // cancel_job cancelled the run: job.cancel() already set the job to
                    // CANCELLED and fired the job event, so the job is not re-terminalized,
                    // but the slot is released and the capability-changed event is still
                    // published so clients can re-query the status.
                    finishInstall(ctx, job, capability, false, "Cancelled by user");
                }
            };
            executor.getStartedEventEmitter().subscribe(started[0]);
            executor.getFinishedEventEmitter().subscribe(finished[0]);
            executor.getInterruptedEventEmitter().subscribe(interrupted[0]);
        }

        /**
         * The listeners must be removed on the first terminal event: they would otherwise
         * fire for future runs of other jobs (the emitters are shared per executor).
         * Unsubscribing from within a listener is safe: the emitter iterates a copy.
         */
        private static void unsubscribeRunLifecycle(JIPipeQueuedRunnableExecutor executor,
                                                    JIPipeRunnable.StartedEventListener started,
                                                    JIPipeRunnable.FinishedEventListener finished,
                                                    JIPipeRunnable.InterruptedEventListener interrupted) {
            executor.getStartedEventEmitter().unsubscribe(started);
            executor.getFinishedEventEmitter().unsubscribe(finished);
            executor.getInterruptedEventEmitter().unsubscribe(interrupted);
        }

        /**
         * The artifact install run: wraps {@link JIPipeArtifactRepositoryApplyInstallUninstallRun}
         * (the same machinery as the desktop AI setup dialog) in a {@link JIPipeRunnable} that
         * carries the capability metadata and the probe-based success verification.
         * <p>
         * The provider's {@code artifactQueryPrefix} is a coordinate prefix (group and
         * artifact id, no version or classifier) while {@code queryPreferredCachedArtifact}
         * matches a glob against the full id ({@code group.id:version-classifier}) with an
         * anchored full match, so the prefix is translated to a version-wildcard glob
         * ({@code prefix + ":*"}) when the artifact is resolved.
         */
        static final class InstallRun extends DefaultJIPipeRunnable {
            private final JIPipeRemoteArtifact artifact;
            private final JIPipeArtifactCapabilityProvider artifactProvider;
            private final String capability;
            private final String providerId;

            InstallRun(JIPipeRemoteArtifact artifact, JIPipeArtifactCapabilityProvider artifactProvider,
                       String capability, String providerId) {
                this.artifact = artifact;
                this.artifactProvider = artifactProvider;
                this.capability = capability;
                this.providerId = providerId;
            }

            String artifactId() {
                return artifact.getFullId();
            }

            @Override
            public void run() {
                getProgressInfo().log("Installing artifact " + artifact.getFullId() + " for capability " + capability);
                // The run swallows per-artifact errors into notifications (the desktop dialog's
                // contract), so the presence probe is the source of truth for success.
                JIPipeArtifactRepositoryApplyInstallUninstallRun installRun =
                        new JIPipeArtifactRepositoryApplyInstallUninstallRun(List.of(artifact), List.of());
                installRun.setProgressInfo(getProgressInfo());
                installRun.run();
            }

            /**
             * Verifies the install with the provider's presence probe.
             *
             * @return the job result when the provider is present, or null when it is not
             */
            ObjectNode verifyInstalled() {
                boolean present = artifactProvider.getProbe().get() == JIPipeCapabilityStatus.State.AVAILABLE;
                if (!present) {
                    return null;
                }
                JIPipeCapabilityRegistry registry = JIPipe.getCapabilities();
                ObjectNode result = mapper.createObjectNode();
                result.put("capability", capability);
                result.put("provider", providerId);
                result.put("artifact", artifact.getFullId());
                result.put("state", stateName(registry.getStatus(capability).state()));
                return result;
            }

            @Override
            public String getTaskLabel() {
                return "Install capability " + capability + " (" + artifact.getFullId() + ")";
            }
        }

        /**
         * The terminal path of one install: releases the in-flight slot (so the
         * capability is installable again) and publishes the capability-changed
         * event on the context event bus. Every terminal outcome (completion,
         * failure, and cancellation, including the cancel-before-finished race)
         * calls this exactly once, so the slot is released and clients can
         * re-query the status on every outcome.
         */
        private static void finishInstall(AutomationContext ctx, InstrumentationJob job,
                                          String capability, boolean success, String error) {
            IN_FLIGHT_INSTALLS.remove(capability);
            publishCapabilityChanged(ctx, job, capability, success, error);
        }

        private static void publishCapabilityChanged(AutomationContext ctx, InstrumentationJob job,
                                                     String capability, boolean success, String error) {
            ctx.getEventBus().publish(new CapabilityChangedEvent(ctx.getProjectId(), capability, job.getId(), success, error));
        }

        private static boolean isActive(InstrumentationJob job) {
            InstrumentationJobStatus status = job.getStatus();
            return status == InstrumentationJobStatus.PENDING || status == InstrumentationJobStatus.RUNNING;
        }
    }
}
