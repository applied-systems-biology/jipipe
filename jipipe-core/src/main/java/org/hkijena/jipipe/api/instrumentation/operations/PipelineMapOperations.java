package org.hkijena.jipipe.api.instrumentation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.instrumentation.InstrumentationAPI;
import org.hkijena.jipipe.api.instrumentation.InstrumentationContext;
import org.hkijena.jipipe.api.instrumentation.InstrumentationOperation;

/**
 * Built-in pipeline map and search operations.
 */
public class PipelineMapOperations {

    public static class GetPipelineMap implements InstrumentationOperation {
        @Override public String getId() { return "get_pipeline_map"; }
        @Override public String getDescription() { return "Get a hierarchical map of the pipeline structure"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            return InstrumentationAPI.getPipelineMap(ctx);
        }
    }

    public static class GetSegmentDetail implements InstrumentationOperation {
        @Override public String getId() { return "get_segment_detail"; }
        @Override public String getDescription() { return "Get detail for a specific pipeline segment"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            return InstrumentationAPI.getSegmentDetail(ctx, params.get("segmentId").asText());
        }
    }

    public static class SearchNodes implements InstrumentationOperation {
        @Override public String getId() { return "search_nodes"; }
        @Override public String getDescription() { return "Search available JIPipe node types"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            String query = params.get("query").asText();
            int limit = params.has("limit") ? params.get("limit").asInt() : 20;
            return InstrumentationAPI.searchNodes(ctx, query, limit);
        }
    }
}
