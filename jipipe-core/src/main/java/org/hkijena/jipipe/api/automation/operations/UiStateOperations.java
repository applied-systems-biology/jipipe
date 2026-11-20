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
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationSchemaSpec;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.JIPipeDesktopPipelineGraphEditorUI;
import org.hkijena.jipipe.desktop.app.settings.JIPipeDesktopProjectOverviewUI;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Built-in UI state operations: read what a project window currently shows (focus
 * awareness), navigate it to a node like the find panel, fit the graph view to all
 * nodes, and clear the editor selection. All are classified read-only: they change at
 * most the view state (selected tab, selection, scroll position, zoom), never project
 * data. All must run on the EDT.
 */
public class UiStateOperations {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static ObjectNode error(String message) {
        return mapper.createObjectNode().put("error", message);
    }

    /**
     * Resolves the project window a UI-state operation acts on: an explicit "project"
     * parameter (a session id as returned by list_projects) wins, otherwise the window the
     * dispatch context is bound to, then the focused project window, then the first open one.
     * Must run on the EDT.
     */
    private static JIPipeDesktopProjectWindow resolveTargetWindow(AutomationContext ctx, JsonNode params) {
        if (params.has("project") && !params.get("project").asText().isBlank()) {
            return AutomationOperationUtils.findProjectWindowByKey(params.get("project").asText().trim());
        }
        return AutomationOperationUtils.resolveDefaultProjectWindow(ctx);
    }

    /**
     * Copy of the find panel's private node-UI lookup: the node UI of a node UUID on an
     * editor canvas, or null. Kept local so the find panel stays untouched.
     */
    private static JIPipeDesktopGraphNodeUI findNodeUI(JIPipeDesktopGraphEditorUI editorUI, UUID nodeId) {
        for (Map.Entry<JIPipeGraphNode, JIPipeDesktopGraphNodeUI> nodeUI : editorUI.getCanvasUI().getNodeUIs().entrySet()) {
            if (nodeId.equals(nodeUI.getKey().getUUIDInParentGraph())) {
                return nodeUI.getValue();
            }
        }
        return null;
    }

    /**
     * Reports the current UI state of a project window.
     */
    public static class GetUiState implements AutomationOperation {

        @Override
        public String getId() {
            return "get_ui_state";
        }

        @Override
        public String getDescription() {
            return "Report the current UI state of a project window: the active tab (type, title, and "
                    + "for a compartment editor the compartment) and, when a compartment editor is "
                    + "shown, the canvas state (graph editor cursor, last mouse position, zoom, and "
                    + "the selected node ids). Read-only";
        }

        @Override
        public String getAgentDocumentation() {
            return getDescription() + ". Use this to become aware of what the user currently looks at "
                    + "before navigating (navigate_to_node) or modifying the pipeline";
        }

        private static final AutomationSchemaSpec SPEC = AutomationSchemaSpec.object()
                .string("project", "Session id of the project window to report (as returned by list_projects). Default: the window this call was routed to, else the focused project window, else the first open one")
                .build();

        @Override
        public AutomationSchemaSpec getSchemaSpec() {
            return SPEC;
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
            JIPipeDesktopProjectWindow window = resolveTargetWindow(ctx, params);
            if (window == null) {
                return error("No matching project window is open");
            }
            JIPipeProject project = window.getProject();
            JIPipeDesktopProjectWorkbench workbench = window.getProjectWorkbench();

            ObjectNode data = mapper.createObjectNode();
            ObjectNode windowNode = data.putObject("window");
            windowNode.put("session_id", window.getSessionId().toString());
            String projectName = project.getMetadata().getName();
            if (projectName == null || projectName.isBlank()) {
                projectName = project.getProjectFile() != null
                        ? project.getProjectFile().getFileName().toString() : "Untitled";
            }
            windowNode.put("project_name", projectName);
            windowNode.put("project_path", project.getProjectFile() != null ? project.getProjectFile().toString() : null);
            putActiveTab(ctx, data, workbench, project);
            return data;
        }

