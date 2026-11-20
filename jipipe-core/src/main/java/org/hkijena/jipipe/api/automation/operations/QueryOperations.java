package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationSchemaUtils;
import org.hkijena.jipipe.api.automation.JIPipeAutomationAPI;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;

import java.util.ArrayList;
import java.util.List;

/**
 * Built-in query operations for the automation layer.
 * Each operation wraps a {@link JIPipeAutomationAPI} method.
 */
public class QueryOperations {

    private static final ObjectMapper mapper = new ObjectMapper();

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
                        "compartment-id": {"type": "string", "description": "Limit the result to this compartment (UUID, alias, or name); omit to return nodes and connections of all compartments"},
                        "fields": {"type": "array", "description": "Whitelist of fields to include in each node object (they are emitted in the canonical order regardless of the request order); omit to include all fields", "items": {"type": "string", "enum": ["id", "name", "typeId", "compartmentId", "uuid", "x", "y"]}},
                        "omit-positions": {"type": "boolean", "description": "Drop the x and y fields from node objects, even when listed in fields"},
                        "connections-only": {"type": "boolean", "description": "Return only the connections: the nodes array is empty"}
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
            List<String> fields = null;
            if (params.has("fields")) {
                fields = new ArrayList<>();
                for (JsonNode field : params.get("fields")) {
                    String name = field.asText();
                    if (!JIPipeAutomationAPI.QUERY_GRAPH_NODE_FIELDS.contains(name)) {
                        return mapper.createObjectNode().put("error", "Unknown field '" + name
                                + "'. Valid fields: " + String.join(", ", JIPipeAutomationAPI.QUERY_GRAPH_NODE_FIELDS));
                    }
                    fields.add(name);
                }
            }
            boolean omitPositions = params.path("omit-positions").asBoolean(false);
            boolean connectionsOnly = params.path("connections-only").asBoolean(false);
            try {
                return JIPipeAutomationAPI.queryGraph(ctx, compartment, fields, omitPositions, connectionsOnly);
            } catch (IllegalArgumentException e) {
                return mapper.createObjectNode().put("error", e.getMessage() + " (see query_compartments for ids)");
            }
        }
    }

    public static class FindNodes implements AutomationOperation {
        @Override
        public String getId() {
            return "find_nodes";
        }

        @Override
        public String getDescription() {
            return "Search nodes inside the open project by name, node type id, and compartment (find_nodes searches the project; search_nodes searches the algorithm catalog)";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "properties": {
                        "query": {"type": "string", "description": "Matched case-insensitively as a substring of the node name; omit or leave blank to match all node names"},
                        "type-id": {"type": "string", "description": "Matched case-insensitively as a substring of the registered node type id; omit or leave blank to match all node types"},
                        "compartment": {"type": "string", "description": "Limit the search to this compartment (UUID, alias, or name); omit or leave blank to search all compartments"},
                        "limit": {"type": "integer", "minimum": 0, "description": "Maximum number of matches to return (default 50)"}
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
            String query = params.has("query") ? params.get("query").asText() : null;
            String typeId = params.has("type-id") ? params.get("type-id").asText() : null;
            String compartment = params.has("compartment") ? params.get("compartment").asText() : null;
            int limit = params.has("limit") ? params.get("limit").asInt() : 50;
            try {
                return JIPipeAutomationAPI.findNodes(ctx, query, typeId, compartment, limit);
            } catch (IllegalArgumentException e) {
                return mapper.createObjectNode().put("error", e.getMessage() + " (see query_compartments for ids)");
            }
        }
    }

    public static class QueryNotes implements AutomationOperation {
        @Override
        public String getId() {
            return "query_notes";
        }

        @Override
        public String getDescription() {
            return "List all canvas notes of the project (comments and text boxes) with id, uuid, typeId, kind, "
                    + "compartmentId, title, and content for text boxes. Positions and sizes are grid cells "
                    + "(1 cell = 25 px): x/y are the stored grid position; text boxes additionally report "
                    + "gridWidth/gridHeight (the values of the grid-width/grid-height parameters) and "
                    + "width/height in pixels. Resize with set_parameter on grid-width/grid-height, create "
                    + "with create_note";
        }

        @Override
        public String getAgentDocumentation() {
            return getDescription() + ". x/y/width/height and gridWidth/gridHeight are grid cells "
                    + "(1 cell = 25 px): to move a note write x/y in grid cells, to resize a text box write "
                    + "the grid-width/grid-height parameters in grid cells (width/height in the response are "
                    + "the same sizes in pixels). Annotation DATA is a different concept: text annotation and "
                    + "data annotation columns live in query_data, not here";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "properties": {
                        "compartment": {"type": "string", "description": "Limit the result to notes within this compartment (UUID, alias, or name); omit to return notes of all compartments"},
                        "kind": {"type": "string", "enum": ["comment", "text-box"], "description": "Only return notes of this kind; omit to return both kinds"}
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
            String compartment = params.has("compartment") ? params.get("compartment").asText() : null;
            String kind = params.has("kind") ? params.get("kind").asText() : null;
            try {
                return JIPipeAutomationAPI.queryNotes(ctx, compartment, kind);
            } catch (IllegalArgumentException e) {
                return mapper.createObjectNode().put("error", e.getMessage() + " (see query_compartments for ids)");
            }
        }
    }

    public static class QueryNode implements AutomationOperation {
        @Override
        public String getId() {
            return "query_node";
        }

        @Override
        public String getDescription() {
            return "Get detailed info for a single node including parameters and slots. Parameter values are normalized objects {value, type, raw?}: value is the effective readable string (expressions without the 'Expression: ' prefix), raw is the plain toString when it differs";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["node-id"],
                      "properties": {
                        "node-id": {"type": "string", "description": "The node id as used by query_graph (alias id or UUID)"},
                        "include-docs": {"type": "boolean", "default": false, "description": "Add documentation metadata to every entry of parameters: the name, description, and important flag from the parameter definition"}
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
            boolean includeDocs = params.path("include-docs").asBoolean(false);
            return JIPipeAutomationAPI.queryNode(ctx, params.get("node-id").asText(), includeDocs);
        }
    }

    public static class GetParameterDiff implements AutomationOperation {
        @Override
        public String getId() {
            return "get_parameter_diff";
        }

        @Override
        public String getDescription() {
            return "Get the changed-vs-default parameter diff of a single node: every parameter of the node is compared against a fresh instance of its node type (the same machinery the project report uses to list non-default parameters), so defaultValue mirrors a fresh node. Both sides are rendered like query_node values and compared as text; defaultValue is JSON null when the fresh instance has no such parameter. Only changed entries are returned unless include-unchanged is set; changedCount counts the changed entries";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["node-id"],
                      "properties": {
                        "node-id": {"type": "string", "description": "The node id as used by query_graph (alias id or UUID)"},
                        "include-unchanged": {"type": "boolean", "default": false, "description": "Also include parameters whose rendered value matches the fresh instance (changed false)"}
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
            boolean includeUnchanged = params.path("include-unchanged").asBoolean(false);
            return JIPipeAutomationAPI.getParameterDiff(ctx, params.get("node-id").asText(), includeUnchanged);
        }
    }

    public static class QueryData implements AutomationOperation {
        @Override
        public String getId() {
            return "query_data";
        }

        @Override
        public String getDescription() {
            return "Get the data table of a node's slot: row count, column names (Name, text-annotation columns, "
                    + "then data-annotation columns), dataAnnotationTypes (data-annotation column to data type id), "
                    + "and up to limit rows with Name, text-annotation and data-annotation values as readable "
                    + "strings. Use describe_data for full column schemas";
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
