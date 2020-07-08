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

package org.hkijena.acaq5.ui.grapheditor;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQGraph;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.grouping.NodeGroup;
import org.hkijena.acaq5.api.history.AddNodeGraphHistorySnapshot;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.MarkdownReader;
import org.hkijena.acaq5.ui.events.AlgorithmUIActionRequestedEvent;
import org.hkijena.acaq5.ui.events.DefaultAlgorithmUIActionRequestedEvent;
import org.hkijena.acaq5.ui.grapheditor.contextmenu.*;
import org.hkijena.acaq5.ui.grapheditor.contextmenu.clipboard.AlgorithmGraphCopyAlgorithmUIAction;
import org.hkijena.acaq5.ui.grapheditor.contextmenu.clipboard.AlgorithmGraphCutAlgorithmUIAction;
import org.hkijena.acaq5.ui.grapheditor.contextmenu.clipboard.AlgorithmGraphPasteAlgorithmUIAction;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQMultiAlgorithmSelectionPanelUI;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQSingleAlgorithmSelectionPanelUI;
import org.hkijena.acaq5.ui.grouping.ACAQNodeGroupUI;
import org.hkijena.acaq5.ui.registries.ACAQUIAlgorithmRegistry;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Editor for a project graph compartment
 */
public class ACAQGraphCompartmentUI extends ACAQGraphEditorUI {

    private final MarkdownReader documentationPanel;
    private boolean disableUpdateOnSelection = false;

    /**
     * Creates a project graph compartment editor
     *
     * @param workbenchUI    The workbench
     * @param algorithmGraph The graph
     * @param compartment    The compartment
     */
    public ACAQGraphCompartmentUI(ACAQWorkbench workbenchUI, ACAQGraph algorithmGraph, String compartment) {
        super(workbenchUI, algorithmGraph, compartment);
        documentationPanel = new MarkdownReader(false);
        documentationPanel.setDocument(MarkdownDocument.fromPluginResource("documentation/algorithm-graph.md"));
        setPropertyPanel(documentationPanel);

        // Set D&D and Copy&Paste behavior
        getCanvasUI().setDragAndDropBehavior(new ACAQStandardDragAndDropBehavior());
        getCanvasUI().setContextActions(Arrays.asList(
                new AlgorithmGraphCutAlgorithmUIAction(),
                new AlgorithmGraphCopyAlgorithmUIAction(),
                new AlgorithmGraphPasteAlgorithmUIAction(),
                AlgorithmUIAction.SEPARATOR,
                new RunAndShowResultsAlgorithmUIAction(),
                new UpdateCacheAlgorithmUIAction(),
                AlgorithmUIAction.SEPARATOR,
                new ExportAlgorithmUIAction(),
                AlgorithmUIAction.SEPARATOR,
                new JsonAlgorithmToGroupAlgorithmUIAction(),
                new GroupAlgorithmUIAction(),
                new CollapseIOInterfaceAlgorithmUIAction(),
                AlgorithmUIAction.SEPARATOR,
                new EnableAlgorithmUIAction(),
                new DisableAlgorithmUIAction(),
                new EnablePassThroughAlgorithmUIAction(),
                new DisablePassThroughAlgorithmUIAction(),
                new DeleteAlgorithmUIAction(),
                AlgorithmUIAction.SEPARATOR,
                new SelectAndMoveNodeHereAlgorithmUIAction()
        ));
    }

//    @Override
//    public void installNodeUIFeatures(ACAQAlgorithmUI ui) {
//        ui.installContextMenu(Arrays.asList(
//                new OpenSettingsAlgorithmContextMenuFeature(),
//                new AddToSelectionAlgorithmContextMenuFeature(),
//                new SeparatorAlgorithmContextMenuFeature(),
//                new RunAndShowResultsAlgorithmContextMenuFeature(),
//                new SeparatorAlgorithmContextMenuFeature(),
//                new CutCopyAlgorithmContextMenuFeature(),
//                new SeparatorAlgorithmContextMenuFeature(),
//                new EnableDisablePassThroughAlgorithmContextMenuFeature(),
//                new SeparatorAlgorithmContextMenuFeature(),
//                new JsonAlgorithmToGroupAlgorithmContextMenuFeature(),
//                new CollapseIOInterfaceAlgorithmContextMenuFeature(),
//                new DeleteAlgorithmContextMenuFeature()
//        ));
//    }