        /**
         * Adds the active-tab state: the tab type (COMPARTMENT_EDITOR for a per-compartment
         * pipeline editor, PROJECT_OVERVIEW for the overview, other otherwise), the singleton
         * tab id when the selected tab is one, the title, and for a compartment editor the
         * compartment and the canvas state.
         */
        private void putActiveTab(AutomationContext ctx, ObjectNode data,
                                  JIPipeDesktopProjectWorkbench workbench, JIPipeProject project) {
            ObjectNode tabNode = data.putObject("active_tab");
            JIPipeDesktopTabPane tabPane = workbench.getDocumentTabPane();
            JIPipeDesktopTabPane.DocumentTab tab = tabPane.getSelectedTab();
            if (tab == null) {
                tabNode.put("type", "none");
                return;
            }
            Component content = tab.getContent();
            String type;
            UUID compartmentUUID = null;
            if (content instanceof JIPipeDesktopPipelineGraphEditorUI) {
                type = "COMPARTMENT_EDITOR";
                compartmentUUID = ((JIPipeDesktopPipelineGraphEditorUI) content).getCompartment();
            } else if (content instanceof JIPipeDesktopProjectOverviewUI) {
                type = "PROJECT_OVERVIEW";
            } else {
                type = "other";
            }
            tabNode.put("id", tabPane.getCurrentlySelectedSingletonTabId());
            tabNode.put("type", type);
            tabNode.put("title", tab.getTitle());
            if (compartmentUUID != null) {
                JIPipeProjectCompartment compartment = project.getCompartments().get(compartmentUUID);
                tabNode.put("compartment_id", compartmentUUID.toString());
                tabNode.put("compartment_name", compartment != null ? compartment.getName() : null);
            }
            if (content instanceof JIPipeDesktopPipelineGraphEditorUI editorUI) {
                putCanvasState(ctx, data, editorUI.getCanvasUI());
            }
        }

        /**
         * Adds the canvas state of the shown compartment editor: the graph editor cursor,
         * the last mouse position (null when the mouse never entered the canvas), the zoom,
         * and the selected node ids (as used by query_graph).
         */
        private void putCanvasState(AutomationContext ctx, ObjectNode data, JIPipeDesktopGraphCanvasUI canvas) {
            ObjectNode canvasNode = data.putObject("canvas");
            Point cursor = canvas.getGraphEditorCursor();
            canvasNode.putObject("cursor").put("x", cursor.x).put("y", cursor.y);
            Point mouse = canvas.getLastMousePosition();
            if (mouse != null) {
                canvasNode.putObject("mouse").put("x", mouse.x).put("y", mouse.y);
            } else {
                canvasNode.putNull("mouse");
            }
            canvasNode.put("zoom", canvas.getZoom());
            ArrayNode selectedNodes = canvasNode.putArray("selected_nodes");
            JIPipeGraph graph = ctx.getGraph();
            for (JIPipeGraphNode node : canvas.getSelectionManager().getSelectedNodes()) {
                selectedNodes.add(graph != null ? graph.getAliasIdOf(node) : node.getUUIDInParentGraph().toString());
            }
        }
    }

    /**
     * Navigates a project window to a node the way the find panel does: the node's
     * compartment editor tab is opened or focused, the node is selected, and the canvas is
     * scrolled to it. No find-panel state is used, so the navigation is fully reproducible
     * from an operation.
     */
    public static class NavigateToNode implements AutomationOperation {

        @Override
        public String getId() {
            return "navigate_to_node";
        }

        @Override
        public String getDescription() {
            return "Navigate a project window to a node like the find panel: open or focus the node's "
                    + "compartment editor tab, select the node, and scroll it into view. Changes view "
                    + "state only, never project data";
        }

        @Override
        public String getAgentDocumentation() {
            return getDescription() + ". The node id is the one used by query_graph. The selection and "
                    + "scroll are applied after the target tab is visible (deferred like in the find "
                    + "panel), so the response reports the target compartment and whether a tab had to "
                    + "be opened rather than the final scroll state";
        }

        private static final AutomationSchemaSpec SPEC = AutomationSchemaSpec.object()
                .string("node", "The node id as used by query_graph (alias id or UUID)").required()
                .string("compartment", "Compartment that contains the node (UUID, alias, or name). Resolved from the graph when omitted")
                .string("project", "Session id of the project window to navigate (as returned by list_projects). Default: the window this call was routed to")
                .build();

