package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.automation.AsyncAutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationSchemaSpec;
import org.hkijena.jipipe.api.automation.AutomationToolScope;
import org.hkijena.jipipe.api.automation.InstrumentationJob;
import org.hkijena.jipipe.api.automation.InstrumentationJobStatus;
import org.hkijena.jipipe.api.automation.events.CapabilityChangedEvent;
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
            // Fail-closed guard: refuse a concurrent install for the same capability
            if (!IN_FLIGHT_INSTALLS.add(capability)) {
                throw new IllegalArgumentException("Install already running for capability: " + capability);
            }
            InstrumentationJob job = ctx.getJobManager().createJob("install_capability",
                    "Install capability " + capability, ctx.getProgressInfo());
            Thread thread = new Thread(() -> {
                ObjectNode result = null;
                String error = null;
                try {
                    result = runInstall(job, capability, providerId);
                } catch (Exception e) {
                    error = e.getMessage() != null ? e.getMessage() : e.toString();
                    if (isActive(job)) {
                        // Surface the failure in the job's progress store (the automation layer's
                        // shared chain): get_errors reports it from there.
                        job.getProgressInfo().log(e);
                    }
                } finally {
                    IN_FLIGHT_INSTALLS.remove(capability);
                }
                if (isActive(job)) {
                    if (result != null) {
                        job.complete(result);
                        publishCapabilityChanged(ctx, job, capability, true, null);
                    } else {
                        job.fail(error);
                        publishCapabilityChanged(ctx, job, capability, false, error);
                    }
                } else {
                    // Cancelled via cancel_job: the cancel path already published the job event
                    publishCapabilityChanged(ctx, job, capability, false, "Cancelled by user");
                }
            }, "automation-install-capability-" + job.getId());
            thread.setDaemon(true);
            thread.start();
            return job;
        }

        /**
         * Runs the artifact install on the calling (background job) thread and
         * returns the job result. Throws when the install did not make the
         * provider present; the worker terminalizes the job and publishes the
         * capability-changed event from there.
         * <p>
         * The run's own progress counters live in resolved child nodes of the
         * job's progress handle (each child carries its own counters, so the
         * root handle does not move as the run progresses), therefore the job
         * reports milestone events at phase boundaries instead of polled
         * fractions: the per-phase log lines and the store records flow through
         * the same progress store and are readable from get_errors and get_log.
         */
        private static ObjectNode runInstall(InstrumentationJob job, String capability, String providerId) throws Exception {
            JIPipeCapabilityRegistry registry = JIPipe.getCapabilities();
            JIPipeCapabilityProvider provider = registry.findProvider(providerId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown provider for capability '" + capability + "': " + providerId));
            if (!(provider instanceof JIPipeArtifactCapabilityProvider artifactProvider)) {
                throw new IllegalArgumentException("Capability '" + capability + "' cannot be installed through JIPipe: "
                        + "provider '" + providerId + "' is not an artifact provider");
            }
            job.updateProgress(0.0, "Resolving artifact for capability " + capability);
            JIPipeArtifact artifact = JIPipe.getArtifacts()
                    .queryPreferredCachedArtifact(artifactProvider.getArtifactQueryPrefix());
            if (!(artifact instanceof JIPipeRemoteArtifact remoteArtifact)) {
                throw new IllegalArgumentException("No installable artifact found for capability '" + capability + "': "
                        + artifactProvider.getArtifactQueryPrefix()
                        + " (check the configured artifact repositories and your internet connection)");
            }
            JIPipeProgressInfo progressInfo = job.getProgressInfo();
            progressInfo.log("Installing artifact " + remoteArtifact.getFullId() + " for capability " + capability);
            job.updateProgress(0.05, "Installing " + remoteArtifact.getFullId());
            JIPipeArtifactRepositoryApplyInstallUninstallRun run =
                    new JIPipeArtifactRepositoryApplyInstallUninstallRun(List.of(remoteArtifact), List.of());
            run.setProgressInfo(progressInfo);
            run.run();
            job.updateProgress(0.9, "Verifying installation of " + remoteArtifact.getFullId());
            // The run swallows per-artifact errors into notifications (the desktop dialog's
            // contract), so the provider's presence probe is the source of truth for success.
            boolean present = provider.getProbe().get() == JIPipeCapabilityStatus.State.AVAILABLE;
            if (!present) {
                throw new IllegalStateException("Install finished but provider '" + providerId
                        + "' is still not present for capability '" + capability + "'");
            }
            ObjectNode result = mapper.createObjectNode();
            result.put("capability", capability);
            result.put("provider", providerId);
            result.put("artifact", remoteArtifact.getFullId());
            result.put("state", stateName(registry.getStatus(capability).state()));
            return result;
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
