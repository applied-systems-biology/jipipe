/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.grapheditor.general;

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.history.JIPipeHistoryJournal;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.GraphEditorUISettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.ZoomViewPort;
import org.hkijena.jipipe.ui.components.icons.SolidColorIcon;
import org.hkijena.jipipe.ui.components.search.SearchBox;
import org.hkijena.jipipe.ui.extension.GraphEditorToolBarButtonExtension;
import org.hkijena.jipipe.ui.grapheditor.general.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeNodeUI;
import org.hkijena.jipipe.ui.theme.ModernMetalTheme;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.CopyImageToClipboard;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jfree.graphics2d.svg.SVGUtils;

import javax.imageio.ImageIO;
import javax.swing.FocusManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A panel around {@link JIPipeGraphCanvasUI} that comes with scrolling/panning, properties panel,
 * and a menu bar
 */
public abstract class JIPipeGraphEditorUI extends JIPipeWorkbenchPanel implements MouseListener, MouseMotionListener {

    public static final int FLAGS_NONE = 0;
    public static final int FLAGS_SPLIT_PANE_VERTICAL = 1;
    public static final int FLAGS_SPLIT_PANE_RATIO_1_1 = 2;
    public static final int FLAGS_SPLIT_PANE_SWITCH_CONTENT = 4;

