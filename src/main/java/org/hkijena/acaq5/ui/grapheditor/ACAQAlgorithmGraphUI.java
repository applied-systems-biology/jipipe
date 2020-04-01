package org.hkijena.acaq5.ui.grapheditor;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.api.events.AlgorithmRegisteredEvent;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.MarkdownReader;
import org.hkijena.acaq5.ui.events.AlgorithmSelectedEvent;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQMultiAlgorithmSelectionPanelUI;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQSingleAlgorithmSelectionPanelUI;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Editor for a project graph compartment
 */
public class ACAQAlgorithmGraphUI extends ACAQProjectWorkbenchPanel implements MouseListener, MouseMotionListener {

    protected JMenuBar menuBar = new JMenuBar();
    private ACAQAlgorithmGraphCanvasUI graphUI;
    private ACAQAlgorithmGraph algorithmGraph;
    private String compartment;

    private JSplitPane splitPane;
    private JScrollPane scrollPane;
    private Point panningOffset = null;
    private Point panningScrollbarOffset = null;
    private boolean isPanning = false;
    private JToggleButton switchPanningDirectionButton;

    private MarkdownReader documentationPanel;

    private Set<ACAQAlgorithmUI> selection = new HashSet<>();

    /**
     * Creates a project graph compartment editor
     *
     * @param workbenchUI    The workbench
     * @param algorithmGraph The graph
     * @param compartment    The compartment
     */
    public ACAQAlgorithmGraphUI(ACAQProjectWorkbench workbenchUI, ACAQAlgorithmGraph algorithmGraph, String compartment) {
        super(workbenchUI);
        this.algorithmGraph = algorithmGraph;
        this.compartment = compartment;
        initialize();
        reloadMenuBar();
        ACAQAlgorithmRegistry.getInstance().getEventBus().register(this);
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

        documentationPanel = new MarkdownReader(false);
        documentationPanel.setDocument(MarkdownDocument.fromPluginResource("documentation/algorithm-graph.md"));

        graphUI = new ACAQAlgorithmGraphCanvasUI(algorithmGraph, compartment);
        graphUI.getEventBus().register(this);
        graphUI.addMouseListener(this);
        graphUI.addMouseMotionListener(this);
        ACAQAlgorithmGraphUIDragAndDrop.install(graphUI);
        scrollPane = new JScrollPane(graphUI);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getHorizontalScrollBar().addAdjustmentListener(e -> {
            graphUI.setNewEntryLocationX(scrollPane.getHorizontalScrollBar().getValue());
        });
        splitPane.setLeftComponent(scrollPane);
        splitPane.setRightComponent(documentationPanel);
        add(splitPane, BorderLayout.CENTER);

        add(menuBar, BorderLayout.NORTH);
        algorithmGraph.getEventBus().register(this);
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
        getProjectWorkbench().sendStatusBarText("Plugins were updated");
    }

    /**
     * Reloads the menu bar
     */
    public void reloadMenuBar() {
        menuBar.removeAll();
        initializeToolbar();
    }

