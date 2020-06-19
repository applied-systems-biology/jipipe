package org.hkijena.acaq5.ui.grapheditor;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.grouping.NodeGroup;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.MarkdownReader;
import org.hkijena.acaq5.ui.events.AlgorithmUIActionRequestedEvent;
import org.hkijena.acaq5.ui.events.DefaultAlgorithmUIActionRequestedEvent;
import org.hkijena.acaq5.ui.grapheditor.contextmenu.*;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQMultiAlgorithmSelectionPanelUI;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQSingleAlgorithmSelectionPanelUI;
import org.hkijena.acaq5.ui.grouping.ACAQNodeGroupUI;
import org.hkijena.acaq5.ui.registries.ACAQUIAlgorithmRegistry;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Editor for a project graph compartment
 */
public class ACAQAlgorithmGraphCompartmentUI extends ACAQAlgorithmGraphEditorUI {

    private MarkdownReader documentationPanel;
    private boolean disableUpdateOnSelection = false;

    /**
     * Creates a project graph compartment editor
     *
     * @param workbenchUI    The workbench
     * @param algorithmGraph The graph
     * @param compartment    The compartment
     */
    public ACAQAlgorithmGraphCompartmentUI(ACAQWorkbench workbenchUI, ACAQAlgorithmGraph algorithmGraph, String compartment) {
        super(workbenchUI, algorithmGraph, compartment);
        documentationPanel = new MarkdownReader(false);
        documentationPanel.setDocument(MarkdownDocument.fromPluginResource("documentation/algorithm-graph.md"));
        setPropertyPanel(documentationPanel);

        // Set D&D and Copy&Paste behavior
        getCanvasUI().setDragAndDropBehavior(new ACAQStandardDragAndDropBehavior());
        getCanvasUI().setCopyPasteBehavior(new ACAQStandardCopyPasteBehavior(this));
        updateContextMenu();
    }

    @Override
    public void installNodeUIFeatures(ACAQAlgorithmUI ui) {
        ui.installContextMenu(Arrays.asList(
                new OpenSettingsAlgorithmContextMenuFeature(),
                new AddToSelectionAlgorithmContextMenuFeature(),
                new SeparatorAlgorithmContextMenuFeature(),
                new RunAndShowResultsAlgorithmContextMenuFeature(),
                new SeparatorAlgorithmContextMenuFeature(),
                new CutCopyAlgorithmContextMenuFeature(),
                new SeparatorAlgorithmContextMenuFeature(),
                new EnableDisablePassThroughAlgorithmContextMenuFeature(),
                new SeparatorAlgorithmContextMenuFeature(),
                new JsonAlgorithmToGroupAlgorithmContextMenuFeature(),
                new CollapseIOInterfaceAlgorithmContextMenuFeature(),
                new DeleteAlgorithmContextMenuFeature()
        ));
    }


    @Override
    public void reloadMenuBar() {
        getMenuBar().removeAll();
        getAddableAlgorithms().clear();
        initializeAddNodesMenus(getMenuBar(), getAlgorithmGraph(), getCompartment(), getAddableAlgorithms());
        initializeCommonActions();
        updateNavigation();
    }

    @Override
    protected void updateSelection() {
        super.updateSelection();
        if (disableUpdateOnSelection)
            return;
        if (getSelection().isEmpty()) {
            setPropertyPanel(documentationPanel);
        } else if (getSelection().size() == 1) {
            ACAQAlgorithmUI ui = getSelection().iterator().next();
            setPropertyPanel(new ACAQSingleAlgorithmSelectionPanelUI((ACAQProjectWorkbench) getWorkbench(), getCanvasUI(), ui.getAlgorithm()));
        } else {
            setPropertyPanel(new ACAQMultiAlgorithmSelectionPanelUI((ACAQProjectWorkbench) getWorkbench(), getCanvasUI(),
                    getSelection().stream().map(ACAQAlgorithmUI::getAlgorithm).collect(Collectors.toSet())));
        }
    }

