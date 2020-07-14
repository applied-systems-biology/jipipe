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

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipeDefaultRegistry;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeInfo;
import org.hkijena.jipipe.api.algorithm.JIPipeGraph;
import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.grouping.NodeGroup;
import org.hkijena.jipipe.api.history.AddNodeGraphHistorySnapshot;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.components.MarkdownReader;
import org.hkijena.jipipe.ui.events.AlgorithmUIActionRequestedEvent;
import org.hkijena.jipipe.ui.events.DefaultAlgorithmUIActionRequestedEvent;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.*;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.clipboard.AlgorithmGraphCopyAlgorithmUIAction;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.clipboard.AlgorithmGraphCutAlgorithmUIAction;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.clipboard.AlgorithmGraphPasteAlgorithmUIAction;
import org.hkijena.jipipe.ui.grapheditor.settings.JIPipeMultiAlgorithmSelectionPanelUI;
import org.hkijena.jipipe.ui.grapheditor.settings.JIPipeSingleAlgorithmSelectionPanelUI;
import org.hkijena.jipipe.ui.grouping.JIPipeNodeGroupUI;
import org.hkijena.jipipe.ui.registries.JIPipeUIAlgorithmRegistry;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Editor for a project graph compartment
 */
public class JIPipeGraphCompartmentUI extends JIPipeGraphEditorUI {

    private final MarkdownReader documentationPanel;
    private boolean disableUpdateOnSelection = false;