        @Override
        public AutomationSchemaSpec getSchemaSpec() {
            return SPEC;
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
            JIPipeProject project = ctx.getProject();
            if (project == null) {
                return error("No project loaded");
            }
            JIPipeGraph graph = ctx.getGraph();
            String nodeRef = params.get("node").asText();
            JIPipeGraphNode node = graph.findNode(nodeRef);
            if (node == null) {
                return error("Node not found: " + nodeRef);
            }
            JIPipeProjectCompartment compartment;
            if (params.has("compartment") && !params.get("compartment").asText().isBlank()) {
                String compartmentRef = params.get("compartment").asText();
                compartment = AutomationOperationUtils.findCompartment(project, compartmentRef);
                if (compartment == null) {
                    return error("Compartment not found: " + compartmentRef);
                }
                if (!Objects.equals(compartment.getProjectCompartmentUUID(), graph.getCompartmentUUIDOf(node))) {
                    return error("Node '" + nodeRef + "' is not in compartment '" + compartmentRef + "'");
                }
            } else {
                UUID compartmentUUID = graph.getCompartmentUUIDOf(node);
                compartment = compartmentUUID != null ? project.getCompartments().get(compartmentUUID) : null;
                if (compartment == null) {
                    return error("Node '" + nodeRef + "' has no compartment");
                }
            }
            JIPipeDesktopProjectWindow window = resolveTargetWindow(ctx, params);
            if (window == null) {
                return error("No matching project window is open");
            }
            if (window.getProject() != project) {
                return error("The project window '" + window.getSessionId()
                        + "' does not hold the project of this call");
            }
            JIPipeDesktopProjectWorkbench workbench = window.getProjectWorkbench();
            UUID nodeUUID = node.getUUIDInParentGraph();

            // The node is visible on the currently shown canvas: select + scroll there
            // (the find panel's same-canvas case)
            JIPipeDesktopTabPane.DocumentTab selectedTab = workbench.getDocumentTabPane().getSelectedTab();
            if (selectedTab != null && selectedTab.getContent() instanceof JIPipeDesktopPipelineGraphEditorUI editorUI) {
                JIPipeDesktopGraphNodeUI nodeUI = findNodeUI(editorUI, nodeUUID);
                if (nodeUI != null) {
                    editorUI.getSelectionManager().setSelection(Collections.singleton(nodeUI));
                    editorUI.scrollToAlgorithm(nodeUI);
                    return result(graph.getAliasIdOf(node), compartment, false);
                }
            }

            // The node lives in another compartment: open (or focus) its editor tab, then
            // select + scroll there (deferred, exactly like the find panel)
            boolean tabOpened = workbench.findOpenPipelineEditorTabs(compartment).isEmpty();
            JIPipeDesktopTabPane.DocumentTab tab = workbench.getOrOpenPipelineEditorTab(compartment, true);
            SwingUtilities.invokeLater(() -> {
                if (tab.getContent() instanceof JIPipeDesktopPipelineGraphEditorUI editorUI) {
                    JIPipeDesktopGraphNodeUI nodeUI = findNodeUI(editorUI, nodeUUID);
                    if (nodeUI != null) {
                        editorUI.getSelectionManager().setSelection(Collections.singleton(nodeUI));
                        editorUI.scrollToAlgorithm(nodeUI);
                        return;
                    }
                }
                workbench.sendStatusBarText("Unable to navigate to node");
            });
            return result(graph.getAliasIdOf(node), compartment, tabOpened);
        }

        /**
         * The navigation outcome: ok plus the resolved target and whether the compartment
         * editor tab had to be opened.
         */
        private static ObjectNode result(String nodeId, JIPipeProjectCompartment compartment, boolean tabOpened) {
            ObjectNode data = mapper.createObjectNode();
            data.put("ok", true);
            data.put("node", nodeId);
            data.put("compartment", compartment.getProjectCompartmentUUID().toString());
            data.put("tab_opened", tabOpened);
            return data;
        }
    }

    /**
     * Fits the graph editor view to all nodes: the active or a given compartment's editor
     * zooms so every node is visible and the content is centered. Like navigate_to_node,
     * the view change is applied after the target tab is visible when a tab must be opened.
     */
    public static class FitToNodes implements AutomationOperation {

        /**
         * The zoom limits of the graph canvas (the same bounds zoomOut/zoomIn enforce).
         */
        private static final double MIN_ZOOM = 0.1;
        private static final double MAX_ZOOM = 3.0;

        @Override
        public String getId() {
            return "fit_to_nodes";
        }

        @Override
        public String getDescription() {
            return "Fit the graph editor view to all nodes: zoom the active or given compartment's "
                    + "editor so every node is visible, and center the content. Changes view "
                    + "state only, never project data";
        }

        @Override
        public String getAgentDocumentation() {
            return getDescription() + ". The zoom and scroll are applied after the target tab is "
                    + "visible (deferred like in the find panel), so the response reports the "
                    + "target compartment and whether a tab had to be opened rather than the "
                    + "final view state";
        }

        private static final AutomationSchemaSpec SPEC = AutomationSchemaSpec.object()
                .string("compartment", "Compartment whose editor tab is fitted (UUID, alias, or name). Omit to fit the active graph editor tab")
                .string("project", "Session id of the project window to navigate (as returned by list_projects). Default: the window this call was routed to")
                .build();