    @Subscribe
    public void onDefaultActionRequested(DefaultAlgorithmUIActionRequestedEvent event) {
        if (event.getUi().getAlgorithm() instanceof NodeGroup) {
            if (event.getUi().getAlgorithm() instanceof NodeGroup) {
                if (getWorkbench() instanceof ACAQProjectWorkbench) {
                    ACAQNodeGroupUI.openGroupNodeGraph(getWorkbench(), (NodeGroup) event.getUi().getAlgorithm(), true);
                }
            }
        }
    }

    /**
     * Listens to events of algorithms requesting some action
     *
     * @param event the event
     */
    @Subscribe
    public void onAlgorithmActionRequested(AlgorithmUIActionRequestedEvent event) {
        if (Objects.equals(event.getAction(), ACAQAlgorithmUI.REQUEST_RUN_AND_SHOW_RESULTS) ||
                Objects.equals(event.getAction(), ACAQAlgorithmUI.REQUEST_RUN_ONLY)) {
            disableUpdateOnSelection = true;
            selectOnly(event.getUi());
            ACAQSingleAlgorithmSelectionPanelUI panel = new ACAQSingleAlgorithmSelectionPanelUI((ACAQProjectWorkbench) getWorkbench(),
                    getCanvasUI(),
                    event.getUi().getAlgorithm());
            setPropertyPanel(panel);
            panel.runTestBench(Objects.equals(event.getAction(), ACAQAlgorithmUI.REQUEST_RUN_AND_SHOW_RESULTS),
                    Objects.equals(event.getAction(), ACAQAlgorithmUI.REQUEST_RUN_ONLY));
            SwingUtilities.invokeLater(() -> disableUpdateOnSelection = false);
        }
    }

    /**
     * Initializes the "Add nodes" menus
     *
     * @param menuBar         The menu bar where the items are created
     * @param algorithmGraph  the algorithm graph where nodes are put
     * @param compartment     the graph compartment where nodes are put
     * @param addedAlgorithms added algorithm types are added to this list
     */
    public static void initializeAddNodesMenus(JMenuBar menuBar, ACAQAlgorithmGraph algorithmGraph, String compartment, Set<ACAQAlgorithmDeclaration> addedAlgorithms) {
        JMenu addDataSourceMenu = new JMenu("Add data");
        addDataSourceMenu.setIcon(UIUtils.getIconFromResources("database.png"));
        initializeAddDataSourceMenu(addDataSourceMenu, algorithmGraph, compartment, addedAlgorithms);
        menuBar.add(addDataSourceMenu);

        JMenu addFilesystemMenu = new JMenu("Filesystem");
        addFilesystemMenu.setIcon(UIUtils.getIconFromResources("tree.png"));
        initializeMenuForCategory(addFilesystemMenu, ACAQAlgorithmCategory.FileSystem, algorithmGraph, compartment, addedAlgorithms);
        menuBar.add(addFilesystemMenu);

        JMenu addAnnotationMenu = new JMenu("Annotation");
        addAnnotationMenu.setIcon(UIUtils.getIconFromResources("label.png"));
        initializeMenuForCategory(addAnnotationMenu, ACAQAlgorithmCategory.Annotation, algorithmGraph, compartment, addedAlgorithms);
        menuBar.add(addAnnotationMenu);

        JMenu addEnhancerMenu = new JMenu("Process");
        addEnhancerMenu.setIcon(UIUtils.getIconFromResources("magic.png"));
        initializeMenuForCategory(addEnhancerMenu, ACAQAlgorithmCategory.Processor, algorithmGraph, compartment, addedAlgorithms);
        menuBar.add(addEnhancerMenu);

        JMenu addConverterMenu = new JMenu("Convert");
        addConverterMenu.setIcon(UIUtils.getIconFromResources("convert.png"));
        initializeMenuForCategory(addConverterMenu, ACAQAlgorithmCategory.Converter, algorithmGraph, compartment, addedAlgorithms);
        menuBar.add(addConverterMenu);

        JMenu addQuantifierMenu = new JMenu("Analyze");
        addQuantifierMenu.setIcon(UIUtils.getIconFromResources("statistics.png"));
        initializeMenuForCategory(addQuantifierMenu, ACAQAlgorithmCategory.Analysis, algorithmGraph, compartment, addedAlgorithms);
        menuBar.add(addQuantifierMenu);

        JMenu addMiscMenu = new JMenu("Miscellaneous");
        addMiscMenu.setIcon(UIUtils.getIconFromResources("module.png"));
        initializeMenuForCategory(addMiscMenu, ACAQAlgorithmCategory.Miscellaneous, algorithmGraph, compartment, addedAlgorithms);
        menuBar.add(addMiscMenu);
    }

