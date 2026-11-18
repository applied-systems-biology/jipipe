/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationSchemaUtils;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewerWindow;

import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Built-in data viewer operations: report what the open data viewer windows show
 * (get_viewer_state), navigate a viewer to a data table row (goto_viewer_row), and close
 * a viewer (close_viewer). Navigation and closing require the window's agent-control
 * flag: the user toggles it in the data viewer ribbon ("Agent control" in the Data band),
 * so an agent can only drive viewers the user handed over (a viewer opened by
 * open_data_viewer starts with the flag on). get_viewer_state is a pure read and needs no
 * flag. All three run on the EDT: they read and act on Swing windows.
 */
public class ViewerOperations {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static ObjectNode error(String message) {
        return mapper.createObjectNode().put("error", message);
    }

    /**
     * The gating error for goto_viewer_row and close_viewer: states that agent control is
     * disabled for the target window and how the user enables it.
     */
    private static ObjectNode agentControlDisabledError() {
        return error("Agent control is disabled for this data viewer window: the user must enable "
                + "it first with the 'Agent control' toggle in the window's ribbon (Data band)");
    }

    /**
     * All open data viewer windows, ordered deterministically by id (the underlying open
     * window set is an unordered HashSet). Must run on the EDT.
     */
    private static List<JIPipeDesktopDataViewerWindow> openViewers() {
        List<JIPipeDesktopDataViewerWindow> viewers = new ArrayList<>(JIPipeDesktopDataViewerWindow.getOpenWindows());
        viewers.sort(Comparator.comparing(JIPipeDesktopDataViewerWindow::getId));
        return viewers;
    }

    /**
     * The viewer window in the active window's owner chain (the same chain
     * {@link AutomationOperationUtils#resolveDefaultProjectWindow} walks), or null.
     * Must run on the EDT.
     */
    private static JIPipeDesktopDataViewerWindow findFocusedViewer() {
        Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        if (activeWindow == null) {
            return null;
        }
        for (Window current = activeWindow; current != null; current = current.getOwner()) {
            if (current instanceof JIPipeDesktopDataViewerWindow viewerWindow) {
                return viewerWindow;
            }
        }
        return null;
    }

    /**
     * Resolution outcome of the viewer window targeting. The viewer is null if the
     * resolution failed (then error is set).
     */
    private record ResolvedViewer(JIPipeDesktopDataViewerWindow viewer, String error) {
    }

    /**
     * Resolves the viewer window an operation acts on. Must run on the EDT.
     * <ul>
     * <li>An explicit "viewer" parameter (a viewer_id as returned by get_viewer_state or
     * open_data_viewer) wins: the window with that id, scoped to the project window of the
     * optional "project" parameter when given.</li>
     * <li>Otherwise, like get_ui_state: the focused viewer window (restricted to the
     * context-bound or explicitly named project window's workbench), then the viewer
     * windows of that project window, then the first open viewer window.</li>
     * </ul>
     */
    private static ResolvedViewer resolveTargetViewer(AutomationContext ctx, JsonNode params) {
        boolean explicitProject = params.has("project") && !params.get("project").asText().isBlank();
        String projectKey = explicitProject ? params.get("project").asText().trim() : ctx.getProjectId();
        JIPipeDesktopProjectWindow projectWindow = projectKey != null
                ? AutomationOperationUtils.findProjectWindowByKey(projectKey) : null;
        if (explicitProject && projectWindow == null) {
            return new ResolvedViewer(null, "No open project window matches '" + params.get("project").asText().trim() + "'");
        }
        List<JIPipeDesktopDataViewerWindow> viewers = openViewers();
        if (params.has("viewer") && !params.get("viewer").asText().isBlank()) {
            String id = params.get("viewer").asText().trim();
            boolean idExists = false;
            for (JIPipeDesktopDataViewerWindow viewer : viewers) {
                if (!viewer.getId().equals(id)) {
                    continue;
                }
                idExists = true;
                if (projectWindow == null || viewer.getDesktopWorkbench() == projectWindow.getProjectWorkbench()) {
                    return new ResolvedViewer(viewer, null);
                }
            }
            if (idExists) {
                return new ResolvedViewer(null, "The data viewer window '" + id
                        + "' does not belong to project window '" + projectKey + "'");
            }
            return new ResolvedViewer(null, "No open data viewer window with id '" + id + "'");
        }
        JIPipeDesktopDataViewerWindow focused = findFocusedViewer();
        if (focused != null && (projectWindow == null
                || focused.getDesktopWorkbench() == projectWindow.getProjectWorkbench())) {
            return new ResolvedViewer(focused, null);
        }
        if (projectWindow != null) {
            for (JIPipeDesktopDataViewerWindow viewer : viewers) {
                if (viewer.getDesktopWorkbench() == projectWindow.getProjectWorkbench()) {
                    return new ResolvedViewer(viewer, null);
                }
            }
            return new ResolvedViewer(null, "No open data viewer window belongs to project window '" + projectKey + "'");
        }
        if (!viewers.isEmpty()) {
            return new ResolvedViewer(viewers.get(0), null);
        }
        return new ResolvedViewer(null, "No data viewer window is open");
    }

