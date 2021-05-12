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

package org.hkijena.jipipe.ui.grapheditor;

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.history.AddNodeGraphHistorySnapshot;
import org.hkijena.jipipe.api.history.MoveNodesGraphHistorySnapshot;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.GraphEditorUISettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.ColorIcon;
import org.hkijena.jipipe.ui.components.SearchBox;
import org.hkijena.jipipe.ui.components.ZoomViewPort;
import org.hkijena.jipipe.ui.extension.GraphEditorToolBarButtonExtension;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
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

    public static final KeyStroke KEY_STROKE_UNDO = KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK, true);
    public static final KeyStroke KEY_STROKE_REDO = KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, true);
    public static final KeyStroke KEY_STROKE_AUTO_LAYOUT = KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, true);
    public static final KeyStroke KEY_STROKE_NAVIGATE = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true);
    public static final KeyStroke KEY_STROKE_ZOOM_IN = KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_MASK, false);
    public static final KeyStroke KEY_STROKE_ZOOM_OUT = KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_MASK, false);
    public static final KeyStroke KEY_STROKE_ZOOM_RESET = KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, InputEvent.CTRL_MASK, false);

    protected JMenuBar menuBar = new JMenuBar();
    private JIPipeGraphCanvasUI canvasUI;
    private JIPipeGraph algorithmGraph;

    private JSplitPane splitPane;
    private JScrollPane scrollPane;
    private Point panningOffset = null;
    private Point panningScrollbarOffset = null;
    private boolean isPanning = false;
    private JToggleButton switchPanningDirectionButton;

    private Set<JIPipeNodeInfo> addableAlgorithms = new HashSet<>();
    private SearchBox<Object> navigator = new SearchBox<>();

    /**
     * @param workbenchUI    the workbench
     * @param algorithmGraph the algorithm graph
     * @param compartment    the graph compartment to display. Set to null to display all compartments
     */
    public JIPipeGraphEditorUI(JIPipeWorkbench workbenchUI, JIPipeGraph algorithmGraph, UUID compartment) {
        super(workbenchUI);
        this.algorithmGraph = algorithmGraph;
        this.canvasUI = new JIPipeGraphCanvasUI(getWorkbench(), algorithmGraph, compartment);
        initialize();
        reloadMenuBar();
        JIPipe.getNodes().getEventBus().register(this);
        algorithmGraph.getEventBus().register(this);
        updateNavigation();
        initializeHotkeys();
        SwingUtilities.invokeLater(canvasUI::crop);
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

    private void initialize() {
        setLayout(new BorderLayout());
        splitPane = new JSplitPane();
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.33);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.66);
            }
        });

        canvasUI.fullRedraw();
        canvasUI.getEventBus().register(this);
        canvasUI.addMouseListener(this);
        canvasUI.addMouseMotionListener(this);
        scrollPane = new JScrollPane(canvasUI);
        scrollPane.getVerticalScrollBar().setUnitIncrement(25);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(25);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        canvasUI.setScrollPane(scrollPane);
        splitPane.setLeftComponent(scrollPane);
        splitPane.setRightComponent(new JPanel());
        add(splitPane, BorderLayout.CENTER);

        add(menuBar, BorderLayout.NORTH);
        navigator.setModel(new DefaultComboBoxModel<>());
        navigator.setDataToString(o -> {
            if (o instanceof JIPipeNodeInfo) {
                return ((JIPipeNodeInfo) o).getName();
            } else if (o instanceof JIPipeNodeUI) {
                return ((JIPipeNodeUI) o).getNode().getName();
            } else {
                return "" + o;
            }
        });
        navigator.setRenderer(new NavigationRenderer());
        navigator.getEventBus().register(this);
        navigator.setRankingFunction(JIPipeGraphEditorUI::rankNavigationEntry);
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
            getCanvasUI().getGraphHistory().addSnapshotBefore(new AddNodeGraphHistorySnapshot(algorithmGraph, Collections.singleton(node)));
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

        navigator.getComboBox().setMaximumSize(new Dimension(200, 32));
        menuBar.add(navigator);
        menuBar.add(Box.createHorizontalStrut(8));

        List<GraphEditorToolBarButtonExtension> graphEditorToolBarButtonExtensions = JIPipe.getCustomMenus().graphEditorToolBarButtonExtensionsFor(this);
        for (GraphEditorToolBarButtonExtension extension : graphEditorToolBarButtonExtensions) {
            UIUtils.makeFlat25x25(extension);
            menuBar.add(extension);
        }

        if (!graphEditorToolBarButtonExtensions.isEmpty())
            menuBar.add(new JSeparator(JSeparator.VERTICAL));

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

        menuBar.add(new JSeparator(JSeparator.VERTICAL));

        initializeViewModeMenu(menuBar);

        JButton autoLayoutButton = new JButton(UIUtils.getIconFromResources("actions/distribute-unclump.png"));
        autoLayoutButton.setToolTipText("<html>Auto-layout all nodes<br><i>Ctrl-Shift-L</i></html>");
        UIUtils.makeFlat25x25(autoLayoutButton);
        autoLayoutButton.addActionListener(e -> {
            canvasUI.getGraphHistory().addSnapshotBefore(new MoveNodesGraphHistorySnapshot(canvasUI.getGraph(), "Auto-layout all nodes"));
            canvasUI.autoLayoutAll();
        });
        menuBar.add(autoLayoutButton);

        JButton centerViewButton = new JButton(UIUtils.getIconFromResources("actions/view-restore.png"));
        centerViewButton.setToolTipText("Center view to nodes");
        UIUtils.makeFlat25x25(centerViewButton);
        centerViewButton.addActionListener(e -> {
            canvasUI.getGraphHistory().addSnapshotBefore(new MoveNodesGraphHistorySnapshot(canvasUI.getGraph(), "Center view to nodes"));
            canvasUI.crop();
        });
        menuBar.add(centerViewButton);

        menuBar.add(new JSeparator(JSeparator.VERTICAL));

        switchPanningDirectionButton = new JToggleButton(UIUtils.getIconFromResources("devices/input-mouse.png"),
                GraphEditorUISettings.getInstance().isSwitchPanningDirection());
        switchPanningDirectionButton.setToolTipText("Reverse panning direction");
        UIUtils.makeFlat25x25(switchPanningDirectionButton);
        switchPanningDirectionButton.setToolTipText("Changes the direction how panning (middle mouse button) affects the view.");
        switchPanningDirectionButton.addActionListener(e -> GraphEditorUISettings.getInstance().setSwitchPanningDirection(switchPanningDirectionButton.isSelected()));
        menuBar.add(switchPanningDirectionButton);

        JToggleButton layoutHelperButton;
        layoutHelperButton = new JToggleButton(UIUtils.getIconFromResources("actions/connector-avoid.png"),
                GraphEditorUISettings.getInstance().isEnableLayoutHelper());
        UIUtils.makeFlat25x25(layoutHelperButton);
        layoutHelperButton.setToolTipText("Auto-layout layout on making data slot connections");
        canvasUI.setLayoutHelperEnabled(GraphEditorUISettings.getInstance().isEnableLayoutHelper());
        layoutHelperButton.addActionListener(e -> {
            canvasUI.setLayoutHelperEnabled(layoutHelperButton.isSelected());
            GraphEditorUISettings.getInstance().setEnableLayoutHelper(layoutHelperButton.isSelected());
        });
        menuBar.add(layoutHelperButton);

        menuBar.add(new JSeparator(JSeparator.VERTICAL));

        JButton exportButton = new JButton(UIUtils.getIconFromResources("actions/document-export.png"));
        exportButton.setToolTipText("Export graph");
        UIUtils.makeFlat25x25(exportButton);

        JPopupMenu exportAsImageMenu = UIUtils.addPopupMenuToComponent(exportButton);

        JMenuItem exportAsPngItem = new JMenuItem("as *.png", UIUtils.getIconFromResources("actions/viewimage.png"));
        exportAsPngItem.addActionListener(e -> createScreenshotPNG());
        exportAsImageMenu.add(exportAsPngItem);
        JMenuItem exportAsSvgItem = new JMenuItem("as *.svg", UIUtils.getIconFromResources("actions/viewimage.png"));
        exportAsSvgItem.addActionListener(e -> createScreenshotSVG());
        exportAsImageMenu.add(exportAsSvgItem);

        menuBar.add(exportButton);

        menuBar.add(new JSeparator(JSeparator.VERTICAL));

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

    private void initializeViewModeMenu(JMenuBar menuBar) {
        ButtonGroup viewModeGroup = new ButtonGroup();
        JButton viewModeButton = new JButton();
        JPopupMenu viewModeMenu = UIUtils.addPopupMenuToComponent(viewModeButton);

        JMenuItem viewModeHorizontalItem = new JCheckBoxMenuItem("Display nodes horizontally");
        viewModeHorizontalItem.setSelected(canvasUI.getViewMode() == JIPipeGraphViewMode.Horizontal);
        viewModeHorizontalItem.addActionListener(e -> {
            canvasUI.setViewMode(JIPipeGraphViewMode.Horizontal);
            canvasUI.getGraph().attachAdditionalMetadata("jipipe:graph:view-mode", JIPipeGraphViewMode.Horizontal);
            updateViewModeMenuIcon(viewModeButton);
        });
        viewModeGroup.add(viewModeHorizontalItem);
        viewModeMenu.add(viewModeHorizontalItem);

        JMenuItem viewModeVerticalItem = new JCheckBoxMenuItem("Display nodes vertically");
        viewModeVerticalItem.setSelected(canvasUI.getViewMode() == JIPipeGraphViewMode.Vertical);
        viewModeVerticalItem.addActionListener(e -> {
            canvasUI.setViewMode(JIPipeGraphViewMode.Vertical);
            canvasUI.getGraph().attachAdditionalMetadata("jipipe:graph:view-mode", JIPipeGraphViewMode.Vertical);
            updateViewModeMenuIcon(viewModeButton);
        });
        viewModeGroup.add(viewModeVerticalItem);
        viewModeMenu.add(viewModeVerticalItem);

        JMenuItem viewModeVerticalCompactItem = new JCheckBoxMenuItem("Display nodes vertically (compact)");
        viewModeVerticalCompactItem.setSelected(canvasUI.getViewMode() == JIPipeGraphViewMode.VerticalCompact);
        viewModeVerticalCompactItem.addActionListener(e -> {
            canvasUI.setViewMode(JIPipeGraphViewMode.VerticalCompact);
            canvasUI.getGraph().attachAdditionalMetadata("jipipe:graph:view-mode", JIPipeGraphViewMode.VerticalCompact);
            updateViewModeMenuIcon(viewModeButton);
        });
        viewModeGroup.add(viewModeVerticalCompactItem);
        viewModeMenu.add(viewModeVerticalCompactItem);

        updateViewModeMenuIcon(viewModeButton);

        UIUtils.makeFlat25x25(viewModeButton);
        menuBar.add(viewModeButton);
    }

    private void updateViewModeMenuIcon(JButton viewModeButton) {
        switch (canvasUI.getViewMode()) {
            case Horizontal:
                viewModeButton.setIcon(UIUtils.getIconFromResources("actions/view-mode-horizontal.png"));
                viewModeButton.setToolTipText("Nodes are displayed horizontally. Click to change.");
                break;
            case Vertical:
                viewModeButton.setIcon(UIUtils.getIconFromResources("actions/view-mode-vertical.png"));
                viewModeButton.setToolTipText("Nodes are displayed vertically. Click to change.");
                break;
            case VerticalCompact:
                viewModeButton.setIcon(UIUtils.getIconFromResources("actions/view-mode-vertical-compact.png"));
                viewModeButton.setToolTipText("Nodes are displayed vertically (compact). Click to change.");
                break;
        }
    }

    private void redo() {
        int scrollX = scrollPane.getHorizontalScrollBar().getValue();
        int scrollY = scrollPane.getVerticalScrollBar().getValue();
        if (canvasUI.getGraphHistory().redo()) {
            getWorkbench().sendStatusBarText("Redo successful");
        } else {
            getWorkbench().sendStatusBarText("Redo unsuccessful");
        }
        SwingUtilities.invokeLater(() -> {
            scrollPane.getHorizontalScrollBar().setValue(scrollX);
            scrollPane.getVerticalScrollBar().setValue(scrollY);
        });
    }

    private void undo() {
        int scrollX = scrollPane.getHorizontalScrollBar().getValue();
        int scrollY = scrollPane.getVerticalScrollBar().getValue();
        if (canvasUI.getGraphHistory().undo()) {
            getWorkbench().sendStatusBarText("Undo successful");
        } else {
            getWorkbench().sendStatusBarText("Undo unsuccessful");
        }
        SwingUtilities.invokeLater(() -> {
            scrollPane.getHorizontalScrollBar().setValue(scrollX);
            scrollPane.getVerticalScrollBar().setValue(scrollY);
        });
    }

    /**
     * @return The edited graph
     */
    public JIPipeGraph getAlgorithmGraph() {
        return algorithmGraph;
    }

    private void createScreenshotSVG() {
        SVGGraphics2D screenshot = canvasUI.createScreenshotSVG();
        Path selectedPath = FileChooserSettings.saveFile(this, FileChooserSettings.KEY_PROJECT, "Export graph as SVG (*.svg)", UIUtils.EXTENSION_FILTER_SVG);
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
        Path selectedPath = FileChooserSettings.saveFile(this, FileChooserSettings.KEY_PROJECT, "Export graph as PNG (*.png)", UIUtils.EXTENSION_FILTER_PNG);
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
        splitPane.setRightComponent(content);
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
            if (!switchPanningDirectionButton.isSelected()) {
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
        boolean canCreateNewNodes = true;
        if (getWorkbench() instanceof JIPipeProjectWorkbench) {
            canCreateNewNodes = !((JIPipeProjectWorkbench) getWorkbench()).getProject().getMetadata().getPermissions().isPreventAddingDeletingNodes();
        }
        DefaultComboBoxModel<Object> model = new DefaultComboBoxModel<>();
        model.removeAllElements();
        for (JIPipeNodeUI ui : canvasUI.getNodeUIs().values().stream().sorted(Comparator.comparing(ui -> ui.getNode().getName())).collect(Collectors.toList())) {
            model.addElement(ui);
        }
        if (canCreateNewNodes) {
            for (JIPipeNodeInfo info : addableAlgorithms.stream()
                    .sorted(Comparator.comparing(JIPipeNodeInfo::getName)).collect(Collectors.toList())) {
                model.addElement(info);
            }
        }
        navigator.setModel(model);
    }

    private static int[] rankNavigationEntry(Object value, String[] searchStrings) {
        if (searchStrings == null || searchStrings.length == 0)
            return new int[0];
        String nameHayStack;
        String descriptionHayStack;
        if (value instanceof JIPipeNodeUI) {
            JIPipeGraphNode node = ((JIPipeNodeUI) value).getNode();
            nameHayStack = node.getName();
            descriptionHayStack = StringUtils.orElse(node.getCustomDescription().getBody(), node.getInfo().getDescription().getBody());
        } else if (value instanceof JIPipeNodeInfo) {
            JIPipeNodeInfo info = (JIPipeNodeInfo) value;
            if (info.isHidden())
                return null;
            nameHayStack = StringUtils.orElse(info.getName(), "").toLowerCase();
            descriptionHayStack = StringUtils.orElse(info.getDescription().getBody(), "").toLowerCase();
        } else {
            return null;
        }

        nameHayStack = nameHayStack.toLowerCase();
        descriptionHayStack = descriptionHayStack.toLowerCase();

        int[] ranks = new int[2];

        for (String string : searchStrings) {
            if (nameHayStack.contains(string.toLowerCase()))
                --ranks[0];
            if (descriptionHayStack.contains(string.toLowerCase()))
                --ranks[1];
        }

        if (ranks[0] == 0 && ranks[1] == 0)
            return null;

        return ranks;
    }

    public static void installContextActionsInto(JToolBar toolBar, Set<JIPipeNodeUI> selection, List<NodeUIContextAction> actionList, JIPipeGraphCanvasUI canvasUI) {
        JPopupMenu overhang = new JPopupMenu();
        boolean scheduledSeparator = false;
        for (NodeUIContextAction action : actionList) {
            if (action == null) {
                scheduledSeparator = true;
                continue;
            }
            boolean matches = action.matches(selection);
            if (!matches && !action.disableOnNonMatch())
                continue;
            if (!action.isShowingInOverhang()) {
                if (scheduledSeparator)
                    toolBar.add(Box.createHorizontalStrut(4));
                JButton button = new JButton(action.getIcon());
                UIUtils.makeFlat25x25(button);
                button.setToolTipText("<html><strong>" + action.getName() + "</strong><br/>" + action.getDescription() + "</html>");
                if (matches)
                    button.addActionListener(e -> action.run(canvasUI, ImmutableSet.copyOf(selection)));
                else
                    button.setEnabled(false);
                toolBar.add(button);
            } else {
                JMenuItem item = new JMenuItem(action.getName(), action.getIcon());
                item.setToolTipText(action.getDescription());
                if (matches)
                    item.addActionListener(e -> action.run(canvasUI, ImmutableSet.copyOf(selection)));
                else
                    item.setEnabled(false);
                overhang.add(item);
            }
        }

        if (overhang.getComponentCount() > 0) {
            toolBar.add(Box.createHorizontalStrut(4));
            JButton button = new JButton("Open with ...");
            UIUtils.makeFlat25x25(button);
            button.setToolTipText("Shows more actions to display the data. On selecting an entry, " +
                    "it becomes the default action.");
            UIUtils.addPopupMenuToComponent(button, overhang);
            toolBar.add(button);
        }
    }

    /**
     * Renders items in the navigator
     */
    public static class NavigationRenderer extends JPanel implements ListCellRenderer<Object> {

        private ColorIcon icon;
        private JLabel iconLabel;
        private JLabel actionLabel;
        private JLabel algorithmLabel;
        private JLabel menuLabel;

        /**
         * Creates a new instance
         */
        public NavigationRenderer() {
            setLayout(new GridBagLayout());
            setOpaque(true);
            setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));


            icon = new ColorIcon(16, 40);
            iconLabel = new JLabel(icon);
            Insets border = new Insets(2, 4, 2, 2);
            add(iconLabel, new GridBagConstraints() {
                {
                    gridx = 0;
                    gridy = 0;
                    gridheight = 2;
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