    /**
     * Initializes the tool bar
     */
    protected void initializeToolbar() {
        initializeAddNodesMenus(menuBar, algorithmGraph, compartment);

        menuBar.add(Box.createHorizontalGlue());

        JButton autoLayoutButton = new JButton(UIUtils.getIconFromResources("auto-layout-all.png"));
        autoLayoutButton.setToolTipText("Auto-layout all nodes");
        UIUtils.makeFlat25x25(autoLayoutButton);
        autoLayoutButton.addActionListener(e -> graphUI.autoLayoutAll());
        menuBar.add(autoLayoutButton);

        switchPanningDirectionButton = new JToggleButton(UIUtils.getIconFromResources("cursor-arrow.png"));
        switchPanningDirectionButton.setToolTipText("Reverse panning direction");
        UIUtils.makeFlat25x25(switchPanningDirectionButton);
        switchPanningDirectionButton.setToolTipText("Changes the direction how panning (middle mouse button) affects the view.");
        menuBar.add(switchPanningDirectionButton);

        JToggleButton layoutHelperButton;
        layoutHelperButton = new JToggleButton(UIUtils.getIconFromResources("auto-layout-connections.png"), true);
        UIUtils.makeFlat25x25(layoutHelperButton);
        layoutHelperButton.setToolTipText("Auto-layout layout on making data slot connections");
        graphUI.setLayoutHelperEnabled(true);
        layoutHelperButton.addActionListener(e -> graphUI.setLayoutHelperEnabled(layoutHelperButton.isSelected()));
        menuBar.add(layoutHelperButton);

        JButton createScreenshotButton = new JButton(UIUtils.getIconFromResources("filetype-image.png"));
        createScreenshotButton.setToolTipText("Export graph as *.png");
        UIUtils.makeFlat25x25(createScreenshotButton);
        createScreenshotButton.addActionListener(e -> createScreenshot());
        menuBar.add(createScreenshotButton);
    }

