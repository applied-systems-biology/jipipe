package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hkijena.jipipe.api.automation.AsyncAutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationSchemaUtils;
import org.hkijena.jipipe.api.automation.InstrumentationJob;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeQueuedRunnableExecutor;
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
     * desktop cache actions invoke. Implementations must not cache data themselves; the
     * production trigger hands the resolved targets to {@link JIPipeDesktopQuickRun}.
     */
    interface CacheTrigger {
        void run(JIPipeProject project, List<JIPipeGraphNode> targets, String mode);
    }

    /**
     * Production trigger: configures {@link JIPipeDesktopQuickRunSettings} exactly like
     * the desktop cache context actions and enqueues the run on the shared run queue.
     * The modes map as: full = cache the targets, intermediate = also store intermediate
     * results, predecessors = run only the dependencies of the targets.
     */
    static void runCacheServices(JIPipeProject project, List<JIPipeGraphNode> targets, String mode) {
        JIPipeDesktopQuickRunSettings settings = new JIPipeDesktopQuickRunSettings(project);
        settings.setSaveToDisk(false);
        settings.setStoreToCache(true);
        settings.setStoreIntermediateResults("intermediate".equals(mode));
        settings.setExcludeSelected("predecessors".equals(mode));
        JIPipeDesktopQuickRun run = new JIPipeDesktopQuickRun(project, targets, settings);
        JIPipeQueuedRunnableExecutor.getInstance().enqueue(run);
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
                return mapper.createObjectNode().put("jobId", job.getId());
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
                    trigger.run(ctx.getProject(), targets, mode);
                    job.complete(null);
                } catch (Exception e) {
                    job.fail(e.getMessage());
                }
            }, "instrumentation-update-cache-" + job.getId());
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
