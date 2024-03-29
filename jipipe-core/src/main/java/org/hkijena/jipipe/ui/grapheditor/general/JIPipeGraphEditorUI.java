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

package org.hkijena.jipipe.ui.grapheditor.general;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeService;
import org.hkijena.jipipe.api.grapheditortool.JIPipeDefaultGraphEditorTool;
import org.hkijena.jipipe.api.grapheditortool.JIPipeGraphEditorTool;
import org.hkijena.jipipe.api.grapheditortool.JIPipeToggleableGraphEditorTool;
import org.hkijena.jipipe.api.history.JIPipeHistoryJournal;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.database.*;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.GraphEditorUISettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.VerticalToolBar;
import org.hkijena.jipipe.ui.components.icons.SolidColorIcon;
import org.hkijena.jipipe.ui.extension.GraphEditorToolBarButtonExtension;
import org.hkijena.jipipe.ui.grapheditor.general.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeGraphNodeUI;
import org.hkijena.jipipe.ui.grapheditor.general.search.NodeDatabaseSearchBox;
import org.hkijena.jipipe.ui.theme.ModernMetalTheme;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.CopyImageToClipboard;
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
import java.util.List;
import java.util.*;

/**
 * A panel around {@link JIPipeGraphCanvasUI} that comes with scrolling/panning, properties panel,
 * and a menu bar
 */