    private void createScreenshot() {
        BufferedImage screenshot = graphUI.createScreenshot();
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export graph as *.png");
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ImageIO.write(screenshot, "PNG", fileChooser.getSelectedFile());
                getProjectWorkbench().sendStatusBarText("Exported graph as " + fileChooser.getSelectedFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @return The edited graph
     */
    public ACAQAlgorithmGraph getAlgorithmGraph() {
        return algorithmGraph;
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
                if (selection.contains(event.getUi())) {
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
     * Clears the algorithm selection
     */
    public void clearSelection() {
        for (ACAQAlgorithmUI ui : selection) {
            ui.setSelected(false);
        }
        selection.clear();
        int dividerLocation = splitPane.getDividerLocation();
        splitPane.setRightComponent(documentationPanel);
        splitPane.setDividerLocation(dividerLocation);
    }

    /**
     * Selects only the specified algorithm
     *
     * @param ui The algorithm UI
     */
    public void selectOnly(ACAQAlgorithmUI ui) {
        if (selection.isEmpty()) {
            addToSelection(ui);
        } else if (selection.size() == 1) {
            if (selection.iterator().next() != ui) {
                clearSelection();
                addToSelection(ui);
            }
        } else {
            clearSelection();
            addToSelection(ui);
        }
    }

    /**
     * Removes an algorithm from the selection
     *
     * @param ui The algorithm UI
     */
    public void removeFromSelection(ACAQAlgorithmUI ui) {
        if (selection.contains(ui)) {
            selection.remove(ui);
            ui.setSelected(false);

            int dividerLocation = splitPane.getDividerLocation();
            if (selection.isEmpty()) {
                splitPane.setRightComponent(documentationPanel);
            } else if (selection.size() == 1) {
                splitPane.setRightComponent(new ACAQSingleAlgorithmSelectionPanelUI(getProjectWorkbench(), algorithmGraph, ui.getAlgorithm()));
            } else {
                splitPane.setRightComponent(new ACAQMultiAlgorithmSelectionPanelUI(getProjectWorkbench(), algorithmGraph,
                        selection.stream().map(ACAQAlgorithmUI::getAlgorithm).collect(Collectors.toSet())));
            }
            splitPane.setDividerLocation(dividerLocation);
        }
    }

    /**
     * Adds an algorithm to the selection
     *
     * @param ui The algorithm UI
     */
    public void addToSelection(ACAQAlgorithmUI ui) {
        selection.add(ui);
        ui.setSelected(true);
        if (selection.size() == 1) {
            int dividerLocation = splitPane.getDividerLocation();
            splitPane.setRightComponent(new ACAQSingleAlgorithmSelectionPanelUI(getProjectWorkbench(), algorithmGraph, ui.getAlgorithm()));
            splitPane.setDividerLocation(dividerLocation);
        } else {
            int dividerLocation = splitPane.getDividerLocation();
            splitPane.setRightComponent(new ACAQMultiAlgorithmSelectionPanelUI(getProjectWorkbench(), algorithmGraph,
                    selection.stream().map(ACAQAlgorithmUI::getAlgorithm).collect(Collectors.toSet())));
            splitPane.setDividerLocation(dividerLocation);
        }
    }

    /**
     * Should be triggered when the algorithm graph is changed
     *
     * @param event The generated event
     */
    @Subscribe
    public void onGraphChanged(AlgorithmGraphChangedEvent event) {
        if (selection.stream().anyMatch(ui -> !algorithmGraph.getAlgorithmNodes().containsValue(ui.getAlgorithm()))) {
            clearSelection();
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
            graphUI.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (isPanning) {
            graphUI.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
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

    /**
     * Initializes the "Add nodes" menus
     *
     * @param menuBar        The menu bar where the items are created
     * @param algorithmGraph the algorithm graph where nodes are put
     * @param compartment    the graph compartment where nodes are put
     */
    public static void initializeAddNodesMenus(JMenuBar menuBar, ACAQAlgorithmGraph algorithmGraph, String compartment) {
        JMenu addDataSourceMenu = new JMenu("Add data");
        addDataSourceMenu.setIcon(UIUtils.getIconFromResources("database.png"));
        initializeAddDataSourceMenu(addDataSourceMenu, algorithmGraph, compartment);
        menuBar.add(addDataSourceMenu);

        JMenu addFilesystemMenu = new JMenu("Filesystem");
        addFilesystemMenu.setIcon(UIUtils.getIconFromResources("tree.png"));
        initializeMenuForCategory(addFilesystemMenu, ACAQAlgorithmCategory.FileSystem, algorithmGraph, compartment);
        menuBar.add(addFilesystemMenu);

        JMenu addAnnotationMenu = new JMenu("Annotation");
        addAnnotationMenu.setIcon(UIUtils.getIconFromResources("label.png"));
        initializeMenuForCategory(addAnnotationMenu, ACAQAlgorithmCategory.Annotation, algorithmGraph, compartment);
        menuBar.add(addAnnotationMenu);

        JMenu addEnhancerMenu = new JMenu("Enhance");
        addEnhancerMenu.setIcon(UIUtils.getIconFromResources("magic.png"));
        initializeMenuForCategory(addEnhancerMenu, ACAQAlgorithmCategory.Enhancer, algorithmGraph, compartment);
        menuBar.add(addEnhancerMenu);

        JMenu addSegmenterMenu = new JMenu("Segment");
        addSegmenterMenu.setIcon(UIUtils.getIconFromResources("segment.png"));
        initializeMenuForCategory(addSegmenterMenu, ACAQAlgorithmCategory.Segmentation, algorithmGraph, compartment);
        menuBar.add(addSegmenterMenu);

        JMenu addConverterMenu = new JMenu("Convert");
        addConverterMenu.setIcon(UIUtils.getIconFromResources("convert.png"));
        initializeMenuForCategory(addConverterMenu, ACAQAlgorithmCategory.Converter, algorithmGraph, compartment);
        menuBar.add(addConverterMenu);

        JMenu addQuantifierMenu = new JMenu("Quantify");
        addQuantifierMenu.setIcon(UIUtils.getIconFromResources("statistics.png"));
        initializeMenuForCategory(addQuantifierMenu, ACAQAlgorithmCategory.Quantifier, algorithmGraph, compartment);
        menuBar.add(addQuantifierMenu);

        JMenu addMiscMenu = new JMenu("Miscellaneous");
        addMiscMenu.setIcon(UIUtils.getIconFromResources("module.png"));
        initializeMenuForCategory(addMiscMenu, ACAQAlgorithmCategory.Miscellaneous, algorithmGraph, compartment);
        menuBar.add(addMiscMenu);
    }

    /**
     * Initializes a menu for one algorithm category
     *
     * @param menu           The menu
     * @param category       The algorithm category
     * @param algorithmGraph the algorithm graph where nodes are added
     * @param compartment    the graph compartment where nodes are put
     */
    public static void initializeMenuForCategory(JMenu menu, ACAQAlgorithmCategory category, ACAQAlgorithmGraph algorithmGraph, String compartment) {
        ACAQDefaultRegistry registryService = ACAQDefaultRegistry.getInstance();
        Set<ACAQAlgorithmDeclaration> algorithmsOfCategory = registryService.getAlgorithmRegistry().getAlgorithmsOfCategory(category);
        if (algorithmsOfCategory.isEmpty()) {
            menu.setVisible(false);
            return;
        }

        Icon icon = new ColorIcon(16, 16, UIUtils.getFillColorFor(category));
        Map<String, Set<ACAQAlgorithmDeclaration>> byMenuPath = ACAQAlgorithmDeclaration.groupByMenuPaths(algorithmsOfCategory);
        Map<String, JMenu> menuTree = UIUtils.createMenuTree(menu, byMenuPath.keySet());

        for (Map.Entry<String, Set<ACAQAlgorithmDeclaration>> entry : byMenuPath.entrySet()) {
            JMenu subMenu = menuTree.get(entry.getKey());
            for (ACAQAlgorithmDeclaration declaration : ACAQAlgorithmDeclaration.getSortedList(entry.getValue())) {
                JMenuItem addItem = new JMenuItem(declaration.getName(), icon);
                addItem.setToolTipText(TooltipUtils.getAlgorithmTooltip(declaration));
                addItem.addActionListener(e -> algorithmGraph.insertNode(declaration.newInstance(), compartment));
                subMenu.add(addItem);
            }
        }
    }

    /**
     * Initializes a menu that adds data sources
     *
     * @param menu           the target menu
     * @param algorithmGraph the algorithm graph where nodes are put
     * @param compartment    the compartment where nodes are put
     */
    public static void initializeAddDataSourceMenu(JMenu menu, ACAQAlgorithmGraph algorithmGraph, String compartment) {
        ACAQDefaultRegistry registryService = ACAQDefaultRegistry.getInstance();
        Map<String, Set<Class<? extends ACAQData>>> dataTypesByMenuPaths = ACAQDatatypeRegistry.getInstance().getDataTypesByMenuPaths();
        Map<String, JMenu> menuTree = UIUtils.createMenuTree(menu, dataTypesByMenuPaths.keySet());

        for (Map.Entry<String, Set<Class<? extends ACAQData>>> entry : dataTypesByMenuPaths.entrySet()) {
            JMenu subMenu = menuTree.get(entry.getKey());
            for (Class<? extends ACAQData> dataClass : ACAQData.getSortedList(entry.getValue())) {
                if (ACAQData.isHidden(dataClass))
                    continue;
                Set<ACAQAlgorithmDeclaration> dataSources = registryService.getAlgorithmRegistry().getDataSourcesFor(dataClass);
                boolean isEmpty = true;
                Icon icon = registryService.getUIDatatypeRegistry().getIconFor(dataClass);
                JMenu dataMenu = new JMenu(ACAQData.getNameOf(dataClass));
                dataMenu.setIcon(icon);

                for (ACAQAlgorithmDeclaration declaration : dataSources) {
                    JMenuItem addItem = new JMenuItem(declaration.getName(), icon);
                    addItem.setToolTipText(TooltipUtils.getAlgorithmTooltip(declaration));
                    addItem.addActionListener(e -> algorithmGraph.insertNode(declaration.newInstance(), compartment));
                    dataMenu.add(addItem);
                    isEmpty = false;
                }

                subMenu.add(dataMenu);
                if (isEmpty)
                    dataMenu.setVisible(false);
            }
        }
    }
}
