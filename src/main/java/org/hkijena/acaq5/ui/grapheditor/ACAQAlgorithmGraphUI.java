package org.hkijena.acaq5.ui.grapheditor;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.api.events.AlgorithmRegistryChangedEvent;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
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
import java.awt.dnd.DropTarget;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ACAQAlgorithmGraphUI extends ACAQUIPanel implements MouseListener, MouseMotionListener {

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

    public ACAQAlgorithmGraphUI(ACAQWorkbenchUI workbenchUI, ACAQAlgorithmGraph algorithmGraph, String compartment) {
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

    @Subscribe
    public void onAlgorithmRegistryChanged(AlgorithmRegistryChangedEvent event) {
        reloadMenuBar();
        getWorkbenchUI().sendStatusBarText("Plugins were updated");
    }

    public void reloadMenuBar() {
        menuBar.removeAll();
        initializeToolbar();
    }

    protected void initializeToolbar() {
        initializeAddNodesMenus();

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
                getWorkbenchUI().sendStatusBarText("Exported graph as " + fileChooser.getSelectedFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected void initializeAddNodesMenus() {
        JMenu addDataSourceMenu = new JMenu("Add data");
        addDataSourceMenu.setIcon(UIUtils.getIconFromResources("database.png"));
        initializeAddDataSourceMenu(addDataSourceMenu);
        menuBar.add(addDataSourceMenu);

        JMenu addFilesystemMenu = new JMenu("Filesystem");
        addFilesystemMenu.setIcon(UIUtils.getIconFromResources("tree.png"));
        initializeMenuForCategory(addFilesystemMenu, ACAQAlgorithmCategory.FileSystem);
        menuBar.add(addFilesystemMenu);

        JMenu addAnnotationMenu = new JMenu("Annotation");
        addAnnotationMenu.setIcon(UIUtils.getIconFromResources("label.png"));
        initializeMenuForCategory(addAnnotationMenu, ACAQAlgorithmCategory.Annotation);
        menuBar.add(addAnnotationMenu);

        JMenu addEnhancerMenu = new JMenu("Enhance");
        addEnhancerMenu.setIcon(UIUtils.getIconFromResources("magic.png"));
        initializeMenuForCategory(addEnhancerMenu, ACAQAlgorithmCategory.Enhancer);
        menuBar.add(addEnhancerMenu);

        JMenu addSegmenterMenu = new JMenu("Segment");
        addSegmenterMenu.setIcon(UIUtils.getIconFromResources("data-types/binary.png"));
        initializeMenuForCategory(addSegmenterMenu, ACAQAlgorithmCategory.Segmenter);
        menuBar.add(addSegmenterMenu);

        JMenu addConverterMenu = new JMenu("Convert");
        addConverterMenu.setIcon(UIUtils.getIconFromResources("convert.png"));
        initializeMenuForCategory(addConverterMenu, ACAQAlgorithmCategory.Converter);
        menuBar.add(addConverterMenu);

        JMenu addQuantifierMenu = new JMenu("Quantify");
        addQuantifierMenu.setIcon(UIUtils.getIconFromResources("statistics.png"));
        initializeMenuForCategory(addQuantifierMenu, ACAQAlgorithmCategory.Quantifier);
        menuBar.add(addQuantifierMenu);

        JMenu addMiscMenu = new JMenu("Miscellaneous");
        addMiscMenu.setIcon(UIUtils.getIconFromResources("module.png"));
        initializeMenuForCategory(addMiscMenu, ACAQAlgorithmCategory.Miscellaneous);
        menuBar.add(addMiscMenu);
    }

    protected void initializeMenuForCategory(JMenu menu, ACAQAlgorithmCategory category) {
        ACAQRegistryService registryService = ACAQRegistryService.getInstance();
        boolean isEmpty = true;
        Icon icon = new ColorIcon(16, 16, UIUtils.getFillColorFor(category));
        for (ACAQAlgorithmDeclaration declaration : registryService.getAlgorithmRegistry().getAlgorithmsOfCategory(category)
                .stream().sorted(Comparator.comparing(ACAQAlgorithmDeclaration::getName)).collect(Collectors.toList())) {
            JMenuItem addItem = new JMenuItem(declaration.getName(), icon);
            addItem.setToolTipText(TooltipUtils.getAlgorithmTooltip(declaration));
            addItem.addActionListener(e -> algorithmGraph.insertNode(declaration.newInstance(), compartment));
            menu.add(addItem);
            isEmpty = false;
        }
        if (isEmpty)
            menu.setVisible(false);
    }

    private void initializeAddDataSourceMenu(JMenu menu) {
        ACAQRegistryService registryService = ACAQRegistryService.getInstance();
        for (Class<? extends ACAQData> dataClass : registryService.getDatatypeRegistry().getRegisteredDataTypes().stream()
                .sorted(Comparator.comparing(ACAQData::getNameOf)).collect(Collectors.toList())) {
            if (ACAQDatatypeRegistry.getInstance().isHidden(dataClass))
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

            menu.add(dataMenu);
            if (isEmpty)
                dataMenu.setVisible(false);
        }
    }

    public ACAQAlgorithmGraph getAlgorithmGraph() {
        return algorithmGraph;
    }

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

    public void clearSelection() {
        for (ACAQAlgorithmUI ui : selection) {
            ui.setSelected(false);
        }
        selection.clear();
        int dividerLocation = splitPane.getDividerLocation();
        splitPane.setRightComponent(documentationPanel);
        splitPane.setDividerLocation(dividerLocation);
    }

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

    public void removeFromSelection(ACAQAlgorithmUI ui) {
        if (selection.contains(ui)) {
            selection.remove(ui);
            ui.setSelected(false);

            int dividerLocation = splitPane.getDividerLocation();
            if (selection.isEmpty()) {
                splitPane.setRightComponent(documentationPanel);
            } else if (selection.size() == 1) {
                splitPane.setRightComponent(new ACAQSingleAlgorithmSelectionPanelUI(getWorkbenchUI(), algorithmGraph, ui.getAlgorithm()));
            } else {
                splitPane.setRightComponent(new ACAQMultiAlgorithmSelectionPanelUI(getWorkbenchUI(), algorithmGraph,
                        selection.stream().map(ACAQAlgorithmUI::getAlgorithm).collect(Collectors.toSet())));
            }
            splitPane.setDividerLocation(dividerLocation);
        }
    }

    public void addToSelection(ACAQAlgorithmUI ui) {
        selection.add(ui);
        ui.setSelected(true);
        if (selection.size() == 1) {
            int dividerLocation = splitPane.getDividerLocation();
            splitPane.setRightComponent(new ACAQSingleAlgorithmSelectionPanelUI(getWorkbenchUI(), algorithmGraph, ui.getAlgorithm()));
            splitPane.setDividerLocation(dividerLocation);
        } else {
            int dividerLocation = splitPane.getDividerLocation();
            splitPane.setRightComponent(new ACAQMultiAlgorithmSelectionPanelUI(getWorkbenchUI(), algorithmGraph,
                    selection.stream().map(ACAQAlgorithmUI::getAlgorithm).collect(Collectors.toSet())));
            splitPane.setDividerLocation(dividerLocation);
        }
    }

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

    public String getCompartment() {
        return compartment;
    }
}
