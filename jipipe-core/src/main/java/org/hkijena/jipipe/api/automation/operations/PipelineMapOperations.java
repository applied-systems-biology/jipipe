package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationSchemaUtils;
import org.hkijena.jipipe.api.automation.JIPipeAutomationAPI;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;

/**
 * Built-in pipeline map and search operations.
 */
public class PipelineMapOperations {

    public static class GetPipelineMap implements AutomationOperation {
        @Override
        public String getId() {
            return "get_pipeline_map";
        }

        @Override
        public String getDescription() {
            return "Get a hierarchical map of the pipeline structure";
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
                        "segment-id": {"type": "string"}
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

    public static class SearchNodes implements AutomationOperation {
        @Override
        public String getId() {
            return "search_nodes";
        }

        @Override
        public String getDescription() {
            return "Search available JIPipe node types";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["query"],
                      "properties": {
                        "query": {"type": "string"},
                        "limit": {"type": "integer"}
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
            String query = params.get("query").asText();
            int limit = params.has("limit") ? params.get("limit").asInt() : 20;
            return JIPipeAutomationAPI.searchNodes(ctx, query, limit);
        }
    }
}
