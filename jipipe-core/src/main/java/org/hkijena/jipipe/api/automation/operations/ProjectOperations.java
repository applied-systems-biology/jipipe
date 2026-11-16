package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.automation.AsyncAutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;

/**
 * Built-in project management operations.
 * These are handled by the server directly (not via JIPipeAutomationAPI)
 * because they require access to the desktop window system.
 */
public class ProjectOperations {

    public static class ListProjects implements AutomationOperation {
        @Override
        public String getId() {
            return "list_projects";
        }

        @Override
        public String getDescription() {
            return "List all open projects";
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode data = mapper.createObjectNode();
            ArrayNode projects = data.putArray("projects");
            for (var window : JIPipeDesktopProjectWindow.getOpenWindows()) {
                ObjectNode p = projects.addObject();
                p.put("id", window.getSessionId().toString());
                p.put("name", window.getProject() != null ? window.getProject().getMetadata().getName() : "Untitled");
                p.put("path", window.getProject() != null && window.getProject().getWorkDirectory() != null
                        ? window.getProject().getWorkDirectory().toString() : null);
                p.put("isOpen", true);
            }
            return data;
        }
    }

    public static class ListOperations implements AutomationOperation {
        @Override
        public String getId() {
            return "list_operations";
        }

        @Override
        public String getDescription() {
            return "List all available operations";
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode data = mapper.createObjectNode();
            ArrayNode ops = data.putArray("operations");
            var registry = JIPipe.getInstance().getAutomation().getOperationRegistry();
            for (var entry : registry.getAll().entrySet()) {
                ObjectNode op = ops.addObject();
                op.put("id", entry.getKey());
                op.put("description", entry.getValue().getDescription());
                op.put("async", entry.getValue() instanceof AsyncAutomationOperation);
            }
            return data;
        }
    }
}
