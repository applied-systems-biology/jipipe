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

package org.hkijena.jipipe.api.shortcuts;

import javax.swing.KeyStroke;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The core catalog of keyboard shortcut actions, exposed as public constants
 * so the desktop UI can reference them by name.
 * <p>
 * Every constant is a fully described {@link JIPipeShortcutAction} with a
 * stable string-literal id (the project rule for registered ids: never
 * code-derived, because the id is persisted in saved keymaps and presets).
 * The id prefixes follow the design's action id scheme:
 * {@code graph.tool.*} for editor tools, {@code graph.*} for editor-level
 * actions, {@code graph.node.*} / {@code graph.edge.*} /
 * {@code graph.compartment.*} for context actions, and {@code project.*} for
 * the workbench menu accelerators.
 * <p>
 * {@link #registerCore()} registers every core constant in the application
 * registry. It is idempotent: an id that is already registered is skipped, so
 * plugins or tests that pre-registered an action are not clobbered.
 */
public final class JIPipeShortcutActions {
    private JIPipeShortcutActions() {
    }

    /**
     * Parses a {@link KeyStroke} from its string form.
     */
    private static KeyStroke stroke(String stroke) {
        KeyStroke result = KeyStroke.getKeyStroke(stroke);
        if (result == null) {
            throw new IllegalArgumentException("Unparseable keystroke: " + stroke);
        }
        return result;
    }

    // ==================== Graph editor tools ====================
    // category "Graph editor tools"; FAMILY_GRAPH (the builder default)

    /**
     * The default (select) tool, also the cancel/escape binding of the graph
     * editor.
     */
    public static final JIPipeShortcutAction TOOL_DEFAULT =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.tool.default", "Default tool", "Graph editor tools")
                    .description("The default tool: select and move nodes, and drag connections")
                    .defaultChord(stroke("pressed ESCAPE"))
                    .build();
    /**
     * The move tool: moves nodes without modifying connections.
     */
    public static final JIPipeShortcutAction TOOL_MOVE =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.tool.move", "Move nodes", "Graph editor tools")
                    .description("Only moves nodes without modifying existing connections")
                    .defaultChord(stroke("control pressed T"), stroke("pressed M"))
                    .build();
    /**
     * The connect tool: connects slots without moving nodes.
     */
    public static final JIPipeShortcutAction TOOL_CONNECT =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.tool.connect", "Connect slots", "Graph editor tools")
                    .description("Only allows the connection of slots without moving nodes around")
                    .defaultChord(stroke("control pressed T"), stroke("pressed C"))
                    .build();
    /**
     * The rewire tool: drags connections from one slot to another.
     */
    public static final JIPipeShortcutAction TOOL_REWIRE =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.tool.rewire", "Rewire", "Graph editor tools")
                    .description("Drag one or multiple connections from one slot to another")
                    .defaultChord(stroke("control pressed T"), stroke("pressed R"))
                    .build();
    /**
     * The crop view tool: centers the view and removes large empty areas.
     */
    public static final JIPipeShortcutAction TOOL_CROP_VIEW =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.tool.crop-view", "Center view to nodes", "Graph editor tools")
                    .description("Crops the view to the current nodes, removing large empty areas")
                    .defaultChord(stroke("control pressed T"), stroke("pressed V"))
                    .build();

    // The four canvas-notes tool actions (text box, arrow, image box, notes
    // edit) are registered by the canvasnotes plugin (Task 7), not here; the
    // LEGACY preset references their stable ids so it can restore the pre-6.0
    // F-key bindings.

    // ==================== Editor-level actions ====================
    // category "Editor"; FAMILY_GRAPH

    /**
     * Undo the last graph editor operation.
     */
    public static final JIPipeShortcutAction EDITOR_UNDO =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.undo", "Undo", "Editor")
                    .description("Undoes the last operation in the graph editor")
                    .defaultChord(stroke("control pressed Z"))
                    .build();
    /**
     * Redo the last undone graph editor operation.
     */
    public static final JIPipeShortcutAction EDITOR_REDO =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.redo", "Redo", "Editor")
                    .description("Redoes the last undone operation in the graph editor")
                    .defaultChord(stroke("control shift pressed Z"))
                    .build();
    /**
     * Zoom in on the graph editor canvas.
     * <p>
     * Two default variants: the main {@code Ctrl+=} fails on layouts where
     * {@code +} sits behind Shift (QWERTZ), so the numpad
     * {@code Ctrl+Num+} is an interchangeable second variant.
     */
    public static final JIPipeShortcutAction EDITOR_ZOOM_IN =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.zoom-in", "Zoom in", "Editor")
                    .description("Zooms in on the graph editor canvas")
                    .defaultVariants(List.of(
                            List.of(stroke("control pressed EQUALS")),
                            List.of(stroke("control pressed ADD"))))
                    .build();
    /**
     * Zoom out on the graph editor canvas.
     * <p>
     * Two default variants: the main {@code Ctrl+-} fails on layouts where
     * {@code -} sits behind Shift (QWERTZ), so the numpad
     * {@code Ctrl+Num-} is an interchangeable second variant.
     */
    public static final JIPipeShortcutAction EDITOR_ZOOM_OUT =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.zoom-out", "Zoom out", "Editor")
                    .description("Zooms out on the graph editor canvas")
                    .defaultVariants(List.of(
                            List.of(stroke("control pressed MINUS")),
                            List.of(stroke("control pressed SUBTRACT"))))
                    .build();
    /**
     * Reset the graph editor zoom to the default.
     * <p>
     * Two default variants for the same QWERTZ reason as the other zoom
     * actions: {@code Ctrl+0} plus the numpad {@code Ctrl+Num0}.
     */
    public static final JIPipeShortcutAction EDITOR_ZOOM_RESET =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.zoom-reset", "Reset zoom", "Editor")
                    .description("Resets the zoom of the graph editor canvas")
                    .defaultVariants(List.of(
                            List.of(stroke("control pressed 0")),
                            List.of(stroke("control pressed NUMPAD0"))))
                    .build();
    /**
     * Move the current selection one step to the left.
     */
    public static final JIPipeShortcutAction EDITOR_MOVE_SELECTION_LEFT =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.move-selection-left", "Move selection left", "Editor")
                    .description("Moves the current selection one step to the left")
                    .defaultChord(stroke("pressed LEFT"))
                    .build();
    /**
     * Move the current selection one step to the right.
     */
    public static final JIPipeShortcutAction EDITOR_MOVE_SELECTION_RIGHT =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.move-selection-right", "Move selection right", "Editor")
                    .description("Moves the current selection one step to the right")
                    .defaultChord(stroke("pressed RIGHT"))
                    .build();
    /**
     * Move the current selection one step up.
     */
    public static final JIPipeShortcutAction EDITOR_MOVE_SELECTION_UP =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.move-selection-up", "Move selection up", "Editor")
                    .description("Moves the current selection one step up")
                    .defaultChord(stroke("pressed UP"))
                    .build();
    /**
     * Move the current selection one step down.
     */
    public static final JIPipeShortcutAction EDITOR_MOVE_SELECTION_DOWN =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.move-selection-down", "Move selection down", "Editor")
                    .description("Moves the current selection one step down")
                    .defaultChord(stroke("pressed DOWN"))
                    .build();

    // ==================== Project menu accelerators ====================
    // category "Menus"; FAMILY_PROJECT; textInputSafe (window-level actions)

    /**
     * Create a new project (File menu).
     */
    public static final JIPipeShortcutAction PROJECT_NEW =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.project.new", "New", "Menus")
                    .description("Creates a new project")
                    .scopeFamily(JIPipeShortcutAction.FAMILY_PROJECT)
                    .textInputSafe(true)
                    .defaultChord(stroke("control pressed N"))
                    .build();
    /**
     * Open a project (File menu).
     */
    public static final JIPipeShortcutAction PROJECT_OPEN =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.project.open", "Open ...", "Menus")
                    .description("Opens an existing project")
                    .scopeFamily(JIPipeShortcutAction.FAMILY_PROJECT)
                    .textInputSafe(true)
                    .defaultChord(stroke("control pressed O"))
                    .build();
    /**
     * Save the current project (File menu).
     */
    public static final JIPipeShortcutAction PROJECT_SAVE =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.project.save", "Save ...", "Menus")
                    .description("Saves the current project")
                    .scopeFamily(JIPipeShortcutAction.FAMILY_PROJECT)
                    .textInputSafe(true)
                    .defaultChord(stroke("control pressed S"))
                    .build();
    /**
     * Save the current project under a new name (File menu).
     */
    public static final JIPipeShortcutAction PROJECT_SAVE_AS =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.project.save-as", "Save as ...", "Menus")
                    .description("Saves the current project under a new name")
                    .scopeFamily(JIPipeShortcutAction.FAMILY_PROJECT)
                    .textInputSafe(true)
                    .defaultChord(stroke("control shift pressed S"))
                    .build();
    /**
     * Open the command bar.
     * <p>
     * Two interchangeable variants: {@code Ctrl+P} (the key moved out of the
     * graph tools by the 6.0 revision) and the command-palette-style
     * {@code Ctrl+Shift+P}.
     */
    public static final JIPipeShortcutAction COMMAND_BAR_OPEN =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.command-bar.open", "Open command bar", "Menus")
                    .description("Opens the command bar")
                    .scopeFamily(JIPipeShortcutAction.FAMILY_PROJECT)
                    .textInputSafe(true)
                    .defaultVariants(List.of(
                            List.of(stroke("control pressed P")),
                            List.of(stroke("control shift pressed P"))))
                    .build();

    // ==================== Context actions: selection ====================

    /**
     * Select all nodes/edges in the current graph editor.
     */
    public static final JIPipeShortcutAction NODE_SELECT_ALL =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.select-all", "Select all", "Selection")
                    .description("Selects all nodes")
                    .defaultChord(stroke("control pressed A"))
                    .build();
    /**
     * Move a selected node to the cursor position (via the selection dialog).
     * <p>
     * The key moved to the {@code Ctrl+T} chord layer (chord: {@code Ctrl+T}
     * then {@code H}); the pre-6.0 {@code Ctrl+G} is restored by the LEGACY
     * preset.
     */
    public static final JIPipeShortcutAction NODE_SELECT_AND_MOVE_HERE =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.select-and-move-here", "Move node here ...", "Selection")
                    .description("Shows a window to select a node and move it to the current cursor")
                    .defaultChord(stroke("control pressed T"), stroke("pressed H"))
                    .build();

    // ==================== Context actions: creation ====================

    /**
     * Add a new node at the cursor position (pipeline editor flavor).
     */
    public static final JIPipeShortcutAction NODE_ADD =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.add", "Add new node here ...", "Creation")
                    .description("Opens a dialog that allows to search and add a node at the specified location")
                    .defaultVariants(List.of(
                            List.of(stroke("shift pressed A")),
                            List.of(stroke("control pressed T"), stroke("pressed N"))))
                    .build();
    /**
     * Add a new compartment at the cursor position (compartments flavor).
     */
    public static final JIPipeShortcutAction COMPARTMENT_ADD =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.compartment.add", "Add new compartment here ...", "Creation")
                    .description("Adds a new compartment at the specified location")
                    .defaultChord(stroke("shift pressed A"))
                    .build();
    /**
     * Add a new parameter set node at the cursor position.
     */
    public static final JIPipeShortcutAction NODE_ADD_PARAMETER_SET =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.add-parameter-set", "Add parameter sets here ...", "Creation")
                    .description("Opens a dialog that allows to add a parameter set node at the specified location")
                    .defaultChord(stroke("control pressed T"), stroke("pressed S"))
                    .build();
    /**
     * Add a tunnel entrance node at the cursor position.
     */
    public static final JIPipeShortcutAction NODE_ADD_TUNNEL_ENTRANCE =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.add-tunnel-entrance", "Add tunnel entrance here ...", "Creation")
                    .description("Adds a tunnel entrance at the specified location and opens its configuration window")
                    .defaultChord(stroke("control pressed T"), stroke("pressed G"))
                    .build();
    /**
     * Add a tunnel exit node at the cursor position.
     */
    public static final JIPipeShortcutAction NODE_ADD_TUNNEL_EXIT =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.add-tunnel-exit", "Add tunnel exit here ...", "Creation")
                    .description("Adds a tunnel exit node at the specified location and opens its configuration window")
                    .defaultChord(stroke("control pressed T"), stroke("pressed X"))
                    .build();

    // ==================== Context actions: clipboard ====================

    /**
     * Copy the selected nodes/edges to the clipboard.
     */
    public static final JIPipeShortcutAction NODE_COPY =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.copy", "Copy nodes", "Selection")
                    .description("Copies the selection to the clipboard")
                    .defaultChord(stroke("control pressed C"))
                    .build();
    /**
     * Copy the selected edges to the clipboard. Shares the copy shortcut with
     * {@link #NODE_COPY}; the two are disjoint by selection availability.
     */
    public static final JIPipeShortcutAction EDGE_COPY =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.edge.copy", "Copy edges", "Selection")
                    .description("Copies the selected edges to the clipboard")
                    .defaultChord(stroke("control pressed C"))
                    .build();
    /**
     * Cut the selected nodes/edges into the clipboard.
     */
    public static final JIPipeShortcutAction NODE_CUT =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.cut", "Cut nodes", "Selection")
                    .description("Cuts the selection into the clipboard")
                    .defaultChord(stroke("control pressed X"))
                    .build();
    /**
     * Paste nodes from the clipboard into the current graph.
     */
    public static final JIPipeShortcutAction NODE_PASTE =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.paste", "Paste nodes", "Selection")
                    .description("Copies nodes from the clipboard into the current graph")
                    .defaultChord(stroke("control pressed V"))
                    .build();
    /**
     * Duplicate the selection without its input connections.
     */
    public static final JIPipeShortcutAction NODE_DUPLICATE =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.duplicate", "Duplicate nodes", "Selection")
                    .description("Duplicates the selection")
                    .defaultChord(stroke("control pressed D"))
                    .build();
    /**
     * Duplicate the selection, preserving its input connections.
     */
    public static final JIPipeShortcutAction NODE_DUPLICATE_WITH_INPUTS =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.duplicate-with-inputs", "Duplicate nodes (+ inputs)", "Selection")
                    .description("Duplicates the selection. Any existing connections to the selected node(s) are preserved")
                    .defaultChord(stroke("control shift pressed D"))
                    .build();

    // ==================== Context actions: deletion ====================

    /**
     * Delete the selected nodes/edges.
     */
    public static final JIPipeShortcutAction NODE_DELETE =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.delete", "Delete", "Selection")
                    .description("Deletes the selected nodes/edges")
                    .defaultChord(stroke("pressed DELETE"))
                    .build();
    /**
     * Remove the selected control point from an edge. Shares the delete
     * shortcut with {@link #NODE_DELETE}; the two are disjoint by selection
     * availability.
     */
    public static final JIPipeShortcutAction EDGE_DELETE_CONTROL_POINT =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.edge.delete-control-point", "Remove control point", "Selection")
                    .description("Removes the selected control points")
                    .defaultChord(stroke("pressed DELETE"))
                    .build();
    /**
     * Clear all control points within the selected edges.
     */
    public static final JIPipeShortcutAction EDGE_CLEAR_CONTROL_POINTS =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.edge.clear-control-points", "Clear control points", "Selection")
                    .description("Removes all control points within the selected edges")
                    .defaultChord(stroke("shift pressed DELETE"))
                    .build();

    // ==================== Context actions: input/output and locking ====================

    /**
     * Enable the selected nodes.
     */
    public static final JIPipeShortcutAction NODE_ENABLE =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.enable", "Enable", "Input/output")
                    .description("Enables the selected algorithms")
                    .defaultChord(stroke("control pressed T"), stroke("shift pressed D"))
                    .build();
    /**
     * Disable the selected nodes.
     */
    public static final JIPipeShortcutAction NODE_DISABLE =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.disable", "Disable", "Input/output")
                    .description("Disables the selected algorithms")
                    .defaultChord(stroke("control pressed T"), stroke("pressed D"))
                    .build();
    /**
     * Set the selected nodes to pass their input through to their output.
     */
    public static final JIPipeShortcutAction PASS_THROUGH_ENABLE =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.pass-through-enable", "Pass-through", "Input/output")
                    .description("Sets the selected algorithms to pass their input through to their output without changes")
                    .defaultChord(stroke("control pressed T"), stroke("pressed P"))
                    .build();
    /**
     * Set the selected nodes to process input normally.
     */
    public static final JIPipeShortcutAction PASS_THROUGH_DISABLE =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.pass-through-disable", "Disable pass-through", "Input/output")
                    .description("Sets the selected algorithms to process input to output")
                    .defaultChord(stroke("control pressed T"), stroke("shift pressed P"))
                    .build();
    /**
     * Lock the location and size of the selected nodes.
     */
    public static final JIPipeShortcutAction NODE_LOCK =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.lock", "Lock location/size", "Input/output")
                    .description("Locks the location and size of all selected nodes")
                    .defaultChord(stroke("control pressed T"), stroke("pressed L"))
                    .build();
    /**
     * Unlock the location and size of the selected nodes.
     */
    public static final JIPipeShortcutAction NODE_UNLOCK =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.unlock", "Unlock location/size", "Input/output")
                    .description("Unlocks the location and size of all selected nodes")
                    .defaultChord(stroke("control pressed T"), stroke("shift pressed L"))
                    .build();

    // ==================== Context actions: layers ====================

    /**
     * Raise the Z position of the selected graph objects.
     */
    public static final JIPipeShortcutAction NODE_RAISE =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.raise", "Raise", "Layers")
                    .description("Raises the Z of all selected graph annotations")
                    .defaultChord(stroke("shift pressed PAGE_UP"))
                    .build();
    /**
     * Lower the Z position of the selected graph objects.
     */
    public static final JIPipeShortcutAction NODE_LOWER =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.lower", "Lower", "Layers")
                    .description("Lowers the Z of all selected graph annotations")
                    .defaultChord(stroke("shift pressed PAGE_DOWN"))
                    .build();
    /**
     * Send the selected graph objects to the foreground.
     */
    public static final JIPipeShortcutAction NODE_SEND_TO_FOREGROUND =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.send-to-foreground", "Send to foreground", "Layers")
                    .description("Sends all selected graph annotations to the foreground")
                    .defaultChord(stroke("control shift pressed PAGE_UP"))
                    .build();
    /**
     * Send the selected graph objects to the background.
     */
    public static final JIPipeShortcutAction NODE_SEND_TO_BACKGROUND =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.send-to-background", "Send to background", "Layers")
                    .description("Sends all selected graph annotations to the background")
                    .defaultChord(stroke("control shift pressed PAGE_DOWN"))
                    .build();

    // ==================== Context actions: running ====================

    /**
     * Run the pipeline up to the selected node and show its results.
     * <p>
     * Unassigned by default (in-app verification wave 1): the action is
     * rarely used and will be deprecated at some point. The pre-6.0
     * Ctrl+Enter binding is available through the LEGACY preset.
     */
    public static final JIPipeShortcutAction NODE_RUN_SHOW_RESULTS =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.run-show-results", "Run & show results", "Running")
                    .description("Runs the pipeline up until this algorithm and shows the results")
                    .build();
    /**
     * Run the pipeline up to the selected node and update its cache.
     */
    public static final JIPipeShortcutAction NODE_UPDATE_CACHE =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.update-cache", "Update cache", "Running")
                    .description("Runs the pipeline up until this algorithm and caches the results. Nothing is written to disk.")
                    .defaultChord(stroke("control pressed T"), stroke("pressed U"))
                    .build();
    /**
     * Run the pipeline up to the selected node and cache its results,
     * including the intermediate results of the node itself.
     */
    public static final JIPipeShortcutAction NODE_CACHE_INTERMEDIATE_RESULTS =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.cache-intermediate-results", "Cache intermediate results", "Running")
                    .description("Runs the pipeline up until this algorithm and caches the results (including intermediate results). Nothing is written to disk.")
                    .defaultChord(stroke("control pressed T"), stroke("shift pressed U"))
                    .build();
    /**
     * Run the pipeline up to the predecessors of the selected node and
     * update its cache.
     */
    public static final JIPipeShortcutAction NODE_UPDATE_PREDECESSOR_CACHE =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.update-predecessor-cache", "Update predecessor cache", "Running")
                    .description("Runs the pipeline up until the predecessors of the selected node. Nothing is written to disk.")
                    .defaultChord(stroke("control pressed T"), stroke("pressed B"))
                    .build();

    // ==================== Context actions: navigation ====================

    /**
     * Move the node UI one step to the left.
     */
    public static final JIPipeShortcutAction NODE_MOVE_UI_LEFT =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.move-ui-left", "Move left", "Navigation")
                    .description("Move node(s) left")
                    .defaultChord(stroke("control pressed LEFT"))
                    .build();
    /**
     * Move the node UI one step to the right.
     */
    public static final JIPipeShortcutAction NODE_MOVE_UI_RIGHT =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.move-ui-right", "Move right", "Navigation")
                    .description("Move node(s) right")
                    .defaultChord(stroke("control pressed RIGHT"))
                    .build();
    /**
     * Move the node UI one step up.
     */
    public static final JIPipeShortcutAction NODE_MOVE_UI_UP =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.move-ui-up", "Move up", "Navigation")
                    .description("Move node(s) up")
                    .defaultChord(stroke("control pressed UP"))
                    .build();
    /**
     * Move the node UI one step down.
     */
    public static final JIPipeShortcutAction NODE_MOVE_UI_DOWN =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.move-ui-down", "Move down", "Navigation")
                    .description("Move node(s) down")
                    .defaultChord(stroke("control pressed DOWN"))
                    .build();

    // ==================== Context actions: isolation ====================

    /**
     * Isolate the selected nodes: remove their external connections.
     */
    public static final JIPipeShortcutAction NODE_ISOLATE =
            JIPipeShortcutAction.builder("org.hkijena.jipipe.graph.node.isolate", "Isolate", "Selection")
                    .description("Removes connections from and to the selected nodes, without removing internal connections")
                    .defaultChord(stroke("control shift pressed I"))
                    .build();

    /**
     * Every core constant, in declaration order, as a stable array.
     */
    private static final JIPipeShortcutAction[] CORE_ACTIONS = {
            // Graph editor tools
            TOOL_DEFAULT, TOOL_MOVE, TOOL_CONNECT, TOOL_REWIRE, TOOL_CROP_VIEW,
            // Editor
            EDITOR_UNDO, EDITOR_REDO, EDITOR_ZOOM_IN, EDITOR_ZOOM_OUT, EDITOR_ZOOM_RESET,
            EDITOR_MOVE_SELECTION_LEFT, EDITOR_MOVE_SELECTION_RIGHT, EDITOR_MOVE_SELECTION_UP, EDITOR_MOVE_SELECTION_DOWN,
            // Menus
            PROJECT_NEW, PROJECT_OPEN, PROJECT_SAVE, PROJECT_SAVE_AS, COMMAND_BAR_OPEN,
            // Selection
            NODE_SELECT_ALL, NODE_SELECT_AND_MOVE_HERE, NODE_ISOLATE,
            NODE_COPY, EDGE_COPY, NODE_CUT, NODE_PASTE, NODE_DUPLICATE, NODE_DUPLICATE_WITH_INPUTS,
            NODE_DELETE, EDGE_DELETE_CONTROL_POINT, EDGE_CLEAR_CONTROL_POINTS,
            // Creation
            NODE_ADD, COMPARTMENT_ADD, NODE_ADD_PARAMETER_SET,
            NODE_ADD_TUNNEL_ENTRANCE, NODE_ADD_TUNNEL_EXIT,
            // Input/output
            NODE_ENABLE, NODE_DISABLE, PASS_THROUGH_ENABLE, PASS_THROUGH_DISABLE,
            NODE_LOCK, NODE_UNLOCK,
            // Layers
            NODE_RAISE, NODE_LOWER, NODE_SEND_TO_FOREGROUND, NODE_SEND_TO_BACKGROUND,
            // Running
            NODE_RUN_SHOW_RESULTS, NODE_UPDATE_CACHE,
            NODE_CACHE_INTERMEDIATE_RESULTS, NODE_UPDATE_PREDECESSOR_CACHE,
            // Navigation
            NODE_MOVE_UI_LEFT, NODE_MOVE_UI_RIGHT, NODE_MOVE_UI_UP, NODE_MOVE_UI_DOWN,
    };

    /**
     * Registers every core shortcut action in the application registry.
     * <p>
     * Idempotent: an id that is already registered (by a plugin, by an earlier
     * call, or by a test) is skipped instead of throwing, so this method can be
     * called unconditionally at startup and on every re-registration.
     */
    public static void registerCore() {
        JIPipeShortcutRegistry registry = JIPipeShortcutRegistry.getInstance();
        for (JIPipeShortcutAction action : CORE_ACTIONS) {
            if (!registry.isRegistered(action.id())) {
                registry.register(action);
            }
        }
    }

    /**
     * Registers one additional context action in the application registry.
     * <p>
     * The core catalog is static, but the concrete context-action classes own
     * their ids (as {@code SHORTCUT_ACTION_ID} constants). This group lets the
     * UI layer register an action whose identity and binding are defined next
     * to the action class itself, reusing the same idempotent semantics as
     * {@link #registerCore()}.
     *
     * @param action the action to register; an id that is already registered
     *               is skipped
     */
    public static void registerContextAction(JIPipeShortcutAction action) {
        if (action == null) {
            return;
        }
        JIPipeShortcutRegistry registry = JIPipeShortcutRegistry.getInstance();
        if (!registry.isRegistered(action.id())) {
            registry.register(action);
        }
    }

    /**
     * The frozen LEGACY preset override map, keyed by action id.
     * <p>
     * Restores every pre-6.0 binding (design Section 8): the bare F-key tools,
     * the F-key notes tools, ESC-cycled notes edit, numpad-only zoom, the
     * {@code L}/{@code Ctrl+L} lock pair, the
     * {@code Ctrl+P}/{@code Ctrl+Shift+P} pass-through pair, the keys
     * freed by the chord-layer revision (the enable/disable pair, add
     * parameter set, move node here, and update cache), and the Ctrl+Enter
     * binding of run & show results (unassigned since the revision). Only the
     * actions whose default changed carry an override; everything else stays
     * on its registry default.
     */
    static Map<String, List<List<KeyStroke>>> legacyOverrides() {
        Map<String, List<List<KeyStroke>>> legacy = new LinkedHashMap<>();
        // Tools: the pre-6.0 bare F keys.
        legacy.put(TOOL_MOVE.id(), List.of(List.of(stroke("pressed F1"))));
        legacy.put(TOOL_CONNECT.id(), List.of(List.of(stroke("pressed F2"))));
        legacy.put(TOOL_REWIRE.id(), List.of(List.of(stroke("pressed F3"))));
        legacy.put(TOOL_CROP_VIEW.id(), List.of(List.of(stroke("pressed F12"))));
        // Notes tools (registered by the canvasnotes plugin, not here): the
        // pre-6.0 bare F keys, referenced by their stable ids.
        legacy.put("org.hkijena.jipipe.canvasnotes.tool.textbox", List.of(List.of(stroke("pressed F4"))));
        legacy.put("org.hkijena.jipipe.canvasnotes.tool.arrow", List.of(List.of(stroke("pressed F6"))));
        legacy.put("org.hkijena.jipipe.canvasnotes.tool.imagebox", List.of(List.of(stroke("pressed F7"))));
        // Notes edit: pre-6.0 ESC, cycled together with the default tool.
        legacy.put("org.hkijena.jipipe.canvasnotes.tool.notes-edit", List.of(List.of(stroke("pressed ESCAPE"))));
        // Zoom: pre-6.0 numpad-only (no Ctrl+= / Ctrl+- / Ctrl+0 variants).
        legacy.put(EDITOR_ZOOM_IN.id(), List.of(List.of(stroke("control pressed ADD"))));
        legacy.put(EDITOR_ZOOM_OUT.id(), List.of(List.of(stroke("control pressed SUBTRACT"))));
        legacy.put(EDITOR_ZOOM_RESET.id(), List.of(List.of(stroke("control pressed NUMPAD0"))));
        // Locking: pre-6.0, the weaker action held the bare key.
        legacy.put(NODE_LOCK.id(), List.of(List.of(stroke("pressed L"))));
        legacy.put(NODE_UNLOCK.id(), List.of(List.of(stroke("control pressed L"))));
        // Pass-through: pre-6.0, Ctrl+P disabled / Ctrl+Shift+P enabled.
        legacy.put(PASS_THROUGH_DISABLE.id(), List.of(List.of(stroke("control pressed P"))));
        legacy.put(PASS_THROUGH_ENABLE.id(), List.of(List.of(stroke("control shift pressed P"))));
        // Chord layer: pre-6.0, these actions held the direct keys that the
        // revision moved under the Ctrl+T chord.
        legacy.put(NODE_ENABLE.id(), List.of(List.of(stroke("control pressed E"))));
        legacy.put(NODE_DISABLE.id(), List.of(List.of(stroke("control shift pressed E"))));
        legacy.put(NODE_ADD_PARAMETER_SET.id(), List.of(List.of(stroke("control shift pressed A"))));
        legacy.put(NODE_SELECT_AND_MOVE_HERE.id(), List.of(List.of(stroke("control pressed G"))));
        legacy.put(NODE_UPDATE_CACHE.id(), List.of(List.of(stroke("control pressed SPACE"))));
        // Run & show results: pre-6.0 held Ctrl+Enter; the revision unassigned it.
        legacy.put(NODE_RUN_SHOW_RESULTS.id(), List.of(List.of(stroke("control pressed ENTER"))));
        return legacy;
    }
}
