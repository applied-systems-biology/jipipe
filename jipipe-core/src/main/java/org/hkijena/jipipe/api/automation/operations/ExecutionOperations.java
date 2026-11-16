package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.api.automation.*;

/**
 * Built-in async execution operations.
 */
public class ExecutionOperations {

    private static final ObjectMapper mapper = new ObjectMapper();

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
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["nodeId"],
                      "properties": {
                        "nodeId": {"type": "string"},
                        "mode": {"type": "string", "enum": ["UPDATE_CACHE", "CACHE_INTERMEDIATE", "UPDATE_PREDECESSOR_CACHE", "PIPELINE_CACHE", "PIPELINE_FILESYSTEM", "PIPELINE_DISCARD"]}
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
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["compartmentId"],
                      "properties": {
                        "compartmentId": {"type": "string"}
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
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "properties": {
                        "mode": {"type": "string", "enum": ["UPDATE_CACHE", "CACHE_INTERMEDIATE", "UPDATE_PREDECESSOR_CACHE", "PIPELINE_CACHE", "PIPELINE_FILESYSTEM", "PIPELINE_DISCARD"]}
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

    public static class GetJobStatus implements AutomationOperation {
        @Override
        public String getId() {
            return "get_job_status";
        }

        @Override
        public String getDescription() {
            return "Get the state and progress of an async execution job";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["job-id"],
                      "properties": {
                        "job-id": {"type": "string"}
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
            return true;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            String jobId = params.get("job-id").asText();
            InstrumentationJob job = ctx.getJobManager().getJob(jobId);
            if (job == null) {
                return mapper.createObjectNode().put("error", "Unknown job id: " + jobId);
            }
            ObjectNode data = mapper.createObjectNode();
            data.put("id", job.getId());
            data.put("operation", job.getOperation());
            data.put("state", job.getStatus().name());
            data.put("progress", job.getProgress());
            if (job.getMessage() != null) {
                data.put("message", job.getMessage());
            }
            if (job.getError() != null) {
                data.put("error", job.getError());
            }
            return data;
        }
    }

    public static class CancelJob implements AutomationOperation {
        @Override
        public String getId() {
            return "cancel_job";
        }

        @Override
        public String getDescription() {
            return "Cancel a pending or running async execution job";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["job-id"],
                      "properties": {
                        "job-id": {"type": "string"}
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
            String jobId = params.get("job-id").asText();
            InstrumentationJob job = ctx.getJobManager().getJob(jobId);
            if (job == null) {
                return mapper.createObjectNode().put("error", "Unknown job id: " + jobId);
            }
            boolean cancelled = ctx.getJobManager().cancelJob(jobId);
            return mapper.createObjectNode().put("cancelled", cancelled);
        }
    }
}
