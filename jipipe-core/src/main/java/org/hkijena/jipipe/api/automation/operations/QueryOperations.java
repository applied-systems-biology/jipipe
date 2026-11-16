package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.automation.JIPipeAutomationAPI;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;

/**
 * Built-in query operations for the instrumentation layer.
 * Each operation wraps a {@link JIPipeAutomationAPI} method.
 */
public class QueryOperations {

    public static class QueryCompartments implements AutomationOperation {
        @Override
        public String getId() {
            return "query_compartments";
        }

        @Override
        public String getDescription() {
            return "List all compartments with node counts";
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            return JIPipeAutomationAPI.queryCompartments(ctx);
        }
    }

    public static class QueryGraph implements AutomationOperation {
        @Override
        public String getId() {
            return "query_graph";
        }

        @Override
        public String getDescription() {
            return "Get nodes and connections for a compartment (or all if not specified)";
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            String compartmentId = params.has("compartmentId") ? params.get("compartmentId").asText() : null;
            return JIPipeAutomationAPI.queryGraph(ctx, compartmentId);
        }
    }

    public static class QueryNode implements AutomationOperation {
        @Override
        public String getId() {
            return "query_node";
        }

        @Override
        public String getDescription() {
            return "Get detailed info for a single node including parameters and slots";
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            return JIPipeAutomationAPI.queryNode(ctx, params.get("nodeId").asText());
        }
    }

    public static class QueryData implements AutomationOperation {
        @Override
        public String getId() {
            return "query_data";
        }

        @Override
        public String getDescription() {
            return "Get data table for a node's slot";
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            String nodeId = params.get("nodeId").asText();
            String slotName = params.get("slotName").asText();
            int limit = params.has("limit") ? params.get("limit").asInt() : 50;
            return JIPipeAutomationAPI.queryData(ctx, nodeId, slotName, limit);
        }
    }
}
