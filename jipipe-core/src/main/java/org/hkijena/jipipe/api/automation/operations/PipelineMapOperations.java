package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationSchemaUtils;
import org.hkijena.jipipe.api.automation.JIPipeAutomationAPI;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationToolScope;

/**
 * Built-in pipeline map and search operations.
 */
public class PipelineMapOperations {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static ObjectNode error(String message) {
        return mapper.createObjectNode().put("error", message);
    }

    public static class GetPipelineMap implements AutomationOperation {
        @Override
        public String getId() {
            return "get_pipeline_map";
        }

        @Override
        public String getDescription() {
            return "Get a hierarchical map of the pipeline: one segment per compartment with its nodes, the compartment-to-compartment wiring (edges: source and target compartment UUID, label = the source compartment's output slot name), and a status per compartment. Status mapping: ERROR = validation errors in the compartment; OK = all nodes cached and no validation issues; WARNING = some nodes cached or validation warnings; UNKNOWN = nothing cached and no issues (an empty compartment is UNKNOWN). cachedNodes/totalNodes carry the exact cache coverage";
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
            return JIPipeAutomationAPI.getPipelineMap(ctx);
        }
    }

    public static class GetSegmentDetail implements AutomationOperation {
        @Override
        public String getId() {
            return "get_segment_detail";
        }

        @Override
        public String getDescription() {
            return "Get detail for a specific pipeline segment";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["segment-id"],
                      "properties": {
                        "segment-id": {"type": "string", "description": "Segment id from get_pipeline_map (a compartment UUID)"}
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
            return JIPipeAutomationAPI.getSegmentDetail(ctx, params.get("segment-id").asText());
        }
    }

    public static class DescribeProject implements AutomationOperation {
        @Override
        public String getId() {
            return "describe_project";
        }

        @Override
        public String getDescription() {
            return "Describe the whole project in one call: metadata, compartments with status and cache coverage, "
                    + "the compartment-to-compartment wiring, annotations, and the nodes whose output slots are "
                    + "exported to disk (slot store-to-disk enabled and not skipped; a run can still exclude nodes, "
                    + "e.g. cache updates). detail=full adds the node list per compartment. For per-node parameter "
                    + "detail use query_node, for the text report use get_project_report, for run errors use get_errors";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "properties": {
                        "detail": {"type": "string", "enum": ["summary", "full"], "default": "summary", "description": "summary: sections with counts. full: additionally the node list per compartment"}
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
            String detail = null;
            if (params != null && params.has("detail")) {
                detail = params.get("detail").asText();
            }
            if (detail != null && !"summary".equals(detail) && !"full".equals(detail)) {
                return error("Unknown detail: " + detail + " (supported: summary, full)");
            }
            return JIPipeAutomationAPI.describeProject(ctx, detail);
        }
    }

    public static class SearchNodes implements AutomationOperation {
        @Override
        public String getId() {
            return "search_nodes";
        }

        @Override
        public String getDescription() {
            return "Search available JIPipe node types (searches the algorithm catalog; use find_nodes to search inside the open project)";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["query"],
                      "properties": {
                        "query": {"type": "string", "description": "Text matched case-insensitively against node type name, id, and description"},
                        "limit": {"type": "integer", "description": "Maximum number of results to return (default 20)"}
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
        public AutomationToolScope getToolScope() {
            return AutomationToolScope.INSTANCE_GLOBAL;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            String query = params.get("query").asText();
            int limit = params.has("limit") ? params.get("limit").asInt() : 20;
            return JIPipeAutomationAPI.searchNodes(ctx, query, limit);
        }
    }
}
