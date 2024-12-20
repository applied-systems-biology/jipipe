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
import org.hkijena.jipipe.api.grapheditortool.JIPipeActionGraphEditorTool;
import org.hkijena.jipipe.api.grapheditortool.JIPipeDefaultGraphEditorTool;
import org.hkijena.jipipe.api.grapheditortool.JIPipeGraphEditorTool;
import org.hkijena.jipipe.api.grapheditortool.JIPipeToggleableGraphEditorTool;
import org.hkijena.jipipe.api.history.JIPipeHistoryJournal;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.commons.components.icons.SolidColorIcon;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeDesktopGenericListCellRenderer;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernMetalTheme;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameter;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeGraphEditorUIApplicationSettings;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;
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
import java.util.List;
import java.util.*;

/**
 * A panel around {@link JIPipeDesktopGraphCanvasUI} that comes with scrolling/panning, properties panel,
 * and a menu bar
 */
public abstract class AbstractJIPipeDesktopGraphEditorUI extends JIPipeDesktopWorkbenchPanel implements MouseListener, MouseMotionListener, Disposable, JIPipeGraph.GraphChangedEventListener,
        JIPipeDesktopGraphCanvasUI.NodeSelectionChangedEventListener,
        JIPipeDesktopGraphCanvasUI.NodeUISelectedEventListener,
        JIPipeDesktopGraphNodeUI.DefaultNodeUIActionRequestedEventListener,
        JIPipeDesktopGraphNodeUI.NodeUIActionRequestedEventListener,
        JIPipeDesktopDockPanel.StateSavedEventListener, JIPipeDesktopDockPanel.PanelSideVisibilityChangedEventListener {

    public static final String DOCK_LOG = "LOG";
    public static final String DOCK_HISTORY = "HISTORY";
    public static final String DOCK_BOOKMARKS = "BOOKMARKS";
    public static final String DOCK_MAP = "MAP";
    public static final String DOCK_ERRORS = "ERRORS";
    public static final String DOCK_CALCULATOR = "CALCULATOR";

    public static final KeyStroke KEY_STROKE_UNDO = KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK, true);
    public static final KeyStroke KEY_STROKE_REDO = KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, true);
    public static final KeyStroke KEY_STROKE_ZOOM_IN = KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_MASK, false);
    public static final KeyStroke KEY_STROKE_ZOOM_OUT = KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_MASK, false);
    public static final KeyStroke KEY_STROKE_ZOOM_RESET = KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, InputEvent.CTRL_MASK, false);
    public static final KeyStroke KEY_STROKE_MOVE_SELECTION_LEFT = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false);
    public static final KeyStroke KEY_STROKE_MOVE_SELECTION_RIGHT = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false);
    public static final KeyStroke KEY_STROKE_MOVE_SELECTION_UP = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false);
    public static final KeyStroke KEY_STROKE_MOVE_SELECTION_DOWN = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false);

    private final JIPipeGraphEditorUIApplicationSettings graphUISettings;
    private final JIPipeDesktopGraphCanvasUI canvasUI;
    private final JIPipeGraph graph;
    private final JIPipeHistoryJournal historyJournal;
    private final List<JIPipeGraphEditorTool> tools = new ArrayList<>();
    private final Map<Class<? extends JIPipeGraphEditorTool>, JIPipeGraphEditorTool> toolMap = new HashMap<>();
    private final BiMap<JIPipeToggleableGraphEditorTool, JToggleButton> toolToggles = HashBiMap.create();
    private final JToolBar toolBar = new JToolBar();
    private final JIPipeDesktopDockPanel dockPanel = new JIPipeDesktopDockPanel();
    private final List<JButton> contextToolbarButtons = new ArrayList<>();
    private JScrollPane scrollPane;
    private Point panningOffset = null;
    private Point panningScrollbarOffset = null;
    private boolean isPanning = false;
    private Set<JIPipeNodeInfo> addableAlgorithms = new HashSet<>();
    private JIPipeToggleableGraphEditorTool currentTool;
    private int contextToolbarInsertLocation;

    /**
     * @param workbenchUI    the workbench
     * @param graph          the algorithm graph
     * @param compartment    the graph compartment to display. Set to null to display all compartments
     * @param historyJournal object that tracks the history of this graph. Set to null to disable the undo feature.
     */
    public AbstractJIPipeDesktopGraphEditorUI(JIPipeDesktopWorkbench workbenchUI, JIPipeGraph graph, UUID compartment, JIPipeHistoryJournal historyJournal, JIPipeGraphEditorUIApplicationSettings settings) {
        super(workbenchUI);
        this.graph = graph;
        this.historyJournal = historyJournal;
        this.canvasUI = new JIPipeDesktopGraphCanvasUI(getDesktopWorkbench(), this, graph, compartment, historyJournal);
        this.graphUISettings = settings;

        initialize();
        graph.getGraphChangedEventEmitter().subscribeWeak(this);

        initializeHotkeys();
        SwingUtilities.invokeLater(() -> {
            canvasUI.crop(true);
            selectDefaultTool();
        });

        restoreDockStateFromSettings();

        dockPanel.getPanelSideVisibilityChangedEventEmitter().subscribe(this);
    }

    /**
     * @param workbenchUI    the workbench
     * @param graph          the algorithm graph
     * @param compartment    the graph compartment to display. Set to null to display all compartments
     * @param historyJournal object that tracks the history of this graph. Set to null to disable the undo feature.
     */
    public AbstractJIPipeDesktopGraphEditorUI(JIPipeDesktopWorkbench workbenchUI, JIPipeGraph graph, UUID compartment, JIPipeHistoryJournal historyJournal) {
        this(workbenchUI, graph, compartment, historyJournal, JIPipeGraphEditorUIApplicationSettings.getInstance());
    }

    protected abstract void restoreDockStateFromSettings();

    protected abstract void saveDockStateToSettings();

    @Override
    public void dispose() {
        graph.getGraphChangedEventEmitter().unsubscribe(this);
        canvasUI.dispose();
    }

    @Override
    public void onDockPanelStateSaved(JIPipeDesktopDockPanel.StateSavedEvent event) {
        saveDockStateToSettings();
    }

    public JIPipeGraphEditorUIApplicationSettings getGraphUISettings() {
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

    public JIPipeDesktopGraphCanvasUI getCanvasUI() {
        return canvasUI;
    }

    private void initialize() {
        setLayout(new BorderLayout());

        toolBar.setFloatable(false);

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

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(toolBar, BorderLayout.NORTH);

        dockPanel.setFloatingPanelMarginTop(40);
        dockPanel.setBackgroundComponent(mainPanel);
        dockPanel.getStateSavedEventEmitter().subscribe(this);
        dockPanel.setShowToolbarLabels(graphUISettings.getDockLayoutSettings().isShowToolbarLabels());
        dockPanel.getParameterChangedEventEmitter().subscribeLambda((emitter, event) -> {
            if ("show-toolbar-labels".equals(event.getKey())) {
                if (graphUISettings.getDockLayoutSettings().isShowToolbarLabels() != dockPanel.isShowToolbarLabels()) {
                    graphUISettings.getDockLayoutSettings().setShowToolbarLabels(dockPanel.isShowToolbarLabels());
                    JIPipe.getSettings().saveLater();
                }
            }
        });

        add(dockPanel, BorderLayout.CENTER);

        initializeEditingToolbar(JIPipeToggleableGraphEditorTool.class);
        toolBar.add(UIUtils.createVerticalSeparator());
        contextToolbarInsertLocation = toolBar.getComponentCount();
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(UIUtils.createVerticalSeparator());
        initializeEditingToolbar(JIPipeActionGraphEditorTool.class);
        toolBar.addSeparator();
        initializeCommonToolbar();
    }

    private void initializeCommonToolbar() {
        if (getHistoryJournal() != null) {
            JButton undoButton = new JButton(UIUtils.getIconFromResources("actions/undo.png"));
            undoButton.setToolTipText("<html>Undo<br><i>Ctrl-Z</i></html>");
            UIUtils.makeButtonFlatWithSize(undoButton, 32, 3);
            undoButton.addActionListener(e -> undo());
            toolBar.add(undoButton);

            JButton redoButton = new JButton(UIUtils.getIconFromResources("actions/edit-redo.png"));
            redoButton.setToolTipText("<html>Redo<br><i>Ctrl-Shift-Z</i></html>");
            UIUtils.makeButtonFlatWithSize(redoButton, 32, 3);
            redoButton.addActionListener(e -> redo());
            toolBar.add(redoButton);

            toolBar.addSeparator();
        }
        {
            JToggleButton lockAnnotationsToggle = new JToggleButton(UIUtils.getIconFromResources("actions/lock-comments.png"));
            UIUtils.makeButtonFlatWithSize(lockAnnotationsToggle, 32, 32);
            lockAnnotationsToggle.setToolTipText("If enabled, you will not be able to accidentally select or modify graph annotations");
            lockAnnotationsToggle.addActionListener(e -> {
                canvasUI.setGraphAnnotationsLocked(lockAnnotationsToggle.isSelected());
            });
            toolBar.add(lockAnnotationsToggle);
        }
        initializeCommonToolbarLayout();
        initializeCommonToolbarExport();
        toolBar.addSeparator();
        initializeCommonToolbarZoom();
        toolBar.add(Box.createHorizontalStrut(8));
    }

    private void initializeCommonToolbarLayout() {

        JPopupMenu layoutMenu = new JPopupMenu();

        JButton layoutButton = new JButton(UIUtils.getIconFromResources("actions/sidebar.png"));
        UIUtils.makeButtonFlatWithSize(layoutButton, 32, 3);
        toolBar.add(layoutButton);

        UIUtils.addReloadablePopupMenuToButton(layoutButton, layoutMenu, () -> {
            layoutMenu.removeAll();

            layoutMenu.add(UIUtils.createMenuItem("Reset layout", "Resets the layout", UIUtils.getIconFromResources("actions/edit-clear-history.png"), () -> {
                restoreDefaultDockState();
                saveDockStateToSettings();
            }));

            if (getDockStateTemplates() != null) {

                for (StringAndStringPairParameter template : getDockStateTemplates()) {
                    layoutMenu.add(UIUtils.createMenuItem(template.getKey(), "Loads the layout", UIUtils.getIconFromResources("actions/sidebar.png"), () -> restoreDockStateTemplate(template.getValue())));
                }

                layoutMenu.addSeparator();

                layoutMenu.add(UIUtils.createMenuItem("Save layout ...", "Saves the current layout", UIUtils.getIconFromResources("actions/filesave.png"), this::createDockStateTemplate));
                layoutMenu.add(UIUtils.createMenuItem("Manage layouts ...", "Manages the list of layouts", UIUtils.getIconFromResources("actions/configure.png"), this::manageDockStateTemplates));
            }

            layoutMenu.addSeparator();

            JMenuItem autoLayoutItem = new JMenuItem("Auto-layout all nodes", UIUtils.getIconFromResources("actions/distribute-unclump.png"));
            autoLayoutItem.addActionListener(e -> {
                if (getHistoryJournal() != null) {
                    getHistoryJournal().snapshot("Auto-layout", "Apply auto-layout", getCompartment(), UIUtils.getIconFromResources("actions/distribute-unclump.png"));
                }
                canvasUI.autoLayoutAll();
            });
            layoutMenu.add(autoLayoutItem);

            JCheckBoxMenuItem layoutOnConnectItem = new JCheckBoxMenuItem("Layout nodes on connect",
                    UIUtils.getIconFromResources("actions/connector-avoid.png"),
                    JIPipeGraphEditorUIApplicationSettings.getInstance().isLayoutAfterConnect());
            layoutOnConnectItem.setToolTipText("Auto-layout layout on making data slot connections");
            layoutOnConnectItem.addActionListener(e -> {
                JIPipeGraphEditorUIApplicationSettings.getInstance().setLayoutAfterConnect(layoutOnConnectItem.isSelected());
            });

            layoutMenu.add(layoutOnConnectItem);

            JCheckBoxMenuItem layoutOnAlgorithmFinderItem = new JCheckBoxMenuItem("Layout nodes on 'Find matching node'",
                    UIUtils.getIconFromResources("actions/connector-avoid.png"),
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
            getDockPanel().restoreState(state);
            saveDockStateToSettings();
        } catch (Throwable e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Unable to restore template!", "Load template", JOptionPane.ERROR_MESSAGE);
        }
    }

    protected abstract StringAndStringPairParameter.List getDockStateTemplates();

    private void manageDockStateTemplates() {
        List<StringAndStringPairParameter> toDelete = UIUtils.getSelectionByDialog(this,
                getDockStateTemplates(),
                Collections.emptyList(),
                "Delete layout templates",
                "Please select the layout to delete",
                new JIPipeDesktopGenericListCellRenderer<>(kv -> new JIPipeDesktopGenericListCellRenderer.RenderedItem(UIUtils.getIconFromResources("actions/sidebar.png"), kv.getKey())),
                ListSelectionMode.MultipleInterval);
        getDockStateTemplates().removeAll(toDelete);
        JIPipe.getSettings().save();
    }

    private void createDockStateTemplate() {
        String newValue = StringUtils.nullToEmpty(JOptionPane.showInputDialog(this, "Please input the name of the layout:", "")).trim();
        if (!StringUtils.isNullOrEmpty(newValue)) {
            JIPipeDesktopDockPanel.State currentState = getDockPanel().getCurrentState();
            getDockStateTemplates().add(new StringAndStringPairParameter(newValue, JsonUtils.toJsonString(currentState)));
            JIPipe.getSettings().save();
        }
    }

    protected abstract void restoreDefaultDockState();

    private void initializeCommonToolbarExport() {
        JButton snapshotButton = new JButton(UIUtils.getIconFromResources("actions/camera.png"));
        UIUtils.makeButtonFlatWithSize(snapshotButton, 32, 3);
        JPopupMenu popupMenu = UIUtils.addPopupMenuToButton(snapshotButton);

        JMenuItem exportToClipboardItem = new JMenuItem("Copy snapshot to clipboard", UIUtils.getIconFromResources("actions/edit-copy.png"));
        exportToClipboardItem.addActionListener(e -> createScreenshotClipboard());
        popupMenu.add(exportToClipboardItem);
        JMenuItem exportAsPngItem = new JMenuItem("Export as *.png", UIUtils.getIconFromResources("actions/viewimage.png"));
        exportAsPngItem.addActionListener(e -> createScreenshotPNG());
        popupMenu.add(exportAsPngItem);
        JMenuItem exportAsSvgItem = new JMenuItem("Export as *.svg", UIUtils.getIconFromResources("actions/viewimage.png"));
        exportAsSvgItem.addActionListener(e -> createScreenshotSVG());
        popupMenu.add(exportAsSvgItem);

        toolBar.add(snapshotButton);
    }

    private void initializeCommonToolbarZoom() {

        JPanel zoomPanel = new JPanel(new BorderLayout());
        zoomPanel.setBorder(UIUtils.createControlBorder());
        zoomPanel.setPreferredSize(new Dimension(100, 32));
        zoomPanel.setMaximumSize(new Dimension(100, 32));

        JButton zoomOutButton = new JButton(UIUtils.getIconFromResources("actions/square-minus.png"));
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
        zoomPanel.add(zoomButton, BorderLayout.CENTER);

        JButton zoomInButton = new JButton(UIUtils.getIconFromResources("actions/square-plus.png"));
        UIUtils.makeButtonFlat25x25(zoomInButton);
        zoomInButton.setToolTipText("<html>Zoom in<br><i>Ctrl-NumPad +</i></html>");
        zoomInButton.addActionListener(e -> canvasUI.zoomIn());
        zoomPanel.add(zoomInButton, BorderLayout.EAST);

        toolBar.add(zoomPanel);
    }

    private void initializeEditingToolbar(Class<? extends JIPipeGraphEditorTool> baseClass) {
        List<JIPipeGraphEditorTool> newTools = new ArrayList<>();
        for (Class<? extends JIPipeGraphEditorTool> klass : JIPipe.getInstance().getGraphEditorToolRegistry().getRegisteredTools()) {
            if (baseClass.isAssignableFrom(klass) && !toolMap.containsKey(klass)) {
                JIPipeGraphEditorTool tool = (JIPipeGraphEditorTool) ReflectionUtils.newInstance(klass);
                if (tool.supports(this)) {
                    tool.setGraphEditor(this);
                    toolMap.put(klass, tool);
                    newTools.add(tool);
                }
            }
        }
        newTools.sort(Comparator.comparing(JIPipeGraphEditorTool::getCategory).thenComparing(JIPipeGraphEditorTool::getPriority));
        tools.addAll(newTools);
        for (int i = 0; i < newTools.size(); i++) {
            JIPipeGraphEditorTool tool = newTools.get(i);
            if (i > 0 && !Objects.equals(tool.getCategory(), newTools.get(i - 1).getCategory())) {
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

                JToggleButton button = new JToggleButton(tool.getIcon());
                button.setToolTipText(TooltipUtils.createTooltipWithShortcut(tool.getName(), tool.getTooltip(), keyBinding));
                button.addActionListener(e -> selectTool(tool));
                UIUtils.makeButtonFlatWithSize(button, 32, 3);
                toolBar.add(button);
                toolToggles.put(toggleableGraphEditorTool, button);

            } else {
                JButton button = new JButton(tool.getIcon());
                button.setToolTipText(TooltipUtils.createTooltipWithShortcut(tool.getName(), tool.getTooltip(), keyBinding));
                button.addActionListener(e -> selectTool(tool));
                UIUtils.makeButtonFlatWithSize(button, 32, 3);
                toolBar.add(button);
            }

        }
    }

    public JIPipeDesktopDockPanel getDockPanel() {
        return dockPanel;
    }

    public List<JIPipeGraphEditorTool> getTools() {
        return Collections.unmodifiableList(tools);
    }

    public JIPipeGraphEditorTool getDefaultTool() {
        return tools.stream().filter(tool -> tool instanceof JIPipeDefaultGraphEditorTool).findFirst().orElse(null);
    }

    public void selectDefaultTool() {
        selectTool(getDefaultTool());
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
            getDesktopWorkbench().sendStatusBarText("Activated tool '" + tool.getName() + "'");
        } else {
            tool.activate();
            getDesktopWorkbench().sendStatusBarText("Activated tool '" + tool.getName() + "'");
        }
    }

    public JIPipeToggleableGraphEditorTool getCurrentTool() {
        return currentTool;
    }

    public Set<JIPipeDesktopGraphNodeUI> getSelection() {
        return canvasUI.getSelection();
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
        Path selectedPath = JIPipeFileChooserApplicationSettings.saveFile(this, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Export graph as SVG (*.svg)", UIUtils.EXTENSION_FILTER_SVG);
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
        Path selectedPath = JIPipeFileChooserApplicationSettings.saveFile(this, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Export graph as PNG (*.png)", UIUtils.EXTENSION_FILTER_PNG);
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

        Set<JIPipeDesktopGraphNodeUI> selection = getSelection();
        for (NodeUIContextAction contextAction : canvasUI.getContextActions()) {
            if (contextAction != null && contextAction.isDisplayedInToolbar() && contextAction.matches(selection)) {
                JButton button = new JButton(contextAction.getIcon());
                button.setToolTipText(TooltipUtils.createTooltipWithShortcut(contextAction.getName(), contextAction.getDescription(), contextAction.getKeyboardShortcut()));
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
        if (scrollPane == null)
            return;
        if (ui == null)
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
    public void selectOnly(JIPipeDesktopGraphNodeUI ui) {
        canvasUI.selectOnly(ui);
        scrollToAlgorithm(ui);
    }

    /**
     * Removes an algorithm from the selection
     *
     * @param ui The algorithm UI
     */
    public void removeFromSelection(JIPipeDesktopGraphNodeUI ui) {
        canvasUI.removeFromSelection(ui);
    }

    /**
     * Adds an algorithm to the selection
     *
     * @param ui The algorithm UI
     */
    public void addToSelection(JIPipeDesktopGraphNodeUI ui) {
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
    public void onGraphCanvasNodeSelectionChanged(JIPipeDesktopGraphCanvasUI.NodeSelectionChangedEvent event) {
        updateSelection();
    }

    @Override
    public void onNodeUISelected(JIPipeDesktopGraphCanvasUI.NodeUISelectedEvent event) {
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
    public void onDefaultNodeUIActionRequested(JIPipeDesktopGraphNodeUI.DefaultNodeUIActionRequestedEvent event) {

    }

    @Override
    public void onNodeUIActionRequested(JIPipeDesktopGraphNodeUI.NodeUIActionRequestedEvent event) {

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
            alternativeLabel.setForeground(JIPipeDesktopModernMetalTheme.PRIMARY6);
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

                alternativeLabel.setForeground(JIPipeDesktopModernMetalTheme.PRIMARY6);
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

            } else if (value instanceof JIPipeDesktopGraphNodeUI) {
                JIPipeGraphNode node = ((JIPipeDesktopGraphNodeUI) value).getNode();
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

                alternativeLabel.setForeground(JIPipeDesktopModernMetalTheme.PRIMARY5);
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
