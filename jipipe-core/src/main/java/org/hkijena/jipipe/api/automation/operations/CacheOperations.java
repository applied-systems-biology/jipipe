package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hkijena.jipipe.api.automation.AsyncAutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationSchemaUtils;
import org.hkijena.jipipe.api.automation.InstrumentationJob;
import org.hkijena.jipipe.api.automation.InstrumentationJobStatus;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeQueuedRunnableExecutor;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRun;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRunSettings;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Built-in cache update operations.
 * <p>
 * The cache run itself is not reimplemented here: the production trigger delegates to
 * {@link JIPipeDesktopQuickRun}, the same service that the desktop cache actions
 * ("Update cache", "Cache intermediate results", "Update cache only predecessors")
 * enqueue on the shared run queue. The delegation sits behind the {@link CacheTrigger}
 * interface so the operation can be tested without executing a pipeline.
 */
public class CacheOperations {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Delegation seam between the update_cache operation and the cache services that the
     * desktop cache actions invoke. Implementations must not cache data themselves: the
     * production trigger hands the resolved targets to {@link JIPipeDesktopQuickRun} and
     * registers the job with the run lifecycle. The trigger owns the job completion: the
     * job must be completed or failed when the delegated work finishes (immediately for
     * synchronous implementations), never by the operation itself.
     */
    interface CacheTrigger {
        void run(JIPipeProject project, List<JIPipeGraphNode> targets, String mode, InstrumentationJob job);
    }

    /**
     * Production trigger: configures {@link JIPipeDesktopQuickRunSettings} exactly like
     * the desktop cache context actions and enqueues the run on the shared run queue.
     * The modes map as: full = cache the targets, intermediate = also store intermediate
     * results, predecessors = run only the dependencies of the targets.
     * <p>
     * The job reflects the QuickRun lifecycle: PENDING while the run is queued, RUNNING
     * once the executor starts it, and COMPLETED or FAILED (with the run error) when the
     * run finishes. {@code cancel_job} cancels the QuickRun on the shared queue (the
     * executor cancels queued and running runs).
     */
    static void runCacheServices(JIPipeProject project, List<JIPipeGraphNode> targets, String mode, InstrumentationJob job) {
        JIPipeDesktopQuickRunSettings settings = new JIPipeDesktopQuickRunSettings(project);
        settings.setSaveToDisk(false);
        settings.setStoreToCache(true);
        settings.setStoreIntermediateResults("intermediate".equals(mode));
        settings.setExcludeSelected("predecessors".equals(mode));
        JIPipeDesktopQuickRun run = new JIPipeDesktopQuickRun(project, targets, settings);
        JIPipeQueuedRunnableExecutor executor = JIPipeQueuedRunnableExecutor.getInstance();
        wireJobToExecutorLifecycle(job, run, executor);
        executor.enqueue(run);
    }