        @Override
        public AutomationSchemaSpec getSchemaSpec() {
            return SPEC;
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
            JIPipeProject project = ctx.getProject();
            if (project == null) {
                return error("No project loaded");
            }

            // The compartment is resolved first (like navigate_to_node), so an unknown
            // compartment is reported even when no project window is open
            JIPipeProjectCompartment compartment = null;
            if (params.has("compartment") && !params.get("compartment").asText().isBlank()) {
                String compartmentRef = params.get("compartment").asText();
                compartment = AutomationOperationUtils.findCompartment(project, compartmentRef);
                if (compartment == null) {
                    return error("Compartment not found: " + compartmentRef);
                }
            }

            JIPipeDesktopProjectWindow window = resolveTargetWindow(ctx, params);
            if (window == null) {
                return error("No matching project window is open");
            }
            if (window.getProject() != project) {
                return error("The project window '" + window.getSessionId()
                        + "' does not hold the project of this call");
            }
            JIPipeDesktopProjectWorkbench workbench = window.getProjectWorkbench();

            // Fit the given compartment's editor tab: open (or focus) it, then apply the
            // fit (deferred, exactly like the find panel's navigation)
            if (compartment != null) {
                boolean tabOpened = workbench.findOpenPipelineEditorTabs(compartment).isEmpty();
                JIPipeDesktopTabPane.DocumentTab tab = workbench.getOrOpenPipelineEditorTab(compartment, true);
                SwingUtilities.invokeLater(() -> {
                    if (tab.getContent() instanceof JIPipeDesktopGraphEditorUI editorUI) {
                        applyFit(editorUI);
                    }
                });
                return result(compartment.getProjectCompartmentUUID().toString(), tabOpened);
            }

            // Fit the editor shown in the active tab (a pipeline or compartments editor)
            JIPipeDesktopTabPane.DocumentTab selectedTab = workbench.getDocumentTabPane().getSelectedTab();
            if (selectedTab == null || !(selectedTab.getContent() instanceof JIPipeDesktopGraphEditorUI editorUI)) {
                return error("no graph editor tab is active; pass compartment to open one");
            }
            AppliedFit applied = applyFit(editorUI);
            UUID compartmentUUID = editorUI.getCompartment();
            ObjectNode data = mapper.createObjectNode();
            data.put("ok", true);
            data.put("compartment", compartmentUUID != null ? compartmentUUID.toString() : null);
            data.put("zoom", applied.zoom());
            data.put("nodes", applied.nodes());
            return data;
        }

        /**
         * Applies the fit to an editor: unions the node bounds measured at the current zoom,
         * computes the fitting zoom and scroll with {@link ViewFit#compute}, sets the zoom,
         * and defers the scrollbar positions (the zoom's re-layout must land before the
         * scroll clamps). An editor without nodes or without a scroll pane is a no-op.
         */
        private static AppliedFit applyFit(JIPipeDesktopGraphEditorUI editorUI) {
            JIPipeDesktopGraphCanvasUI canvas = editorUI.getCanvasUI();
            Rectangle2D bounds = null;
            for (JIPipeDesktopGraphNodeUI nodeUI : canvas.getNodeUIs().values()) {
                Rectangle nodeBounds = nodeUI.getBounds();
                if (bounds == null) {
                    bounds = new Rectangle2D.Double(nodeBounds.getX(), nodeBounds.getY(),
                            nodeBounds.getWidth(), nodeBounds.getHeight());
                } else {
                    bounds.add(nodeBounds);
                }
            }
            JScrollPane scrollPane = canvas.getScrollPane();
            if (bounds == null || scrollPane == null) {
                return new AppliedFit(canvas.getZoom(), 0);
            }
            ViewFit.FitResult fit = ViewFit.compute(bounds, canvas.getZoom(),
                    scrollPane.getViewport().getExtentSize().width,
                    scrollPane.getViewport().getExtentSize().height, MIN_ZOOM, MAX_ZOOM);
            canvas.setZoom(fit.zoom());
            SwingUtilities.invokeLater(() -> {
                scrollPane.getHorizontalScrollBar().setValue(fit.scrollX());
                scrollPane.getVerticalScrollBar().setValue(fit.scrollY());
            });
            return new AppliedFit(fit.zoom(), canvas.getNodeUIs().size());
        }

        /**
         * The outcome of applying a fit: the zoom that is now set (the unchanged current
         * zoom for a no-op) and the number of nodes the fit covered (0 for a no-op).
         */
        private record AppliedFit(double zoom, int nodes) {
        }

