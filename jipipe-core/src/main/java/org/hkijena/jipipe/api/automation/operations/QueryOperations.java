package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationSchemaUtils;
import org.hkijena.jipipe.api.automation.JIPipeAutomationAPI;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;

/**
 * Built-in query operations for the automation layer.
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
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.emptyObjectSchema();
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
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "properties": {
                        "compartment-id": {"type": "string", "description": "Limit the result to this compartment (UUID or alias); omit to return nodes and connections of all compartments"}
                      },
                      "additionalProperties": false
                    }""");
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
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            String compartment = params.has("compartment-id") ? params.get("compartment-id").asText() : null;
            return JIPipeAutomationAPI.queryGraph(ctx, compartment);
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
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["node-id"],
                      "properties": {
                        "node-id": {"type": "string", "description": "The node id as used by query_graph (alias id or UUID)"}
                      },
                      "additionalProperties": false
                    }""");
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
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            return JIPipeAutomationAPI.queryNode(ctx, params.get("node-id").asText());
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
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["node-id", "slot-name"],
                      "properties": {
                        "node-id": {"type": "string", "description": "The node id as used by query_graph (alias id or UUID)"},
                        "slot-name": {"type": "string", "description": "Name of the node's output or input slot (output slots are checked first)"},
                        "limit": {"type": "integer", "description": "Maximum number of rows to return (default 50)"}
                      },
                      "additionalProperties": false
                    }""");
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
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            String node = params.get("node-id").asText();
            String slot = params.get("slot-name").asText();
            int limit = params.has("limit") ? params.get("limit").asInt() : 50;
            return JIPipeAutomationAPI.queryData(ctx, node, slot, limit);
        }
    }
}
