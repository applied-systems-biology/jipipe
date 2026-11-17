package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.api.automation.JIPipeAutomationAPI;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationSchemaUtils;
import org.hkijena.jipipe.api.automation.events.CompartmentChangedEvent;

/**
 * Built-in modification operations.
 */
public class ModificationOperations {

    public static class AddNode implements AutomationOperation {
        @Override
        public String getId() {
            return "add_node";
        }

        @Override
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Add a node to a compartment";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["node-type-id"],
                      "properties": {
                        "compartment-id": {"type": "string"},
                        "node-type-id": {"type": "string"},
                        "x": {"type": "integer"},
                        "y": {"type": "integer"}
                      },
                      "additionalProperties": false
                    }""");
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Modify;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            String compartment = params.has("compartment-id") ? params.get("compartment-id").asText() : null;
            String nodeType = params.get("node-type-id").asText();
            int x = params.has("x") ? params.get("x").asInt() : 0;
            int y = params.has("y") ? params.get("y").asInt() : 0;
            String alias = JIPipeAutomationAPI.addNode(ctx, compartment, nodeType, x, y);
            // Result payload key: the kebab-case convention applies to input parameters only
            ObjectNode result = new ObjectMapper().createObjectNode();
            result.put("nodeId", alias);
            return result;
        }
    }

    public static class RemoveNode implements AutomationOperation {
        @Override
        public String getId() {
            return "remove_node";
        }

        @Override
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Remove a node from the graph";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["node-id"],
                      "properties": {
                        "node-id": {"type": "string"}
                      },
                      "additionalProperties": false
                    }""");
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Modify;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            JIPipeAutomationAPI.removeNode(ctx, params.get("node-id").asText());
            return new ObjectMapper().createObjectNode().put("success", true);
        }
    }

    public static class AddConnection implements AutomationOperation {
        @Override
        public String getId() {
            return "add_connection";
        }

        @Override
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Connect two slots";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["source", "source-slot", "target", "target-slot"],
                      "properties": {
                        "source": {"type": "string"},
                        "source-slot": {"type": "string"},
                        "target": {"type": "string"},
                        "target-slot": {"type": "string"}
                      },
                      "additionalProperties": false
                    }""");
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Modify;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            JIPipeAutomationAPI.addConnection(ctx,
                    params.get("source").asText(), params.get("source-slot").asText(),
                    params.get("target").asText(), params.get("target-slot").asText());
            return new ObjectMapper().createObjectNode().put("success", true);
        }
    }

    public static class RemoveConnection implements AutomationOperation {
        @Override
        public String getId() {
            return "remove_connection";
        }

        @Override
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Disconnect two slots";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["source", "source-slot", "target", "target-slot"],
                      "properties": {
                        "source": {"type": "string"},
                        "source-slot": {"type": "string"},
                        "target": {"type": "string"},
                        "target-slot": {"type": "string"}
                      },
                      "additionalProperties": false
                    }""");
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Modify;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            JIPipeAutomationAPI.removeConnection(ctx,
                    params.get("source").asText(), params.get("source-slot").asText(),
                    params.get("target").asText(), params.get("target-slot").asText());
            return new ObjectMapper().createObjectNode().put("success", true);
        }
    }

    public static class SetParameter implements AutomationOperation {
        @Override
        public String getId() {
            return "set_parameter";
        }

        @Override
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Set a parameter on a node";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["node-id", "key", "value"],
                      "properties": {
                        "node-id": {"type": "string"},
                        "key": {"type": "string"},
                        "value": {"type": "string"}
                      },
                      "additionalProperties": false
                    }""");
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Modify;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            JIPipeAutomationAPI.setParameter(ctx,
                    params.get("node-id").asText(), params.get("key").asText(), params.get("value").asText());
            return new ObjectMapper().createObjectNode().put("success", true);
        }
    }

    public static class SetProjectMetadata implements AutomationOperation {
        @Override
        public String getId() {
            return "set_project_metadata";
        }

        @Override
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Set project name and/or description";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "properties": {
                        "name": {"type": "string"},
                        "description": {"type": "string"}
                      },
                      "additionalProperties": false
                    }""");
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Modify;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            String name = params.has("name") ? params.get("name").asText() : null;
            String description = params.has("description") ? params.get("description").asText() : null;
            JIPipeAutomationAPI.setProjectMetadata(ctx, name, description);
            return new ObjectMapper().createObjectNode().put("success", true);
        }
    }

    public static class AddCompartment implements AutomationOperation {
        @Override
        public String getId() {
            return "add_compartment";
        }

        @Override
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Create a new compartment";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["name"],
                      "properties": {
                        "name": {"type": "string"}
                      },
                      "additionalProperties": false
                    }""");
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Modify;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            String id = JIPipeAutomationAPI.addCompartment(ctx, params.get("name").asText());
            // Result payload key: the kebab-case convention applies to input parameters only
            return new ObjectMapper().createObjectNode().put("compartmentId", id);
        }
    }

    public static class RenameCompartment implements AutomationOperation {
        @Override
        public String getId() {
            return "rename_compartment";
        }

        @Override
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Rename a compartment";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["compartment-id", "name"],
                      "properties": {
                        "compartment-id": {"type": "string"},
                        "name": {"type": "string"}
                      },
                      "additionalProperties": false
                    }""");
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Modify;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            var project = ctx.getProject();
            if (project == null) throw new IllegalStateException("No project available");
            String compartment = params.get("compartment-id").asText();
            String name = params.get("name").asText();
            var comp = project.findCompartment(compartment);
            if (comp == null) throw new IllegalArgumentException("Compartment not found: " + compartment);
            comp.setCustomName(name);
            ctx.getEventBus().publish(new CompartmentChangedEvent(
                    ctx.getProjectId(), comp.getProjectCompartmentUUID().toString(), name, "renamed"));
            return new ObjectMapper().createObjectNode().put("success", true);
        }
    }
}
