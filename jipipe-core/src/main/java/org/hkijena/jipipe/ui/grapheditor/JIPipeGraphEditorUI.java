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
import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.api.algorithm.JIPipeGraph;
import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
import org.hkijena.jipipe.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.jipipe.api.events.AlgorithmRegisteredEvent;
import org.hkijena.jipipe.api.history.AddNodeGraphHistorySnapshot;
import org.hkijena.jipipe.api.history.MoveNodesGraphHistorySnapshot;
import org.hkijena.jipipe.api.registries.JIPipeAlgorithmRegistry;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.GraphEditorUISettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.ColorIcon;
import org.hkijena.jipipe.ui.components.SearchBox;
import org.hkijena.jipipe.ui.events.AlgorithmEvent;
import org.hkijena.jipipe.ui.events.AlgorithmSelectedEvent;
import org.hkijena.jipipe.ui.events.AlgorithmSelectionChangedEvent;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.AlgorithmUIAction;
import org.hkijena.jipipe.ui.registries.JIPipeUIAlgorithmRegistry;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jfree.graphics2d.svg.SVGUtils;

import javax.imageio.ImageIO;
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

    protected JMenuBar menuBar = new JMenuBar();
    private JIPipeGraphCanvasUI canvasUI;
    private JIPipeGraph algorithmGraph;
    private String compartment;

    private JSplitPane splitPane;
    private JScrollPane scrollPane;
    private Point panningOffset = null;
    private Point panningScrollbarOffset = null;
    private boolean isPanning = false;
    private JToggleButton switchPanningDirectionButton;

    private Set<JIPipeAlgorithmDeclaration> addableAlgorithms = new HashSet<>();
    private SearchBox<Object> navigator = new SearchBox<>();
    private JMenuItem cutContextMenuItem;
    private JMenuItem copyContextMenuItem;
    private JMenuItem pasteContextMenuItem;

    /**
     * @param workbenchUI    the workbench
     * @param algorithmGraph the algorithm graph
     * @param compartment    the graph compartment to display. Set to null to display all compartments
     */
    public JIPipeGraphEditorUI(JIPipeWorkbench workbenchUI, JIPipeGraph algorithmGraph, String compartment) {
        super(workbenchUI);
        this.algorithmGraph = algorithmGraph;
        this.compartment = compartment;
        initialize();
        reloadMenuBar();
        JIPipeAlgorithmRegistry.getInstance().getEventBus().register(this);
        algorithmGraph.getEventBus().register(this);
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

        canvasUI = new JIPipeGraphCanvasUI(getWorkbench(), algorithmGraph, compartment);
        canvasUI.fullRedraw();
        canvasUI.getEventBus().register(this);
        canvasUI.addMouseListener(this);
        canvasUI.addMouseMotionListener(this);
        scrollPane = new JScrollPane(canvasUI);
        scrollPane.getVerticalScrollBar().setUnitIncrement(25);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(25);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        splitPane.setLeftComponent(scrollPane);
        splitPane.setRightComponent(new JPanel());
        add(splitPane, BorderLayout.CENTER);

        add(menuBar, BorderLayout.NORTH);
        navigator.setModel(new DefaultComboBoxModel<>());
        navigator.setRenderer(new NavigationRenderer());
        navigator.addItemListener(e -> navigatorNavigate());
        navigator.setFilterFunction(JIPipeGraphEditorUI::filterNavigationEntry);
    }

    private void navigatorNavigate() {
        if (navigator.getSelectedItem() instanceof JIPipeNodeUI) {
            selectOnly((JIPipeNodeUI) navigator.getSelectedItem());
            navigator.setSelectedItem(null);
        } else if (navigator.getSelectedItem() instanceof JIPipeAlgorithmDeclaration) {
            JIPipeAlgorithmDeclaration declaration = (JIPipeAlgorithmDeclaration) navigator.getSelectedItem();
            JIPipeGraphNode node = declaration.newInstance();
            getCanvasUI().getGraphHistory().addSnapshotBefore(new AddNodeGraphHistorySnapshot(algorithmGraph, Collections.singleton(node)));
            algorithmGraph.insertNode(node, compartment);
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

        JButton undoButton = new JButton(UIUtils.getIconFromResources("undo.png"));
        undoButton.setToolTipText("Undo");
        UIUtils.makeFlat25x25(undoButton);
        undoButton.addActionListener(e -> canvasUI.getGraphHistory().undo());
        menuBar.add(undoButton);

        JButton redoButton = new JButton(UIUtils.getIconFromResources("redo.png"));
        redoButton.setToolTipText("Redo");
        UIUtils.makeFlat25x25(redoButton);
        redoButton.addActionListener(e -> canvasUI.getGraphHistory().redo());
        menuBar.add(redoButton);

        menuBar.add(Box.createHorizontalStrut(8));

        ButtonGroup viewModeGroup = new ButtonGroup();

        JToggleButton viewModeHorizontalButton = new JToggleButton(UIUtils.getIconFromResources("view-horizontal.png"));
        viewModeHorizontalButton.setToolTipText("Display nodes horizontally");
        UIUtils.makeFlat25x25(viewModeHorizontalButton);
        viewModeHorizontalButton.setSelected(canvasUI.getCurrentViewMode() == JIPipeGraphCanvasUI.ViewMode.Horizontal);
        viewModeHorizontalButton.addActionListener(e -> canvasUI.setCurrentViewMode(JIPipeGraphCanvasUI.ViewMode.Horizontal));
        viewModeGroup.add(viewModeHorizontalButton);
        menuBar.add(viewModeHorizontalButton);

        JToggleButton viewModeVerticalButton = new JToggleButton(UIUtils.getIconFromResources("view-vertical.png"));
        viewModeVerticalButton.setToolTipText("Display nodes vertically");
        UIUtils.makeFlat25x25(viewModeVerticalButton);
        viewModeVerticalButton.setSelected(canvasUI.getCurrentViewMode() == JIPipeGraphCanvasUI.ViewMode.Vertical);
        viewModeVerticalButton.addActionListener(e -> canvasUI.setCurrentViewMode(JIPipeGraphCanvasUI.ViewMode.Vertical));
        viewModeGroup.add(viewModeVerticalButton);
        menuBar.add(viewModeVerticalButton);

        menuBar.add(Box.createHorizontalStrut(8));

        JButton autoLayoutButton = new JButton(UIUtils.getIconFromResources("auto-layout-all.png"));
        autoLayoutButton.setToolTipText("Auto-layout all nodes");
        UIUtils.makeFlat25x25(autoLayoutButton);
        autoLayoutButton.addActionListener(e -> {
            canvasUI.getGraphHistory().addSnapshotBefore(new MoveNodesGraphHistorySnapshot(canvasUI.getGraph(), "Auto-layout all nodes"));
            canvasUI.autoLayoutAll();
        });
        menuBar.add(autoLayoutButton);

        JButton centerViewButton = new JButton(UIUtils.getIconFromResources("algorithms/view-restore.png"));
        centerViewButton.setToolTipText("Center view to nodes");
        UIUtils.makeFlat25x25(centerViewButton);
        centerViewButton.addActionListener(e -> {
            canvasUI.getGraphHistory().addSnapshotBefore(new MoveNodesGraphHistorySnapshot(canvasUI.getGraph(), "Center view to nodes"));
            canvasUI.crop();
        });
        menuBar.add(centerViewButton);

        menuBar.add(Box.createHorizontalStrut(8));

        switchPanningDirectionButton = new JToggleButton(UIUtils.getIconFromResources("cursor-arrow.png"),
                GraphEditorUISettings.getInstance().isSwitchPanningDirection());
        switchPanningDirectionButton.setToolTipText("Reverse panning direction");
        UIUtils.makeFlat25x25(switchPanningDirectionButton);
        switchPanningDirectionButton.setToolTipText("Changes the direction how panning (middle mouse button) affects the view.");
        switchPanningDirectionButton.addActionListener(e -> GraphEditorUISettings.getInstance().setSwitchPanningDirection(switchPanningDirectionButton.isSelected()));
        menuBar.add(switchPanningDirectionButton);

        JToggleButton layoutHelperButton;
        layoutHelperButton = new JToggleButton(UIUtils.getIconFromResources("auto-layout-connections.png"),
                GraphEditorUISettings.getInstance().isEnableLayoutHelper());
        UIUtils.makeFlat25x25(layoutHelperButton);
        layoutHelperButton.setToolTipText("Auto-layout layout on making data slot connections");
        canvasUI.setLayoutHelperEnabled(GraphEditorUISettings.getInstance().isEnableLayoutHelper());
        layoutHelperButton.addActionListener(e -> {
            canvasUI.setLayoutHelperEnabled(layoutHelperButton.isSelected());
            GraphEditorUISettings.getInstance().setEnableLayoutHelper(layoutHelperButton.isSelected());
        });
        menuBar.add(layoutHelperButton);

        menuBar.add(Box.createHorizontalStrut(8));

        JButton exportButton = new JButton(UIUtils.getIconFromResources("export.png"));
        exportButton.setToolTipText("Export graph");
        UIUtils.makeFlat25x25(exportButton);

        JPopupMenu exportAsImageMenu = UIUtils.addPopupMenuToComponent(exportButton);

        JMenuItem exportAsPngItem = new JMenuItem("as *.png", UIUtils.getIconFromResources("filetype-image.png"));
        exportAsPngItem.addActionListener(e -> createScreenshotPNG());
        exportAsImageMenu.add(exportAsPngItem);
        JMenuItem exportAsSvgItem = new JMenuItem("as *.svg", UIUtils.getIconFromResources("filetype-image.png"));
        exportAsSvgItem.addActionListener(e -> createScreenshotSVG());
        exportAsImageMenu.add(exportAsSvgItem);

        menuBar.add(exportButton);
    }

    /**
     * @return The edited graph
     */
    public JIPipeGraph getAlgorithmGraph() {
        return algorithmGraph;
    }

    private void createScreenshotSVG() {
        SVGGraphics2D screenshot = canvasUI.createScreenshotSVG();
        Path selectedPath = FileChooserSettings.saveFile(this, FileChooserSettings.KEY_PROJECT, "Export graph as SVG (*.svg)", ".svg");
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
        Path selectedPath = FileChooserSettings.saveFile(this, FileChooserSettings.KEY_PROJECT, "Export graph as PNG (*.png)", ".png");
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
    public void onSelectionChanged(AlgorithmSelectionChangedEvent event) {
        updateSelection();
    }

    /**
     * Should be triggered when new algorithms are registered.
     * Reloads the menu
     *
     * @param event Generated event
     */
    @Subscribe
    public void onAlgorithmRegistryChanged(AlgorithmRegisteredEvent event) {
        reloadMenuBar();
        getWorkbench().sendStatusBarText("Plugins were updated");
    }

    /**
     * Should be triggered when an algorithm was selected
     *
     * @param event The generated event
     */
    @Subscribe
    public void onAlgorithmSelected(AlgorithmSelectedEvent event) {
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
    public void onAlgorithmEvent(AlgorithmEvent event) {
        if (event.getUi() != null) {
            scrollToAlgorithm(event.getUi());
        }
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
    public void onGraphChanged(AlgorithmGraphChangedEvent event) {
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
                boolean ex = nx < 0;
                boolean ey = ny < 0;
                if (ex || ey) {
                    canvasUI.expandLeftTop(ex, ey);
                    if (ex) {
                        nx = JIPipeNodeUI.SLOT_UI_WIDTH;
                        panningOffset.x += JIPipeNodeUI.SLOT_UI_WIDTH;
                    }
                    if (ey) {
                        ny = JIPipeNodeUI.SLOT_UI_HEIGHT;
                        panningOffset.y += JIPipeNodeUI.SLOT_UI_HEIGHT;
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
    public String getCompartment() {
        return compartment;
    }

    protected JMenuBar getMenuBar() {
        return menuBar;
    }

    public Set<JIPipeAlgorithmDeclaration> getAddableAlgorithms() {
        return addableAlgorithms;
    }

    public void setAddableAlgorithms(Set<JIPipeAlgorithmDeclaration> addableAlgorithms) {
        this.addableAlgorithms = addableAlgorithms;
        updateNavigation();
    }

    /**
     * Updates the navigation list
     */
    public void updateNavigation() {
        DefaultComboBoxModel<Object> model = (DefaultComboBoxModel<Object>) navigator.getModel();
        model.removeAllElements();
        for (JIPipeNodeUI ui : canvasUI.getNodeUIs().values().stream().sorted(Comparator.comparing(ui -> ui.getNode().getName())).collect(Collectors.toList())) {
            model.addElement(ui);
        }
        for (JIPipeAlgorithmDeclaration declaration : addableAlgorithms.stream()
                .sorted(Comparator.comparing(JIPipeAlgorithmDeclaration::getName)).collect(Collectors.toList())) {
            model.addElement(declaration);
        }
    }

    private static boolean filterNavigationEntry(Object entry, String searchString) {
        String haystack = "";
        JIPipeAlgorithmDeclaration algorithmDeclaration = null;
        if (entry instanceof JIPipeNodeUI) {
            haystack += ((JIPipeNodeUI) entry).getNode().getName();
            algorithmDeclaration = ((JIPipeNodeUI) entry).getNode().getDeclaration();
        } else if (entry instanceof JIPipeAlgorithmDeclaration) {
            if (((JIPipeAlgorithmDeclaration) entry).isHidden())
                return false;
            algorithmDeclaration = (JIPipeAlgorithmDeclaration) entry;
        }
        if (algorithmDeclaration != null) {
            haystack += algorithmDeclaration.getName() + algorithmDeclaration.getDescription()
                    + algorithmDeclaration.getMenuPath();
        }
        return haystack.toLowerCase().contains(searchString.toLowerCase());
    }

    public static void installContextActionsInto(JToolBar toolBar, Set<JIPipeNodeUI> selection, List<AlgorithmUIAction> actionList, JIPipeGraphCanvasUI canvasUI) {
        JPopupMenu overhang = new JPopupMenu();
        boolean scheduledSeparator = false;
        for (AlgorithmUIAction action : actionList) {
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
            JButton button = new JButton(UIUtils.getIconFromResources("ellipsis-h.png"));
            button.setToolTipText("More actions ...");
            UIUtils.addPopupMenuToComponent(button, overhang);
            toolBar.add(button);
        }
    }

    /**
     * Renders items in the navigator
     */
    public static class NavigationRenderer extends JLabel implements ListCellRenderer<Object> {

        /**
         * Creates a new instance
         */
        public NavigationRenderer() {
            setOpaque(true);
            setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

            if (value instanceof JIPipeAlgorithmDeclaration) {
                JIPipeAlgorithmDeclaration declaration = (JIPipeAlgorithmDeclaration) value;
                String menuPath = declaration.getCategory().toString();
                if (!StringUtils.isNullOrEmpty(declaration.getMenuPath())) {
                    menuPath += " &gt; " + String.join(" &gt; ", declaration.getMenuPath().split("\n"));
                }

                setIcon(new ColorIcon(16, 40, Color.WHITE, UIUtils.getFillColorFor(declaration)));
                setText(String.format("<html><table cellpadding=\"1\"><tr><td><span style=\"color: green;\">Create</span></td>" +
                                "<td><img src=\"%s\"/></td>" +
                                "<td>%s</td></tr>" +
                                "<tr><td></td>" +
                                "<td></td>" +
                                "<td><span style=\"color: gray;\">%s</span></td></tr></table></html>",
                        JIPipeUIAlgorithmRegistry.getInstance().getIconURLFor(declaration),
                        HtmlEscapers.htmlEscaper().escape(declaration.getName()),
                        menuPath
                ));
            } else if (value instanceof JIPipeNodeUI) {
                JIPipeNodeUI ui = (JIPipeNodeUI) value;
                JIPipeAlgorithmDeclaration declaration = ui.getNode().getDeclaration();
                String menuPath = declaration.getCategory().toString();
                if (!StringUtils.isNullOrEmpty(declaration.getMenuPath())) {
                    menuPath += " &gt; " + String.join(" &gt; ", declaration.getMenuPath().split("\n"));
                }
                setIcon(new ColorIcon(16, 40, UIUtils.getFillColorFor(declaration), UIUtils.getBorderColorFor(declaration)));
                setText(String.format("<html><table cellpadding=\"1\"><tr><td><span style=\"color: blue;\">Navigate</span></td>" +
                                "<td><img src=\"%s\"/></td>" +
                                "<td>%s</td></tr>" +
                                "<tr><td></td>" +
                                "<td></td>" +
                                "<td><span style=\"color: gray;\">%s</span></td></tr></table></html>",
                        JIPipeUIAlgorithmRegistry.getInstance().getIconURLFor(declaration),
                        HtmlEscapers.htmlEscaper().escape(declaration.getName()),
                        menuPath
                ));
            } else {
                setText("<Null>");
            }

            if (isSelected) {
                setBackground(new Color(184, 207, 229));
            } else {
                setBackground(new Color(255, 255, 255));
            }
            return this;
        }
    }
}