    @Override
    public void reloadMenuBar() {
        getMenuBar().removeAll();
        getAddableAlgorithms().clear();
        initializeAddNodesMenus(this, getMenuBar(), getAddableAlgorithms());
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
            ACAQNodeUI ui = getSelection().iterator().next();
            setPropertyPanel(new ACAQSingleAlgorithmSelectionPanelUI(this, ui.getNode()));
        } else {
            setPropertyPanel(new ACAQMultiAlgorithmSelectionPanelUI((ACAQProjectWorkbench) getWorkbench(), getCanvasUI(),
                    getSelection().stream().map(ACAQNodeUI::getNode).collect(Collectors.toSet())));
        }
    }

    @Subscribe
    public void onDefaultActionRequested(DefaultAlgorithmUIActionRequestedEvent event) {
        if (event.getUi().getNode() instanceof NodeGroup) {
            if (event.getUi().getNode() instanceof NodeGroup) {
                if (getWorkbench() instanceof ACAQProjectWorkbench) {
                    ACAQNodeGroupUI.openGroupNodeGraph(getWorkbench(), (NodeGroup) event.getUi().getNode(), true);
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
        if (Objects.equals(event.getAction(), ACAQNodeUI.REQUEST_RUN_AND_SHOW_RESULTS) ||
                Objects.equals(event.getAction(), ACAQNodeUI.REQUEST_RUN_ONLY)) {
            disableUpdateOnSelection = true;
            selectOnly(event.getUi());
            ACAQSingleAlgorithmSelectionPanelUI panel = new ACAQSingleAlgorithmSelectionPanelUI(this,
                    event.getUi().getNode());
            setPropertyPanel(panel);
            panel.runTestBench(Objects.equals(event.getAction(), ACAQNodeUI.REQUEST_RUN_AND_SHOW_RESULTS),
                    Objects.equals(event.getAction(), ACAQNodeUI.REQUEST_RUN_ONLY));
            SwingUtilities.invokeLater(() -> disableUpdateOnSelection = false);
        }
    }

    /**
     * Initializes the "Add nodes" menus
     *
     * @param graphEditorUI   the graph editor
     * @param menuBar         The menu bar where the items are created
     * @param addedAlgorithms added algorithm types are added to this list
     */
    public static void initializeAddNodesMenus(ACAQGraphEditorUI graphEditorUI, JMenuBar menuBar, Set<ACAQAlgorithmDeclaration> addedAlgorithms) {
        JMenu addDataSourceMenu = new JMenu("Add data");
        addDataSourceMenu.setIcon(UIUtils.getIconFromResources("database.png"));
        initializeAddDataSourceMenu(graphEditorUI, addDataSourceMenu, addedAlgorithms);
        menuBar.add(addDataSourceMenu);

        JMenu addFilesystemMenu = new JMenu("Filesystem");
        addFilesystemMenu.setIcon(UIUtils.getIconFromResources("tree.png"));
        initializeMenuForCategory(graphEditorUI, addFilesystemMenu, ACAQAlgorithmCategory.FileSystem, addedAlgorithms);
        menuBar.add(addFilesystemMenu);

        JMenu addAnnotationMenu = new JMenu("Annotation");
        addAnnotationMenu.setIcon(UIUtils.getIconFromResources("label.png"));
        initializeMenuForCategory(graphEditorUI, addAnnotationMenu, ACAQAlgorithmCategory.Annotation, addedAlgorithms);
        menuBar.add(addAnnotationMenu);

        JMenu addEnhancerMenu = new JMenu("Process");
        addEnhancerMenu.setIcon(UIUtils.getIconFromResources("magic.png"));
        initializeMenuForCategory(graphEditorUI, addEnhancerMenu, ACAQAlgorithmCategory.Processor, addedAlgorithms);
        menuBar.add(addEnhancerMenu);

        JMenu addConverterMenu = new JMenu("Convert");
        addConverterMenu.setIcon(UIUtils.getIconFromResources("convert.png"));
        initializeMenuForCategory(graphEditorUI, addConverterMenu, ACAQAlgorithmCategory.Converter, addedAlgorithms);
        menuBar.add(addConverterMenu);

        JMenu addQuantifierMenu = new JMenu("Analyze");
        addQuantifierMenu.setIcon(UIUtils.getIconFromResources("statistics.png"));
        initializeMenuForCategory(graphEditorUI, addQuantifierMenu, ACAQAlgorithmCategory.Analysis, addedAlgorithms);
        menuBar.add(addQuantifierMenu);

        JMenu addMiscMenu = new JMenu("Miscellaneous");
        addMiscMenu.setIcon(UIUtils.getIconFromResources("module.png"));
        initializeMenuForCategory(graphEditorUI, addMiscMenu, ACAQAlgorithmCategory.Miscellaneous, addedAlgorithms);
        menuBar.add(addMiscMenu);
    }

    /**
     * Initializes a menu for one algorithm category
     *
     * @param graphEditorUI   the graph editor
     * @param menu            The menu
     * @param category        The algorithm category
     * @param addedAlgorithms added algorithm types are added to this list
     */
    public static void initializeMenuForCategory(ACAQGraphEditorUI graphEditorUI, JMenu menu, ACAQAlgorithmCategory category, Set<ACAQAlgorithmDeclaration> addedAlgorithms) {
        ACAQGraph algorithmGraph = graphEditorUI.getAlgorithmGraph();
        String compartment = graphEditorUI.getCompartment();
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
                addItem.addActionListener(e -> {
                    ACAQGraphNode node = declaration.newInstance();
                    graphEditorUI.getCanvasUI().getGraphHistory().addSnapshotBefore(new AddNodeGraphHistorySnapshot(algorithmGraph, Collections.singleton(node)));
                    algorithmGraph.insertNode(node, compartment);
                });
                addedAlgorithms.add(declaration);
                subMenu.add(addItem);
            }
        }
    }

    /**
     * Initializes a menu that adds data sources
     *
     * @param graphEditorUI   the editor
     * @param menu            the target menu
     * @param addedAlgorithms added algorithm types are added to this list
     */
    public static void initializeAddDataSourceMenu(ACAQGraphEditorUI graphEditorUI, JMenu menu, Set<ACAQAlgorithmDeclaration> addedAlgorithms) {
        ACAQGraph algorithmGraph = graphEditorUI.getAlgorithmGraph();
        String compartment = graphEditorUI.getCompartment();
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
                    addItem.addActionListener(e -> {
                        ACAQGraphNode node = declaration.newInstance();
                        graphEditorUI.getCanvasUI().getGraphHistory().addSnapshotBefore(new AddNodeGraphHistorySnapshot(algorithmGraph, Collections.singleton(node)));
                        algorithmGraph.insertNode(node, compartment);
                    });
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
