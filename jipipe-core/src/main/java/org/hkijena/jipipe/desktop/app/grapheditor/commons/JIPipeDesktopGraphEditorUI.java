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

package org.hkijena.jipipe.desktop.app.grapheditor.commons;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.grapheditortool.JIPipeDesktopActionGraphEditorTool;
import org.hkijena.jipipe.api.grapheditortool.JIPipeDesktopGraphEditorTool;
import org.hkijena.jipipe.api.grapheditortool.JIPipeDesktopToggleableGraphEditorTool;
import org.hkijena.jipipe.api.grapheditortool.tools.ConnectGraphEditorTool;
import org.hkijena.jipipe.api.grapheditortool.tools.CropViewGraphEditorTool;
import org.hkijena.jipipe.api.grapheditortool.tools.DefaultGraphEditorTool;
import org.hkijena.jipipe.api.grapheditortool.tools.MoveNodesGraphEditorTool;
import org.hkijena.jipipe.api.grapheditortool.tools.RewireGraphEditorTool;
import org.hkijena.jipipe.api.history.JIPipeHistoryJournal;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.taxonomy.TaxonomyView;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.settings.JIPipeSettingKey;
import org.hkijena.jipipe.api.settings.SettingsBinding;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutActions;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutBinding;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutRegistry;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutSettings;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasGrid;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.events.JIPipeDesktopGraphCanvasUINodeSelectedEvent;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.events.JIPipeDesktopGraphCanvasUINodeSelectedEventListener;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.events.JIPipeDesktopGraphCanvasUINodeSelectionChangedEvent;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.events.JIPipeDesktopGraphCanvasUINodeSelectionChangedEventListener;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.managers.JIPipeDesktopGraphCanvasSelectionManager;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.GraphInteractiveObjectUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.events.DefaultNodeUIActionRequestedEvent;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.events.DefaultNodeUIActionRequestedEventListener;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.events.NodeUIActionRequestedEvent;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.events.NodeUIActionRequestedEventListener;
import org.hkijena.jipipe.desktop.app.grapheditor.contextpanel.JIPipeDesktopGraphEditorContextPanel;
import org.hkijena.jipipe.desktop.app.shortcuts.JIPipeDesktopShortcutDispatcher;
import org.hkijena.jipipe.desktop.app.shortcuts.ShortcutScope;
import org.hkijena.jipipe.desktop.commons.components.icons.SolidColorIcon;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeDesktopGenericListCellRenderer;
import org.hkijena.jipipe.desktop.commons.dialogs.JIPipeDesktopDialogs;
import org.hkijena.jipipe.plugins.canvasnotes.tools.ArrowNoteGraphNodeTool;
import org.hkijena.jipipe.plugins.canvasnotes.tools.EditNotesGraphNodeTool;
import org.hkijena.jipipe.plugins.canvasnotes.tools.ImageBoxNoteGraphNodeTool;
import org.hkijena.jipipe.plugins.canvasnotes.tools.TextBoxNoteGraphNodeTool;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameter;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameterList;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeGraphEditorUIApplicationSettings;
import org.hkijena.jipipe.plugins.settings.SettingsKeys;
import org.hkijena.jipipe.utils.*;
import org.hkijena.jipipe.utils.json.JIPipePathMetadataStore;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.hkijena.jipipe.utils.ui.CopyImageToClipboard;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;
import org.hkijena.jipipe.utils.ui.ListSelectionMode;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jfree.graphics2d.svg.SVGUtils;
import org.scijava.Disposable;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/**
 * A panel around {@link JIPipeDesktopGraphCanvasUI} that comes with scrolling/panning, properties panel,
 * and a menu bar
 */