    /**
     * Initializes a menu for one algorithm category
     *
     * @param menu            The menu
     * @param category        The algorithm category
     * @param algorithmGraph  the algorithm graph where nodes are added
     * @param compartment     the graph compartment where nodes are put
     * @param addedAlgorithms added algorithm types are added to this list
     */
    public static void initializeMenuForCategory(JMenu menu, ACAQAlgorithmCategory category, ACAQAlgorithmGraph algorithmGraph, String compartment, Set<ACAQAlgorithmDeclaration> addedAlgorithms) {
        ACAQDefaultRegistry registryService = ACAQDefaultRegistry.getInstance();
        Set<ACAQAlgorithmDeclaration> algorithmsOfCategory = registryService.getAlgorithmRegistry().getAlgorithmsOfCategory(category);
        if (algorithmsOfCategory.isEmpty()) {
            menu.setVisible(false);
            return;
        }

        Map<String, Set<ACAQAlgorithmDeclaration>> byMenuPath = ACAQAlgorithmDeclaration.groupByMenuPaths(algorithmsOfCategory);
        Map<String, JMenu> menuTree = UIUtils.createMenuTree(menu, byMenuPath.keySet());

        for (Map.Entry<String, Set<ACAQAlgorithmDeclaration>> entry : byMenuPath.entrySet()) {
            JMenu subMenu = menuTree.get(entry.getKey());
            for (ACAQAlgorithmDeclaration declaration : ACAQAlgorithmDeclaration.getSortedList(entry.getValue())) {
                if (declaration.isHidden())
                    continue;
                JMenuItem addItem = new JMenuItem(declaration.getName(), ACAQUIAlgorithmRegistry.getInstance().getIconFor(declaration));
                addItem.setToolTipText(TooltipUtils.getAlgorithmTooltip(declaration));
                addItem.addActionListener(e -> algorithmGraph.insertNode(declaration.newInstance(), compartment));
                addedAlgorithms.add(declaration);
                subMenu.add(addItem);
            }
        }
    }

    /**
     * Initializes a menu that adds data sources
     *
     * @param menu            the target menu
     * @param algorithmGraph  the algorithm graph where nodes are put
     * @param compartment     the compartment where nodes are put
     * @param addedAlgorithms added algorithm types are added to this list
     */
    public static void initializeAddDataSourceMenu(JMenu menu, ACAQAlgorithmGraph algorithmGraph, String compartment, Set<ACAQAlgorithmDeclaration> addedAlgorithms) {
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
                    if (declaration.isHidden())
                        continue;
                    JMenuItem addItem = new JMenuItem(declaration.getName(), ACAQUIAlgorithmRegistry.getInstance().getIconFor(declaration));
                    addItem.setToolTipText(TooltipUtils.getAlgorithmTooltip(declaration));
                    addItem.addActionListener(e -> algorithmGraph.insertNode(declaration.newInstance(), compartment));
                    addedAlgorithms.add(declaration);
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