    /**
     * The project window selected by an explicit "project" parameter, or null (no
     * explicit scoping). Must run on the EDT.
     */
    private static JIPipeDesktopProjectWindow explicitProjectWindow(JsonNode params) {
        if (params.has("project") && !params.get("project").asText().isBlank()) {
            return AutomationOperationUtils.findProjectWindowByKey(params.get("project").asText().trim());
        }
        return null;
    }

    /**
     * Adds the state of one viewer window: id, display name, data type of the current
     * record, the current row (1-based) and row count when a data table is shown, the
     * current data annotation column, and the agent-control flag.
     */
    private static void putViewerState(ObjectNode node, JIPipeDesktopDataViewerWindow viewer) {
        node.put("viewer_id", viewer.getId());
        node.put("display_name", viewer.getDisplayName());
        if (viewer.getDataBrowser() != null) {
            node.put("data_type", JIPipe.getDataTypes().getIdOf(viewer.getDataBrowser().getDataClass()));
        } else {
            node.putNull("data_type");
        }
        int rowCount = viewer.getDataTableRowCount();
        if (rowCount > 0) {
            node.put("row", viewer.getCurrentDataRow() + 1);
            node.put("total_rows", rowCount);
            String annotationColumn = viewer.getCurrentDataAnnotationColumn();
            if (annotationColumn != null) {
                node.put("annotation_column", annotationColumn);
            }
        } else {
            node.putNull("row");
            node.put("total_rows", 0);
        }
        node.put("agent_controlled", viewer.isAgentControlled());
    }

    /**
     * Reports what the open data viewer windows show. Pure read: needs no agent-control
     * flag (the flag is part of the reported state).
     */
    public static class GetViewerState implements AutomationOperation {

        @Override
        public String getId() {
            return "get_viewer_state";
        }

        @Override
        public String getDescription() {
            return "Report what the open data viewer windows show: for each viewer its id, display "
                    + "name, data type, the current row and row count (when a data table is shown), "
                    + "the current data annotation column, and whether agent control is enabled. "
                    + "Read-only";
        }

        @Override
        public String getAgentDocumentation() {
            return getDescription() + ". Pass viewer to report a single window. The viewer_id of "
                    + "each window is what goto_viewer_row, close_viewer, and open_data_viewer "
                    + "(result) use; agent_controlled tells whether that window can be navigated "
                    + "or closed by an agent (the user enables it in the data viewer ribbon)";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "properties": {
                        "project": {"type": "string", "description": "Session id of the project window whose viewer windows are reported (as returned by list_projects). Default: all open viewer windows"},
                        "viewer": {"type": "string", "description": "Id of one viewer window (viewer_id as returned by this operation or open_data_viewer). Default: report all matching viewer windows"}
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
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            if (params.has("viewer") && !params.get("viewer").asText().isBlank()) {
                ResolvedViewer resolved = resolveTargetViewer(ctx, params);
                if (resolved.error() != null) {
                    return error(resolved.error());
                }
                ObjectNode data = mapper.createObjectNode();
                putViewerState(data, resolved.viewer());
                return data;
            }
            JIPipeDesktopProjectWindow projectWindow = explicitProjectWindow(params);
            List<JIPipeDesktopDataViewerWindow> viewers = openViewers();
            ObjectNode data = mapper.createObjectNode();
            ArrayNode array = data.putArray("viewers");
            int count = 0;
            for (JIPipeDesktopDataViewerWindow viewer : viewers) {
                if (projectWindow != null && viewer.getDesktopWorkbench() != projectWindow.getProjectWorkbench()) {
                    continue;
                }
                putViewerState(array.addObject(), viewer);
                ++count;
            }
            data.put("count", count);
            return data;
        }
    }

