package org.hkijena.jipipe.api.instrumentation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hkijena.jipipe.api.instrumentation.*;

/**
 * Built-in async execution operations.
 */
public class ExecutionOperations {

    public static class RunNode implements AsyncInstrumentationOperation {
        @Override public String getId() { return "run_node"; }
        @Override public String getDescription() { return "Run pipeline up to a node with specified mode"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) throws Exception {
            InstrumentationJob job = executeAsync(ctx, params);
            return new ObjectMapper().createObjectNode().put("jobId", job.getId());
        }
        @Override public InstrumentationJob executeAsync(InstrumentationContext ctx, JsonNode params) {
            String nodeId = params.get("nodeId").asText();
            RunMode mode = RunMode.valueOf(params.has("mode") ? params.get("mode").asText() : "UPDATE_CACHE");
            return InstrumentationAPI.runNode(ctx, nodeId, mode);
        }
    }

    public static class RunCompartment implements AsyncInstrumentationOperation {
        @Override public String getId() { return "run_compartment"; }
        @Override public String getDescription() { return "Run all output nodes of a compartment"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) throws Exception {
            InstrumentationJob job = executeAsync(ctx, params);
            return new ObjectMapper().createObjectNode().put("jobId", job.getId());
        }
        @Override public InstrumentationJob executeAsync(InstrumentationContext ctx, JsonNode params) {
            return InstrumentationAPI.runCompartment(ctx, params.get("compartmentId").asText());
        }
    }

    public static class RunPipeline implements AsyncInstrumentationOperation {
        @Override public String getId() { return "run_pipeline"; }
        @Override public String getDescription() { return "Run the entire pipeline"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) throws Exception {
            InstrumentationJob job = executeAsync(ctx, params);
            return new ObjectMapper().createObjectNode().put("jobId", job.getId());
        }
        @Override public InstrumentationJob executeAsync(InstrumentationContext ctx, JsonNode params) {
            RunMode mode = RunMode.valueOf(params.has("mode") ? params.get("mode").asText() : "PIPELINE_CACHE");
            return InstrumentationAPI.runPipeline(ctx, mode);
        }
    }
}