    /**
     * Creates a project graph compartment editor
     *
     * @param workbenchUI    The workbench
     * @param algorithmGraph The graph
     * @param compartment    The compartment
     */
    public JIPipeGraphCompartmentUI(JIPipeWorkbench workbenchUI, JIPipeGraph algorithmGraph, String compartment) {
        super(workbenchUI, algorithmGraph, compartment);
        documentationPanel = new MarkdownReader(false);
        documentationPanel.setDocument(MarkdownDocument.fromPluginResource("documentation/algorithm-graph.md"));
        setPropertyPanel(documentationPanel);

        // Set D&D and Copy&Paste behavior
        getCanvasUI().setDragAndDropBehavior(new JIPipeStandardDragAndDropBehavior());
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
//    public void installNodeUIFeatures(JIPipeAlgorithmUI ui) {
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
            JIPipeNodeUI ui = getSelection().iterator().next();
            setPropertyPanel(new JIPipeSingleAlgorithmSelectionPanelUI(this, ui.getNode()));
        } else {
            setPropertyPanel(new JIPipeMultiAlgorithmSelectionPanelUI((JIPipeProjectWorkbench) getWorkbench(), getCanvasUI(),
                    getSelection().stream().map(JIPipeNodeUI::getNode).collect(Collectors.toSet())));
        }
    }

    @Subscribe
    public void onDefaultActionRequested(DefaultAlgorithmUIActionRequestedEvent event) {
        if (event.getUi().getNode() instanceof NodeGroup) {
            if (event.getUi().getNode() instanceof NodeGroup) {
                if (getWorkbench() instanceof JIPipeProjectWorkbench) {
                    JIPipeNodeGroupUI.openGroupNodeGraph(getWorkbench(), (NodeGroup) event.getUi().getNode(), true);
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
        boolean runAndShowResults = Objects.equals(event.getAction(), JIPipeNodeUI.REQUEST_RUN_AND_SHOW_RESULTS);
        boolean updateCache = Objects.equals(event.getAction(), JIPipeNodeUI.REQUEST_UPDATE_CACHE);
        if (runAndShowResults ||
                updateCache) {
            disableUpdateOnSelection = true;
            selectOnly(event.getUi());
            JIPipeSingleAlgorithmSelectionPanelUI panel = new JIPipeSingleAlgorithmSelectionPanelUI(this,
                    event.getUi().getNode());
            setPropertyPanel(panel);
            panel.runTestBench(runAndShowResults,
                    updateCache, !updateCache);
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
    public static void initializeAddNodesMenus(JIPipeGraphEditorUI graphEditorUI, JMenuBar menuBar, Set<JIPipeNodeInfo> addedAlgorithms) {
        JMenu addDataSourceMenu = new JMenu("Add data");
        addDataSourceMenu.setIcon(UIUtils.getIconFromResources("database.png"));
        initializeAddDataSourceMenu(graphEditorUI, addDataSourceMenu, addedAlgorithms);
        menuBar.add(addDataSourceMenu);

        JMenu addFilesystemMenu = new JMenu("Filesystem");
        addFilesystemMenu.setIcon(UIUtils.getIconFromResources("tree.png"));
        initializeMenuForCategory(graphEditorUI, addFilesystemMenu, JIPipeAlgorithmCategory.FileSystem, addedAlgorithms);
        menuBar.add(addFilesystemMenu);

        JMenu addAnnotationMenu = new JMenu("Annotation");
        addAnnotationMenu.setIcon(UIUtils.getIconFromResources("label.png"));
        initializeMenuForCategory(graphEditorUI, addAnnotationMenu, JIPipeAlgorithmCategory.Annotation, addedAlgorithms);
        menuBar.add(addAnnotationMenu);

        JMenu addEnhancerMenu = new JMenu("Process");
        addEnhancerMenu.setIcon(UIUtils.getIconFromResources("magic.png"));
        initializeMenuForCategory(graphEditorUI, addEnhancerMenu, JIPipeAlgorithmCategory.Processor, addedAlgorithms);
        menuBar.add(addEnhancerMenu);

        JMenu addConverterMenu = new JMenu("Convert");
        addConverterMenu.setIcon(UIUtils.getIconFromResources("convert.png"));
        initializeMenuForCategory(graphEditorUI, addConverterMenu, JIPipeAlgorithmCategory.Converter, addedAlgorithms);
        menuBar.add(addConverterMenu);

        JMenu addQuantifierMenu = new JMenu("Analyze");
        addQuantifierMenu.setIcon(UIUtils.getIconFromResources("statistics.png"));
        initializeMenuForCategory(graphEditorUI, addQuantifierMenu, JIPipeAlgorithmCategory.Analysis, addedAlgorithms);
        menuBar.add(addQuantifierMenu);

        JMenu addMiscMenu = new JMenu("Miscellaneous");
        addMiscMenu.setIcon(UIUtils.getIconFromResources("module.png"));
        initializeMenuForCategory(graphEditorUI, addMiscMenu, JIPipeAlgorithmCategory.Miscellaneous, addedAlgorithms);
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
    public static void initializeMenuForCategory(JIPipeGraphEditorUI graphEditorUI, JMenu menu, JIPipeAlgorithmCategory category, Set<JIPipeNodeInfo> addedAlgorithms) {
        JIPipeGraph algorithmGraph = graphEditorUI.getAlgorithmGraph();
        String compartment = graphEditorUI.getCompartment();
        JIPipeDefaultRegistry registryService = JIPipeDefaultRegistry.getInstance();
        Set<JIPipeNodeInfo> algorithmsOfCategory = registryService.getAlgorithmRegistry().getAlgorithmsOfCategory(category);
        if (algorithmsOfCategory.isEmpty()) {
            menu.setVisible(false);
            return;
        }

        Map<String, Set<JIPipeNodeInfo>> byMenuPath = JIPipeNodeInfo.groupByMenuPaths(algorithmsOfCategory);
        Map<String, JMenu> menuTree = UIUtils.createMenuTree(menu, byMenuPath.keySet());

        for (Map.Entry<String, Set<JIPipeNodeInfo>> entry : byMenuPath.entrySet()) {
            JMenu subMenu = menuTree.get(entry.getKey());
            for (JIPipeNodeInfo info : JIPipeNodeInfo.getSortedList(entry.getValue())) {
                if (info.isHidden())
                    continue;
                JMenuItem addItem = new JMenuItem(info.getName(), JIPipeUIAlgorithmRegistry.getInstance().getIconFor(info));
                addItem.setToolTipText(TooltipUtils.getAlgorithmTooltip(info));
                addItem.addActionListener(e -> {
                    JIPipeGraphNode node = info.newInstance();
                    graphEditorUI.getCanvasUI().getGraphHistory().addSnapshotBefore(new AddNodeGraphHistorySnapshot(algorithmGraph, Collections.singleton(node)));
                    algorithmGraph.insertNode(node, compartment);
                });
                addedAlgorithms.add(info);
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
    public static void initializeAddDataSourceMenu(JIPipeGraphEditorUI graphEditorUI, JMenu menu, Set<JIPipeNodeInfo> addedAlgorithms) {
        JIPipeGraph algorithmGraph = graphEditorUI.getAlgorithmGraph();
        String compartment = graphEditorUI.getCompartment();
        JIPipeDefaultRegistry registryService = JIPipeDefaultRegistry.getInstance();
        Map<String, Set<Class<? extends JIPipeData>>> dataTypesByMenuPaths = JIPipeDatatypeRegistry.getInstance().getDataTypesByMenuPaths();
        Map<String, JMenu> menuTree = UIUtils.createMenuTree(menu, dataTypesByMenuPaths.keySet());

        for (Map.Entry<String, Set<Class<? extends JIPipeData>>> entry : dataTypesByMenuPaths.entrySet()) {
            JMenu subMenu = menuTree.get(entry.getKey());
            for (Class<? extends JIPipeData> dataClass : JIPipeData.getSortedList(entry.getValue())) {
                if (JIPipeData.isHidden(dataClass))
                    continue;
                Set<JIPipeNodeInfo> dataSources = registryService.getAlgorithmRegistry().getDataSourcesFor(dataClass);
                boolean isEmpty = true;
                Icon icon = registryService.getUIDatatypeRegistry().getIconFor(dataClass);
                JMenu dataMenu = new JMenu(JIPipeData.getNameOf(dataClass));
                dataMenu.setIcon(icon);

                for (JIPipeNodeInfo info : dataSources) {
                    if (info.isHidden())
                        continue;
                    JMenuItem addItem = new JMenuItem(info.getName(), JIPipeUIAlgorithmRegistry.getInstance().getIconFor(info));
                    addItem.setToolTipText(TooltipUtils.getAlgorithmTooltip(info));
                    addItem.addActionListener(e -> {
                        JIPipeGraphNode node = info.newInstance();
                        graphEditorUI.getCanvasUI().getGraphHistory().addSnapshotBefore(new AddNodeGraphHistorySnapshot(algorithmGraph, Collections.singleton(node)));
                        algorithmGraph.insertNode(node, compartment);
                    });
                    addedAlgorithms.add(info);
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
