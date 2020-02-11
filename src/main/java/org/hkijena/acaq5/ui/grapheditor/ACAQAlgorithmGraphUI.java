package org.hkijena.acaq5.ui.grapheditor;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.components.MarkdownReader;
import org.hkijena.acaq5.ui.events.ACAQAlgorithmUIOpenSettingsRequested;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Set;

public class ACAQAlgorithmGraphUI extends ACAQUIPanel {

    private ACAQAlgorithmGraphCanvasUI graphUI;
    protected JMenuBar menuBar = new JMenuBar();
    private ACAQAlgorithmGraph algorithmGraph;
    private JSplitPane splitPane;
    private ACAQAlgorithmSettingsUI currentAlgorithmSettings;

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
        splitPane.setLeftComponent(new JScrollPane(graphUI) {
            {
                setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
                setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            }
        });
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
        addSegmenterMenu.setIcon(UIUtils.getIconFromResources("data-type-binary.png"));
        initializeMenuForCategory(addSegmenterMenu, ACAQAlgorithmCategory.Segmenter);
        menuBar.add(addSegmenterMenu);

        JMenu addConverterMenu = new JMenu("Convert");
        addConverterMenu.setIcon(UIUtils.getIconFromResources("convert.png"));
        initializeMenuForCategory(addConverterMenu, ACAQAlgorithmCategory.Converter);
        menuBar.add(addConverterMenu);

        menuBar.add(Box.createHorizontalGlue());
        JButton autoLayoutButton = new JButton("Auto layout", UIUtils.getIconFromResources("sort.png"));
        menuBar.add(autoLayoutButton);
    }

    private void initializeMenuForCategory(JMenu menu, ACAQAlgorithmCategory category) {
        ACAQRegistryService registryService = ACAQRegistryService.getInstance();
        for(Class<? extends ACAQAlgorithm> algorithmClass : registryService.getAlgorithmRegistry().getAlgorithmsOfCategory(category)) {
            JMenuItem addItem = new JMenuItem(ACAQAlgorithm.getName(algorithmClass), UIUtils.getIconFromResources("cog.png"));
            ACAQDocumentation documentation = algorithmClass.getAnnotation(ACAQDocumentation.class);
            if(!documentation.description().isEmpty())
                addItem.setToolTipText(documentation.description());
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
                JMenu dataMenu = new JMenu(ACAQData.getName(dataClass));
                dataMenu.setIcon(icon);

                for(Class<? extends ACAQDataSource<ACAQData>> sourceClass : dataSources) {
                    JMenuItem addItem = new JMenuItem(ACAQAlgorithm.getName(sourceClass), icon);
                    ACAQDocumentation documentation = sourceClass.getAnnotation(ACAQDocumentation.class);
                    if(!documentation.description().isEmpty())
                        addItem.setToolTipText(documentation.description());
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
            currentAlgorithmSettings = new ACAQAlgorithmSettingsUI(event.getUi().getAlgorithm());
            splitPane.setRightComponent(currentAlgorithmSettings);
        }
    }
}