public abstract class JIPipeGraphEditorUI extends JIPipeWorkbenchPanel implements MouseListener, MouseMotionListener, Disposable, JIPipeGraph.GraphChangedEventListener,
        JIPipeService.NodeInfoRegisteredEventListener,
        JIPipeGraphCanvasUI.NodeSelectionChangedEventListener,
        JIPipeGraphCanvasUI.NodeUISelectedEventListener,
        JIPipeGraphNodeUI.DefaultNodeUIActionRequestedEventListener,
        JIPipeGraphNodeUI.NodeUIActionRequestedEventListener, NodeDatabaseSearchBox.SelectedEventListener {

    public static final int FLAGS_NONE = 0;
    public static final int FLAGS_SPLIT_PANE_VERTICAL = 1;
    public static final int FLAGS_SPLIT_PANE_RATIO_1_1 = 2;
    public static final int FLAGS_SPLIT_PANE_SWITCH_CONTENT = 4;

    public static final KeyStroke KEY_STROKE_UNDO = KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK, true);
    public static final KeyStroke KEY_STROKE_REDO = KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, true);
    public static final KeyStroke KEY_STROKE_ZOOM_IN = KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_MASK, false);
    public static final KeyStroke KEY_STROKE_ZOOM_OUT = KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_MASK, false);
    public static final KeyStroke KEY_STROKE_ZOOM_RESET = KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, InputEvent.CTRL_MASK, false);
    public static final KeyStroke KEY_STROKE_MOVE_SELECTION_LEFT = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false);
    public static final KeyStroke KEY_STROKE_MOVE_SELECTION_RIGHT = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false);
    public static final KeyStroke KEY_STROKE_MOVE_SELECTION_UP = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false);
    public static final KeyStroke KEY_STROKE_MOVE_SELECTION_DOWN = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false);
    private final GraphEditorUISettings graphUISettings;
    private final JIPipeGraphCanvasUI canvasUI;
    private final JIPipeGraph graph;
    private NodeDatabaseSearchBox nodeDatabaseSearchBox;
    private final JIPipeHistoryJournal historyJournal;
    private final int flags;
    private final JMenu graphMenu = new JMenu("Workflow");
    private final List<JIPipeGraphEditorTool> tools = new ArrayList<>();
    private final Map<Class<? extends JIPipeGraphEditorTool>, JIPipeGraphEditorTool> toolMap = new HashMap<>();
    private final BiMap<JIPipeToggleableGraphEditorTool, JToggleButton> toolToggles = HashBiMap.create();
    protected JMenuBar menuBar = new JMenuBar();
    private JSplitPane splitPane;
    private JScrollPane scrollPane;
    private Point panningOffset = null;
    private Point panningScrollbarOffset = null;
    private boolean isPanning = false;
    private Set<JIPipeNodeInfo> addableAlgorithms = new HashSet<>();
    private JIPipeToggleableGraphEditorTool currentTool;

    /**
     * @param workbenchUI    the workbench
     * @param graph          the algorithm graph
     * @param compartment    the graph compartment to display. Set to null to display all compartments
     * @param historyJournal object that tracks the history of this graph. Set to null to disable the undo feature.
     * @param flags          additional flags
     */
    public JIPipeGraphEditorUI(JIPipeWorkbench workbenchUI, JIPipeGraph graph, UUID compartment, JIPipeHistoryJournal historyJournal, GraphEditorUISettings settings, int flags) {
        super(workbenchUI);
        this.graph = graph;
        this.historyJournal = historyJournal;
        this.flags = flags;
        this.canvasUI = new JIPipeGraphCanvasUI(getWorkbench(), this, graph, compartment, historyJournal);
        this.graphUISettings = settings;

        initialize();
        reloadMenuBar();
        JIPipe.getInstance().getNodeInfoRegisteredEventEmitter().subscribeWeak(this);
        graph.getGraphChangedEventEmitter().subscribeWeak(this);

        initializeHotkeys();
        SwingUtilities.invokeLater(() -> {
            canvasUI.crop(true);
            selectDefaultTool();
        });
    }

    /**
     * @param workbenchUI    the workbench
     * @param graph          the algorithm graph
     * @param compartment    the graph compartment to display. Set to null to display all compartments
     * @param historyJournal object that tracks the history of this graph. Set to null to disable the undo feature.
     */
    public JIPipeGraphEditorUI(JIPipeWorkbench workbenchUI, JIPipeGraph graph, UUID compartment, JIPipeHistoryJournal historyJournal) {
        this(workbenchUI, graph, compartment, historyJournal, GraphEditorUISettings.getInstance(), JIPipeGraphEditorUI.FLAGS_NONE);
    }

    public static void installContextActionsInto(JToolBar toolBar, Set<JIPipeGraphNodeUI> selection, List<NodeUIContextAction> actionList, JIPipeGraphCanvasUI canvasUI) {
        JPopupMenu menu = new JPopupMenu();
        for (NodeUIContextAction action : actionList) {
            if (action == null) {
                menu.addSeparator();
                continue;
            }
            if (action.isHidden())
                continue;
            boolean matches = action.matches(selection);
            if (!matches && !action.disableOnNonMatch())
                continue;

            JMenuItem item = new JMenuItem(action.getName(), action.getIcon());
            item.setToolTipText(action.getDescription());
            if (matches)
                item.addActionListener(e -> action.run(canvasUI, ImmutableSet.copyOf(selection)));
            else
                item.setEnabled(false);
            menu.add(item);
        }

        if (menu.getComponentCount() > 0) {
            JButton button = new JButton(UIUtils.getIconFromResources("actions/open-menu.png"));
            UIUtils.makeFlat25x25(button);
            button.setToolTipText("Shows the context menu for the selected node. Alternatively, you can also right-click the node");
            UIUtils.addPopupMenuToButton(button, menu);
            toolBar.add(Box.createHorizontalStrut(4), 0);
            toolBar.add(button, 0);
        }
    }

    @Override
    public void dispose() {
        JIPipe.getInstance().getNodeInfoRegisteredEventEmitter().unsubscribe(this);
        graph.getGraphChangedEventEmitter().unsubscribe(this);
        canvasUI.dispose();
//        getRootPane().unregisterKeyboardAction(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
    }

    public int getFlags() {
        return flags;
    }

    public JMenu getGraphMenu() {
        return graphMenu;
    }

    public GraphEditorUISettings getGraphUISettings() {
        return graphUISettings;
    }

    private void initializeHotkeys() {

        registerKeyboardAction(e -> undo(), KEY_STROKE_UNDO, JComponent.WHEN_IN_FOCUSED_WINDOW);
        registerKeyboardAction(e -> redo(), KEY_STROKE_REDO, JComponent.WHEN_IN_FOCUSED_WINDOW);
        registerKeyboardAction(e -> canvasUI.zoomIn(), KEY_STROKE_ZOOM_IN, JComponent.WHEN_IN_FOCUSED_WINDOW);
        registerKeyboardAction(e -> canvasUI.zoomOut(), KEY_STROKE_ZOOM_OUT, JComponent.WHEN_IN_FOCUSED_WINDOW);
        registerKeyboardAction(e -> canvasUI.resetZoom(), KEY_STROKE_ZOOM_RESET, JComponent.WHEN_IN_FOCUSED_WINDOW);

        canvasUI.registerKeyboardAction(e -> canvasUI.moveSelection(-1, 0, false), KEY_STROKE_MOVE_SELECTION_LEFT, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        canvasUI.registerKeyboardAction(e -> canvasUI.moveSelection(1, 0, false), KEY_STROKE_MOVE_SELECTION_RIGHT, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        canvasUI.registerKeyboardAction(e -> canvasUI.moveSelection(0, -1, false), KEY_STROKE_MOVE_SELECTION_UP, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        canvasUI.registerKeyboardAction(e -> canvasUI.moveSelection(0, 1, false), KEY_STROKE_MOVE_SELECTION_DOWN, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    public JIPipeGraphCanvasUI getCanvasUI() {
        return canvasUI;
    }

    public boolean isFlagSet(int flag) {
        return (flags & flag) == flag;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        int splitPaneSplit = isFlagSet(FLAGS_SPLIT_PANE_VERTICAL) ? JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT;
        AutoResizeSplitPane.Ratio splitPaneRatio;
        if (isFlagSet(FLAGS_SPLIT_PANE_RATIO_1_1)) {
            splitPaneRatio = new AutoResizeSplitPane.FixedRatio(AutoResizeSplitPane.RATIO_1_TO_1);
        } else {
            splitPaneRatio = new AutoResizeSplitPane.DynamicSidebarRatio();
        }
        splitPane = new AutoResizeSplitPane(splitPaneSplit, splitPaneRatio);

        canvasUI.fullRedraw();
        canvasUI.getNodeUISelectedEventEmitter().subscribe(this);
        canvasUI.getNodeSelectionChangedEventEmitter().subscribe(this);
        canvasUI.getDefaultAlgorithmUIActionRequestedEventEmitter().subscribe(this);
        canvasUI.getNodeUIActionRequestedEventEmitter().subscribe(this);
        canvasUI.addMouseListener(this);
        canvasUI.addMouseMotionListener(this);
        scrollPane = new JScrollPane(canvasUI);
        scrollPane.getVerticalScrollBar().setUnitIncrement(25);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(25);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        if (isFlagSet(FLAGS_SPLIT_PANE_SWITCH_CONTENT)) {
            splitPane.setRightComponent(scrollPane);
            splitPane.setLeftComponent(new JPanel());
        } else {
            splitPane.setLeftComponent(scrollPane);
            splitPane.setRightComponent(new JPanel());
        }
        add(splitPane, BorderLayout.CENTER);

//        menuBar.setLayout(new BoxLayout(menuBar, BoxLayout.X_AXIS));
        add(menuBar, BorderLayout.NORTH);

        // Init search box
        JIPipeNodeDatabase database;
        if(getWorkbench() instanceof JIPipeProjectWorkbench) {
            database = ((JIPipeProjectWorkbench) getWorkbench()).getNodeDatabase();
        }
        else {
            database = JIPipeNodeDatabase.getInstance();
        }
        nodeDatabaseSearchBox = new NodeDatabaseSearchBox(getWorkbench(), canvasUI, getNodeDatabaseRole(), database);
        nodeDatabaseSearchBox.setAllowNew(graphUISettings.getSearchSettings().isSearchFindNewNodes());
        nodeDatabaseSearchBox.setAllowExisting(graphUISettings.getSearchSettings().isSearchFindExistingNodes());
        nodeDatabaseSearchBox.getSelectedEventEmitter().subscribe(this);

        initializeEditingToolbar();
    }

    private void initializeEditingToolbar() {
        VerticalToolBar toolBar = new VerticalToolBar();

        for (Class<? extends JIPipeGraphEditorTool> klass : JIPipe.getInstance().getGraphEditorToolRegistry().getRegisteredTools()) {
            JIPipeGraphEditorTool tool = (JIPipeGraphEditorTool) ReflectionUtils.newInstance(klass);
            if (tool.supports(this)) {
                tool.setGraphEditor(this);
                tools.add(tool);
                toolMap.put(klass, tool);
            }
        }
        tools.sort(Comparator.comparing(JIPipeGraphEditorTool::getCategory).thenComparing(JIPipeGraphEditorTool::getPriority));
        for (int i = 0; i < tools.size(); i++) {
            JIPipeGraphEditorTool tool = tools.get(i);
            if (i > 0 && !Objects.equals(tool.getCategory(), tools.get(i - 1).getCategory())) {
                toolBar.addSeparator();
            }

            // Hotkeys
            KeyStroke keyBinding = tool.getKeyBinding();
            if (keyBinding != null) {
                registerKeyboardAction(e -> selectTool(tool),
                        keyBinding,
                        JComponent.WHEN_IN_FOCUSED_WINDOW);
            }

            if (tool instanceof JIPipeToggleableGraphEditorTool) {
                JIPipeToggleableGraphEditorTool toggleableGraphEditorTool = (JIPipeToggleableGraphEditorTool) tool;

                JToggleButton toggleButton = new JToggleButton(tool.getIcon());
                toggleButton.setToolTipText("<html><strong>" + tool.getName() + "</strong><br/><br/>" + tool.getTooltip() +
                        (keyBinding != null ? "<br><br>Shortcut: <i><strong>" + UIUtils.keyStrokeToString(keyBinding) + "</strong></i>" : "") + "</html>");
                toggleButton.addActionListener(e -> selectTool(tool));
                UIUtils.makeFlat25x25(toggleButton);
                toolBar.add(toggleButton);
                toolToggles.put(toggleableGraphEditorTool, toggleButton);

            } else {
                JButton button = new JButton(tool.getIcon());
                button.setToolTipText("<html><strong>" + tool.getName() + "</strong><br/><br/>" + tool.getTooltip() +
                        (keyBinding != null ? "<br><br>Shortcut: <i><strong>" + UIUtils.keyStrokeToString(keyBinding) + "</strong></i>" : "") + "</html>");
                button.addActionListener(e -> selectTool(tool));
                UIUtils.makeFlat25x25(button);
                toolBar.add(button);
            }

        }


        add(toolBar, BorderLayout.WEST);
    }

    public List<JIPipeGraphEditorTool> getTools() {
        return Collections.unmodifiableList(tools);
    }

    public void selectDefaultTool() {
        selectTool(tools.stream().filter(tool -> tool instanceof JIPipeDefaultGraphEditorTool).findFirst().orElse(null));
    }

    public void selectTool(JIPipeGraphEditorTool tool) {
        if (tool instanceof JIPipeToggleableGraphEditorTool) {
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
                canvasUI.setCurrentTool(null);
            }

            JToggleButton toggleButton = toolToggles.get(tool);
            toggleButton.setSelected(true);
            currentTool = (JIPipeToggleableGraphEditorTool) tool;
            canvasUI.setCurrentTool(currentTool);
            tool.activate();
            getWorkbench().sendStatusBarText("Activated tool '" + tool.getName() + "'");
        } else {
            tool.activate();
            getWorkbench().sendStatusBarText("Activated tool '" + tool.getName() + "'");
        }
    }

    public JIPipeToggleableGraphEditorTool getCurrentTool() {
        return currentTool;
    }

    public Set<JIPipeGraphNodeUI> getSelection() {
        return canvasUI.getSelection();
    }

    /**
     * Reloads the menu bar
     */
    public void reloadMenuBar() {
        menuBar.removeAll();
        initializeCommonActions();
    }

    /**
     * Initializes the tool bar
     */
    protected void initializeCommonActions() {
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(Box.createHorizontalStrut(8));

        menuBar.add(nodeDatabaseSearchBox);
        nodeDatabaseSearchBox.setVisible(graphUISettings.getSearchSettings().isEnableSearch());
        menuBar.add(Box.createHorizontalStrut(8));
        menuBar.add(Box.createVerticalStrut(42));

        List<GraphEditorToolBarButtonExtension> graphEditorToolBarButtonExtensions = JIPipe.getCustomMenus().graphEditorToolBarButtonExtensionsFor(this);
        for (GraphEditorToolBarButtonExtension extension : graphEditorToolBarButtonExtensions) {
            UIUtils.makeFlat25x25(extension);
            menuBar.add(extension);
        }

        if (!graphEditorToolBarButtonExtensions.isEmpty())
            menuBar.add(new JSeparator(JSeparator.VERTICAL));

        if (getHistoryJournal() != null) {
            JButton undoButton = new JButton(UIUtils.getIconFromResources("actions/undo.png"));
            undoButton.setToolTipText("<html>Undo<br><i>Ctrl-Z</i></html>");
            UIUtils.makeFlat25x25(undoButton);
            undoButton.addActionListener(e -> undo());
            menuBar.add(undoButton);

            JButton redoButton = new JButton(UIUtils.getIconFromResources("actions/edit-redo.png"));
            redoButton.setToolTipText("<html>Redo<br><i>Ctrl-Shift-Z</i></html>");
            UIUtils.makeFlat25x25(redoButton);
            redoButton.addActionListener(e -> redo());
            menuBar.add(redoButton);
        }

        initializeCenterViewCommand(graphMenu);
        initializeToggleHideEdgesCommand(graphMenu);
        initializeExportMenu(graphMenu);
        initializeLayoutMenu(graphMenu);
        initializeSearchMenu(graphMenu);

        menuBar.add(UIUtils.createVerticalSeparator());

        initializeZoomMenu();

        menuBar.add(UIUtils.createVerticalSeparator());

        graphMenu.setIcon(UIUtils.getIconFromResources("actions/bars.png"));
        menuBar.add(graphMenu);
    }

    private void initializeToggleHideEdgesCommand(JMenu graphMenu) {
        JCheckBoxMenuItem toggle = new JCheckBoxMenuItem("Auto-mute edges", canvasUI.isAutoMuteEdges());
        toggle.addActionListener(e -> {
            canvasUI.setAutoMuteEdges(toggle.getState());
            canvasUI.repaint(50);
        });
        graphMenu.add(toggle);
    }

    private void initializeZoomMenu() {
        JButton zoomOutButton = new JButton(UIUtils.getIconFromResources("actions/square-minus.png"));
        UIUtils.makeFlat25x25(zoomOutButton);
        zoomOutButton.setToolTipText("<html>Zoom out<br><i>Ctrl-NumPad -</i></html>");
        zoomOutButton.addActionListener(e -> canvasUI.zoomOut());
        menuBar.add(zoomOutButton);

        JButton zoomButton = new JButton((int) (canvasUI.getZoom() * 100) + "%");
        zoomButton.setToolTipText("<html>Change zoom<br>Reset zoom: <i>Ctrl-NumPad 0</i></html>");
        canvasUI.getZoomChangedEventEmitter().subscribeLambda((emitter, event) -> {
            zoomButton.setText((int) (canvasUI.getZoom() * 100) + "%");
        });
        zoomButton.setBorder(null);
        JPopupMenu zoomMenu = UIUtils.addPopupMenuToButton(zoomButton);
        for (double zoom = 0.1; zoom <= 3; zoom += 0.25) {
            JMenuItem changeZoomItem = new JMenuItem((int) (zoom * 100) + "%", UIUtils.getIconFromResources("actions/zoom.png"));
            double finalZoom = zoom;
            changeZoomItem.addActionListener(e -> canvasUI.setZoom(finalZoom));
            zoomMenu.add(changeZoomItem);
        }
        zoomMenu.addSeparator();
        zoomMenu.add(UIUtils.createMenuItem("Reset zoom", "Resets the zoom to 100%",
                UIUtils.getIconFromResources("actions/edit-reset.png"), () -> canvasUI.setZoom(1)));
        JMenuItem changeZoomToItem = new JMenuItem("Set zoom value ...");
        changeZoomToItem.addActionListener(e -> {
            String zoomInput = JOptionPane.showInputDialog(this, "Please enter a new zoom value (in %)", (int) (canvasUI.getZoom() * 100) + "%");
            if (!StringUtils.isNullOrEmpty(zoomInput)) {
                zoomInput = zoomInput.replace("%", "");
                try {
                    int percentage = Integer.parseInt(zoomInput);
                    canvasUI.setZoom(percentage / 100.0);
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }
            }
        });
        zoomMenu.add(changeZoomToItem);
        menuBar.add(zoomButton);

        JButton zoomInButton = new JButton(UIUtils.getIconFromResources("actions/square-plus.png"));
        UIUtils.makeFlat25x25(zoomInButton);
        zoomInButton.setToolTipText("<html>Zoom in<br><i>Ctrl-NumPad +</i></html>");
        zoomInButton.addActionListener(e -> canvasUI.zoomIn());
        menuBar.add(zoomInButton);
    }

    private void initializeSearchMenu(JMenu graphMenu) {
        JMenu searchMenu = new JMenu("Search");
        searchMenu.setIcon(UIUtils.getIconFromResources("actions/search.png"));
        graphMenu.add(searchMenu);

        JMenuItem searchEnabledItem = new JCheckBoxMenuItem("Enable search");
        searchEnabledItem.setSelected(graphUISettings.getSearchSettings().isEnableSearch());
        searchEnabledItem.addActionListener(e -> {
            graphUISettings.getSearchSettings().setEnableSearch(searchEnabledItem.isSelected());
            if(!JIPipe.NO_SETTINGS_AUTOSAVE) {
                JIPipe.getInstance().getSettingsRegistry().save();
            }
            nodeDatabaseSearchBox.setVisible(graphUISettings.getSearchSettings().isEnableSearch());
            menuBar.revalidate();
        });
        searchMenu.add(searchEnabledItem);

        JMenuItem searchFindNewNodes = new JCheckBoxMenuItem("Search can create new nodes");
        searchFindNewNodes.setSelected(graphUISettings.getSearchSettings().isSearchFindNewNodes());
        searchFindNewNodes.addActionListener(e -> {
            graphUISettings.getSearchSettings().setSearchFindNewNodes(searchFindNewNodes.isSelected());
            if(!JIPipe.NO_SETTINGS_AUTOSAVE) {
                JIPipe.getInstance().getSettingsRegistry().save();
            }
            nodeDatabaseSearchBox.setAllowNew(searchFindNewNodes.isSelected());
        });
        searchMenu.add(searchFindNewNodes);

        JMenuItem searchFindExistingNodes = new JCheckBoxMenuItem("Search can find existing nodes");
        searchFindExistingNodes.setSelected(graphUISettings.getSearchSettings().isSearchFindExistingNodes());
        searchFindExistingNodes.addActionListener(e -> {
            graphUISettings.getSearchSettings().setSearchFindExistingNodes(searchFindExistingNodes.isSelected());
            if(!JIPipe.NO_SETTINGS_AUTOSAVE) {
                JIPipe.getInstance().getSettingsRegistry().save();
            }
            nodeDatabaseSearchBox.setAllowExisting(searchFindExistingNodes.isSelected());
        });
        searchMenu.add(searchFindExistingNodes);
    }

    private void initializeExportMenu(JMenu graphMenu) {
        JMenu exportAsImageMenu = new JMenu("Export as image");
        exportAsImageMenu.setIcon(UIUtils.getIconFromResources("actions/document-export.png"));
        graphMenu.add(exportAsImageMenu);

        JMenuItem exportToClipboardItem = new JMenuItem("Copy snapshot to clipboard", UIUtils.getIconFromResources("actions/edit-copy.png"));
        exportToClipboardItem.addActionListener(e -> createScreenshotClipboard());
        exportAsImageMenu.add(exportToClipboardItem);
        JMenuItem exportAsPngItem = new JMenuItem("Export as *.png", UIUtils.getIconFromResources("actions/viewimage.png"));
        exportAsPngItem.addActionListener(e -> createScreenshotPNG());
        exportAsImageMenu.add(exportAsPngItem);
        JMenuItem exportAsSvgItem = new JMenuItem("Export as *.svg", UIUtils.getIconFromResources("actions/viewimage.png"));
        exportAsSvgItem.addActionListener(e -> createScreenshotSVG());
        exportAsImageMenu.add(exportAsSvgItem);
    }

    private void initializeCenterViewCommand(JMenu graphMenu) {
        JMenuItem centerViewButton = new JMenuItem("Center view to nodes");
        centerViewButton.setIcon(UIUtils.getIconFromResources("actions/view-restore.png"));
        centerViewButton.addActionListener(e -> {
            if (getHistoryJournal() != null) {
                getHistoryJournal().snapshot("Center view to nodes",
                        "Apply center view to nodes",
                        getCompartment(),
                        UIUtils.getIconFromResources("actions/view-restore.png"));
            }
            canvasUI.crop(true);
        });
        graphMenu.add(centerViewButton);
    }

    private void initializeLayoutMenu(JMenu graphMenu) {

        JMenu layoutMenu = new JMenu("Layout");
        layoutMenu.setIcon(UIUtils.getIconFromResources("actions/distribute-randomize.png"));
        graphMenu.add(layoutMenu);

        JMenuItem autoLayoutItem = new JMenuItem("Auto-layout all nodes", UIUtils.getIconFromResources("actions/distribute-unclump.png"));
        autoLayoutItem.addActionListener(e -> {
            if (getHistoryJournal() != null) {
                getHistoryJournal().snapshot("Auto-layout", "Apply auto-layout", getCompartment(), UIUtils.getIconFromResources("actions/distribute-unclump.png"));
            }
            canvasUI.autoLayoutAll();
        });
        autoLayoutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
        layoutMenu.add(autoLayoutItem);

        JCheckBoxMenuItem layoutOnConnectItem = new JCheckBoxMenuItem("Layout nodes on connect",
                UIUtils.getIconFromResources("actions/connector-avoid.png"),
                GraphEditorUISettings.getInstance().isLayoutAfterConnect());
        layoutOnConnectItem.setToolTipText("Auto-layout layout on making data slot connections");
        layoutOnConnectItem.addActionListener(e -> {
            GraphEditorUISettings.getInstance().setLayoutAfterConnect(layoutOnConnectItem.isSelected());
        });

        layoutMenu.add(layoutOnConnectItem);

        JCheckBoxMenuItem layoutOnAlgorithmFinderItem = new JCheckBoxMenuItem("Layout nodes on 'Find matching algorithm'",
                UIUtils.getIconFromResources("actions/connector-avoid.png"),
                GraphEditorUISettings.getInstance().isLayoutAfterAlgorithmFinder());
        layoutOnAlgorithmFinderItem.setToolTipText("Auto-layout layout on utilizing the 'Find matching algorithm' feature");
        layoutOnAlgorithmFinderItem.addActionListener(e -> {
            GraphEditorUISettings.getInstance().setLayoutAfterAlgorithmFinder(layoutOnAlgorithmFinderItem.isSelected());
        });
        layoutMenu.add(layoutOnAlgorithmFinderItem);
    }

    public void createScreenshotClipboard() {
        BufferedImage screenshot = canvasUI.createScreenshotPNG();
        CopyImageToClipboard copyImageToClipboard = new CopyImageToClipboard();
        copyImageToClipboard.copyImage(screenshot);
        getWorkbench().sendStatusBarText("Copied screenshot to clipboard");
    }

    private void redo() {
        if (getHistoryJournal() != null) {
            int scrollX = scrollPane.getHorizontalScrollBar().getValue();
            int scrollY = scrollPane.getVerticalScrollBar().getValue();
            if (getHistoryJournal().redo(getCompartment())) {
                getWorkbench().sendStatusBarText("Redo successful");
            } else {
                getWorkbench().sendStatusBarText("Redo unsuccessful");
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
                getWorkbench().sendStatusBarText("Undo successful");
            } else {
                getWorkbench().sendStatusBarText("Undo unsuccessful");
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
        Path selectedPath = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Projects, "Export graph as SVG (*.svg)", UIUtils.EXTENSION_FILTER_SVG);
        if (selectedPath != null) {
            try {
                SVGUtils.writeToSVG(selectedPath.toFile(), screenshot.getSVGElement());
                getWorkbench().sendStatusBarText("Exported graph as " + selectedPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void createScreenshotPNG() {
        BufferedImage screenshot = canvasUI.createScreenshotPNG();
        Path selectedPath = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Projects, "Export graph as PNG (*.png)", UIUtils.EXTENSION_FILTER_PNG);
        if (selectedPath != null) {
            try {
                ImageIO.write(screenshot, "PNG", selectedPath.toFile());
                getWorkbench().sendStatusBarText("Exported graph as " + selectedPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected void updateSelection() {
    }


    /**
     * Scrolls to the specified algorithm UI
     *
     * @param ui the algorithm
     */
    public void scrollToAlgorithm(JIPipeGraphNodeUI ui) {
        if (scrollPane == null)
            return;
        if(ui == null)
            return;
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

    /**
     * Clears the algorithm selection
     */
    public void clearSelection() {
        canvasUI.clearSelection();
    }

    /**
     * Selects only the specified algorithm
     *
     * @param ui The algorithm UI
     */
    public void selectOnly(JIPipeGraphNodeUI ui) {
        canvasUI.selectOnly(ui);
        scrollToAlgorithm(ui);
    }

    /**
     * Removes an algorithm from the selection
     *
     * @param ui The algorithm UI
     */
    public void removeFromSelection(JIPipeGraphNodeUI ui) {
        canvasUI.removeFromSelection(ui);
    }

    protected Component getPropertyPanel() {
        return splitPane.getRightComponent();
    }

    /**
     * Sets the component displayed in the right property panel
     *
     * @param content         the component
     * @param disposeExisting if the old component should be disposed
     */
    protected void setPropertyPanel(Component content, boolean disposeExisting) {
        int dividerLocation = splitPane.getDividerLocation();
        if (isFlagSet(FLAGS_SPLIT_PANE_SWITCH_CONTENT)) {
            if (disposeExisting && splitPane.getLeftComponent() instanceof Disposable) {
                ((Disposable) splitPane.getLeftComponent()).dispose();
            }
            splitPane.setLeftComponent(content);
        } else {
            if (disposeExisting && splitPane.getRightComponent() instanceof Disposable) {
                ((Disposable) splitPane.getRightComponent()).dispose();
            }
            splitPane.setRightComponent(content);
        }
        splitPane.setDividerLocation(dividerLocation);
    }

    /**
     * Adds an algorithm to the selection
     *
     * @param ui The algorithm UI
     */
    public void addToSelection(JIPipeGraphNodeUI ui) {
        canvasUI.addToSelection(ui);
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
                    ex = (int) Math.ceil(1.0 * -nx / (canvasUI.getViewMode().getGridWidth() * canvasUI.getZoom()));
                }
                if (ny < 0) {
                    ey = (int) Math.ceil(1.0 * -ny / (canvasUI.getViewMode().getGridHeight() * canvasUI.getZoom()));
                }
                if (ex > 0 || ey > 0) {
                    canvasUI.expandLeftTop(ex, ey);
                    if (ex > 0) {
                        nx = canvasUI.getViewMode().getGridWidth();
                        panningOffset.x += canvasUI.getViewMode().getGridWidth();
                    }
                    if (ey > 0) {
                        ny = canvasUI.getViewMode().getGridHeight();
                        panningOffset.y += canvasUI.getViewMode().getGridHeight();
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
        return canvasUI.getCompartment();
    }

    protected JMenuBar getMenuBar() {
        return menuBar;
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
    public void onJIPipeNodeInfoRegistered(JIPipeService.NodeInfoRegisteredEvent event) {
        reloadMenuBar();
        getWorkbench().sendStatusBarText("Plugins were updated");
    }

    @Override
    public void onGraphCanvasNodeSelectionChanged(JIPipeGraphCanvasUI.NodeSelectionChangedEvent event) {
        updateSelection();
    }

    @Override
    public void onNodeUISelected(JIPipeGraphCanvasUI.NodeUISelectedEvent event) {
        if (event.getNodeUI() != null) {
            if (event.isAddToSelection()) {
                if (canvasUI.getSelection().contains(event.getNodeUI())) {
                    removeFromSelection(event.getNodeUI());
                } else {
                    addToSelection(event.getNodeUI());
                }
            } else {
                selectOnly(event.getNodeUI());
            }
        } else {
            clearSelection();
        }
    }

    @Override
    public void onDefaultNodeUIActionRequested(JIPipeGraphNodeUI.DefaultNodeUIActionRequestedEvent event) {

    }

    @Override
    public void onNodeUIActionRequested(JIPipeGraphNodeUI.NodeUIActionRequestedEvent event) {

    }

    public abstract JIPipeNodeDatabaseRole getNodeDatabaseRole();

    @Override
    public void onSearchBoxSelectedEvent(NodeDatabaseSearchBox.SelectedEvent event) {
        JIPipeNodeDatabaseEntry entry = event.getValue();

        if(entry instanceof CreateNewNodeByInfoDatabaseEntry) {

            if (!JIPipeProjectWorkbench.canAddOrDeleteNodes(getWorkbench()))
                return;
            JIPipeNodeInfo info = ((CreateNewNodeByInfoDatabaseEntry) entry).getNodeInfo();
            JIPipeGraphNode node = info.newInstance();
            if (getHistoryJournal() != null) {
                getHistoryJournal().snapshotBeforeAddNode(node, getCompartment());
            }
            canvasUI.getScheduledSelection().clear();
            canvasUI.getScheduledSelection().add(node);
            graph.insertNode(node, getCompartment());
            nodeDatabaseSearchBox.setSelectedItem(null);
        }
        else if(entry instanceof CreateNewNodeByExampleDatabaseEntry) {
            if (!JIPipeProjectWorkbench.canAddOrDeleteNodes(getWorkbench()))
                return;
            JIPipeNodeExample example = ((CreateNewNodeByExampleDatabaseEntry) entry).getExample();
            JIPipeNodeInfo info = example.getNodeInfo();
            JIPipeGraphNode node = info.newInstance();
            if (node instanceof JIPipeAlgorithm) {
                ((JIPipeAlgorithm) node).loadExample(example);
            }
            if (getHistoryJournal() != null) {
                getHistoryJournal().snapshotBeforeAddNode(node, getCompartment());
            }
            canvasUI.getScheduledSelection().clear();
            canvasUI.getScheduledSelection().add(node);
            graph.insertNode(node, getCompartment());
            nodeDatabaseSearchBox.setSelectedItem(null);
        }
        else if(entry instanceof ExistingPipelineNodeDatabaseEntry) {
            JIPipeGraphNode graphNode = ((ExistingPipelineNodeDatabaseEntry) entry).getGraphNode();
            selectOnly(canvasUI.getNodeUIs().get(graphNode));
            nodeDatabaseSearchBox.setSelectedItem(null);
        }
        else if(entry instanceof ExistingCompartmentDatabaseEntry) {
            JIPipeGraphNode graphNode = ((ExistingCompartmentDatabaseEntry) entry).getCompartment();
            selectOnly(canvasUI.getNodeUIs().get(graphNode));
            nodeDatabaseSearchBox.setSelectedItem(null);
        }
    }

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
            setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
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
            menuLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 12));
            add(menuLabel, new GridBagConstraints() {
                {
                    gridx = 2;
                    gridy = 1;
                    anchor = WEST;
                    insets = border;
                }
            });

            alternativeLabel = new JLabel();
            alternativeLabel.setForeground(ModernMetalTheme.PRIMARY6);
            alternativeLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 12));
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
                String menuPath = info.getCategory().getName();
                if (!StringUtils.isNullOrEmpty(info.getMenuPath())) {
                    menuPath += " > " + String.join(" > ", info.getMenuPath().split("\n"));
                }

                icon.setFillColor(Color.WHITE);
                icon.setBorderColor(UIUtils.getFillColorFor(info));
                actionLabel.setText("Create");
                actionLabel.setForeground(new Color(0, 128, 0));
                algorithmLabel.setText(info.getName());
                algorithmLabel.setIcon(JIPipe.getNodes().getIconFor(info));
                menuLabel.setText(menuPath);

                alternativeLabel.setForeground(ModernMetalTheme.PRIMARY6);
                if (info.getAliases().isEmpty()) {
                    alternativeLabel.setText("");
                } else {
                    StringBuilder builder = new StringBuilder();
                    builder.append("Alias: ");
                    List<JIPipeNodeMenuLocation> alternativeMenuLocations = info.getAliases();
                    for (int i = 0; i < alternativeMenuLocations.size(); i++) {
                        if (i > 0) {
                            builder.append(", ");
                        }
                        JIPipeNodeMenuLocation location = alternativeMenuLocations.get(i);
                        builder.append(location.getCategory().getName()).append(" > ").append(String.join(" > ", location.getMenuPath().split("\n"))).append(" > ").append(StringUtils.orElse(location.getAlternativeName(), info.getName()));
                    }
                    alternativeLabel.setText(builder.toString());
                }

            } else if (value instanceof JIPipeGraphNodeUI) {
                JIPipeGraphNode node = ((JIPipeGraphNodeUI) value).getNode();
                JIPipeNodeInfo info = node.getInfo();
                String menuPath = info.getCategory().getName();
                if (!StringUtils.isNullOrEmpty(info.getMenuPath())) {
                    menuPath += " > " + String.join(" > ", info.getMenuPath().split("\n"));
                }

                icon.setFillColor(UIUtils.getFillColorFor(info));
                icon.setBorderColor(UIUtils.getBorderColorFor(info));
                actionLabel.setText("Navigate");
                actionLabel.setForeground(Color.BLUE);
                algorithmLabel.setText(node.getName());
                algorithmLabel.setIcon(JIPipe.getNodes().getIconFor(info));
                menuLabel.setText(menuPath);
                alternativeLabel.setText("");
            } else if (value instanceof JIPipeNodeExample) {
                JIPipeNodeExample example = (JIPipeNodeExample) value;
                JIPipeNodeInfo info = example.getNodeInfo();
                String menuPath = info.getCategory().getName();
                if (!StringUtils.isNullOrEmpty(info.getMenuPath())) {
                    menuPath += " > " + String.join(" > ", info.getMenuPath().split("\n"));
                }

                icon.setFillColor(Color.WHITE);
                icon.setBorderColor(UIUtils.getFillColorFor(info));
                actionLabel.setText("Create");
                actionLabel.setForeground(new Color(0, 128, 0));
                algorithmLabel.setText(info.getName() + ": " + example.getNodeTemplate().getName());
                algorithmLabel.setIcon(JIPipe.getNodes().getIconFor(info));
                menuLabel.setText(menuPath);

                alternativeLabel.setForeground(ModernMetalTheme.PRIMARY5);
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