    /**
     * Navigates a data viewer window to a data table row. Changes view state only (like
     * navigate_to_node), so it is classified read-only, but it requires the window's
     * agent-control flag.
     */
    public static class GotoViewerRow implements AutomationOperation {

        @Override
        public String getId() {
            return "goto_viewer_row";
        }

        @Override
        public String getDescription() {
            return "Navigate a data viewer window to a data table row (1-based). Changes view "
                    + "state only, never project data. Requires agent control to be enabled on "
                    + "the viewer window; the user toggles it in the data viewer ribbon "
                    + "('Agent control' in the Data band)";
        }

        @Override
        public String getAgentDocumentation() {
            return getDescription() + ". An out-of-range row returns an error naming the valid "
                    + "range 1..total_rows; a window that does not display a data table cannot "
                    + "be navigated. Use get_viewer_state to learn the viewer_id and the current "
                    + "row";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["row"],
                      "properties": {
                        "row": {"type": "integer", "description": "Row to show, 1-based (row 1 is the first data row)"},
                        "viewer": {"type": "string", "description": "Id of the viewer window to navigate (viewer_id from get_viewer_state or open_data_viewer). Default: the focused viewer window, then the context-bound project window's viewers, then the first open one"},
                        "project": {"type": "string", "description": "Session id of the project window whose viewer windows are addressed (as returned by list_projects). Default: no project scoping"}
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
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            ResolvedViewer resolved = resolveTargetViewer(ctx, params);
            if (resolved.error() != null) {
                return error(resolved.error());
            }
            JIPipeDesktopDataViewerWindow viewer = resolved.viewer();
            if (!viewer.isAgentControlled()) {
                return agentControlDisabledError();
            }
            int row = params.get("row").asInt();
            int total = viewer.getDataTableRowCount();
            if (total == 0) {
                return error("The data viewer window '" + viewer.getId() + "' does not display a data table");
            }
            if (row < 1 || row > total) {
                return error("Row " + row + " out of range: the data viewer window '" + viewer.getId()
                        + "' shows " + total + " " + (total == 1 ? "row" : "rows") + " (valid range 1.." + total + ")");
            }
            viewer.navigateToRow(row);
            ObjectNode data = mapper.createObjectNode();
            data.put("ok", true);
            data.put("viewer_id", viewer.getId());
            data.put("row", row);
            data.put("total_rows", total);
            return data;
        }
    }

    /**
     * Closes a data viewer window (disposes it). A real modification: requires the
     * window's agent-control flag.
     */
    public static class CloseViewer implements AutomationOperation {

        @Override
        public String getId() {
            return "close_viewer";
        }

        @Override
        public String getDescription() {
            return "Close a data viewer window (disposes it). Requires agent control to be "
                    + "enabled on the viewer window; the user toggles it in the data viewer "
                    + "ribbon ('Agent control' in the Data band)";
        }

        @Override
        public String getAgentDocumentation() {
            return getDescription() + ". Use get_viewer_state to learn the viewer_id; the "
                    + "default target is the focused viewer window, then the context-bound "
                    + "project window's viewers, then the first open one";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "properties": {
                        "viewer": {"type": "string", "description": "Id of the viewer window to close (viewer_id from get_viewer_state or open_data_viewer). Default: the focused viewer window, then the context-bound project window's viewers, then the first open one"},
                        "project": {"type": "string", "description": "Session id of the project window whose viewer windows are addressed (as returned by list_projects). Default: no project scoping"}
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
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            ResolvedViewer resolved = resolveTargetViewer(ctx, params);
            if (resolved.error() != null) {
                return error(resolved.error());
            }
            JIPipeDesktopDataViewerWindow viewer = resolved.viewer();
            if (!viewer.isAgentControlled()) {
                return agentControlDisabledError();
            }
            String id = viewer.getId();
            viewer.dispose();
            ObjectNode data = mapper.createObjectNode();
            data.put("ok", true);
            data.put("viewer_id", id);
            data.put("closed", true);
            return data;
        }
    }
}
