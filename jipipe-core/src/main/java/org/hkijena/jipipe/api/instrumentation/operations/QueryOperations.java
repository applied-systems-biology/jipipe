package org.hkijena.jipipe.api.instrumentation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.instrumentation.InstrumentationAPI;
import org.hkijena.jipipe.api.instrumentation.InstrumentationContext;
import org.hkijena.jipipe.api.instrumentation.InstrumentationOperation;

/**
 * Built-in query operations for the instrumentation layer.
 * Each operation wraps a {@link InstrumentationAPI} method.
 */
public class QueryOperations {

    public static class QueryCompartments implements InstrumentationOperation {
        @Override public String getId() { return "query_compartments"; }
        @Override public String getDescription() { return "List all compartments with node counts"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            return InstrumentationAPI.queryCompartments(ctx);
        }
    }

    public static class QueryGraph implements InstrumentationOperation {
        @Override public String getId() { return "query_graph"; }
        @Override public String getDescription() { return "Get nodes and connections for a compartment (or all if not specified)"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            String compartmentId = params.has("compartmentId") ? params.get("compartmentId").asText() : null;
            return InstrumentationAPI.queryGraph(ctx, compartmentId);
        }
    }

    public static class QueryNode implements InstrumentationOperation {
        @Override public String getId() { return "query_node"; }
        @Override public String getDescription() { return "Get detailed info for a single node including parameters and slots"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            return InstrumentationAPI.queryNode(ctx, params.get("nodeId").asText());
        }
    }

    public static class QueryData implements InstrumentationOperation {
        @Override public String getId() { return "query_data"; }
        @Override public String getDescription() { return "Get data table for a node's slot"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            String nodeId = params.get("nodeId").asText();
            String slotName = params.get("slotName").asText();
            int limit = params.has("limit") ? params.get("limit").asInt() : 50;
            return InstrumentationAPI.queryData(ctx, nodeId, slotName, limit);
        }
    }
}
