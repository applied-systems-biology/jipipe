package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hkijena.jipipe.api.automation.*;

/**
 * Built-in async execution operations.
 */
public class ExecutionOperations {

    public static class RunNode implements AsyncAutomationOperation {
        @Override
        public String getId() {
            return "run_node";
        }

        @Override
        public String getDescription() {
            return "Run pipeline up to a node with specified mode";
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) throws Exception {
            InstrumentationJob job = executeAsync(ctx, params);
            return new ObjectMapper().createObjectNode().put("jobId", job.getId());
        }

        @Override
        public InstrumentationJob executeAsync(AutomationContext ctx, JsonNode params) {
            String nodeId = params.get("nodeId").asText();
            RunMode mode = RunMode.valueOf(params.has("mode") ? params.get("mode").asText() : "UPDATE_CACHE");
            return JIPipeAutomationAPI.runNode(ctx, nodeId, mode);
        }
    }

    public static class RunCompartment implements AsyncAutomationOperation {
        @Override
        public String getId() {
            return "run_compartment";
        }

        @Override
        public String getDescription() {
            return "Run all output nodes of a compartment";
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) throws Exception {
            InstrumentationJob job = executeAsync(ctx, params);
            return new ObjectMapper().createObjectNode().put("jobId", job.getId());
        }

        @Override
        public InstrumentationJob executeAsync(AutomationContext ctx, JsonNode params) {
            return JIPipeAutomationAPI.runCompartment(ctx, params.get("compartmentId").asText());
        }
    }

    public static class RunPipeline implements AsyncAutomationOperation {
        @Override
        public String getId() {
            return "run_pipeline";
        }

        @Override
        public String getDescription() {
            return "Run the entire pipeline";
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) throws Exception {
            InstrumentationJob job = executeAsync(ctx, params);
            return new ObjectMapper().createObjectNode().put("jobId", job.getId());
        }

        @Override
        public InstrumentationJob executeAsync(AutomationContext ctx, JsonNode params) {
            RunMode mode = RunMode.valueOf(params.has("mode") ? params.get("mode").asText() : "PIPELINE_CACHE");
            return JIPipeAutomationAPI.runPipeline(ctx, mode);
        }
    }
}
