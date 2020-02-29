package org.hkijena.acaq5.ui.grapheditor;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.MarkdownReader;
import org.hkijena.acaq5.ui.events.OpenSettingsUIRequestedEvent;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQAlgorithmSettingsPanelUI;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Set;

public class ACAQAlgorithmGraphUI extends ACAQUIPanel implements MouseListener, MouseMotionListener {

    private ACAQAlgorithmGraphCanvasUI graphUI;
    protected JMenuBar menuBar = new JMenuBar();
    private ACAQAlgorithmGraph algorithmGraph;
    private String compartment;

    private JSplitPane splitPane;
    private JScrollPane scrollPane;
    private ACAQAlgorithmSettingsPanelUI currentAlgorithmSettings;
    private JToggleButton autoLayoutButton;

    private Point panningOffset = null;
    private Point panningScrollbarOffset = null;
    private boolean isPanning = false;
    private JToggleButton switchPanningDirectionButton;

    private MarkdownReader documentationPanel;

    public ACAQAlgorithmGraphUI(ACAQWorkbenchUI workbenchUI, ACAQAlgorithmGraph algorithmGraph, String compartment) {
        super(workbenchUI);
        this.algorithmGraph = algorithmGraph;
        this.compartment = compartment;
        initialize();
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
        initializeToolbar();
        algorithmGraph.getEventBus().register(this);
    }

    protected void initializeToolbar() {
        initializeAddNodesMenus();
        menuBar.add(Box.createHorizontalGlue());
        switchPanningDirectionButton = new JToggleButton(UIUtils.getIconFromResources("cursor-arrow.png"));
        switchPanningDirectionButton.setToolTipText("Reverse panning direction");
        UIUtils.makeFlat(switchPanningDirectionButton);
        switchPanningDirectionButton.setToolTipText("Changes the direction how panning (middle mouse button) affects the view.");
        menuBar.add(switchPanningDirectionButton);

        autoLayoutButton = new JToggleButton(UIUtils.getIconFromResources("sort.png"), true);
        autoLayoutButton.setToolTipText("Auto layout");
        graphUI.setLayoutHelperEnabled(true);
        autoLayoutButton.addActionListener(e -> graphUI.setLayoutHelperEnabled(autoLayoutButton.isSelected()));
        menuBar.add(autoLayoutButton);

        JButton createScreenshotButton = new JButton(UIUtils.getIconFromResources("filetype-image.png"));
        createScreenshotButton.setToolTipText("Export graph as *.png");
        UIUtils.makeFlat(createScreenshotButton);
        createScreenshotButton.addActionListener(e -> createScreenshot());
        menuBar.add(createScreenshotButton);
    }

    private void createScreenshot() {
        BufferedImage screenshot = graphUI.createScreenshot();
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export graph as *.png");
        if(fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
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
        initializeMenuForCategory(addQuantifierMenu, ACAQAlgorithmCategory.Quantififer);
        menuBar.add(addQuantifierMenu);
    }

    protected void initializeMenuForCategory(JMenu menu, ACAQAlgorithmCategory category) {
        ACAQRegistryService registryService = ACAQRegistryService.getInstance();
        boolean isEmpty = true;
        Icon icon = new ColorIcon(16, 16, UIUtils.getFillColorFor(category));
        for(ACAQAlgorithmDeclaration declaration : registryService.getAlgorithmRegistry().getAlgorithmsOfCategory(category)) {
            JMenuItem addItem = new JMenuItem(declaration.getName(), icon);
            addItem.setToolTipText(TooltipUtils.getAlgorithmTooltip(declaration));
            addItem.addActionListener(e -> algorithmGraph.insertNode(declaration.newInstance(), compartment));
            menu.add(addItem);
            isEmpty = false;
        }
        if(isEmpty)
            menu.setVisible(false);
    }

    private void initializeAddDataSourceMenu(JMenu menu) {
        ACAQRegistryService registryService = ACAQRegistryService.getInstance();
        for(Class<? extends ACAQData> dataClass : registryService.getDatatypeRegistry().getRegisteredDataTypes()) {
            if(ACAQDatatypeRegistry.getInstance().isHidden(dataClass))
                continue;
            Set<ACAQAlgorithmDeclaration> dataSources = registryService.getAlgorithmRegistry().getDataSourcesFor(dataClass);
            boolean isEmpty = true;
            Icon icon = registryService.getUIDatatypeRegistry().getIconFor(dataClass);
            JMenu dataMenu = new JMenu(ACAQData.getNameOf(dataClass));
            dataMenu.setIcon(icon);

            for(ACAQAlgorithmDeclaration declaration : dataSources) {
                JMenuItem addItem = new JMenuItem(declaration.getName(), icon);
                addItem.setToolTipText(TooltipUtils.getAlgorithmTooltip(declaration));
                addItem.addActionListener(e -> algorithmGraph.insertNode(declaration.newInstance(), compartment));
                dataMenu.add(addItem);
                isEmpty = false;
            }

            menu.add(dataMenu);
            if(isEmpty)
                dataMenu.setVisible(false);
        }
    }

    public ACAQAlgorithmGraph getAlgorithmGraph() {
        return algorithmGraph;
    }

    @Subscribe
    public void onOpenAlgorithmSettings(OpenSettingsUIRequestedEvent event) {
        if(currentAlgorithmSettings == null || currentAlgorithmSettings.getAlgorithm() != event.getUi().getAlgorithm()) {
            currentAlgorithmSettings = new ACAQAlgorithmSettingsPanelUI(getWorkbenchUI(), algorithmGraph, event.getUi().getAlgorithm());
            int dividerLocation = splitPane.getDividerLocation();
            splitPane.setRightComponent(currentAlgorithmSettings);
            splitPane.setDividerLocation(dividerLocation);
        }
    }

    @Subscribe
    public void onGraphChanged(AlgorithmGraphChangedEvent event) {
        if(currentAlgorithmSettings != null && !algorithmGraph.getAlgorithmNodes().containsValue(currentAlgorithmSettings.getAlgorithm())) {
            int dividerLocation = splitPane.getDividerLocation();
            splitPane.setRightComponent(documentationPanel);
            splitPane.setDividerLocation(dividerLocation);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if(SwingUtilities.isMiddleMouseButton(e)) {
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
        if(isPanning) {
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
        if(isPanning && panningOffset != null && panningScrollbarOffset != null) {
            int x = e.getX() - scrollPane.getHorizontalScrollBar().getValue();
            int y = e.getY() - scrollPane.getVerticalScrollBar().getValue();
            int dx = x - panningOffset.x;
            int dy = y - panningOffset.y;
            if(!switchPanningDirectionButton.isSelected()) {
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