    public static final KeyStroke KEY_STROKE_UNDO = KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK, true);
    public static final KeyStroke KEY_STROKE_REDO = KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, true);
    public static final KeyStroke KEY_STROKE_AUTO_LAYOUT = KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, true);
    public static final KeyStroke KEY_STROKE_NAVIGATE = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true);
    public static final KeyStroke KEY_STROKE_ZOOM_IN = KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_MASK, false);
    public static final KeyStroke KEY_STROKE_ZOOM_OUT = KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_MASK, false);
    public static final KeyStroke KEY_STROKE_ZOOM_RESET = KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, InputEvent.CTRL_MASK, false);
    private final GraphEditorUISettings graphUISettings;
    private final JIPipeGraphCanvasUI canvasUI;
    private final JIPipeGraph algorithmGraph;
    private final SearchBox<Object> navigator = new SearchBox<>();
    private final JIPipeHistoryJournal historyJournal;
    private final int flags;
    private final JMenu graphMenu = new JMenu("Graph");
    protected JMenuBar menuBar = new JMenuBar();
    private JSplitPane splitPane;
    private JScrollPane scrollPane;
    private Point panningOffset = null;
    private Point panningScrollbarOffset = null;
    private boolean isPanning = false;
    private Set<JIPipeNodeInfo> addableAlgorithms = new HashSet<>();

    /**
     * @param workbenchUI    the workbench
     * @param algorithmGraph the algorithm graph
     * @param compartment    the graph compartment to display. Set to null to display all compartments
     * @param historyJournal object that tracks the history of this graph. Set to null to disable the undo feature.
     * @param flags          additional flags
     */
    public JIPipeGraphEditorUI(JIPipeWorkbench workbenchUI, JIPipeGraph algorithmGraph, UUID compartment, JIPipeHistoryJournal historyJournal, GraphEditorUISettings settings, int flags) {
        super(workbenchUI);
        this.algorithmGraph = algorithmGraph;
        this.historyJournal = historyJournal;
        this.flags = flags;
        this.canvasUI = new JIPipeGraphCanvasUI(getWorkbench(), this, algorithmGraph, compartment, historyJournal);
        this.graphUISettings = settings;
        initialize();
        reloadMenuBar();
        JIPipe.getNodes().getEventBus().register(this);
        algorithmGraph.getEventBus().register(this);
        updateNavigation();
        initializeHotkeys();
        SwingUtilities.invokeLater(() -> canvasUI.crop(true));
    }

    /**
     * @param workbenchUI    the workbench
     * @param algorithmGraph the algorithm graph
     * @param compartment    the graph compartment to display. Set to null to display all compartments
     * @param historyJournal object that tracks the history of this graph. Set to null to disable the undo feature.
     */
    public JIPipeGraphEditorUI(JIPipeWorkbench workbenchUI, JIPipeGraph algorithmGraph, UUID compartment, JIPipeHistoryJournal historyJournal) {
        this(workbenchUI, algorithmGraph, compartment, historyJournal, GraphEditorUISettings.getInstance(), JIPipeGraphEditorUI.FLAGS_NONE);
    }

    private static int[] rankNavigationEntry(Object value, String[] searchStrings) {
        if (searchStrings == null || searchStrings.length == 0)
            return new int[0];
        String nameHayStack;
        String menuHayStack;
        String descriptionHayStack;
        if (value instanceof JIPipeNodeUI) {
            JIPipeGraphNode node = ((JIPipeNodeUI) value).getNode();
            nameHayStack = node.getName();
            menuHayStack = node.getInfo().getCategory().getName() + "\n" + node.getInfo().getMenuPath();
            for (JIPipeNodeMenuLocation location : node.getInfo().getAliases()) {
                if (!StringUtils.isNullOrEmpty(location.getAlternativeName())) {
                    nameHayStack += location.getAlternativeName().toLowerCase();
                }
                menuHayStack += location.getMenuPath();
            }
            descriptionHayStack = StringUtils.orElse(node.getCustomDescription().getBody(), node.getInfo().getDescription().getBody());
        } else if (value instanceof JIPipeNodeInfo) {
            JIPipeNodeInfo info = (JIPipeNodeInfo) value;
            if (info.isHidden())
                return null;
            nameHayStack = StringUtils.orElse(info.getName(), "").toLowerCase();
            menuHayStack = info.getCategory().getName() + "\n" + info.getMenuPath();
            for (JIPipeNodeMenuLocation location : info.getAliases()) {
                if (!StringUtils.isNullOrEmpty(location.getAlternativeName())) {
                    nameHayStack += location.getAlternativeName().toLowerCase();
                }
                menuHayStack += location.getMenuPath();
            }
            descriptionHayStack = StringUtils.orElse(info.getDescription().getBody(), "").toLowerCase();
        } else if (value instanceof JIPipeNodeExample) {
            JIPipeNodeExample example = (JIPipeNodeExample) value;
            JIPipeNodeInfo info = example.getNodeInfo();
            if (info.isHidden())
                return null;
            nameHayStack = StringUtils.orElse(example.getNodeTemplate().getName() + info.getName(), "").toLowerCase();
            menuHayStack = info.getCategory().getName() + "\n" + info.getMenuPath();
            for (JIPipeNodeMenuLocation location : info.getAliases()) {
                if (!StringUtils.isNullOrEmpty(location.getAlternativeName())) {
                    nameHayStack += location.getAlternativeName().toLowerCase();
                }
                menuHayStack += location.getMenuPath();
            }
            descriptionHayStack = StringUtils.orElse(example.getNodeTemplate().getDescription().getBody() + info.getDescription().getBody(), "").toLowerCase();
        } else {
            return null;
        }

        nameHayStack = nameHayStack.toLowerCase();
        menuHayStack = menuHayStack.toLowerCase();
        descriptionHayStack = descriptionHayStack.toLowerCase();

        int[] ranks = new int[3];

        for (int i = 0; i < searchStrings.length; i++) {
            String string = searchStrings[i];
            if (nameHayStack.contains(string.toLowerCase()))
                --ranks[0];
            if (i == 0 && nameHayStack.startsWith(string.toLowerCase()))
                ranks[0] -= 2;
            if (menuHayStack.contains(string.toLowerCase()))
                --ranks[1];
            if (descriptionHayStack.contains(string.toLowerCase()))
                --ranks[2];
        }

        if (ranks[0] == 0 && ranks[1] == 0 && ranks[2] == 0)
            return null;

        return ranks;
    }

    public static void installContextActionsInto(JToolBar toolBar, Set<JIPipeNodeUI> selection, List<NodeUIContextAction> actionList, JIPipeGraphCanvasUI canvasUI) {
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
            UIUtils.addPopupMenuToComponent(button, menu);
            toolBar.add(Box.createHorizontalStrut(4), 0);
            toolBar.add(button, 0);
        }
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
        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.addKeyEventDispatcher(e -> {
            KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
            if (this.isDisplayable() && FocusManager.getCurrentManager().getFocusOwner() == canvasUI) {
                if (Objects.equals(keyStroke, KEY_STROKE_UNDO)) {
                    undo();
                    return true;
                } else if (Objects.equals(keyStroke, KEY_STROKE_REDO)) {
                    redo();
                    return true;
                } else if (Objects.equals(keyStroke, KEY_STROKE_AUTO_LAYOUT)) {
                    getWorkbench().sendStatusBarText("Auto-layout");
                    canvasUI.autoLayoutAll();
                    return true;
                } else if (Objects.equals(keyStroke, KEY_STROKE_NAVIGATE)) {
                    navigator.requestFocusInWindow();
                    return true;
                } else if (Objects.equals(keyStroke, KEY_STROKE_ZOOM_IN)) {
                    canvasUI.zoomIn();
                } else if (Objects.equals(keyStroke, KEY_STROKE_ZOOM_OUT)) {
                    canvasUI.zoomOut();
                } else if (Objects.equals(keyStroke, KEY_STROKE_ZOOM_RESET)) {
                    canvasUI.resetZoom();
                }
            }
            return false;
        });
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
        double splitPaneRatio = isFlagSet(FLAGS_SPLIT_PANE_RATIO_1_1) ? AutoResizeSplitPane.RATIO_1_TO_1 : AutoResizeSplitPane.RATIO_3_TO_1;
        splitPane = new AutoResizeSplitPane(splitPaneSplit, splitPaneRatio);

        canvasUI.fullRedraw();
        canvasUI.getEventBus().register(this);
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
        navigator.setModel(new DefaultComboBoxModel<>());
        navigator.setDataToString(o -> {
            if (o instanceof JIPipeNodeInfo) {
                return ((JIPipeNodeInfo) o).getName();
            } else if (o instanceof JIPipeNodeUI) {
                return ((JIPipeNodeUI) o).getNode().getName();
            } else if (o instanceof JIPipeNodeExample) {
                return ((JIPipeNodeExample) o).getNodeTemplate().getName() + ((JIPipeNodeExample) o).getNodeInfo().getName();
            } else {
                return "" + o;
            }
        });
        navigator.setRenderer(new NavigationRenderer());
        navigator.getEventBus().register(this);
        navigator.setRankingFunction(JIPipeGraphEditorUI::rankNavigationEntry);

        initializeAnnotationTools();
    }

    private void initializeAnnotationTools() {
        JToolBar toolBar = new JToolBar(null, JToolBar.VERTICAL);
        add(toolBar, BorderLayout.WEST);
    }

    /**
     * Triggered when the user selected something in the navigator
     *
     * @param event the event
     */
    @Subscribe
    public void onNavigatorNavigate(SearchBox.SelectedEvent<Object> event) {
        if (event.getValue() instanceof JIPipeNodeUI) {
            selectOnly((JIPipeNodeUI) event.getValue());
            navigator.setSelectedItem(null);
        } else if (event.getValue() instanceof JIPipeNodeInfo) {
            if (!JIPipeProjectWorkbench.canAddOrDeleteNodes(getWorkbench()))
                return;
            JIPipeNodeInfo info = (JIPipeNodeInfo) event.getValue();
            JIPipeGraphNode node = info.newInstance();
            if (getHistoryJournal() != null) {
                getHistoryJournal().snapshotBeforeAddNode(node, getCompartment());
            }
            canvasUI.getScheduledSelection().clear();
            canvasUI.getScheduledSelection().add(node);
            algorithmGraph.insertNode(node, getCompartment());
            navigator.setSelectedItem(null);
        } else if (event.getValue() instanceof JIPipeNodeExample) {
            if (!JIPipeProjectWorkbench.canAddOrDeleteNodes(getWorkbench()))
                return;
            JIPipeNodeExample example = (JIPipeNodeExample) event.getValue();
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
            algorithmGraph.insertNode(node, getCompartment());
            navigator.setSelectedItem(null);
        }
    }

    public Set<JIPipeNodeUI> getSelection() {
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

        menuBar.add(navigator);
        navigator.setVisible(graphUISettings.getSearchSettings().isEnableSearch());
        menuBar.add(Box.createHorizontalStrut(8));

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

        menuBar.add(UIUtils.createVerticalSeparator());

        graphMenu.setIcon(UIUtils.getIconFromResources("actions/configure.png"));
        menuBar.add(graphMenu);

        initializeCenterViewCommand(graphMenu);
        initializeToggleHideEdgesCommand(graphMenu);
        initializeToggleShowLabelsCommand(graphMenu);
        initializeExportMenu(graphMenu);
        initializeLayoutMenu(graphMenu);
        initializeSearchMenu(graphMenu);

        menuBar.add(UIUtils.createVerticalSeparator());

        initializeZoomMenu();
    }

    private void initializeToggleShowLabelsCommand(JMenu graphMenu) {
        JCheckBoxMenuItem toggle = new JCheckBoxMenuItem("Show input labels", graphUISettings.isAutoHideDrawLabels());
        toggle.addActionListener(e -> {
            graphUISettings.setAutoHideDrawLabels(toggle.getState());
            canvasUI.repaint(50);
        });
        graphMenu.add(toggle);
    }

    private void initializeToggleHideEdgesCommand(JMenu graphMenu) {
        JCheckBoxMenuItem toggle = new JCheckBoxMenuItem("Auto-hide edges", graphUISettings.isAutoHideEdgeEnabled());
        toggle.addActionListener(e -> {
            graphUISettings.setAutoHideEdgeEnabled(toggle.getState());
            canvasUI.repaint(50);
        });
        graphMenu.add(toggle);
    }

    private void initializeZoomMenu() {
        JButton zoomOutButton = new JButton(UIUtils.getIconFromResources("actions/zoom-out.png"));
        UIUtils.makeFlat25x25(zoomOutButton);
        zoomOutButton.setToolTipText("<html>Zoom out<br><i>Ctrl-NumPad -</i></html>");
        zoomOutButton.addActionListener(e -> canvasUI.zoomOut());
        menuBar.add(zoomOutButton);

        JButton zoomButton = new JButton((int) (canvasUI.getZoom() * 100) + "%");
        zoomButton.setToolTipText("<html>Change zoom<br>Reset zoom: <i>Ctrl-NumPad 0</i></html>");
        canvasUI.getEventBus().register(new Object() {
            @Subscribe
            public void onZoomChanged(ZoomViewPort.ZoomChangedEvent event) {
                zoomButton.setText((int) (canvasUI.getZoom() * 100) + "%");
            }
        });
        zoomButton.setBorder(null);
        JPopupMenu zoomMenu = UIUtils.addPopupMenuToComponent(zoomButton);
        for (double zoom = 0.5; zoom <= 2; zoom += 0.25) {
            JMenuItem changeZoomItem = new JMenuItem((int) (zoom * 100) + "%", UIUtils.getIconFromResources("actions/zoom.png"));
            double finalZoom = zoom;
            changeZoomItem.addActionListener(e -> canvasUI.setZoom(finalZoom));
            zoomMenu.add(changeZoomItem);
        }
        zoomMenu.addSeparator();
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

        JButton zoomInButton = new JButton(UIUtils.getIconFromResources("actions/zoom-in.png"));
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
            JIPipe.getInstance().getSettingsRegistry().save();
            navigator.setVisible(graphUISettings.getSearchSettings().isEnableSearch());
            menuBar.revalidate();
        });
        searchMenu.add(searchEnabledItem);

        JMenuItem searchFindNewNodes = new JCheckBoxMenuItem("Search can create new nodes");
        searchFindNewNodes.setSelected(graphUISettings.getSearchSettings().isSearchFindNewNodes());
        searchFindNewNodes.addActionListener(e -> {
            graphUISettings.getSearchSettings().setSearchFindNewNodes(searchFindNewNodes.isSelected());
            JIPipe.getInstance().getSettingsRegistry().save();
            updateNavigation();
        });
        searchMenu.add(searchFindNewNodes);

        JMenuItem searchFindExistingNodes = new JCheckBoxMenuItem("Search can find existing nodes");
        searchFindExistingNodes.setSelected(graphUISettings.getSearchSettings().isSearchFindExistingNodes());
        searchFindExistingNodes.addActionListener(e -> {
            graphUISettings.getSearchSettings().setSearchFindExistingNodes(searchFindExistingNodes.isSelected());
            JIPipe.getInstance().getSettingsRegistry().save();
            updateNavigation();
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
    public JIPipeGraph getAlgorithmGraph() {
        return algorithmGraph;
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

    @Subscribe
    public void onSelectionChanged(JIPipeGraphCanvasUI.AlgorithmSelectionChangedEvent event) {
        updateSelection();
    }

    /**
     * Should be triggered when new algorithms are registered.
     * Reloads the menu
     *
     * @param event Generated event
     */
    @Subscribe
    public void onAlgorithmRegistryChanged(JIPipe.NodeInfoRegisteredEvent event) {
        reloadMenuBar();
        getWorkbench().sendStatusBarText("Plugins were updated");
    }

    /**
     * Should be triggered when an algorithm was selected
     *
     * @param event The generated event
     */
    @Subscribe
    public void onAlgorithmSelected(JIPipeGraphCanvasUI.AlgorithmSelectedEvent event) {
        if (event.getUi() != null) {
            if (event.isAddToSelection()) {
                if (canvasUI.getSelection().contains(event.getUi())) {
                    removeFromSelection(event.getUi());
                } else {
                    addToSelection(event.getUi());
                }
            } else {
                selectOnly(event.getUi());
            }
        } else {
            clearSelection();
        }
    }

    /**
     * Triggered when something interesting happens in the graph and the UI should scroll to it
     *
     * @param event generated event
     */
    @Subscribe
    public void onAlgorithmEvent(JIPipeNodeUI.AlgorithmEvent event) {
//        if (event.getUi() != null) {
//            scrollToAlgorithm(event.getUi());
//        }
    }

    /**
     * Scrolls to the specified algorithm UI
     *
     * @param ui the algorithm
     */
    public void scrollToAlgorithm(JIPipeNodeUI ui) {
        if (scrollPane == null)
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
    public void selectOnly(JIPipeNodeUI ui) {
        canvasUI.selectOnly(ui);
        scrollToAlgorithm(ui);
    }

    /**
     * Removes an algorithm from the selection
     *
     * @param ui The algorithm UI
     */
    public void removeFromSelection(JIPipeNodeUI ui) {
        canvasUI.removeFromSelection(ui);
    }

    protected Component getPropertyPanel() {
        return splitPane.getRightComponent();
    }

    /**
     * Sets the component displayed in the right property panel
     *
     * @param content the component
     */
    protected void setPropertyPanel(Component content) {
        int dividerLocation = splitPane.getDividerLocation();
        if (isFlagSet(FLAGS_SPLIT_PANE_SWITCH_CONTENT)) {
            splitPane.setLeftComponent(content);
        } else {
            splitPane.setRightComponent(content);
        }
        splitPane.setDividerLocation(dividerLocation);
    }

    /**
     * Adds an algorithm to the selection
     *
     * @param ui The algorithm UI
     */
    public void addToSelection(JIPipeNodeUI ui) {
        canvasUI.addToSelection(ui);
    }

    /**
     * Should be triggered when the algorithm graph is changed
     *
     * @param event The generated event
     */
    @Subscribe
    public void onGraphChanged(JIPipeGraph.GraphChangedEvent event) {
        updateNavigation();
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
        updateNavigation();
    }

    /**
     * Updates the navigation list
     */
    public void updateNavigation() {
        boolean canCreateNewNodes = graphUISettings.getSearchSettings().isSearchFindNewNodes();
        if (canCreateNewNodes) {
            if (getWorkbench() instanceof JIPipeProjectWorkbench) {
                canCreateNewNodes = !((JIPipeProjectWorkbench) getWorkbench()).getProject().getMetadata().getPermissions().isPreventAddingDeletingNodes();
            }
        }
        DefaultComboBoxModel<Object> model = new DefaultComboBoxModel<>();
        model.removeAllElements();
        if (graphUISettings.getSearchSettings().isSearchFindExistingNodes()) {
            for (JIPipeNodeUI ui : canvasUI.getNodeUIs().values().stream().sorted(Comparator.comparing(ui -> ui.getNode().getName())).collect(Collectors.toList())) {
                model.addElement(ui);
            }
        }
        if (canCreateNewNodes) {
            for (JIPipeNodeInfo info : addableAlgorithms.stream()
                    .sorted(Comparator.comparing(JIPipeNodeInfo::getName)).collect(Collectors.toList())) {
                model.addElement(info);
                if (getWorkbench() instanceof JIPipeProjectWorkbench) {
                    for (JIPipeNodeExample example : ((JIPipeProjectWorkbench) getWorkbench()).getProject().getNodeExamples(info.getId())) {
                        model.addElement(example);
                    }
                }
            }
        }
        navigator.setModel(model);
    }

    public JIPipeHistoryJournal getHistoryJournal() {
        return historyJournal;
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

            } else if (value instanceof JIPipeNodeUI) {
                JIPipeGraphNode node = ((JIPipeNodeUI) value).getNode();
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
