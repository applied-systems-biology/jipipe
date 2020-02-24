package org.hkijena.acaq5.ui.grapheditor;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSource;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.components.MarkdownReader;
import org.hkijena.acaq5.ui.events.ACAQAlgorithmUIOpenSettingsRequested;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQAlgorithmSettingsUI;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Set;

public class ACAQAlgorithmGraphUI extends ACAQUIPanel implements MouseListener, MouseMotionListener {

    private ACAQAlgorithmGraphCanvasUI graphUI;
    protected JMenuBar menuBar = new JMenuBar();
    private ACAQAlgorithmGraph algorithmGraph;
    private JSplitPane splitPane;
    private JScrollPane scrollPane;
    private ACAQAlgorithmSettingsUI currentAlgorithmSettings;
    private JToggleButton autoLayoutButton;

    private Point panningOffset = null;
    private Point panningScrollbarOffset = null;
    private boolean isPanning = false;
    private JToggleButton switchPanningDirectionButton;

    public ACAQAlgorithmGraphUI(ACAQWorkbenchUI workbenchUI, ACAQAlgorithmGraph algorithmGraph) {
        super(workbenchUI);
        this.algorithmGraph = algorithmGraph;
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

        graphUI = new ACAQAlgorithmGraphCanvasUI(algorithmGraph);
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
        splitPane.setRightComponent(new MarkdownReader(false) {
            {
                loadFromResource("documentation/algorithm-graph.md");
            }
        });
        add(splitPane, BorderLayout.CENTER);

        add(menuBar, BorderLayout.NORTH);
        initializeToolbar();
    }

    protected void initializeToolbar() {
        JMenu addDataSourceMenu = new JMenu("Add data");
        addDataSourceMenu.setIcon(UIUtils.getIconFromResources("database.png"));
        initializeAddDataSourceMenu(addDataSourceMenu);
        menuBar.add(addDataSourceMenu);

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

        menuBar.add(Box.createHorizontalGlue());


        switchPanningDirectionButton = new JToggleButton("Reverse panning direction",
                UIUtils.getIconFromResources("cursor-arrow.png"));
        UIUtils.makeFlat(switchPanningDirectionButton);
        switchPanningDirectionButton.setToolTipText("Changes the direction how panning (middle mouse button) affects the view.");
        menuBar.add(switchPanningDirectionButton);

        autoLayoutButton = new JToggleButton("Auto layout", UIUtils.getIconFromResources("sort.png"), true);
        graphUI.setLayoutHelperEnabled(true);
        autoLayoutButton.addActionListener(e -> graphUI.setLayoutHelperEnabled(autoLayoutButton.isSelected()));
        menuBar.add(autoLayoutButton);
    }

    private void initializeMenuForCategory(JMenu menu, ACAQAlgorithmCategory category) {
        ACAQRegistryService registryService = ACAQRegistryService.getInstance();
        for(Class<? extends ACAQAlgorithm> algorithmClass : registryService.getAlgorithmRegistry().getAlgorithmsOfCategory(category)) {
            JMenuItem addItem = new JMenuItem(ACAQAlgorithm.getNameOf(algorithmClass), UIUtils.getIconFromResources("cog.png"));
            addItem.setToolTipText(TooltipUtils.getAlgorithmTooltip(algorithmClass));
            addItem.addActionListener(e -> algorithmGraph.insertNode(ACAQAlgorithm.createInstance(algorithmClass)));
            menu.add(addItem);
        }
    }

    private void initializeAddDataSourceMenu(JMenu menu) {
        ACAQRegistryService registryService = ACAQRegistryService.getInstance();
        for(Class<? extends ACAQData> dataClass : registryService.getDatatypeRegistry().getRegisteredDataTypes()) {
            Set<Class<? extends ACAQDataSource<ACAQData>>> dataSources = registryService.getAlgorithmRegistry().getDataSourcesFor(dataClass);
            if(!dataSources.isEmpty()) {
                Icon icon = registryService.getUIDatatypeRegistry().getIconFor(dataClass);
                JMenu dataMenu = new JMenu(ACAQData.getNameOf(dataClass));
                dataMenu.setIcon(icon);

                for(Class<? extends ACAQDataSource<ACAQData>> sourceClass : dataSources) {
                    JMenuItem addItem = new JMenuItem(ACAQAlgorithm.getNameOf(sourceClass), icon);
                    addItem.setToolTipText(TooltipUtils.getAlgorithmTooltip(sourceClass));
                    addItem.addActionListener(e -> algorithmGraph.insertNode(ACAQAlgorithm.createInstance(sourceClass)));
                    dataMenu.add(addItem);
                }

                menu.add(dataMenu);
            }
        }
    }

    public ACAQAlgorithmGraph getAlgorithmGraph() {
        return algorithmGraph;
    }

    @Subscribe
    public void onOpenAlgorithmSettings(ACAQAlgorithmUIOpenSettingsRequested event) {
        if(currentAlgorithmSettings == null || currentAlgorithmSettings.getAlgorithm() != event.getUi().getAlgorithm()) {
            currentAlgorithmSettings = new ACAQAlgorithmSettingsUI(getWorkbenchUI(), algorithmGraph, event.getUi().getAlgorithm());
            int dividerLocation = splitPane.getDividerLocation();
            splitPane.setRightComponent(currentAlgorithmSettings);
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
            int y = e.getX() - scrollPane.getVerticalScrollBar().getValue();
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
            int y = e.getX() - scrollPane.getVerticalScrollBar().getValue();
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
}