public abstract class JIPipeDesktopGraphEditorUI extends JIPipeDesktopWorkbenchPanel implements MouseListener, MouseMotionListener, Disposable, JIPipeGraph.GraphChangedEventListener,
        JIPipeDesktopGraphCanvasUINodeSelectionChangedEventListener,
        JIPipeDesktopGraphCanvasUINodeSelectedEventListener,
        DefaultNodeUIActionRequestedEventListener,
        NodeUIActionRequestedEventListener,
        JIPipeDesktopDockPanel.StateSavedEventListener, JIPipeDesktopDockPanel.PanelSideVisibilityChangedEventListener {

    public static final String DOCK_LOG = "LOG_v2";
    public static final String DOCK_HISTORY = "HISTORY";
    public static final String DOCK_ERRORS = "ERRORS";
    public static final String DOCK_CALCULATOR = "CALCULATOR";
    public static final String DOCK_CONTEXT_PANEL = "CONTEXT_PANEL";
    public static final String DOCK_FIND = "FIND";
    public static final String DOCK_RELATIONS = "RELATIONS";

    /**
     * Maps the graph editor tool classes to the ids of the shortcut actions
     * that select them: the five core tools and the four canvas notes tools
     * (registered by the canvasnotes plugin).
     */
    private static final Map<Class<? extends JIPipeDesktopGraphEditorTool>, String> TOOL_SHORTCUT_IDS = Map.of(
            DefaultGraphEditorTool.class, JIPipeShortcutActions.TOOL_DEFAULT.id(),
            MoveNodesGraphEditorTool.class, JIPipeShortcutActions.TOOL_MOVE.id(),
            ConnectGraphEditorTool.class, JIPipeShortcutActions.TOOL_CONNECT.id(),
            RewireGraphEditorTool.class, JIPipeShortcutActions.TOOL_REWIRE.id(),
            CropViewGraphEditorTool.class, JIPipeShortcutActions.TOOL_CROP_VIEW.id(),
            EditNotesGraphNodeTool.class, EditNotesGraphNodeTool.SHORTCUT_ACTION_ID,
            TextBoxNoteGraphNodeTool.class, TextBoxNoteGraphNodeTool.SHORTCUT_ACTION_ID,
            ArrowNoteGraphNodeTool.class, ArrowNoteGraphNodeTool.SHORTCUT_ACTION_ID,
            ImageBoxNoteGraphNodeTool.class, ImageBoxNoteGraphNodeTool.SHORTCUT_ACTION_ID);

    private final JIPipeGraphEditorUIApplicationSettings graphUISettings;
    private final JIPipeDesktopGraphCanvasUI canvasUI;
    private final JIPipeGraph graph;
    private final JIPipeHistoryJournal historyJournal;
    private final List<JIPipeDesktopGraphEditorTool> tools = new ArrayList<>();
    private final Map<Class<? extends JIPipeDesktopGraphEditorTool>, JIPipeDesktopGraphEditorTool> toolMap = new HashMap<>();
    private final BiMap<JIPipeDesktopToggleableGraphEditorTool, JToggleButton> toolToggles = HashBiMap.create();
    private final JToolBar toolBar = new JToolBar();
    private final JIPipeDesktopDockPanel dockPanel = new JIPipeDesktopDockPanel();
    private final List<JButton> contextToolbarButtons = new ArrayList<>();
    private final JIPipeDesktopGraphEditorContextPanel contextPanel = new JIPipeDesktopGraphEditorContextPanel(this);
    /**
     * The live binding to {@link SettingsKeys#SHOW_DOCK_TOOLBAR_LABELS}. Bound
     * once with this editor, subscribed for live updates and written through
     * when the dock panel's own toggle changes. Released in {@link #dispose()}
     * - a binding registers a document path listener, so its owner must close
     * it when the owner dies.
     */
    private SettingsBinding<Boolean> showDockToolbarLabelsBinding;
    /**
     * The dock panel's "show toolbar labels" toggle listener, held as a field
     * so {@link #dispose()} can unsubscribe it. (A subscription made with
     * {@code subscribeLambda} cannot be removed: the emitter's
     * {@code LambdaSubscriber} never matches a listener.)
     */
    private final JIPipeParameterCollection.ParameterChangedEventListener
            showDockToolbarLabelsParameterListener = this::onDockPanelParameterChanged;
    /**
     * The single live binding to {@link SettingsKeys#DOCK_LAYOUT_TEMPLATES},
     * shared by the read ({@link #getDockStateTemplates()}) and the write
     * ({@link #saveDockStateTemplates()}) path of both graph editor flavors,
     * created lazily and released in {@link #dispose()}. Binding per action
     * instead would leak one document listener per "Save layout" click.
     */
    private SettingsBinding<StringAndStringPairParameterList> dockLayoutTemplatesBinding;
    /**
     * The shortcut scope of this editor, registered with the application
     * shortcut dispatcher in {@link #initializeShortcutScope()} and
     * unregistered in {@link #dispose()}. One scope per editor instance: the
     * flavors inherit the scope from this base class and do not register
     * their own.
     */
    private ShortcutScope shortcutScope;
    private JScrollPane scrollPane;
    /**
     * The floating map panel over the canvas, placed on the overlay's drag
     * layer. Created in {@link #initialize()} before the toolbar, so the
     * map toggle can bind to it.
     */
    private JIPipeDesktopFloatingMapPanel floatingMapPanel;
    /**
     * The toolbar toggle that shows or hides {@link #floatingMapPanel}.
     */
    private JToggleButton mapToggle;
    /**
     * The live binding to {@link #getMapStateKey()}. Created lazily (the
     * editor may be built before the settings service exists) and closed in
     * {@link #closeDockStateBinding()} when the editor is disposed. A
     * binding registers a document path listener, so its owner must close it
     * when the owner dies.
     */
    private SettingsBinding<JIPipeDesktopMapState> mapStateBinding;
    private Point panningOffset = null;
    private Point panningScrollbarOffset = null;
    private boolean isPanning = false;
    /** Whether {@link #dispose()} already ran (it is idempotent). */
    private boolean disposed;
    private Set<JIPipeNodeInfo> addableAlgorithms = new HashSet<>();
    private JIPipeDesktopToggleableGraphEditorTool currentTool;
    private int contextToolbarInsertLocation;

    /**
     * @param workbenchUI    the workbench
     * @param graph          the algorithm graph
     * @param compartment    the graph compartment to display. Set to null to display all compartments
     * @param historyJournal object that tracks the history of this graph. Set to null to disable the undo feature.
     */
    public JIPipeDesktopGraphEditorUI(JIPipeDesktopWorkbench workbenchUI, JIPipeGraph graph, UUID compartment, JIPipeHistoryJournal historyJournal, JIPipeGraphEditorUIApplicationSettings settings) {
        super(workbenchUI);
        this.graph = graph;
        this.historyJournal = historyJournal;
        this.canvasUI = new JIPipeDesktopGraphCanvasUI(getDesktopWorkbench(), this, graph, compartment, historyJournal);
        this.graphUISettings = settings;

        initialize();
        graph.getGraphChangedEventEmitter().subscribeWeak(this);

        initializeShortcutScope();
        SwingUtilities.invokeLater(() -> {
            canvasUI.crop(true);
            selectDefaultTool();
        });

        // Always show right panel
        dockPanel.setAlwaysShowRightPanel(true);
        restoreDockStateFromSettings();
        initializeMapStateFromSettings();

        dockPanel.getPanelSideVisibilityChangedEventEmitter().subscribe(this);
        rebuildContextPanel();
    }

    /**
     * @param workbenchUI    the workbench
     * @param graph          the algorithm graph
     * @param compartment    the graph compartment to display. Set to null to display all compartments
     * @param historyJournal object that tracks the history of this graph. Set to null to disable the undo feature.
     */
    public JIPipeDesktopGraphEditorUI(JIPipeDesktopWorkbench workbenchUI, JIPipeGraph graph, UUID compartment, JIPipeHistoryJournal historyJournal) {
        this(workbenchUI, graph, compartment, historyJournal, JIPipeGraphEditorUIApplicationSettings.getInstance());
    }

    protected abstract void restoreDockStateFromSettings();

    protected abstract void saveDockStateToSettings();

    /**
     * @return the settings key that stores this flavor's floating map state
     * (the counterpart of the flavor's dock state). A flavor that does not
     * persist its map (see
     * {@link org.hkijena.jipipe.desktop.app.compat.JIPipeDesktopRunSingleAlgorithmSettingsPanelIOEditor})
     * returns null and keeps {@link #initializeMapStateFromSettings()} and
     * {@link #saveMapStateToSettings()} inert, so the key is never read or
     * written.
     */
    protected abstract JIPipeSettingKey<JIPipeDesktopMapState> getMapStateKey();

    /**
     * Connects the floating map panel to {@link #getMapStateKey()}, using the
     * same binding pattern as the flavor dock states: the stored value is
     * applied to the panel, every change made by another view (a second
     * graph editor, the settings UI, a merged foreign write) is re-applied
     * live, and the panel's own changes are written through by
     * {@link #saveMapStateToSettings()}.
     */
    protected void initializeMapStateFromSettings() {
        if (!JIPipe.isInstantiated()) {
            return;
        }
        mapStateBinding = JIPipe.getSettingsService().bind(getMapStateKey());
        applyMapState(mapStateBinding.get());
        mapStateBinding.subscribe(this::applyMapState);
    }

    /**
     * Applies a map state to the floating map panel and syncs the toolbar
     * toggle. The panel itself ignores states equal to the current one, so
     * the echo of our own write is inert.
     *
     * @param state the (new) map state
     */
    private void applyMapState(JIPipeDesktopMapState state) {
        floatingMapPanel.applyState(state);
        mapToggle.setSelected(state.isVisible());
    }

    /**
     * Writes the floating map panel's state through to
     * {@link #getMapStateKey()}. Called after every user change of the map
     * (move, resize, lock, layers, visibility), never per mouse move.
     */
    protected void saveMapStateToSettings() {
        if (JIPipe.isInstantiated()) {
            if (mapStateBinding == null) {
                mapStateBinding = JIPipe.getSettingsService().bind(getMapStateKey());
            }
            mapStateBinding.set(floatingMapPanel.getState());
        }
    }

    /**
     * Shows or hides the floating map: the shared path of the toolbar toggle
     * and the map header's hide button (the map panel invokes this method).
     *
     * @param visible whether the map is shown
     */
    void setMapVisible(boolean visible) {
        floatingMapPanel.setVisibleInCanvas(visible);
        mapToggle.setSelected(visible);
        saveMapStateToSettings();
    }

    @Override
    public void dispose() {
        // Idempotent: an editor tab is disposed when the user closes the tab
        // (forceCloseTab) and again when the project window closes (the tab
        // pane's teardown). The bindings' close() is idempotent, but the
        // unsubscribe/canvas work must not repeat.
        if (disposed) {
            return;
        }
        disposed = true;
        graph.getGraphChangedEventEmitter().unsubscribe(this);
        dockPanel.getParameterChangedEventEmitter().unsubscribe(showDockToolbarLabelsParameterListener);
        // The scope must not outlive the editor: a disposed scope would
        // still resolve runnables against the dead canvas
        if (shortcutScope != null) {
            JIPipeDesktopShortcutDispatcher.getInstance().unregisterScope(shortcutScope);
            shortcutScope = null;
        }
        // Each binding owns a document path listener: release the ones this
        // editor created, so opening and closing editors does not accumulate
        // listeners for the life of the JVM.
        if (showDockToolbarLabelsBinding != null) {
            showDockToolbarLabelsBinding.close();
            showDockToolbarLabelsBinding = null;
        }
        if (dockLayoutTemplatesBinding != null) {
            dockLayoutTemplatesBinding.close();
            dockLayoutTemplatesBinding = null;
        }
        closeDockStateBinding();
        canvasUI.dispose();
    }

    /**
     * Releases the base-owned map-state binding, then lets the flavor
     * release its own dock-state binding. Overridden by every flavor that
     * binds a dock state: the override must call
     * {@code super.closeDockStateBinding()} (the map binding is owned by
     * this base class) and close its own dock-state binding field.
     */
    protected void closeDockStateBinding() {
        if (mapStateBinding != null) {
            mapStateBinding.close();
            mapStateBinding = null;
        }
    }

    @Override
    public void onDockPanelStateSaved(JIPipeDesktopDockPanel.StateSavedEvent event) {
        saveDockStateToSettings();
    }

    public JIPipeGraphEditorUIApplicationSettings getGraphUISettings() {
        return graphUISettings;
    }

    /**
     * Registers the editor's shortcut scope with the application-wide
     * shortcut dispatcher (which is installed there as well). The resolver
     * answers, for a shortcut action id:
     * <ul>
     *     <li>context action ids: the canvas context action with this id,
     *     only when it matches the current selection (mirroring the old
     *     per-canvas hotkey dispatcher)</li>
     *     <li>tool ids: selecting the mapped graph editor tool</li>
     *     <li>editor-level ids: undo, redo, zoom, and selection moves</li>
     * </ul>
     * Anything else resolves to null, so the dispatcher falls through to the
     * next scope.
     */
    private void initializeShortcutScope() {
        shortcutScope = new ShortcutScope(this, actionId -> {
            // Context actions: the first action carrying the id must match
            // the current selection, otherwise the action is unavailable
            Set<JIPipeDesktopGraphInteractiveObjectUI> selection = canvasUI.getSelectionManager().getSelection();
            for (GraphInteractiveObjectUIContextAction action : canvasUI.getContextActions()) {
                if (action == null || !actionId.equals(action.getShortcutActionId()))
                    continue;
                if (!action.matches(selection))
                    return null;
                return () -> {
                    // Mirror the old canvas hotkey dispatcher: place the
                    // graph edit cursor at the mouse position, so
                    // cursor-relative actions ("add node here") act there
                    Point mousePosition = canvasUI.getMousePosition(true);
                    if (mousePosition != null) {
                        canvasUI.setGraphEditCursor(mousePosition);
                    }
                    action.run(canvasUI, selection);
                };
            }
            // Tools: selecting the mapped graph editor tool. Pressing the
            // chord of the already-active tool switches back to the default
            // tool (the successor of the old keybinding cycling: today
            // every tool has its own chord, so a chord never selects
            // between several tools)
            for (Map.Entry<Class<? extends JIPipeDesktopGraphEditorTool>, String> toolEntry : TOOL_SHORTCUT_IDS.entrySet()) {
                if (toolEntry.getValue().equals(actionId)) {
                    JIPipeDesktopGraphEditorTool tool = toolMap.get(toolEntry.getKey());
                    if (tool == null) {
                        return null;
                    }
                    return () -> {
                        if (tool == currentTool) {
                            selectDefaultTool();
                        } else {
                            selectTool(tool);
                        }
                    };
                }
            }
            // Editor-level actions
            if (JIPipeShortcutActions.EDITOR_UNDO.id().equals(actionId)) {
                return this::undo;
            } else if (JIPipeShortcutActions.EDITOR_REDO.id().equals(actionId)) {
                return this::redo;
            } else if (JIPipeShortcutActions.EDITOR_ZOOM_IN.id().equals(actionId)) {
                return canvasUI::zoomIn;
            } else if (JIPipeShortcutActions.EDITOR_ZOOM_OUT.id().equals(actionId)) {
                return canvasUI::zoomOut;
            } else if (JIPipeShortcutActions.EDITOR_ZOOM_RESET.id().equals(actionId)) {
                return canvasUI::resetZoom;
            } else if (JIPipeShortcutActions.EDITOR_MOVE_SELECTION_LEFT.id().equals(actionId)) {
                return () -> canvasUI.moveSelection(-1, 0, false);
            } else if (JIPipeShortcutActions.EDITOR_MOVE_SELECTION_RIGHT.id().equals(actionId)) {
                return () -> canvasUI.moveSelection(1, 0, false);
            } else if (JIPipeShortcutActions.EDITOR_MOVE_SELECTION_UP.id().equals(actionId)) {
                return () -> canvasUI.moveSelection(0, -1, false);
            } else if (JIPipeShortcutActions.EDITOR_MOVE_SELECTION_DOWN.id().equals(actionId)) {
                return () -> canvasUI.moveSelection(0, 1, false);
            } else {
                return null;
            }
        });
        JIPipeDesktopShortcutDispatcher.getInstance().registerScope(shortcutScope);
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    public JIPipeDesktopGraphCanvasUI getCanvasUI() {
        return canvasUI;
    }

    public JIPipeDesktopFloatingMapPanel getFloatingMapPanel() {
        return floatingMapPanel;
    }

    private void initialize() {
        // The application-wide shortcut dispatcher: installed once (idempotent),
        // before any scope registers itself
        JIPipeDesktopShortcutDispatcher.install();
        setLayout(new BorderLayout());

        toolBar.setFloatable(false);

        canvasUI.updateInteractiveObjects(true);
        canvasUI.getSelectionManager().getNodeUISelectedEventEmitter().subscribe(this);
        canvasUI.getSelectionManager().getNodeSelectionChangedEventEmitter().subscribe(this);
        canvasUI.getDefaultAlgorithmUIActionRequestedEventEmitter().subscribe(this);
        canvasUI.getNodeUIActionRequestedEventEmitter().subscribe(this);
        canvasUI.addMouseListener(this);
        canvasUI.addMouseMotionListener(this);
        scrollPane = new JScrollPane(canvasUI);
        scrollPane.getVerticalScrollBar().setUnitIncrement(25);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(25);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JLayeredPane mapOverlay = new JLayeredPane();
        mapOverlay.add(scrollPane, JLayeredPane.DEFAULT_LAYER);
        floatingMapPanel = new JIPipeDesktopFloatingMapPanel(this);
        mapOverlay.add(floatingMapPanel, JLayeredPane.DRAG_LAYER);
        // A layered pane has no layout manager: keep the scroll pane
        // covering the overlay at all times
        mapOverlay.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                scrollPane.setBounds(0, 0, mapOverlay.getWidth(), mapOverlay.getHeight());
                // Manual bounds trigger no layout pass: revalidate and repaint
                // explicitly so the viewport follows the grown scroll pane
                scrollPane.revalidate();
                scrollPane.repaint();
                // The canvas preferred size is content-driven: it does not
                // follow the overlay when the content exceeds the viewport,
                // so the map re-clamps against the resized overlay here
                floatingMapPanel.reclampToOverlay();
            }
        });
        JPanel mainPanel = new JPanel(new BorderLayout());
        // The toolbar stays outside the overlay, so the floating map can
        // never cover it; the overlay wraps only the scroll pane
        mainPanel.add(mapOverlay, BorderLayout.CENTER);
        mainPanel.add(toolBar, BorderLayout.NORTH);

        dockPanel.setMainComponent(mainPanel);
        dockPanel.getStateSavedEventEmitter().subscribe(this);
        initializeShowDockToolbarLabels();
        dockPanel.addDockPanel(DOCK_CONTEXT_PANEL,
                "Tools",
                JIPipe.RESOURCES.getIcon24("actions/tools-wizard.png"),
                JIPipeDesktopDockPanel.PanelLocation.TopRight,
                false,
                -100,
                contextPanel);
        dockPanel.setFallbackRight(DOCK_CONTEXT_PANEL);
        dockPanel.setFallbackLeft(DOCK_CONTEXT_PANEL);

        add(dockPanel, BorderLayout.CENTER);

        initializeEditingToolbar(JIPipeDesktopToggleableGraphEditorTool.class);
        toolBar.add(UIUtils.createVerticalSeparator());
        contextToolbarInsertLocation = toolBar.getComponentCount();
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(UIUtils.createVerticalSeparator());
        initializeEditingToolbar(JIPipeDesktopActionGraphEditorTool.class);
        toolBar.addSeparator();
        initializeCommonToolbar();
    }

    /**
     * Connects the dock panel's "show toolbar labels" toggle to
     * {@link SettingsKeys#SHOW_DOCK_TOOLBAR_LABELS}.
     * <p>
     * The binding is the single source of truth: the stored value is applied
     * to the panel at construction, every change made by any other view (a
     * second graph editor, the settings UI, a merged foreign write) is
     * re-applied live, and the panel's own toggle is written through. The
     * echo of an applied value is inert: the binding never reports a value
     * equal to the current one, and applying compares before it touches the
     * panel.
     */
    private void initializeShowDockToolbarLabels() {
        if (!JIPipe.isInstantiated()) {
            return;
        }
        showDockToolbarLabelsBinding = JIPipe.getSettingsService().bind(SettingsKeys.SHOW_DOCK_TOOLBAR_LABELS);
        applyShowDockToolbarLabels(showDockToolbarLabelsBinding.get());
        showDockToolbarLabelsBinding.subscribe(this::applyShowDockToolbarLabels);
        dockPanel.getParameterChangedEventEmitter().subscribe(showDockToolbarLabelsParameterListener);
    }

    /**
     * Writes the dock panel's own toggle state through the binding. A stable
     * field (not a lambda) so {@link #dispose()} can unsubscribe it.
     *
     * @param event the parameter change event of the dock panel
     */
    private void onDockPanelParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if ("show-toolbar-labels".equals(event.getKey()) && showDockToolbarLabelsBinding != null) {
            showDockToolbarLabelsBinding.set(dockPanel.isShowToolbarLabels());
        }
    }

    /**
     * @param showLabels the stored value; applied to the dock panel only when
     *                   it differs from what the panel shows
     */
    private void applyShowDockToolbarLabels(boolean showLabels) {
        if (dockPanel.isShowToolbarLabels() != showLabels) {
            dockPanel.setShowToolbarLabels(showLabels);
        }
    }

    private void initializeCommonToolbar() {
        if (getHistoryJournal() != null) {
            JButton undoButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/undo.png"));
            undoButton.setToolTipText("<html>Undo<br><i>Ctrl-Z</i></html>");
            UIUtils.makeButtonFlatWithSize(undoButton, 32, 3);
            undoButton.addActionListener(e -> undo());
            toolBar.add(undoButton);

            JButton redoButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/edit-redo.png"));
            redoButton.setToolTipText("<html>Redo<br><i>Ctrl-Shift-Z</i></html>");
            UIUtils.makeButtonFlatWithSize(redoButton, 32, 3);
            redoButton.addActionListener(e -> redo());
            toolBar.add(redoButton);

            toolBar.addSeparator();
        }
        initializeCommonToolbarLayout();
        initializeCommonToolbarExport();
        toolBar.addSeparator();

        mapToggle = new JToggleButton(JIPipe.RESOURCES.getIcon16("actions/map.png"));
        mapToggle.setToolTipText("Show or hide the floating map");
        UIUtils.makeButtonFlatWithSize(mapToggle, 32, 3);
        mapToggle.addActionListener(e -> setMapVisible(mapToggle.isSelected()));
        toolBar.add(mapToggle);

        initializeCommonToolbarZoom();

        toolBar.add(Box.createHorizontalStrut(8));
    }

    private void initializeCommonToolbarLayout() {

        JPopupMenu layoutMenu = new JPopupMenu();

        JButton layoutButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/sidebar.png"));
        UIUtils.makeButtonFlatWithSize(layoutButton, 32, 3);
        toolBar.add(layoutButton);

        UIUtils.addReloadablePopupMenuToButton(layoutButton, layoutMenu, () -> {
            layoutMenu.removeAll();

            layoutMenu.add(UIUtils.createMenuItem("Reset layout", "Resets the layout", JIPipe.RESOURCES.getIcon16("actions/edit-clear-history.png"), () -> {
                restoreDefaultDockState();
                saveDockStateToSettings();
            }));

            if (getDockStateTemplates() != null) {

                for (StringAndStringPairParameter template : getDockStateTemplates()) {
                    layoutMenu.add(UIUtils.createMenuItem(template.getKey(), "Loads the layout", JIPipe.RESOURCES.getIcon16("actions/sidebar.png"), () -> restoreDockStateTemplate(template.getValue())));
                }

                layoutMenu.addSeparator();

                layoutMenu.add(UIUtils.createMenuItem("Save layout ...", "Saves the current layout", JIPipe.RESOURCES.getIcon16("actions/filesave.png"), this::createDockStateTemplate));
                layoutMenu.add(UIUtils.createMenuItem("Manage layouts ...", "Manages the list of layouts", JIPipe.RESOURCES.getIcon16("actions/configure.png"), this::manageDockStateTemplates));
            }

            layoutMenu.addSeparator();

            JMenuItem autoLayoutItem = new JMenuItem("Auto-layout all nodes", JIPipe.RESOURCES.getIcon16("actions/distribute-unclump.png"));
            autoLayoutItem.addActionListener(e -> {
                if (getHistoryJournal() != null) {
                    getHistoryJournal().snapshot("Auto-layout", "Apply auto-layout", getCompartment(), JIPipe.RESOURCES.getIcon16("actions/distribute-unclump.png"));
                }
                canvasUI.autoLayoutAll();
            });
            layoutMenu.add(autoLayoutItem);

            JCheckBoxMenuItem layoutOnConnectItem = new JCheckBoxMenuItem("Layout nodes on connect",
                    JIPipe.RESOURCES.getIcon16("actions/connector-avoid.png"),
                    JIPipeGraphEditorUIApplicationSettings.getInstance().isLayoutAfterConnect());
            layoutOnConnectItem.setToolTipText("Auto-layout layout on making data slot connections");
            layoutOnConnectItem.addActionListener(e -> {
                JIPipeGraphEditorUIApplicationSettings.getInstance().setLayoutAfterConnect(layoutOnConnectItem.isSelected());
            });

            layoutMenu.add(layoutOnConnectItem);

            JCheckBoxMenuItem layoutOnAlgorithmFinderItem = new JCheckBoxMenuItem("Layout nodes on 'Find matching node'",
                    JIPipe.RESOURCES.getIcon16("actions/connector-avoid.png"),
                    JIPipeGraphEditorUIApplicationSettings.getInstance().isLayoutAfterAlgorithmFinder());
            layoutOnAlgorithmFinderItem.setToolTipText("Auto-layout layout on utilizing the 'Find matching node' feature");
            layoutOnAlgorithmFinderItem.addActionListener(e -> {
                JIPipeGraphEditorUIApplicationSettings.getInstance().setLayoutAfterAlgorithmFinder(layoutOnAlgorithmFinderItem.isSelected());
            });
            layoutMenu.add(layoutOnAlgorithmFinderItem);
        });

    }

    private void restoreDockStateTemplate(String value) {
        try {
            JIPipeDesktopDockPanel.State state = JsonUtils.readFromString(value, JIPipeDesktopDockPanel.State.class);
            getDockPanel().restoreState(state.withoutContextDockVisibilities());
            saveDockStateToSettings();
        } catch (Throwable e) {
            e.printStackTrace();
            JIPipeDesktopDialogs.message(this, "Load template", "Unable to restore template!");
        }
    }

    /**
     * The saved dock layout templates: the live value of
     * {@link SettingsKeys#DOCK_LAYOUT_TEMPLATES}, read through the one binding
     * this editor holds for that key (created on first use, closed in
     * {@link #dispose()}). Used by both graph editor flavors.
     *
     * @return the templates, or {@code null} when the settings service does
     * not exist yet
     */
    protected StringAndStringPairParameterList getDockStateTemplates() {
        if (JIPipe.isInstantiated()) {
            return dockLayoutTemplatesBinding().get();
        }
        return null;
    }

    /**
     * @return the editor's single template binding, created lazily (the
     * editor may be built before the settings service exists)
     */
    private SettingsBinding<StringAndStringPairParameterList> dockLayoutTemplatesBinding() {
        if (dockLayoutTemplatesBinding == null) {
            dockLayoutTemplatesBinding = JIPipe.getSettingsService().bind(SettingsKeys.DOCK_LAYOUT_TEMPLATES);
        }
        return dockLayoutTemplatesBinding;
    }

    private void manageDockStateTemplates() {
        List<StringAndStringPairParameter> toDelete = UIUtils.getSelectionByDialog(this,
                getDockStateTemplates(),
                Collections.emptyList(),
                "Delete layout templates",
                "Please select the layout to delete",
                new JIPipeDesktopGenericListCellRenderer<>(kv -> new JIPipeDesktopGenericListCellRenderer.RenderedItem(JIPipe.RESOURCES.getIcon16("actions/sidebar.png"), kv.getKey())),
                ListSelectionMode.MultipleInterval);
        getDockStateTemplates().removeAll(toDelete);
        saveDockStateTemplates();
    }

    private void createDockStateTemplate() {
        JIPipeDesktopDialogs.inputText(this, "Input", "Please input the name of the layout:", null)
                .map(String::trim)
                .ifPresent(newValue -> {
                    JIPipeDesktopDockPanel.State currentState = getDockPanel().getCurrentState().withoutContextDockVisibilities();
                    getDockStateTemplates().add(new StringAndStringPairParameter(newValue, JsonUtils.toJsonString(currentState)));
                    saveDockStateTemplates();
                });
    }

    /**
     * Writes the layout template list modified in place by
     * {@link #manageDockStateTemplates()}/{@link #createDockStateTemplate()} to
     * its setting. The list is the live value read from the setting, so it has
     * to go back through the editor's binding - an explicit settings save alone
     * does not see the in-place change. Reuses the binding created by the read
     * path: repeated saves never add a document listener.
     */
    private void saveDockStateTemplates() {
        StringAndStringPairParameterList templates = getDockStateTemplates();
        if (templates == null) {
            return;
        }
        dockLayoutTemplatesBinding().set(templates);
        JIPipe.getSettings().save();
    }

    protected abstract void restoreDefaultDockState();

    private void initializeCommonToolbarExport() {
        JButton snapshotButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/camera.png"));
        UIUtils.makeButtonFlatWithSize(snapshotButton, 32, 3);
        JPopupMenu popupMenu = UIUtils.addPopupMenuToButton(snapshotButton);

        JMenuItem exportToClipboardItem = new JMenuItem("Copy snapshot to clipboard", JIPipe.RESOURCES.getIcon16("actions/edit-copy.png"));
        exportToClipboardItem.addActionListener(e -> createScreenshotClipboard());
        popupMenu.add(exportToClipboardItem);
        JMenuItem exportAsPngItem = new JMenuItem("Export as *.png", JIPipe.RESOURCES.getIcon16("actions/viewimage.png"));
        exportAsPngItem.addActionListener(e -> createScreenshotPNG());
        popupMenu.add(exportAsPngItem);
        JMenuItem exportAsSvgItem = new JMenuItem("Export as *.svg", JIPipe.RESOURCES.getIcon16("actions/viewimage.png"));
        exportAsSvgItem.addActionListener(e -> createScreenshotSVG());
        popupMenu.add(exportAsSvgItem);

        toolBar.add(snapshotButton);
    }

    private void initializeCommonToolbarZoom() {

        JPanel zoomPanel = new JPanel(new BorderLayout());
        zoomPanel.setBorder(UIUtils.createControlBorder());
        zoomPanel.setPreferredSize(new Dimension(100, 32));
        zoomPanel.setMaximumSize(new Dimension(100, 32));

        JButton zoomOutButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/square-minus.png"));
        UIUtils.makeButtonFlat25x25(zoomOutButton);
        zoomOutButton.setToolTipText("<html>Zoom out<br><i>Ctrl-NumPad -</i></html>");
        zoomOutButton.addActionListener(e -> canvasUI.zoomOut());
        zoomPanel.add(zoomOutButton, BorderLayout.WEST);

        JButton zoomButton = new JButton((int) (canvasUI.getZoom() * 100) + "%");
        zoomButton.setToolTipText("<html>Change zoom<br>Reset zoom: <i>Ctrl-NumPad 0</i></html>");
        canvasUI.getZoomChangedEventEmitter().subscribeLambda((emitter, event) -> {
            zoomButton.setText((int) (canvasUI.getZoom() * 100) + "%");
        });
        zoomButton.setBorder(null);
        JPopupMenu zoomMenu = UIUtils.addPopupMenuToButton(zoomButton);
        for (double zoom = 0.1; zoom <= 3; zoom += 0.25) {
            JMenuItem changeZoomItem = new JMenuItem((int) (zoom * 100) + "%", JIPipe.RESOURCES.getIcon16("actions/zoom.png"));
            double finalZoom = zoom;
            changeZoomItem.addActionListener(e -> canvasUI.setZoom(finalZoom));
            zoomMenu.add(changeZoomItem);
        }
        zoomMenu.addSeparator();
        zoomMenu.add(UIUtils.createMenuItem("Reset zoom", "Resets the zoom to 100%",
                JIPipe.RESOURCES.getIcon16("actions/edit-reset.png"), () -> canvasUI.setZoom(1)));
        JMenuItem changeZoomToItem = new JMenuItem("Set zoom value ...");
        changeZoomToItem.addActionListener(e -> {
            // String input (not inputInteger): the value carries a "%" suffix
            JIPipeDesktopDialogs.inputText(this, "Input", "Please enter a new zoom value (in %)", (int) (canvasUI.getZoom() * 100) + "%")
                    .ifPresent(zoomInput -> {
                        String percentageString = zoomInput.replace("%", "");
                        try {
                            int percentage = Integer.parseInt(percentageString);
                            canvasUI.setZoom(percentage / 100.0);
                        } catch (NumberFormatException ex) {
                            ex.printStackTrace();
                        }
                    });
        });
        zoomMenu.add(changeZoomToItem);
        zoomPanel.add(zoomButton, BorderLayout.CENTER);

        JButton zoomInButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/square-plus.png"));
        UIUtils.makeButtonFlat25x25(zoomInButton);
        zoomInButton.setToolTipText("<html>Zoom in<br><i>Ctrl-NumPad +</i></html>");
        zoomInButton.addActionListener(e -> canvasUI.zoomIn());
        zoomPanel.add(zoomInButton, BorderLayout.EAST);

        toolBar.add(zoomPanel);
    }

    private void initializeEditingToolbar(Class<? extends JIPipeDesktopGraphEditorTool> baseClass) {
        List<JIPipeDesktopGraphEditorTool> newTools = new ArrayList<>();
        for (Class<? extends JIPipeDesktopGraphEditorTool> klass : JIPipe.getInstance().getGraphEditorTools().getRegisteredTools()) {
            if (baseClass.isAssignableFrom(klass) && !toolMap.containsKey(klass)) {
                JIPipeDesktopGraphEditorTool tool = (JIPipeDesktopGraphEditorTool) ReflectionUtils.newInstance(klass);
                if (tool.supports(this)) {
                    tool.setGraphEditor(this);
                    toolMap.put(klass, tool);
                    newTools.add(tool);
                }
            }
        }
        newTools.sort(Comparator.comparing(JIPipeDesktopGraphEditorTool::getCategory).thenComparing(JIPipeDesktopGraphEditorTool::getPriority));
        tools.addAll(newTools);

        for (int i = 0; i < newTools.size(); i++) {
            JIPipeDesktopGraphEditorTool tool = newTools.get(i);
            if (i > 0 && !Objects.equals(tool.getCategory(), newTools.get(i - 1).getCategory())) {
                toolBar.addSeparator();
            }

            if (tool instanceof JIPipeDesktopToggleableGraphEditorTool toggleableGraphEditorTool) {

                JToggleButton button = new JToggleButton(tool.getIcon());
                button.setToolTipText(TooltipUtils.createTooltipWithShortcutText(tool.getName(), tool.getTooltip(), shortcutVariantsText(TOOL_SHORTCUT_IDS.get(tool.getClass()))));
                button.addActionListener(e -> selectTool(tool));
                UIUtils.makeButtonFlatWithSize(button, 32, 3);
                toolBar.add(button);
                toolToggles.put(toggleableGraphEditorTool, button);

            } else {
                JButton button = new JButton(tool.getIcon());
                button.setToolTipText(TooltipUtils.createTooltipWithShortcutText(tool.getName(), tool.getTooltip(), shortcutVariantsText(TOOL_SHORTCUT_IDS.get(tool.getClass()))));
                button.addActionListener(e -> selectTool(tool));
                UIUtils.makeButtonFlatWithSize(button, 32, 3);
                toolBar.add(button);
            }

        }
    }

    public JIPipeDesktopDockPanel getDockPanel() {
        return dockPanel;
    }

    /**
     * Renders the effective variants of a shortcut action for display, e.g.
     * "Ctrl+= or Ctrl+Num+". Unassigned or unknown ids render as null, so
     * the tooltip shows no shortcut.
     *
     * @param actionId the shortcut action id, may be null
     * @return the rendered variants or null
     */
    private String shortcutVariantsText(String actionId) {
        if (actionId == null) {
            return null;
        }
        JIPipeShortcutBinding binding = JIPipeShortcutRegistry.getInstance().bindingFor(actionId, JIPipeShortcutSettings.current());
        if (binding.variants().isEmpty()) {
            return null;
        }
        return JIPipeShortcutRegistry.formatVariants(binding.variants());
    }

    public List<JIPipeDesktopGraphEditorTool> getTools() {
        return Collections.unmodifiableList(tools);
    }

    /**
     * @param toolClass the exact tool class
     * @return the tool instance of this editor that was registered for the
     * given class, or null when the class is not registered or the tool
     * does not support this editor
     */
    public <T extends JIPipeDesktopGraphEditorTool> T getTool(Class<T> toolClass) {
        return toolClass.cast(toolMap.get(toolClass));
    }

    public JIPipeDesktopGraphEditorTool getDefaultTool() {
        return tools.stream().filter(tool -> tool instanceof DefaultGraphEditorTool).findFirst().orElse(null);
    }

    public void selectDefaultTool() {
        selectTool(getDefaultTool());
    }

    public void selectTool(JIPipeDesktopGraphEditorTool tool) {
        if (tool == null) {
            tool = getDefaultTool();
        }
        if (tool instanceof JIPipeDesktopToggleableGraphEditorTool toggleableTool) {

            if (tool == currentTool) {
                JToggleButton toggleButton = toolToggles.get(currentTool);
                toggleButton.setSelected(true);
                return;
            }
            if (currentTool != null) {
                // Deselect current tool
                JToggleButton toggleButton = toolToggles.get(currentTool);
                toggleButton.setSelected(false);
                currentTool.deactivate();
                currentTool = null;
                canvasUI.getToolManager().setToDefaultTool();
            }

            JToggleButton toggleButton = toolToggles.get(toggleableTool);
            toggleButton.setSelected(true);
            currentTool = toggleableTool;

            canvasUI.getToolManager().setCurrentTool(currentTool);
            tool.activate();
            getDesktopWorkbench().sendStatusBarText("Activated tool '" + tool.getName() + "'");
        } else {
            tool.activate();
            getDesktopWorkbench().sendStatusBarText("Activated tool '" + tool.getName() + "'");
        }
        canvasUI.repaintLowLag();
        rebuildContextPanel();
    }

    public void rebuildContextPanel() {
        contextPanel.rebuild();
    }

    public JIPipeDesktopToggleableGraphEditorTool getCurrentTool() {
        return currentTool;
    }

    public JIPipeDesktopGraphCanvasSelectionManager getSelectionManager() {
        return canvasUI.getSelectionManager();
    }

    public void createScreenshotClipboard() {
        BufferedImage screenshot = canvasUI.createScreenshotPNG();
        CopyImageToClipboard copyImageToClipboard = new CopyImageToClipboard();
        copyImageToClipboard.copyImage(screenshot);
        getDesktopWorkbench().sendStatusBarText("Copied screenshot to clipboard");
    }

    private void redo() {
        if (getHistoryJournal() != null) {
            int scrollX = scrollPane.getHorizontalScrollBar().getValue();
            int scrollY = scrollPane.getVerticalScrollBar().getValue();
            if (getHistoryJournal().redo(getCompartment())) {
                getDesktopWorkbench().sendStatusBarText("Redo successful");
            } else {
                getDesktopWorkbench().sendStatusBarText("Redo unsuccessful");
            }
            SwingUtilities.invokeLater(() -> {
                scrollPane.getHorizontalScrollBar().setValue(scrollX);
                scrollPane.getVerticalScrollBar().setValue(scrollY);
            });
        }
    }

    private void undo() {
        if (getHistoryJournal() != null) {
            int scrollX = scrollPane.getHorizontalScrollBar().getValue();
            int scrollY = scrollPane.getVerticalScrollBar().getValue();
            if (getHistoryJournal().undo(getCompartment())) {
                getDesktopWorkbench().sendStatusBarText("Undo successful");
            } else {
                getDesktopWorkbench().sendStatusBarText("Undo unsuccessful");
            }
            SwingUtilities.invokeLater(() -> {
                scrollPane.getHorizontalScrollBar().setValue(scrollX);
                scrollPane.getVerticalScrollBar().setValue(scrollY);
            });
        }
    }

    /**
     * @return The edited graph
     */
    public JIPipeGraph getGraph() {
        return graph;
    }

    private void createScreenshotSVG() {
        SVGGraphics2D screenshot = canvasUI.createScreenshotSVG();
        Path selectedPath = JIPipeDesktop.saveFile(this, getDesktopWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Export graph as SVG (*.svg)", HTMLText.EMPTY, PathUtils.EXTENSION_FILTER_SVG);
        if (selectedPath != null) {
            try {
                SVGUtils.writeToSVG(selectedPath.toFile(), screenshot.getSVGElement());
                getDesktopWorkbench().sendStatusBarText("Exported graph as " + selectedPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void createScreenshotPNG() {
        BufferedImage screenshot = canvasUI.createScreenshotPNG();
        Path selectedPath = JIPipeDesktop.saveFile(this, getDesktopWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Export graph as PNG (*.png)", HTMLText.EMPTY, PathUtils.EXTENSION_FILTER_PNG);
        if (selectedPath != null) {
            try {
                ImageIO.write(screenshot, "PNG", selectedPath.toFile());
                getDesktopWorkbench().sendStatusBarText("Exported graph as " + selectedPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected void updateSelection() {
        updateContextToolbar();
    }

    public void updateContextToolbar() {
        for (JButton button : contextToolbarButtons) {
            toolBar.remove(button);
        }

        Set<JIPipeDesktopGraphInteractiveObjectUI> selection = getSelectionManager().getSelection();
        for (GraphInteractiveObjectUIContextAction contextAction : canvasUI.getContextActions()) {
            if (contextAction != null && contextAction.isDisplayedInToolbar() && contextAction.matches(selection)) {
                JButton button = new JButton(contextAction.getIcon());
                button.setToolTipText(TooltipUtils.createTooltipWithShortcutText(contextAction.getName(), contextAction.getDescription(), shortcutVariantsText(contextAction.getShortcutActionId())));
                button.addActionListener(e -> {
                    contextAction.run(canvasUI, selection);
                    if (contextAction.isDisplayedInToolbar()) {
                        updateContextToolbar();
                    }
                });
                UIUtils.makeButtonFlatWithSize(button, 32, 3);
                toolBar.add(button, contextToolbarInsertLocation);
                contextToolbarButtons.add(button);
            }
        }

        toolBar.revalidate();
        toolBar.repaint();
    }


    /**
     * Scrolls to the specified algorithm UI
     *
     * @param ui the algorithm
     */
    public void scrollToAlgorithm(JIPipeDesktopGraphNodeUI ui) {
        if (scrollPane == null) {
            return;
        }
        if (ui == null) {
            return;
        }
        int minViewX = scrollPane.getHorizontalScrollBar().getValue();
        int maxViewX = minViewX + scrollPane.getHorizontalScrollBar().getVisibleAmount();
        int minViewY = scrollPane.getVerticalScrollBar().getValue();
        int maxViewY = minViewY + scrollPane.getVerticalScrollBar().getVisibleAmount();
        if (ui.getX() < minViewX || ui.getX() > maxViewX) {
            scrollPane.getHorizontalScrollBar().setValue(ui.getX());
        }
        if (ui.getY() < minViewY || ui.getY() > maxViewY) {
            scrollPane.getVerticalScrollBar().setValue(ui.getY());
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isMiddleMouseButton(e)) {
            isPanning = true;
            int x = e.getX() - scrollPane.getHorizontalScrollBar().getValue();
            int y = e.getY() - scrollPane.getVerticalScrollBar().getValue();
            panningOffset = new Point(x, y);
            panningScrollbarOffset = new Point(scrollPane.getHorizontalScrollBar().getValue(),
                    scrollPane.getVerticalScrollBar().getValue());
            canvasUI.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (isPanning) {
            canvasUI.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
        isPanning = false;
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (isPanning && panningOffset != null && panningScrollbarOffset != null) {
            int x = e.getX() - scrollPane.getHorizontalScrollBar().getValue();
            int y = e.getY() - scrollPane.getVerticalScrollBar().getValue();
            int dx = x - panningOffset.x;
            int dy = y - panningOffset.y;
            if (!graphUISettings.isSwitchPanningDirection()) {
                dx = -dx;
                dy = -dy;
            }
            int nx = panningScrollbarOffset.x + dx;
            int ny = panningScrollbarOffset.y + dy;

            // Infinite scroll (left, top)
            {
                int ex = 0;
                int ey = 0;
                if (nx < 0) {
                    ex = (int) Math.ceil(1.0 * -nx / (JIPipeDesktopGraphCanvasGrid.GRID_WIDTH * canvasUI.getZoom()));
                }
                if (ny < 0) {
                    ey = (int) Math.ceil(1.0 * -ny / (JIPipeDesktopGraphCanvasGrid.GRID_HEIGHT * canvasUI.getZoom()));
                }
                if (ex > 0 || ey > 0) {
                    canvasUI.expandLeftTop(ex, ey);
                    if (ex > 0) {
                        nx = JIPipeDesktopGraphCanvasGrid.GRID_WIDTH;
                        panningOffset.x += JIPipeDesktopGraphCanvasGrid.GRID_WIDTH;
                    }
                    if (ey > 0) {
                        ny = JIPipeDesktopGraphCanvasGrid.GRID_HEIGHT;
                        panningOffset.y += JIPipeDesktopGraphCanvasGrid.GRID_HEIGHT;
                    }
                }
            }
            // Infinite scroll (right, bottom)
            {
                int mnx = nx + scrollPane.getHorizontalScrollBar().getVisibleAmount();
                int mny = ny + scrollPane.getVerticalScrollBar().getVisibleAmount();
                boolean ex = mnx > scrollPane.getHorizontalScrollBar().getMaximum();
                boolean ey = mny > scrollPane.getVerticalScrollBar().getMaximum();
                if (ex || ey) {
                    int exv = Math.max(0, mnx - scrollPane.getHorizontalScrollBar().getMaximum());
                    int eyv = Math.max(0, mny - scrollPane.getVerticalScrollBar().getMaximum());
                    canvasUI.expandRightBottom(exv, eyv);
                }
            }

            scrollPane.getHorizontalScrollBar().setValue(nx);
            scrollPane.getVerticalScrollBar().setValue(ny);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }

    /**
     * @return The displayed graph compartment
     */
    public UUID getCompartment() {
        return canvasUI.getCompartmentUUID();
    }

    public Set<JIPipeNodeInfo> getAddableAlgorithms() {
        return addableAlgorithms;
    }

    public void setAddableAlgorithms(Set<JIPipeNodeInfo> addableAlgorithms) {
        this.addableAlgorithms = addableAlgorithms;
//        updateNavigation();
    }

    public JIPipeHistoryJournal getHistoryJournal() {
        return historyJournal;
    }

    @Override
    public void onGraphChanged(JIPipeGraph.GraphChangedEvent event) {
//        updateNavigation();
    }

    @Override
    public void onGraphCanvasNodeSelectionChanged(JIPipeDesktopGraphCanvasUINodeSelectionChangedEvent event) {
        updateSelection();
        rebuildContextPanel();
    }

    @Override
    public void onNodeUISelected(JIPipeDesktopGraphCanvasUINodeSelectedEvent event) {
//        if (event.getNodeUI() != null) {
//            if (event.isAddToSelection()) {
//                if (getSelectionManager().getSelection().contains(event.getNodeUI())) {
//                    getSelectionManager().removeFromSelection(event.getNodeUI());
//                } else {
//                    getSelectionManager().addToSelection(event.getNodeUI());
//                }
//            } else {
//                getSelectionManager().selectOnly(event.getNodeUI());
//            }
//        } else {
//            getSelectionManager().clearSelection();
//        }
    }

    @Override
    public void onDefaultNodeUIActionRequested(DefaultNodeUIActionRequestedEvent event) {
        if (event.getUi() != null && event.getUi().getNode() instanceof JIPipeDesktopInteractiveDefaultActionGraphNode node) {
            node.onDefaultNodeUIActionRequested(this, event);
        }
    }

    @Override
    public void onNodeUIActionRequested(NodeUIActionRequestedEvent event) {

    }

    @Override
    public void onPanelSideVisibilityChanged(JIPipeDesktopDockPanel.PanelSideVisibilityChangedEvent event) {
        if (event.getPanelSide() == JIPipeDesktopDockPanel.PanelSide.Right && !event.isVisible()) {
            JScrollBar horizontalScrollBar = scrollPane.getHorizontalScrollBar();
            int oldVisibleAmount = horizontalScrollBar.getVisibleAmount();
            int oldValue = horizontalScrollBar.getValue();
//            System.out.println("old_value: " + horizontalScrollBar.getValue() + "/" + horizontalScrollBar.getMaximum() + " [" + horizontalScrollBar.getVisibleAmount() + "]");
            SwingUtilities.invokeLater(() -> {
//                System.out.println("new_value: " + horizontalScrollBar.getValue() + "/" + horizontalScrollBar.getMaximum() + " [" + horizontalScrollBar.getVisibleAmount() + "]");

                if (horizontalScrollBar.getValue() != oldValue) {
                    int newVisibleAmount = horizontalScrollBar.getVisibleAmount();
                    int shift = Math.max(0, newVisibleAmount - oldVisibleAmount);

                    if (shift > 0) {
                        canvasUI.expandRightBottom(shift, 0);
                        SwingUtilities.invokeLater(() -> {
                            horizontalScrollBar.setValue(horizontalScrollBar.getValue() + shift);
                        });
                    }
                }

            });
        }
    }

    /**
     * Executed before opening the context menu
     * Right before adding the partition entries and after all auto-generated items
     *
     * @param menu the menu
     */
    public abstract void beforeOpenContextMenu(JPopupMenu menu);

    /**
     * Triggered by the canvas if an empty region is double-clicked
     *
     * @param mouseEvent the mouse event
     */
    public abstract void onCanvasEmptyDoubleClick(MouseEvent mouseEvent);

    public JIPipeDesktopGraphEditorContextPanel getContextPanel() {
        return contextPanel;
    }

    public void selectToolByClass(Class<? extends JIPipeDesktopToggleableGraphEditorTool> toolClass) {
        for (JIPipeDesktopGraphEditorTool tool : tools) {
            if (toolClass.isInstance(tool)) {
                selectTool(tool);
                return;
            }
        }
    }


//    @Override
//    public void onSearchBoxSelectedEvent(JIPipeDesktopNodeDatabaseSearchBox.SelectedEvent event) {
//        JIPipeNodeDatabaseEntry entry = event.getValue();
//
//        if (entry instanceof CreateNewNodeByInfoDatabaseEntry) {
//
//            if (!JIPipeDesktopProjectWorkbench.canAddOrDeleteNodes(getDesktopWorkbench()))
//                return;
//            JIPipeNodeInfo info = ((CreateNewNodeByInfoDatabaseEntry) entry).getNodeInfo();
//            JIPipeGraphNode node = info.newInstance();
//            if (getHistoryJournal() != null) {
//                getHistoryJournal().snapshotBeforeAddNode(node, getCompartment());
//            }
//            canvasUI.getScheduledSelection().clear();
//            canvasUI.getScheduledSelection().add(node);
//            graph.insertNode(node, getCompartment());
//            nodeDatabaseSearchBox.setSelectedItem(null);
//        } else if (entry instanceof CreateNewNodeByExampleDatabaseEntry) {
//            if (!JIPipeDesktopProjectWorkbench.canAddOrDeleteNodes(getDesktopWorkbench()))
//                return;
//            JIPipeNodeExample example = ((CreateNewNodeByExampleDatabaseEntry) entry).getExample();
//            JIPipeNodeInfo info = example.getNodeInfo();
//            JIPipeGraphNode node = info.newInstance();
//            if (node instanceof JIPipeAlgorithm) {
//                ((JIPipeAlgorithm) node).loadExample(example);
//            }
//            if (getHistoryJournal() != null) {
//                getHistoryJournal().snapshotBeforeAddNode(node, getCompartment());
//            }
//            canvasUI.getScheduledSelection().clear();
//            canvasUI.getScheduledSelection().add(node);
//            graph.insertNode(node, getCompartment());
//            nodeDatabaseSearchBox.setSelectedItem(null);
//        } else if (entry instanceof ExistingPipelineNodeDatabaseEntry) {
//            JIPipeGraphNode graphNode = ((ExistingPipelineNodeDatabaseEntry) entry).getGraphNode();
//            selectOnly(canvasUI.getNodeUIs().get(graphNode));
//            nodeDatabaseSearchBox.setSelectedItem(null);
//        } else if (entry instanceof ExistingCompartmentDatabaseEntry) {
//            JIPipeGraphNode graphNode = ((ExistingCompartmentDatabaseEntry) entry).getCompartment();
//            selectOnly(canvasUI.getNodeUIs().get(graphNode));
//            nodeDatabaseSearchBox.setSelectedItem(null);
//        }
//    }

    /**
     * Renders items in the navigator
     */
    public static class NavigationRenderer extends JPanel implements ListCellRenderer<Object> {

        private final SolidColorIcon icon;
        private final JLabel actionLabel;

        private final JLabel alternativeLabel;
        private final JLabel algorithmLabel;
        private final JLabel menuLabel;

        /**
         * Creates a new instance
         */
        public NavigationRenderer() {
            setLayout(new GridBagLayout());
            setOpaque(true);
            setFont(new Font(Font.DIALOG, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeNormal()));
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));


            icon = new SolidColorIcon(16, 50);
            JLabel iconLabel = new JLabel(icon);
            Insets border = new Insets(2, 4, 2, 2);
            add(iconLabel, new GridBagConstraints() {
                {
                    gridx = 0;
                    gridy = 0;
                    gridheight = 3;
                    anchor = WEST;
                    insets = border;
                }
            });

            actionLabel = new JLabel();
            add(actionLabel, new GridBagConstraints() {
                {
                    gridx = 1;
                    gridy = 0;
                    anchor = WEST;
                    insets = border;
                }
            });
            algorithmLabel = new JLabel();
            add(algorithmLabel, new GridBagConstraints() {
                {
                    gridx = 2;
                    gridy = 0;
                    anchor = WEST;
                    insets = border;
                }
            });
            menuLabel = new JLabel();
            menuLabel.setForeground(Color.GRAY);
            menuLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, ThemeUtils.getCurrentStyle().getFontSizeNormal()));
            add(menuLabel, new GridBagConstraints() {
                {
                    gridx = 2;
                    gridy = 1;
                    anchor = WEST;
                    insets = border;
                }
            });

            alternativeLabel = new JLabel();
            alternativeLabel.setForeground(ThemeUtils.getCurrentStyle().getSecondaryColor());
            alternativeLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, ThemeUtils.getCurrentStyle().getFontSizeNormal()));
            add(alternativeLabel, new GridBagConstraints() {
                {
                    gridx = 2;
                    gridy = 2;
                    anchor = WEST;
                    insets = border;
                }
            });

            JPanel glue = new JPanel();
            glue.setOpaque(false);
            add(glue, new GridBagConstraints() {
                {
                    gridx = 3;
                    weightx = 1;
                }
            });
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

            if (value instanceof JIPipeNodeInfo) {
                JIPipeNodeInfo info = (JIPipeNodeInfo) value;
                // Location (Task 21b): converted ⇒ "function chain > node",
                // unconverted ⇒ legacy "category [> menuPath] > node".
                String menuPath = TaxonomyView.locationPath(info);

                icon.setFillColor(Color.WHITE);
                icon.setBorderColor(ThemeUtils.getNodeFillColor(info));
                actionLabel.setText("Create");
                actionLabel.setForeground(new Color(0, 128, 0));
                algorithmLabel.setText(info.getName());
                algorithmLabel.setIcon(JIPipe.getNodes().getIconFor(info));
                menuLabel.setText(menuPath);

                alternativeLabel.setForeground(ThemeUtils.getCurrentStyle().getSecondaryColor());
                // Task 26a: alias rows removed (converted nodes have no aliases).
                alternativeLabel.setText("");

            } else if (value instanceof JIPipeDesktopGraphNodeUI) {
                JIPipeGraphNode node = ((JIPipeDesktopGraphNodeUI) value).getNode();
                JIPipeNodeInfo info = node.getInfo();
                // Location (Task 21b): converted ⇒ "function chain > node",
                // unconverted ⇒ legacy "category [> menuPath] > node".
                String menuPath = TaxonomyView.locationPath(info);

                icon.setFillColor(ThemeUtils.getNodeFillColor(info));
                icon.setBorderColor(ThemeUtils.getNodeBorderColor(info));
                actionLabel.setText("Navigate");
                actionLabel.setForeground(Color.BLUE);
                algorithmLabel.setText(node.getName());
                algorithmLabel.setIcon(JIPipe.getNodes().getIconFor(info));
                menuLabel.setText(menuPath);
                alternativeLabel.setText("");
            } else if (value instanceof JIPipeNodeExample) {
                JIPipeNodeExample example = (JIPipeNodeExample) value;
                JIPipeNodeInfo info = example.getNodeInfo();
                // Location (Task 21b): converted ⇒ "function chain > node",
                // unconverted ⇒ legacy "category [> menuPath] > node".
                String menuPath = TaxonomyView.locationPath(info);

                icon.setFillColor(Color.WHITE);
                icon.setBorderColor(ThemeUtils.getNodeFillColor(info));
                actionLabel.setText("Create");
                actionLabel.setForeground(new Color(0, 128, 0));
                algorithmLabel.setText(info.getName() + ": " + example.getNodeTemplate().getName());
                algorithmLabel.setIcon(JIPipe.getNodes().getIconFor(info));
                menuLabel.setText(menuPath);

                alternativeLabel.setForeground(ThemeUtils.getCurrentStyle().getPrimaryColor());
                alternativeLabel.setText("Example");

            }

            if (isSelected) {
                setBackground(UIManager.getColor("List.selectionBackground"));
            } else {
                setBackground(UIManager.getColor("List.background"));
            }
            return this;
        }
    }
}