    /**
     * Registers the job with the executor lifecycle of the given run and arms the job
     * cancellation handler ({@link JIPipeQueuedRunnableExecutor#cancel} supports queued
     * and running runs). Must be called BEFORE the run is enqueued: the executor starts
     * an enqueued run synchronously when its queue is empty, so the started event can
     * fire while {@code enqueue()} is still on the stack - a later subscription would
     * miss it.
     * <p>
     * The executor re-emits the worker events of every run unconditionally, so the
     * listeners filter by run identity (the shared queue also carries unrelated runs).
     */
    static void wireJobToExecutorLifecycle(InstrumentationJob job, JIPipeRunnable run, JIPipeQueuedRunnableExecutor executor) {
        job.setCancellationHandler(() -> executor.cancel(run));
        final JIPipeRunnable.StartedEventListener[] started = new JIPipeRunnable.StartedEventListener[1];
        final JIPipeRunnable.FinishedEventListener[] finished = new JIPipeRunnable.FinishedEventListener[1];
        final JIPipeRunnable.InterruptedEventListener[] interrupted = new JIPipeRunnable.InterruptedEventListener[1];
        started[0] = event -> {
            if (event.getRun() == run) {
                job.setStatus(InstrumentationJobStatus.RUNNING);
            }
        };
        finished[0] = event -> {
            if (event.getRun() != run) {
                return;
            }
            unsubscribeRunLifecycle(executor, started[0], finished[0], interrupted[0]);
            if (isActive(job)) {
                job.complete(null);
            }
        };
        interrupted[0] = event -> {
            if (event.getRun() != run) {
                return;
            }
            unsubscribeRunLifecycle(executor, started[0], finished[0], interrupted[0]);
            if (isActive(job)) {
                Throwable exception = event.getException();
                String message = exception == null ? "Cache run failed"
                        : exception.getMessage() != null ? exception.getMessage() : exception.toString();
                // Surface the run failure in the job's progress store (the automation layer's
                // shared per-project chain): get_errors reports it from there. The job error
                // alone is only visible via get_job_status.
                job.getProgressInfo().log(exception != null ? exception : new RuntimeException(message));
                job.fail(message);
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

    private static boolean isActive(InstrumentationJob job) {
        InstrumentationJobStatus status = job.getStatus();
        return status == InstrumentationJobStatus.PENDING || status == InstrumentationJobStatus.RUNNING;
    }

    public static class UpdateCache implements AsyncAutomationOperation {
        private final CacheTrigger trigger;

        /**
         * Production constructor: delegates to the desktop cache services.
         */
        public UpdateCache() {
            this(CacheOperations::runCacheServices);
        }

        /**
         * Test seam: delegates to the given trigger instead of the cache services.
         */
        UpdateCache(CacheTrigger trigger) {
            this.trigger = trigger;
        }

        @Override
        public String getId() {
            return "update_cache";
        }

        @Override
        public String getDescription() {
            return "Update the cache by running the pipeline for a node, a compartment, or the whole project";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["scope", "mode"],
                      "properties": {
                        "scope": {"type": "string", "enum": ["node", "compartment", "pipeline"]},
                        "mode": {"type": "string", "enum": ["full", "intermediate", "predecessors"]},
                        "compartment": {"type": "string"},
                        "node": {"type": "string"}
                      },
                      "additionalProperties": false
                    }""");
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
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            try {
                InstrumentationJob job = executeAsync(ctx, params);
                // Plan-pinned envelope: {"job-id": ...} is the key get_job_status and cancel_job consume
                return mapper.createObjectNode().put("job-id", job.getId());
            } catch (IllegalArgumentException e) {
                return mapper.createObjectNode().put("error", e.getMessage());
            }
        }

        @Override
        public InstrumentationJob executeAsync(AutomationContext ctx, JsonNode params) {
            String scope = params.get("scope").asText();
            String mode = params.get("mode").asText();
            List<JIPipeGraphNode> targets = resolveTargets(ctx, params, scope);
            InstrumentationJob job = ctx.getJobManager().createJob("update_cache",
                    "Update cache (" + mode + ")", ctx.getProgressInfo());
            Thread thread = new Thread(() -> {
                try {
                    // The trigger registers the job with the delegated cache run: the job
                    // completes or fails when the run finishes, not when the trigger returns.
                    trigger.run(ctx.getProject(), targets, mode, job);
                } catch (Exception e) {
                    job.fail(e.getMessage());
                }
            }, "automation-update-cache-" + job.getId());
            thread.setDaemon(true);
            thread.start();
            return job;
        }

        /**
         * Resolves the cache targets from the scope. Conditional requirements that the
         * schema cannot express (compartment/node depending on the scope) are enforced here.
         */
        private List<JIPipeGraphNode> resolveTargets(AutomationContext ctx, JsonNode params, String scope) {
            JIPipeProject project = ctx.getProject();
            if (project == null) {
                throw new IllegalArgumentException("No project loaded");
            }
            switch (scope) {
                case "node": {
                    String compartment = textOrNull(params, "compartment");
                    if (compartment == null) {
                        throw new IllegalArgumentException("compartment is required when scope is 'node'");
                    }
                    String node = textOrNull(params, "node");
                    if (node == null) {
                        throw new IllegalArgumentException("node is required when scope is 'node'");
                    }
                    var resolved = AutomationOperationUtils.findCompartment(project, compartment);
                    if (resolved == null) {
                        throw new IllegalArgumentException("Compartment not found: " + compartment);
                    }
                    JIPipeGraphNode target = ctx.getGraph().findNode(node);
                    if (target == null) {
                        throw new IllegalArgumentException("Node not found: " + node);
                    }
                    if (target.getInfo() == null || !target.getInfo().isRunnable()) {
                        throw new IllegalArgumentException("Node is not runnable: " + node);
                    }
                    UUID nodeCompartment = ctx.getGraph().getCompartmentUUIDOf(target);
                    if (!Objects.equals(nodeCompartment, resolved.getProjectCompartmentUUID())) {
                        throw new IllegalArgumentException(
                                "Node '" + node + "' is not in compartment '" + compartment + "'");
                    }
                    return List.of(target);
                }
                case "compartment": {
                    String compartment = textOrNull(params, "compartment");
                    if (compartment == null) {
                        throw new IllegalArgumentException("compartment is required when scope is 'compartment'");
                    }
                    var resolved = AutomationOperationUtils.findCompartment(project, compartment);
                    if (resolved == null) {
                        throw new IllegalArgumentException("Compartment not found: " + compartment);
                    }
                    List<JIPipeGraphNode> targets = ctx.getGraph()
                            .getNodesWithinCompartment(resolved.getProjectCompartmentUUID()).stream()
                            .filter(n -> n.getInfo() != null && n.getInfo().isRunnable())
                            .collect(Collectors.toList());
                    if (targets.isEmpty()) {
                        throw new IllegalArgumentException("No runnable nodes in compartment: " + compartment);
                    }
                    return targets;
                }
                default: {
                    // 'pipeline': the schema enum restricts the scope to the handled values
                    List<JIPipeGraphNode> targets = ctx.getGraph().getGraphNodes().stream()
                            .filter(n -> n.getInfo() != null && n.getInfo().isRunnable())
                            .collect(Collectors.toList());
                    if (targets.isEmpty()) {
                        throw new IllegalArgumentException("No runnable nodes in project");
                    }
                    return targets;
                }
            }
        }

        private static String textOrNull(JsonNode params, String field) {
            return params.has(field) && !params.get(field).isNull() ? params.get(field).asText() : null;
        }
    }
}
