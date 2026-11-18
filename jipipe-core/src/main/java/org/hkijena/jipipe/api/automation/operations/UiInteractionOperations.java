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
import org.hkijena.jipipe.api.automation.AutomationSchemaUtils;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutAction;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutRegistry;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUIActiveArea;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.triggers.JIPipeDesktopGraphNodeUIRunNodeActiveArea;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.triggers.JIPipeDesktopGraphNodeUISlotActiveArea;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.triggers.JIPipeDesktopGraphNodeUISlotButtonActiveArea;
import org.hkijena.jipipe.desktop.app.shortcuts.JIPipeDesktopShortcutDispatcher;
import org.hkijena.jipipe.desktop.app.settings.JIPipeDesktopProjectOverviewUI;
import org.hkijena.jipipe.desktop.app.ui.input.AgentElementProbe;
import org.hkijena.jipipe.desktop.app.ui.input.AgentInputDispatch;
import org.hkijena.jipipe.desktop.app.ui.input.AgentInputSystem;
import org.hkijena.jipipe.desktop.app.ui.input.AgentWindowResolver;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Structured UI operations and synthetic input for agents (work item #1379, layers 1 and 2).
 * <p>
 * Layer 1 (structured, no synthetic events): {@code fire_action}, {@code list_ui_actions},
 * {@code set_dock_visible}, {@code list_docks}. These act through the resolved target
 * editor's own scope (its dispatcher scope for actions, its dock panel for docks), never
 * against the user's current keyboard focus.
 * <p>
 * Layer 2 (the coordinate escape hatch for custom-painted surfaces): {@code ui_probe},
 * {@code ui_click}, {@code ui_drag}, {@code ui_scroll}, {@code ui_type_text}. Points are
 * accepted only inside showing {@link Window}s of the JIPipe JVM; every response echoes
 * the resolved element (window title, component chain, the probe of the target point),
 * and delivery is explicitly not proof of effect: a successful call confirms the event
 * reached the component, so the agent re-reads state to confirm the intended change
 * (the probe-act-confirm loop of the design spec, section 4.5).
 * <p>
 * The window resolution for the Layer 2 point (see {@link #resolveWindowAt}):
 * {@code Window.getWindows()} has no documented z-order, so the FOCUSED window
 * ({@code KeyboardFocusManager.getFocusedWindow()}) is preferred when it contains the
 * point, and the remaining windows are iterated in creation order with the topmost
 * treated as LAST (the {@code AgentWindowResolver.resolve} contract: the caller owns
 * the ordering).
 * <p>
 * All operations run on the EDT (the MCP integration dispatches them there via
 * {@code requiresEdt()}); the system input mode hops off the EDT internally on its
 * bounded executor.
 */
public class UiInteractionOperations {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Shared description of the optional "project" parameter (kept identical across the
     * nine operations: the MCP layer routes calls by project key, which is the project
     * window session id).
     */
    private static final String PROJECT_PARAM_DESCRIPTION =
            "Session id of the project window (as returned by list_projects). Default: the "
                    + "window this call was routed to, else the focused project window, else the first open one";

    /**
     * Shared description of the input mode parameter of the input operations.
     */
    private static final String INPUT_MODE_DESCRIPTION =
            "How the input is delivered: dispatch (default) sends synthetic AWT events through "
                    + "the component, deterministic and display-independent; system uses a physical "
                    + "Robot (java.awt.Robot) for surfaces that read the OS input state, requires a "
                    + "display, and runs off the event dispatch thread with a bounded wait";

    /**
     * Shared probe-act-confirm guidance clause for the input operations: a successful
     * delivery only confirms the event reached the component, not that it did what the
     * agent expected.
     */
    private static final String CONFIRM_CLAUSE =
            " Delivery is not effect: ok only confirms the event reached the component - "
                    + "re-read state (ui_probe, get_ui_state, a screenshot, or get_viewer_state) to "
                    + "confirm the intended change, and re-probe and retry instead of assuming "
                    + "success. Points are accepted only inside showing JIPipe windows; a point "
                    + "outside them is refused with the allowed rectangles.";

    private static ObjectNode error(String message) {
        return mapper.createObjectNode().put("error", message);
    }

    /**
     * The schema property node of the shared "project" parameter (built here, not inside a
     * text block: the shared description constant is reused verbatim by every operation that
     * scopes by project window).
     */
    private static ObjectNode projectProperty(String extra) {
        ObjectNode property = mapper.createObjectNode().put("type", "string");
        String description = PROJECT_PARAM_DESCRIPTION;
        if (extra != null && !extra.isBlank()) {
            description = description + extra;
        }
        return property.put("description", description);
    }

    /**
     * Adds a property node to the schema's "properties" object (created when the schema has
     * none). {@code ObjectNode.putObject} would REPLACE an existing properties object with a
     * fresh empty one, silently dropping every other parameter, so the existing object is
     * mutated in place.
     */
    private static void putProperty(ObjectNode schema, String name, ObjectNode property) {
        if (schema.has("properties") && schema.get("properties").isObject()) {
            ((ObjectNode) schema.get("properties")).set(name, property);
        } else {
            schema.putObject("properties").set(name, property);
        }
    }

    /**
     * A schema text block that carries the shared "project" property (the description
     * cannot be a constant interpolation inside a text block, so the property is added
     * after parsing).
     */
    private static JsonNode schemaWithProject(String schema, String projectExtra) {
        ObjectNode object = (ObjectNode) AutomationSchemaUtils.parseJson(schema);
        putProperty(object, "project", projectProperty(projectExtra));
        return object;
    }

    /**
     * A schema text block for the input operations: carries the shared "input-mode" and
     * "project" properties.
     */
    private static JsonNode schemaWithInputModeAndProject(String schema, String projectExtra) {
        ObjectNode object = (ObjectNode) AutomationSchemaUtils.parseJson(schema);
        putProperty(object, "input-mode", inputModeProperty());
        putProperty(object, "project", projectProperty(projectExtra));
        return object;
    }

    /**
     * The schema property node of the shared "input-mode" parameter.
     */
    private static ObjectNode inputModeProperty() {
        ObjectNode property = mapper.createObjectNode();
        property.put("type", "string");
        property.putArray("enum").add("dispatch").add("system");
        property.put("default", "dispatch");
        property.put("description", INPUT_MODE_DESCRIPTION);
        return property;
    }

    /**
     * Resolves the project window an operation acts on (the {@code get_ui_state} chain):
     * an explicit "project" parameter wins, otherwise the window the dispatch context is
     * bound to, then the focused project window, then the first open one. Must run on the
     * EDT.
     */
    private static JIPipeDesktopProjectWindow resolveTargetWindow(AutomationContext ctx, JsonNode params) {
        if (params.has("project") && !params.get("project").asText().isBlank()) {
            return AutomationOperationUtils.findProjectWindowByKey(params.get("project").asText().trim());
        }
        return AutomationOperationUtils.resolveDefaultProjectWindow(ctx);
    }

    /**
     * The content of the project window's currently selected tab, or null.
     */
    private static Component selectedTabContent(JIPipeDesktopProjectWindow window) {
        JIPipeDesktopProjectWorkbench workbench = window.getProjectWorkbench();
        JIPipeDesktopTabPane.DocumentTab tab = workbench.getDocumentTabPane().getSelectedTab();
        return tab != null ? tab.getContent() : null;
    }

    /**
     * The graph editor of the currently selected tab, or null when the tab is not a graph
     * editor (e.g. the project overview).
     */
    private static JIPipeDesktopGraphEditorUI selectedEditor(JIPipeDesktopProjectWindow window) {
        return selectedTabContent(window) instanceof JIPipeDesktopGraphEditorUI editor ? editor : null;
    }

    /**
     * The dock panel of the target editor: the selected tab's graph editor or project
     * overview, or null when neither is shown (both carry a {@code JIPipeDesktopDockPanel}).
     */
    private static JIPipeDesktopDockPanel selectedDock(JIPipeDesktopProjectWindow window) {
        Component content = selectedTabContent(window);
        if (content instanceof JIPipeDesktopGraphEditorUI editor) {
            return editor.getDockPanel();
        }
        if (content instanceof JIPipeDesktopProjectOverviewUI overview) {
            return overview.getDockPanel();
        }
        return null;
    }

    private static int intParam(JsonNode params, String field, int defaultValue) {
        return params.has(field) ? params.get(field).asInt(defaultValue) : defaultValue;
    }

    private static boolean boolParam(JsonNode params, String field, boolean defaultValue) {
        return params.has(field) ? params.get(field).asBoolean(defaultValue) : defaultValue;
    }

    /**
     * The parsed input mode, or the error for an unknown value.
     */
    private static final class InputModeOrError {
        final AgentInputDispatch.AgentInputMode mode;
        final String error;

        private InputModeOrError(AgentInputDispatch.AgentInputMode mode, String error) {
            this.mode = mode;
            this.error = error;
        }
    }

    /**
     * Resolves the input-mode parameter (default "dispatch") and, when the mode is
     * "system", checks its display gate: a headless environment is refused with a clear
     * error (no bare AWTException escapes the operation envelope).
     */
    private static InputModeOrError resolveInputMode(JsonNode params) {
        String mode = params.has("input-mode") ? params.get("input-mode").asText() : "dispatch";
        if ("dispatch".equals(mode)) {
            return new InputModeOrError(AgentInputDispatch.MODE, null);
        }
        if ("system".equals(mode)) {
            if (!AgentInputSystem.MODE.isAvailable()) {
                return new InputModeOrError(null,
                        "input-mode 'system' requires a display (headless environment); "
                                + "use the default input-mode 'dispatch'");
            }
            return new InputModeOrError(AgentInputSystem.MODE, null);
        }
        return new InputModeOrError(null, "Unknown input-mode '" + mode + "'; expected 'dispatch' or 'system'");
    }

    /**
     * The mouse button id for the "button" parameter.
     */
    private static Integer buttonId(JsonNode params) {
        String button = params.has("button") ? params.get("button").asText() : "left";
        return switch (button) {
            case "left" -> MouseEvent.BUTTON1;
            case "middle" -> MouseEvent.BUTTON2;
            case "right" -> MouseEvent.BUTTON3;
            default -> null;
        };
    }

    /**
     * The outcome of resolving a screen point against the JIPipe windows (must run on the
     * EDT).
     *
     * @param window            the resolved showing window (null when the point is outside
     *                          all JIPipe windows)
     * @param hit               the component under the point (the window itself when the
     *                          point is in a region without any component, e.g. the window
     *                          background)
     * @param local             the point in the hit component's coordinate system
     * @param refusal           the refusal message (null on success)
     * @param allowedRectangles the on-screen bounds of all showing JIPipe windows (null on success)
     */
    private record WindowResolution(Window window, Component hit, Point local,
                                    String refusal, List<Rectangle> allowedRectangles) {
    }

    /**
     * Resolves a screen point to the JIPipe window that should receive input at that
     * point, and hit-tests the component under the point.
     * <p>
     * Ordering: {@code Window.getWindows()} has no documented z-order, so the FOCUSED
     * window ({@code KeyboardFocusManager.getFocusedWindow()}) is preferred when it
     * contains the point (the focused window is the one the user is looking at, and it is
     * effectively topmost for its own area); the remaining showing windows are then
     * iterated in creation order with the topmost treated as LAST (the
     * {@link AgentWindowResolver#resolve} contract: the caller owns the ordering).
     * <p>
     * A point over a JIPipe window region without any component (the window background)
     * still resolves the window and acts (the plan's Review Focus item 1): the hit is the
     * window itself. Points outside every JIPipe window are refused with the allowed
     * rectangles.
     */
    private static WindowResolution resolveWindowAt(int x, int y) {
        Point screenPoint = new Point(x, y);
        Window focusedWindow = null;
        try {
            focusedWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
        } catch (Throwable t) {
            // Headless or a focus manager that cannot be consulted: fall back to the
            // creation-order iteration alone
            focusedWindow = null;
        }
        if (focusedWindow != null && focusedWindow.isShowing()
                && focusedWindow.getBounds().contains(screenPoint)) {
            return hitIn(focusedWindow, screenPoint);
        }
        // Cache the windows array once so the bounds list and the window lookup agree
        // (a second Window.getWindows() call could return a different array if a window
        // opens or closes between the two calls)
        Window[] windows = Window.getWindows();
        List<AgentWindowResolver.WindowBounds> bounds = new ArrayList<>();
        List<Window> candidates = new ArrayList<>();
        for (Window window : windows) {
            if (window != focusedWindow) {
                bounds.add(AgentWindowResolver.of(window));
                candidates.add(window);
            }
        }
        AgentWindowResolver.WindowBounds resolved = AgentWindowResolver.resolve(bounds, screenPoint);
        if (resolved == null) {
            // The allowed rectangles cover ALL showing JIPipe windows, including the
            // focused one (it was only excluded from the resolution search because it
            // does not contain the point)
            List<AgentWindowResolver.WindowBounds> allBounds = new ArrayList<>(bounds);
            if (focusedWindow != null && focusedWindow.isShowing()) {
                allBounds.add(AgentWindowResolver.of(focusedWindow));
            }
            return new WindowResolution(null, null, null,
                    "The point (" + x + ", " + y + ") is not inside a JIPipe window",
                    AgentWindowResolver.allowedRectangles(allBounds));
        }
        // Walked by index in REVERSE, never by value: WindowBounds is a value record,
        // so two windows with identical on-screen bounds compare equal and a value
        // lookup would hit-test the first (possibly hidden) one. The resolver returns
        // topmost last, so the reverse walk finds the resolved window first and the
        // topmost window wins for overlapping bounds, exactly like a real click.
        for (int i = candidates.size() - 1; i >= 0; i--) {
            if (bounds.get(i).equals(resolved)) {
                return hitIn(candidates.get(i), screenPoint);
            }
        }
        throw new IllegalStateException("The window resolver returned a candidate's own bounds");
    }

    /**
     * Converts the screen point into the resolved window and returns the component under
     * the point (the window itself when the point is not over any component).
     */
    private static WindowResolution hitIn(Window window, Point screenPoint) {
        // Hit-test the Swing root pane (the decorated-window hit surface: a JFrame's
        // content lives in its JRootPane, and the root pane accounts for the decoration
        // insets when the screen point is converted). Container#getComponentAt is SHALLOW
        // (it returns the immediate child only), so the deepest component under the point
        // is found with the same front-to-back, recursive traversal the AWT event dispatch
        // uses to route real mouse events (spec 4.1: the probe hit-tests like the mouse).
        JComponent root = SwingUtilities.getRootPane(window);
        Component hitSurface = root != null ? root : window;
        Point surfaceLocal = new Point(screenPoint);
        SwingUtilities.convertPointFromScreen(surfaceLocal, hitSurface);
        Component component = deepestComponentAt((Container) hitSurface, surfaceLocal);
        if (component == null) {
            // The point is over a region with no component (the window background or a
            // layout gap): the hit surface itself is the target, so the probe still names
            // the window and the input operations still act (the plan's Review Focus 1)
            component = hitSurface;
        }
        Point componentLocal = new Point(surfaceLocal);
        SwingUtilities.convertPoint(hitSurface, componentLocal, component);
        return new WindowResolution(window, component, componentLocal, null, null);
    }

    /**
     * The deepest visible component of the container at the point (in the container's
     * coordinate system), found by a front-to-back, recursive traversal: this is exactly
     * how the AWT event dispatch routes a real mouse event to a leaf. Returns null when no
     * visible child contains the point (the caller then uses the container itself).
     */
    private static Component deepestComponentAt(Container container, Point pointInContainer) {
        for (int i = container.getComponentCount() - 1; i >= 0; i--) {
            Component child = container.getComponent(i);
            if (!child.isVisible()) {
                continue;
            }
            Point childPoint = new Point(pointInContainer);
            SwingUtilities.convertPoint(container, childPoint, child);
            if (child.contains(childPoint)) {
                if (child instanceof Container childContainer) {
                    Component deeper = deepestComponentAt(childContainer, childPoint);
                    return deeper != null ? deeper : child;
                }
                return child;
            }
        }
        return null;
    }

    /**
     * The top levels of the component's class name chain (window first), separated by '>'.
     */
    private static String componentChain(Component component, int levels) {
        List<String> names = new ArrayList<>();
        for (Component c = component; c != null && names.size() < levels; c = c.getParent()) {
            String simpleName = c.getClass().getSimpleName();
            if (!simpleName.isEmpty()) {
                names.add(simpleName);
            }
        }
        return String.join(">", names);
    }

    /**
     * The on-screen bounds of all showing JIPipe windows, as JSON (the refusal payload of
     * the point-based operations).
     */
    private static ArrayNode allowedRectanglesNode(List<Rectangle> rectangles) {
        ArrayNode array = mapper.createArrayNode();
        for (Rectangle rectangle : rectangles) {
            ObjectNode node = array.addObject();
            node.put("x", rectangle.x);
            node.put("y", rectangle.y);
            node.put("width", rectangle.width);
            node.put("height", rectangle.height);
        }
        return array;
    }

    /**
     * The window title for the responses: the window's title, or its class simple name
     * when it has none.
     */
    private static String windowTitle(Window window) {
        if (window instanceof java.awt.Frame frame) {
            String title = frame.getTitle();
            if (title != null && !title.isBlank()) {
                return title;
            }
        }
        if (window.getAccessibleContext() != null) {
            String accessibleName = window.getAccessibleContext().getAccessibleName();
            if (accessibleName != null && !accessibleName.isBlank()) {
                return accessibleName;
            }
        }
        return window.getClass().getSimpleName();
    }

    /**
     * The accessible name of the component, or null when it has none (passed to the
     * pure suggestion table for non-dock components).
     */
    private static String accessibleNameOf(Component component) {
        if (component instanceof JComponent jComponent && jComponent.getAccessibleContext() != null) {
            String name = jComponent.getAccessibleContext().getAccessibleName();
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        return null;
    }

    /**
     * The dock panel that hosts the component (walking up the ancestor chain), or null.
     */
    private static JIPipeDesktopDockPanel dockPanelOf(Component component) {
        for (Component c = component; c != null; c = c.getParent()) {
            if (c instanceof JIPipeDesktopDockPanel dockPanel) {
                return dockPanel;
            }
        }
        return null;
    }

    /**
     * The dock id of the dock panel content hosting the component, or null when the
     * component is not inside a specific panel's content (e.g. the dock toolbar buttons
     * and the split pane borders: dock chrome, no single dock id applies).
     */
    private static String dockIdOf(Component component) {
        JIPipeDesktopDockPanel dockPanel = dockPanelOf(component);
        if (dockPanel == null) {
            return null;
        }
        for (JIPipeDesktopDockPanel.Panel panel : dockPanel.getPanels().values()) {
            JComponent panelComponent = panel.getComponent();
            if (panelComponent != null
                    && (panelComponent == component || SwingUtilities.isDescendingFrom(component, panelComponent))) {
                return panel.getId();
            }
        }
        return null;
    }

    /**
     * The id of a fireable action whose label matches the button text of a probed button
     * or menu item: the registry lookup the pure probe helper deliberately leaves to the
     * operation (the button branch suggests nothing; the operation fills in the fireable
     * action when the label matches uniquely). Returns null when there is no unique
     * match.
     */
    private static String fireableActionIdForText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String match = null;
        for (JIPipeShortcutAction action : JIPipeShortcutRegistry.getInstance().all()) {
            if (!action.name().equals(text)) {
                continue;
            }
            if (match != null && !match.equals(action.id())) {
                return null; // ambiguous label
            }
            match = action.id();
        }
        return match;
    }

    /**
     * The canvas region under the point of a picked node UI, and the slot name when the
     * region is a slot. The node's own active areas (the same hit regions the mouse
     * listeners use, sorted by priority with the whole-node area last) are tested in
     * order: the run badge (its active area), a slot (slot or slot button active area),
     * another node-internal area (input config, add-slot button), or the node body.
     */
    private record CanvasRegionMatch(AgentElementProbe.CanvasRegion region, String slotName) {
    }

    /**
     * The canvas region under the point of a picked node UI. The point arrives in the
     * hit component's coordinate system (the window's hit test returns either the node UI
     * or the canvas or an ancestor), so it is converted into the node UI's coordinates
     * first.
     */
    private static CanvasRegionMatch nodeRegion(JIPipeDesktopGraphNodeUI nodeUI,
                                                Component hitComponent, Point hitLocal) {
        Point nodeLocal = new Point(hitLocal);
        SwingUtilities.convertPoint(hitComponent, nodeLocal, nodeUI);
        for (JIPipeDesktopGraphNodeUIActiveArea area : nodeUI.getActiveAreas()) {
            Rectangle hitArea = area.getZoomedHitArea();
            if (hitArea != null && hitArea.contains(nodeLocal)) {
                if (area instanceof JIPipeDesktopGraphNodeUIRunNodeActiveArea) {
                    return new CanvasRegionMatch(AgentElementProbe.CanvasRegion.RUN_BADGE, null);
                }
                if (area instanceof JIPipeDesktopGraphNodeUISlotActiveArea slotArea) {
                    return new CanvasRegionMatch(AgentElementProbe.CanvasRegion.SLOT, slotArea.getSlotName());
                }
                if (area instanceof JIPipeDesktopGraphNodeUISlotButtonActiveArea slotButton) {
                    JIPipeDesktopGraphNodeUISlotActiveArea slotArea = slotButton.getUISlot();
                    return new CanvasRegionMatch(AgentElementProbe.CanvasRegion.SLOT,
                            slotArea != null ? slotArea.getSlotName() : null);
                }
                return new CanvasRegionMatch(AgentElementProbe.CanvasRegion.INTERNAL, null);
            }
        }
        return new CanvasRegionMatch(AgentElementProbe.CanvasRegion.NODE_BODY, null);
    }

    /**
     * The id of a probed node UI in the format the {@code run_node} operation expects
     * (alias id or UUID, the node-id format of the query operations): the alias id of
     * the node in its canvas graph when the canvas is part of a project, else the node's
     * UUID in the parent graph.
     */
    private static String nodeUIId(JIPipeDesktopGraphNodeUI nodeUI) {
        JIPipeGraphNode node = nodeUI.getNode();
        JIPipeDesktopGraphCanvasUI canvasUI = nodeUI.getGraphCanvasUI();
        if (node != null && canvasUI != null && canvasUI.getGraph() != null) {
            try {
                String alias = canvasUI.getGraph().getAliasIdOf(node);
                if (alias != null && !alias.isBlank()) {
                    return alias;
                }
            } catch (RuntimeException ignored) {
                // A node that is no longer in the graph: fall through to the UUID
            }
        }
        return node != null && node.getUUIDInParentGraph() != null
                ? node.getUUIDInParentGraph().toString()
                : "unknown";
    }

    /**
     * Classifies what is under the point of a resolved window (the shared body of
     * {@code ui_probe} and the {@code probe} field of the input operation responses;
     * must run on the EDT): a graph-canvas node (body, slot, or run badge) through the
     * canvas pickers and the node's own active areas, a dock panel hit when the
     * component sits inside a specific panel's content (the dock id names the
     * set_dock_visible argument), otherwise a component (with a best-effort
     * button-text to fireable-action lookup for buttons and menu items).
     */
    private static ObjectNode probeAt(WindowResolution resolution) {
        ObjectNode result = mapper.createObjectNode();
        result.put("window", windowTitle(resolution.window()));
        Component hit = resolution.hit();

        String kind;
        String detail;
        String suggestedOperation = null;
        String suggestedArguments = null;

        // The window's hit test returns the deepest component at the point. Node UIs are
        // heavy children of the canvas (so a point over a node returns the node UI,
        // unless it is over a region where the canvas itself is returned due to the
        // overlap rules). Both cases are handled by the canvas picker: it answers the
        // topmost node UI at canvas-local coordinates, exactly like the mouse events.
        JIPipeDesktopGraphCanvasUI canvasUI = hit instanceof JIPipeDesktopGraphCanvasUI canvas
                ? canvas
                : canvasAncestorOf(hit);
        JIPipeDesktopGraphNodeUI nodeUI = null;
        if (hit instanceof JIPipeDesktopGraphNodeUI node) {
            nodeUI = node;
        } else if (canvasUI != null) {
            Point canvasLocal = new Point(resolution.local());
            SwingUtilities.convertPoint(hit, canvasLocal, canvasUI);
            MouseEvent pickEvent = new MouseEvent(canvasUI, MouseEvent.MOUSE_MOVED,
                    System.currentTimeMillis(), 0,
                    canvasLocal.x, canvasLocal.y, 0, false, MouseEvent.NOBUTTON);
            nodeUI = canvasUI.pickNodeUI(pickEvent);
            if (nodeUI != null) {
                // The hit test returned a component other than the node itself (the
                // canvas or an ancestor): re-express the point in the node's
                // coordinates so the region classification and the echoed
                // target_component both refer to the node
                Point nodeLocal = new Point(canvasLocal);
                SwingUtilities.convertPoint(canvasUI, nodeLocal, nodeUI);
                resolution = new WindowResolution(resolution.window(), nodeUI, nodeLocal, null, null);
                hit = nodeUI;
            }
        }

        if (nodeUI != null) {
            CanvasRegionMatch match = nodeRegion(nodeUI, hit, resolution.local());
            String nodeId = nodeUIId(nodeUI);
            AgentElementProbe.ProbeHit hitProbe = AgentElementProbe.suggestForCanvasRegion(
                    match.region(), nodeId, match.slotName());
            kind = hitProbe.kind();
            detail = hitProbe.detail();
            suggestedOperation = hitProbe.suggestedOperation();
            suggestedArguments = hitProbe.suggestedArguments();
        } else {
            // A component inside a specific panel's content is a DOCK hit: the resolved
            // dock id directly names the set_dock_visible argument. The hit is
            // constructed here (identical to the pure table's dock branch) instead of
            // going through the table: its dock rule matches the wrapper class name
            // 'JIPipeDesktopDockPanel$Panel', which a Component ancestor chain cannot
            // contain (the wrapper is not a Component, and simple names carry no '$').
            // Dock chrome (toolbar buttons, split pane borders) has no dock id and
            // falls through to the plain component table, so no set_dock_visible
            // suggestion is made there.
            String dockId = dockIdOf(hit);
            AgentElementProbe.ProbeHit hitProbe;
            if (dockId != null) {
                hitProbe = new AgentElementProbe.ProbeHit("dock", dockId, "set_dock_visible",
                        "{\"dock-id\": \"" + dockId + "\"}");
            } else {
                hitProbe = AgentElementProbe.suggestForComponent(
                        componentChain(hit, 6), accessibleNameOf(hit));
            }
            kind = hitProbe.kind();
            detail = hitProbe.detail();
            suggestedOperation = hitProbe.suggestedOperation();
            suggestedArguments = hitProbe.suggestedArguments();
            String buttonText = null;
            if (hit instanceof JButton button) {
                buttonText = button.getText();
            } else if (hit instanceof JToggleButton toggleButton) {
                buttonText = toggleButton.getText();
            } else if (hit instanceof JMenuItem menuItem) {
                buttonText = menuItem.getText();
            }
            if ("component".equals(kind) && buttonText != null) {
                String actionId = fireableActionIdForText(buttonText);
                if (actionId != null) {
                    suggestedOperation = "fire_action";
                    suggestedArguments = "{\"action-id\": \"" + actionId + "\"}";
                }
            }
        }

        result.put("kind", kind);
        result.put("detail", detail);
        result.put("suggested_operation", suggestedOperation);
        result.put("suggested_arguments", suggestedArguments);
        return result;
    }

    /**
     * The graph canvas UI inside the component's ancestor chain, or null.
     */
    private static JIPipeDesktopGraphCanvasUI canvasAncestorOf(Component component) {
        for (Component c = component; c != null; c = c.getParent()) {
            if (c instanceof JIPipeDesktopGraphCanvasUI canvasUI) {
                return canvasUI;
            }
        }
        return null;
    }

    /**
     * The refusal result for a point outside every JIPipe window.
     */
    private static ObjectNode refusalError(WindowResolution resolution) {
        ObjectNode error = mapper.createObjectNode();
        error.put("error", resolution.refusal());
        error.set("allowed_rectangles", allowedRectanglesNode(resolution.allowedRectangles()));
        return error;
    }

    /**
     * The self-describing response of the input operations (spec 4.1): the mode that
     * ran, the resolved window and component, and the probe of the target point, so
     * every click is self-describing and the agent never acts blind.
     */
    private static ObjectNode inputResult(AgentInputDispatch.AgentInputMode mode,
                                          WindowResolution resolution) {
        ObjectNode data = mapper.createObjectNode();
        data.put("ok", true);
        data.put("input_mode", mode.name());
        data.put("window", windowTitle(resolution.window()));
        data.put("target_component", componentChain(resolution.hit(), 3));
        data.set("probe", probeAt(resolution));
        return data;
    }

    /**
     * Fires a registered UI action by id in the target editor's dispatcher scope.
     */
    public static class FireAction implements AutomationOperation {

        @Override
        public String getId() {
            return "fire_action";
        }

        @Override
        public String getDescription() {
            return "Fire a registered UI action (shortcut action, context action, dock toggle) by id in the "
                    + "target project window's currently shown editor. The action is resolved in the target "
                    + "editor's own dispatcher scope (its context actions match the editor's current selection), "
                    + "never against the user's current keyboard focus, so the user's focus is left alone. "
                    + "Use list_ui_actions to discover the fireable ids.";
        }

        @Override
        public JsonNode getInputSchema() {
            return schemaWithProject("""
                    {
                      "type": "object",
                      "required": ["action-id"],
                      "properties": {
                        "action-id": {"type": "string", "description": "The id of the UI action to fire (as listed by list_ui_actions): a context action id, a tool or editor-level id, or 'dock:<dock-id>' for a dock panel toggle"}
                      },
                      "additionalProperties": false
                    }""", null);
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
            JIPipeDesktopProjectWindow window = resolveTargetWindow(ctx, params);
            if (window == null) {
                return error("No matching project window is open");
            }
            JIPipeDesktopGraphEditorUI editorUI = selectedEditor(window);
            if (editorUI == null) {
                return error("The selected tab of the project window is not a graph editor; "
                        + "actions fire against a graph editor's scope");
            }
            String actionId = params.get("action-id").asText();
            // The target editor's dispatcher scope: the focus owner passed to
            // resolveAction is the editor's canvas (a descendant of the editor's scope
            // root), so the editor's context actions, tools, and editor-level actions
            // answer for the TARGET editor, never for wherever the user's keyboard focus
            // currently is. Window-level scopes (dock toggles, command bar) answer
            // against the window hosting the anchor (this editor's window): they match
            // the focus owner's window, so any focus inside this window qualifies.
            // Resolving through the dispatcher (instead of calling a scope directly)
            // keeps the exact text-input guards the keyboard path applies.
            Runnable runnable = JIPipeDesktopShortcutDispatcher.getInstance()
                    .resolveAction(actionId, editorUI.getCanvasUI());
            if (runnable == null) {
                return error("No action '" + actionId + "' is available in the current editor; "
                        + "use list_ui_actions to see the fireable ids");
            }
            try {
                runnable.run();
            } catch (RuntimeException e) {
                return error("Firing action '" + actionId + "' failed: " + e.getMessage());
            }
            ObjectNode data = mapper.createObjectNode();
            data.put("ok", true);
            data.put("action_id", actionId);
            return data;
        }
    }

    /**
     * Lists the UI actions that are fireable in the target editor's scope.
     */
    public static class ListUiActions implements AutomationOperation {

        @Override
        public String getId() {
            return "list_ui_actions";
        }

        @Override
        public String getDescription() {
            return "List the UI actions that are fireable (via fire_action) in the target project window's "
                    + "currently shown editor: the registry actions whose id the editor's dispatcher scope "
                    + "answers (context actions matching the current selection, tools, editor-level actions, "
                    + "and the window's dock toggles). Read-only";
        }

        @Override
        public JsonNode getInputSchema() {
            return schemaWithProject("""
                    {
                      "type": "object",
                      "properties": {},
                      "additionalProperties": false
                    }""", null);
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
            JIPipeDesktopGraphEditorUI editorUI = selectedEditor(window);
            ArrayNode actions = mapper.createArrayNode();
            if (editorUI != null) {
                // The same focus owner fire_action resolves with: the ids the list
                // reports are exactly the ids fire_action can fire in this editor
                Component focusOwner = editorUI.getCanvasUI();
                for (JIPipeShortcutAction action : JIPipeShortcutRegistry.getInstance().all()) {
                    if (JIPipeDesktopShortcutDispatcher.getInstance().resolveAction(action.id(), focusOwner)
                            != null) {
                        ObjectNode node = actions.addObject();
                        node.put("id", action.id());
                        node.put("name", action.name());
                        node.put("category", action.category());
                    }
                }
            }
            ObjectNode data = mapper.createObjectNode();
            data.set("actions", actions);
            return data;
        }
    }

    /**
     * Shows or hides a dock panel of the target editor (view state only, the
     * {@code navigate_to_node} precedent).
     */
    public static class SetDockVisible implements AutomationOperation {

        @Override
        public String getId() {
            return "set_dock_visible";
        }

        @Override
        public String getDescription() {
            return "Show or hide a dock panel of the target project window's currently shown editor (graph "
                    + "editor or project overview). Changes view state only, never project data. Use list_docks "
                    + "to see the available dock ids.";
        }

        @Override
        public JsonNode getInputSchema() {
            return schemaWithProject("""
                    {
                      "type": "object",
                      "required": ["dock-id"],
                      "properties": {
                        "dock-id": {"type": "string", "description": "The dock panel id (as listed by list_docks), for example ADD_NODES, FIND, ERRORS, or _PARAMETERS"},
                        "visible": {"type": "boolean", "default": true, "description": "Whether the dock panel should be shown (default true) or hidden"}
                      },
                      "additionalProperties": false
                    }""", null);
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
            JIPipeDesktopDockPanel dockPanel = selectedDock(window);
            if (dockPanel == null) {
                return error("The selected tab of the project window has no dock panels");
            }
            String dockId = params.get("dock-id").asText();
            if (!dockPanel.containsPanel(dockId)) {
                List<String> available = new ArrayList<>(dockPanel.getPanels().keySet());
                return error("Unknown dock id '" + dockId + "'. Available dock ids: "
                        + String.join(", ", available));
            }
            boolean visible = boolParam(params, "visible", true);
            if (visible) {
                dockPanel.activatePanel(dockId, true);
            } else {
                dockPanel.deactivatePanel(dockId, true);
            }
            ObjectNode data = mapper.createObjectNode();
            data.put("ok", true);
            data.put("dock_id", dockId);
            data.put("visible", visible);
            return data;
        }
    }

    /**
     * Lists the dock panels of the target editor with their ids, titles, and visible
     * state.
     */
    public static class ListDocks implements AutomationOperation {

        @Override
        public String getId() {
            return "list_docks";
        }

        @Override
        public String getDescription() {
            return "List the dock panels of the target project window's currently shown editor (graph editor "
                    + "or project overview): id, title, and visible state. Read-only";
        }

        @Override
        public JsonNode getInputSchema() {
            return schemaWithProject("""
                    {
                      "type": "object",
                      "properties": {},
                      "additionalProperties": false
                    }""", null);
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
            JIPipeDesktopDockPanel dockPanel = selectedDock(window);
            if (dockPanel == null) {
                return error("The selected tab of the project window has no dock panels");
            }
            ArrayNode docks = mapper.createArrayNode();
            for (JIPipeDesktopDockPanel.Panel panel : dockPanel.getPanels().values()) {
                ObjectNode node = docks.addObject();
                node.put("id", panel.getId());
                String title = panel.getName();
                node.put("title", title != null && !title.isBlank() ? title : panel.getId());
                node.put("visible", panel.isVisible());
            }
            ObjectNode data = mapper.createObjectNode();
            data.set("docks", docks);
            return data;
        }
    }

    /**
     * Probes what is under a screen point (the virtual cursor's peek): the window and the
     * element there, with the Layer 1 operation to prefer when the element has a semantic
     * identity. Delivers no event.
     */
    public static class UiProbe implements AutomationOperation {

        @Override
        public String getId() {
            return "ui_probe";
        }

        @Override
        public String getDescription() {
            return "Probe what is under a screen point WITHOUT delivering any event: the JIPipe window, and "
                    + "the element there (a graph canvas node with its region - body, slot, or run badge - "
                    + "and the node id, or a component with its class chain). When the element has a semantic "
                    + "identity (a node's run badge, a dock panel, a button matching a fireable action) the "
                    + "result names the Layer 1 operation to prefer instead of a raw ui_click "
                    + "(suggested_operation and suggested_arguments). Use this to aim before ui_click, "
                    + "ui_drag, or ui_scroll. Read-only; points outside JIPipe windows are refused with the "
                    + "allowed rectangles";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["x", "y"],
                      "properties": {
                        "x": {"type": "integer", "description": "The x coordinate in screen coordinates"},
                        "y": {"type": "integer", "description": "The y coordinate in screen coordinates"}
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
            WindowResolution resolution = resolveWindowAt(
                    intParam(params, "x", 0), intParam(params, "y", 0));
            if (resolution.window() == null) {
                return refusalError(resolution);
            }
            return probeAt(resolution);
        }
    }

    /**
     * Delivers a mouse click at a screen point.
     */
    public static class UiClick implements AutomationOperation {

        @Override
        public String getId() {
            return "ui_click";
        }

        @Override
        public String getDescription() {
            return "Deliver a mouse click (single, double, or triple) at a screen point inside a JIPipe window."
                    + CONFIRM_CLAUSE;
        }

        @Override
        public JsonNode getInputSchema() {
            return schemaWithInputModeAndProject("""
                    {
                      "type": "object",
                      "required": ["x", "y"],
                      "properties": {
                        "x": {"type": "integer", "description": "The x coordinate in screen coordinates"},
                        "y": {"type": "integer", "description": "The y coordinate in screen coordinates"},
                        "click-count": {"type": "integer", "default": 1, "description": "Number of clicks, limited to 1 (default, single), 2 (double), or 3 (triple)"},
                        "button": {"type": "string", "enum": ["left", "middle", "right"], "default": "left", "description": "The mouse button. A right click sets the popup trigger flag on the press (the real X11 convention), so popup menus that react to isPopupTrigger() open"}
                      },
                      "additionalProperties": false
                    }""", " (informational for this operation: the point alone resolves the window)");
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
            WindowResolution resolution = resolveWindowAt(
                    intParam(params, "x", 0), intParam(params, "y", 0));
            if (resolution.window() == null) {
                return refusalError(resolution);
            }
            InputModeOrError modeOrError = resolveInputMode(params);
            if (modeOrError.error != null) {
                return error(modeOrError.error);
            }
            Integer buttonId = buttonId(params);
            if (buttonId == null) {
                return error("Unknown button '" + params.get("button").asText()
                        + "'; expected left, middle, or right");
            }
            int clickCount = intParam(params, "click-count", 1);
            if (clickCount < 1 || clickCount > 3) {
                return error("click-count is limited to 1 (single), 2 (double), or 3 (triple)");
            }
            try {
                modeOrError.mode.click(resolution.hit(), resolution.local().x, resolution.local().y,
                        clickCount, buttonId);
            } catch (Exception e) {
                return error("Input delivery failed: " + e.getMessage());
            }
            return inputResult(modeOrError.mode, resolution);
        }
    }

    /**
     * Delivers a mouse drag from one screen point to another.
     */
    public static class UiDrag implements AutomationOperation {

        @Override
        public String getId() {
            return "ui_drag";
        }

        @Override
        public String getDescription() {
            return "Deliver a mouse drag from one screen point to another inside a JIPipe window (the same "
                    + "window must contain both points). The drag is delivered as a press, ten interpolated "
                    + "movement events, and a release." + CONFIRM_CLAUSE;
        }

        @Override
        public JsonNode getInputSchema() {
            return schemaWithInputModeAndProject("""
                    {
                      "type": "object",
                      "required": ["from-x", "from-y", "to-x", "to-y"],
                      "properties": {
                        "from-x": {"type": "integer", "description": "The start x coordinate in screen coordinates"},
                        "from-y": {"type": "integer", "description": "The start y coordinate in screen coordinates"},
                        "to-x": {"type": "integer", "description": "The end x coordinate in screen coordinates"},
                        "to-y": {"type": "integer", "description": "The end y coordinate in screen coordinates"},
                        "button": {"type": "string", "enum": ["left", "middle", "right"], "default": "left", "description": "The mouse button held during the drag"}
                      },
                      "additionalProperties": false
                    }""", " (informational for this operation: the points alone resolve the window)");
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
            WindowResolution resolution = resolveWindowAt(
                    intParam(params, "from-x", 0), intParam(params, "from-y", 0));
            if (resolution.window() == null) {
                return refusalError(resolution);
            }
            InputModeOrError modeOrError = resolveInputMode(params);
            if (modeOrError.error != null) {
                return error(modeOrError.error);
            }
            Point toScreen = new Point(intParam(params, "to-x", 0), intParam(params, "to-y", 0));
            if (!resolution.window().getBounds().contains(toScreen)) {
                ObjectNode refusal = mapper.createObjectNode();
                refusal.put("error", "The end point (" + toScreen.x + ", " + toScreen.y
                        + ") is not inside the resolved JIPipe window; both points must be in the same window");
                refusal.set("allowed_rectangles", allowedRectanglesNode(
                        AgentWindowResolver.allowedRectangles(
                                List.of(AgentWindowResolver.of(resolution.window())))));
                return refusal;
            }
            Integer buttonId = buttonId(params);
            if (buttonId == null) {
                return error("Unknown button '" + params.get("button").asText()
                        + "'; expected left, middle, or right");
            }
            Point toWindow = new Point(toScreen);
            SwingUtilities.convertPointFromScreen(toWindow, resolution.window());
            Point toLocal = new Point(toWindow);
            SwingUtilities.convertPoint(resolution.window(), toLocal, resolution.hit());
            try {
                modeOrError.mode.drag(resolution.hit(), resolution.local().x, resolution.local().y,
                        toLocal.x, toLocal.y, buttonId);
            } catch (Exception e) {
                return error("Input delivery failed: " + e.getMessage());
            }
            return inputResult(modeOrError.mode, resolution);
        }
    }

    /**
     * Delivers a mouse wheel rotation at a screen point.
     */
    public static class UiScroll implements AutomationOperation {

        @Override
        public String getId() {
            return "ui_scroll";
        }

        @Override
        public String getDescription() {
            return "Deliver a signed mouse wheel rotation at a screen point inside a JIPipe window. The amount "
                    + "is the wheel rotation in the standard wheel orientation: positive scrolls down/away from "
                    + "the user, negative scrolls up/towards the user. One rotation per unit, so scroll listeners "
                    + "read exactly the amount (getWheelRotation and getUnitsToScroll agree). The delivered wheel "
                    + "events carry no keyboard modifiers (the canvas zoom is a physical Ctrl+wheel gesture and "
                    + "cannot be reached through this operation)." + CONFIRM_CLAUSE;
        }

        @Override
        public JsonNode getInputSchema() {
            return schemaWithInputModeAndProject("""
                    {
                      "type": "object",
                      "required": ["x", "y", "amount"],
                      "properties": {
                        "x": {"type": "integer", "description": "The x coordinate in screen coordinates"},
                        "y": {"type": "integer", "description": "The y coordinate in screen coordinates"},
                        "amount": {"type": "integer", "description": "The signed wheel rotation: positive scrolls down/away from the user, negative scrolls up/towards the user. The delivered events carry no keyboard modifiers"}
                      },
                      "additionalProperties": false
                    }""", " (informational for this operation: the point alone resolves the window)");
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
            WindowResolution resolution = resolveWindowAt(
                    intParam(params, "x", 0), intParam(params, "y", 0));
            if (resolution.window() == null) {
                return refusalError(resolution);
            }
            InputModeOrError modeOrError = resolveInputMode(params);
            if (modeOrError.error != null) {
                return error(modeOrError.error);
            }
            int amount = intParam(params, "amount", 0);
            try {
                modeOrError.mode.scroll(resolution.hit(), resolution.local().x, resolution.local().y, amount);
            } catch (Exception e) {
                return error("Input delivery failed: " + e.getMessage());
            }
            return inputResult(modeOrError.mode, resolution);
        }
    }

    /**
     * Types text into the current keyboard focus owner.
     */
    public static class UiTypeText implements AutomationOperation {

        @Override
        public String getId() {
            return "ui_type_text";
        }

        @Override
        public String getDescription() {
            return "Type text into the component that currently holds the keyboard focus (text only, no Enter "
                    + "key is ever sent). The intended sequence is ui_click into a field first (which transfers "
                    + "the focus), then ui_type_text. Refuses when no keyboard focus owner is inside a showing "
                    + "JIPipe window. Newline characters are not supported; deliver each line as a separate "
                    + "call. In system mode only printable ASCII is typeable." + CONFIRM_CLAUSE;
        }

        @Override
        public JsonNode getInputSchema() {
            return schemaWithInputModeAndProject("""
                    {
                      "type": "object",
                      "required": ["text"],
                      "properties": {
                        "text": {"type": "string", "description": "The text to type into the current keyboard focus owner. No newline characters (deliver each line as a separate call); system mode supports printable ASCII only, dispatch mode supports any character"}
                      },
                      "additionalProperties": false
                    }""", " (informational for this operation: the keyboard focus owner resolves the target)");
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
            InputModeOrError modeOrError = resolveInputMode(params);
            if (modeOrError.error != null) {
                return error(modeOrError.error);
            }
            String text = params.get("text").asText();
            // Newlines are refused for BOTH modes before any delivery (the dispatch mode
            // would otherwise silently transport them via KEY_TYPED while the system mode
            // throws; the operation keeps a uniform "text only" error instead)
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\n' || c == '\r') {
                    return error("ui_type_text delivers text only; newline characters are not supported - "
                            + "deliver each line as a separate call");
                }
            }
            Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (focusOwner == null) {
                return error("No keyboard focus owner inside a JIPipe window; click into a field first");
            }
            Window focusWindow = SwingUtilities.getWindowAncestor(focusOwner);
            if (focusWindow == null || !focusWindow.isShowing()) {
                return error("No keyboard focus owner inside a JIPipe window; click into a field first");
            }
            try {
                modeOrError.mode.typeText(focusOwner, text);
            } catch (Exception e) {
                return error("Input delivery failed: " + e.getMessage());
            }
            ObjectNode data = mapper.createObjectNode();
            data.put("ok", true);
            data.put("input_mode", modeOrError.mode.name());
            data.put("window", windowTitle(focusWindow));
            data.put("target_component", componentChain(focusOwner, 3));
            return data;
        }
    }
}