        /**
         * The fit outcome for a deferred application: ok plus the resolved target and
         * whether the compartment editor tab had to be opened. The zoom and node count
         * are unknown here (the fit runs deferred), so they are not reported.
         */
        private static ObjectNode result(String compartmentUUID, boolean tabOpened) {
            ObjectNode data = mapper.createObjectNode();
            data.put("ok", true);
            data.put("compartment", compartmentUUID);
            data.put("tab_opened", tabOpened);
            return data;
        }
    }

    /**
     * Clears the selection of the active or a given compartment's graph editor. Like
     * navigate_to_node, the view change is applied after the target tab is visible when
     * a tab must be opened.
     */
    public static class ClearSelection implements AutomationOperation {

        @Override
        public String getId() {
            return "clear_selection";
        }

        @Override
        public String getDescription() {
            return "Clear the node selection of the active or given compartment's graph editor. "
                    + "Changes view state only, never project data";
        }

        @Override
        public String getAgentDocumentation() {
            return getDescription() + ". The clear is applied after the target tab is visible "
                    + "(deferred like in the find panel), so the response reports the target "
                    + "compartment and whether a tab had to be opened rather than the number "
                    + "of cleared items";
        }

        private static final AutomationSchemaSpec SPEC = AutomationSchemaSpec.object()
                .string("compartment", "Compartment whose editor tab is cleared (UUID, alias, or name). Omit to clear the active graph editor tab")
                .string("project", "Session id of the project window to navigate (as returned by list_projects). Default: the window this call was routed to")
                .build();

        @Override
        public AutomationSchemaSpec getSchemaSpec() {
            return SPEC;
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
            JIPipeProject project = ctx.getProject();
            if (project == null) {
                return error("No project loaded");
            }

            // The compartment is resolved first (like navigate_to_node), so an unknown
            // compartment is reported even when no project window is open
            JIPipeProjectCompartment compartment = null;
            if (params.has("compartment") && !params.get("compartment").asText().isBlank()) {
                String compartmentRef = params.get("compartment").asText();
                compartment = AutomationOperationUtils.findCompartment(project, compartmentRef);
                if (compartment == null) {
                    return error("Compartment not found: " + compartmentRef);
                }
            }

            JIPipeDesktopProjectWindow window = resolveTargetWindow(ctx, params);
            if (window == null) {
                return error("No matching project window is open");
            }
            if (window.getProject() != project) {
                return error("The project window '" + window.getSessionId()
                        + "' does not hold the project of this call");
            }
            JIPipeDesktopProjectWorkbench workbench = window.getProjectWorkbench();

            // Clear the selection in the given compartment's editor tab: open (or focus)
            // it, then clear (deferred, exactly like the find panel's navigation)
            if (compartment != null) {
                boolean tabOpened = workbench.findOpenPipelineEditorTabs(compartment).isEmpty();
                JIPipeDesktopTabPane.DocumentTab tab = workbench.getOrOpenPipelineEditorTab(compartment, true);
                SwingUtilities.invokeLater(() -> {
                    if (tab.getContent() instanceof JIPipeDesktopGraphEditorUI editorUI) {
                        editorUI.getSelectionManager().clearSelection();
                    }
                });
                return result(compartment.getProjectCompartmentUUID().toString(), tabOpened);
            }

            // Clear the selection of the editor shown in the active tab (a pipeline or
            // compartments editor)
            JIPipeDesktopTabPane.DocumentTab selectedTab = workbench.getDocumentTabPane().getSelectedTab();
            if (selectedTab == null || !(selectedTab.getContent() instanceof JIPipeDesktopGraphEditorUI editorUI)) {
                return error("no graph editor tab is active; pass compartment to open one");
            }
            int cleared = editorUI.getSelectionManager().getSelection().size();
            editorUI.getSelectionManager().clearSelection();
            UUID compartmentUUID = editorUI.getCompartment();
            ObjectNode data = mapper.createObjectNode();
            data.put("ok", true);
            data.put("compartment", compartmentUUID != null ? compartmentUUID.toString() : null);
            data.put("cleared", cleared);
            return data;
        }

        /**
         * The clear outcome for a deferred application: ok plus the resolved target and
         * whether the compartment editor tab had to be opened. The number of cleared items
         * is unknown here (the clear runs deferred), so it is not reported.
         */
        private static ObjectNode result(String compartmentUUID, boolean tabOpened) {
            ObjectNode data = mapper.createObjectNode();
            data.put("ok", true);
            data.put("compartment", compartmentUUID);
            data.put("tab_opened", tabOpened);
            return data;
        }
    }
}
