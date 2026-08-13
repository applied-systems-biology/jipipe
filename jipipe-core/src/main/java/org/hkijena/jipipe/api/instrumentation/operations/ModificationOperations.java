package org.hkijena.jipipe.api.instrumentation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.api.instrumentation.InstrumentationAPI;
import org.hkijena.jipipe.api.instrumentation.InstrumentationContext;
import org.hkijena.jipipe.api.instrumentation.InstrumentationOperation;
import org.hkijena.jipipe.api.instrumentation.events.CompartmentChangedEvent;

/**
 * Built-in modification operations.
 */
public class ModificationOperations {

    public static class AddNode implements InstrumentationOperation {
        @Override public String getId() { return "add_node"; }
        @Override public String getDescription() { return "Add a node to a compartment"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            String compartmentId = params.has("compartmentId") ? params.get("compartmentId").asText() : null;
            String nodeTypeId = params.get("nodeTypeId").asText();
            int x = params.has("x") ? params.get("x").asInt() : 0;
            int y = params.has("y") ? params.get("y").asInt() : 0;
            String alias = InstrumentationAPI.addNode(ctx, compartmentId, nodeTypeId, x, y);
            ObjectNode result = new ObjectMapper().createObjectNode();
            result.put("nodeId", alias);
            return result;
        }
    }

    public static class RemoveNode implements InstrumentationOperation {
        @Override public String getId() { return "remove_node"; }
        @Override public String getDescription() { return "Remove a node from the graph"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            InstrumentationAPI.removeNode(ctx, params.get("nodeId").asText());
            return new ObjectMapper().createObjectNode().put("success", true);
        }
    }

    public static class AddConnection implements InstrumentationOperation {
        @Override public String getId() { return "add_connection"; }
        @Override public String getDescription() { return "Connect two slots"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            InstrumentationAPI.addConnection(ctx,
                    params.get("source").asText(), params.get("sourceSlot").asText(),
                    params.get("target").asText(), params.get("targetSlot").asText());
            return new ObjectMapper().createObjectNode().put("success", true);
        }
    }

    public static class RemoveConnection implements InstrumentationOperation {
        @Override public String getId() { return "remove_connection"; }
        @Override public String getDescription() { return "Disconnect two slots"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            InstrumentationAPI.removeConnection(ctx,
                    params.get("source").asText(), params.get("sourceSlot").asText(),
                    params.get("target").asText(), params.get("targetSlot").asText());
            return new ObjectMapper().createObjectNode().put("success", true);
        }
    }

    public static class SetParameter implements InstrumentationOperation {
        @Override public String getId() { return "set_parameter"; }
        @Override public String getDescription() { return "Set a parameter on a node"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            InstrumentationAPI.setParameter(ctx,
                    params.get("nodeId").asText(), params.get("key").asText(), params.get("value").asText());
            return new ObjectMapper().createObjectNode().put("success", true);
        }
    }

    public static class SetProjectMetadata implements InstrumentationOperation {
        @Override public String getId() { return "set_project_metadata"; }
        @Override public String getDescription() { return "Set project name and/or description"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            String name = params.has("name") ? params.get("name").asText() : null;
            String description = params.has("description") ? params.get("description").asText() : null;
            InstrumentationAPI.setProjectMetadata(ctx, name, description);
            return new ObjectMapper().createObjectNode().put("success", true);
        }
    }

    public static class AddCompartment implements InstrumentationOperation {
        @Override public String getId() { return "add_compartment"; }
        @Override public String getDescription() { return "Create a new compartment"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            String id = InstrumentationAPI.addCompartment(ctx, params.get("name").asText());
            return new ObjectMapper().createObjectNode().put("compartmentId", id);
        }
    }

    public static class RenameCompartment implements InstrumentationOperation {
        @Override public String getId() { return "rename_compartment"; }
        @Override public String getDescription() { return "Rename a compartment"; }
        @Override public JsonNode execute(InstrumentationContext ctx, JsonNode params) {
            var project = ctx.getProject();
            if (project == null) throw new IllegalStateException("No project available");
            String compartmentId = params.get("compartmentId").asText();
            String name = params.get("name").asText();
            var comp = project.findCompartment(compartmentId);
            if (comp == null) throw new IllegalArgumentException("Compartment not found: " + compartmentId);
            comp.setCustomName(name);
            ctx.getEventBus().publish(new CompartmentChangedEvent(
                    ctx.getProjectId(), comp.getProjectCompartmentUUID().toString(), name, "renamed"));
            return new ObjectMapper().createObjectNode().put("success", true);
        }
    }
}
