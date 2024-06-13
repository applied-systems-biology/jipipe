/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.app.grapheditor.pipeline;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartmentOutput;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.grouping.JIPipeNodeGroup;
import org.hkijena.jipipe.api.history.JIPipeDedicatedGraphHistoryJournal;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseRole;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.bookmarks.JIPipeDesktopBookmarkListPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorMinimap;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.*;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.compartments.contextmenu.clipboard.clipboard.*;
import org.hkijena.jipipe.desktop.app.grapheditor.groups.JIPipeDesktopNodeGroupUI;
import org.hkijena.jipipe.desktop.app.grapheditor.pipeline.actions.JIPipeDesktopRunAndShowResultsAction;
import org.hkijena.jipipe.desktop.app.grapheditor.pipeline.actions.JIPipeDesktopUpdateCacheAction;
import org.hkijena.jipipe.desktop.app.grapheditor.pipeline.dragdrop.JIPipeCreateNodesFromDraggedDataDragAndDropBehavior;
import org.hkijena.jipipe.desktop.app.grapheditor.pipeline.properties.JIPipeDesktopPipelineMultiAlgorithmSelectionPanelUI;
import org.hkijena.jipipe.desktop.app.grapheditor.pipeline.properties.JIPipeDesktopPipelineSingleAlgorithmSelectionPanelUI;
import org.hkijena.jipipe.desktop.app.history.JIPipeDesktopHistoryJournalUI;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.nodetemplate.NodeTemplateBox;
import org.hkijena.jipipe.plugins.nodetemplate.NodeTemplateMenu;
import org.hkijena.jipipe.plugins.nodetoolboxtool.NodeToolBox;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.JIPipeGeneralUIApplicationSettings;
import org.hkijena.jipipe.utils.*;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Editor for a project graph compartment
 */
public class JIPipePipelineGraphEditorUI extends JIPipeDesktopGraphEditorUI {

    private JPanel defaultPanel;
    private boolean disableUpdateOnSelection = false;

    /**
     * Creates a project graph compartment editor
     *
     * @param workbenchUI    The workbench
     * @param algorithmGraph The graph
     * @param compartment    The compartment
     */
    public JIPipePipelineGraphEditorUI(JIPipeDesktopWorkbench workbenchUI, JIPipeGraph algorithmGraph, UUID compartment) {
        super(workbenchUI, algorithmGraph, compartment, algorithmGraph.getProject() != null ? algorithmGraph.getProject().getHistoryJournal() : new JIPipeDedicatedGraphHistoryJournal(algorithmGraph));
        initializeDefaultPanel();
        setPropertyPanel(defaultPanel, true);

        // Set D&D and Copy&Paste behavior
        initializeContextActions();
    }

    /**
     * Initializes the "Add nodes" menus
     *
     * @param graphEditorUI   the graph editor
     * @param menuBar         The menu bar where the items are created
     * @param addedAlgorithms added algorithm types are added to this list
     */
    public static void initializeAddNodesMenus(JIPipeDesktopGraphEditorUI graphEditorUI, JMenuBar menuBar, Set<JIPipeNodeInfo> addedAlgorithms) {

        for (JIPipeNodeTypeCategory category : JIPipe.getNodes().getRegisteredCategories().values().stream()
                .sorted(Comparator.comparing(JIPipeNodeTypeCategory::getUIOrder)).collect(Collectors.toList())) {
            if (category instanceof DataSourceNodeTypeCategory) {
                JMenu addDataSourceMenu = new JMenu(category.getName());
                addDataSourceMenu.setIcon(category.getIcon());
                initializeAddDataSourceMenu(graphEditorUI, addDataSourceMenu, addedAlgorithms);
                menuBar.add(addDataSourceMenu);
            } else if (category.isVisibleInGraphCompartment()) {
                JMenu menu = new JMenu(category.getName());
                menu.setIcon(category.getIcon());
                initializeMenuForCategory(graphEditorUI, menu, category, addedAlgorithms);
                menuBar.add(menu);
            }
        }

        // Add template menu
        menuBar.add(new NodeTemplateMenu(graphEditorUI.getDesktopWorkbench(), graphEditorUI));
    }

    /**
     * Initializes a menu for one algorithm category
     *
     * @param graphEditorUI   the graph editor
     * @param menu            The menu
     * @param category        The algorithm category
     * @param addedAlgorithms added algorithm types are added to this list
     */
    public static void initializeMenuForCategory(JIPipeDesktopGraphEditorUI graphEditorUI, JMenu menu, JIPipeNodeTypeCategory category, Set<JIPipeNodeInfo> addedAlgorithms) {
        JIPipeGraph algorithmGraph = graphEditorUI.getGraph();
        JIPipe registryService = JIPipe.getInstance();
        Set<JIPipeNodeInfo> algorithmsOfCategory = registryService.getNodeRegistry().getNodesOfCategory(category, true);
        if (algorithmsOfCategory.isEmpty()) {
            menu.setVisible(false);
            return;
        }

        Map<String, Set<JIPipeNodeInfo>> byMenuPath = JIPipeNodeInfo.groupByMenuPaths(category, algorithmsOfCategory);
        Map<String, JMenu> menuTree = UIUtils.createMenuTree(menu, byMenuPath.keySet());

        for (Map.Entry<String, Set<JIPipeNodeInfo>> entry : byMenuPath.entrySet()) {
            JMenu subMenu = menuTree.get(entry.getKey());
            for (JIPipeNodeInfo info : JIPipeNodeInfo.getSortedList(entry.getValue())) {
                if (info.isHidden())
                    continue;
                if (subMenu.getMenuComponentCount() >= 30) {
                    JMenu moreMenu = new JMenu("More ...");
                    subMenu.add(moreMenu);
                    subMenu = moreMenu;
                }
                String name = info.getName();
                boolean hasAlternativeName = false;

                // Alternative names
                for (JIPipeNodeMenuLocation location : info.getAliases()) {
                    if (StringUtils.isNullOrEmpty(location.getAlternativeName()))
                        continue;
                    if (Objects.equals(category.getId(), location.getCategory().getId())) {
                        String locationPath = StringUtils.getCleanedMenuPath(location.getMenuPath());
                        if (Objects.equals(locationPath, entry.getKey())) {
                            name = location.getAlternativeName();
                            hasAlternativeName = true;
                            break;
                        }
                    }
                }

                JMenuItem addItem = new JMenuItem(name, JIPipe.getNodes().getIconFor(info));
                addItem.setToolTipText(TooltipUtils.getAlgorithmTooltip(info));
                boolean finalHasAlternativeName = hasAlternativeName;
                String finalName = name;
                addItem.addActionListener(e -> {
                    if (!JIPipeDesktopProjectWorkbench.canAddOrDeleteNodes(graphEditorUI.getDesktopWorkbench()))
                        return;
                    JIPipeGraphNode node = info.newInstance();
                    if (finalHasAlternativeName) {
                        node.setCustomName(finalName);
                    }
                    graphEditorUI.getCanvasUI().getHistoryJournal().snapshotBeforeAddNode(node, graphEditorUI.getCompartment());
                    graphEditorUI.getCanvasUI().getScheduledSelection().clear();
                    graphEditorUI.getCanvasUI().getScheduledSelection().add(node);
                    algorithmGraph.insertNode(node, graphEditorUI.getCompartment());
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
    public static void initializeAddDataSourceMenu(JIPipeDesktopGraphEditorUI graphEditorUI, JMenu menu, Set<JIPipeNodeInfo> addedAlgorithms) {
        JIPipeGraph algorithmGraph = graphEditorUI.getGraph();
        JIPipe registryService = JIPipe.getInstance();
        Map<String, Set<Class<? extends JIPipeData>>> dataTypesByMenuPaths = JIPipe.getDataTypes().getDataTypesByMenuPaths();
        Map<String, JMenu> menuTree = UIUtils.createMenuTree(menu, dataTypesByMenuPaths.keySet());

        for (Map.Entry<String, Set<Class<? extends JIPipeData>>> entry : dataTypesByMenuPaths.entrySet()) {
            JMenu subMenu = menuTree.get(entry.getKey());
            for (Class<? extends JIPipeData> dataClass : JIPipeData.getSortedList(entry.getValue())) {
                if (JIPipeData.isHidden(dataClass))
                    continue;
                Set<JIPipeNodeInfo> dataSources = registryService.getNodeRegistry().getMenuDataSourcesFor(dataClass);
                boolean isEmpty = true;
                Icon icon = registryService.getDatatypeRegistry().getIconFor(dataClass);
                JMenu dataMenu = new JMenu(JIPipeData.getNameOf(dataClass));
                dataMenu.setIcon(icon);

                for (JIPipeNodeInfo info : dataSources) {
                    if (info.isHidden())
                        continue;
                    JMenuItem addItem = new JMenuItem(info.getName(), JIPipe.getNodes().getIconFor(info));
                    addItem.setToolTipText(TooltipUtils.getAlgorithmTooltip(info));
                    addItem.addActionListener(e -> {
                        if (!JIPipeDesktopProjectWorkbench.canAddOrDeleteNodes(graphEditorUI.getDesktopWorkbench()))
                            return;
                        JIPipeGraphNode node = info.newInstance();
                        graphEditorUI.getHistoryJournal().snapshotBeforeAddNode(node, graphEditorUI.getCompartment());
                        algorithmGraph.insertNode(node, graphEditorUI.getCompartment());
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

        // Remove empty menus
        boolean changed;
        Set<JMenuItem> invisible = new HashSet<>();
        do {
            changed = false;
            for (JMenu item : menuTree.values()) {
                if (invisible.contains(item))
                    continue;
                boolean hasVisible = false;
                for (int i = 0; i < item.getItemCount(); i++) {
                    if (item.getItem(i).isVisible()) {
                        hasVisible = true;
                        break;
                    }
                }
                if (!hasVisible) {
                    item.setVisible(false);
                    invisible.add(item);
                    changed = true;
                }
            }
        }
        while (changed);

    }

    private void initializeContextActions() {
        getCanvasUI().setDragAndDropBehavior(new JIPipeCreateNodesFromDraggedDataDragAndDropBehavior());
        List<NodeUIContextAction> nodeSpecificContextActions = new ArrayList<>();
        if (JIPipeGeneralUIApplicationSettings.getInstance().isAddContextActionsToContextMenu()) {
            for (JIPipeNodeInfo info : JIPipe.getNodes().getRegisteredNodeInfos().values()) {
                for (Method method : info.getInstanceClass().getMethods()) {
                    JIPipeContextAction actionAnnotation = method.getAnnotation(JIPipeContextAction.class);
                    if (actionAnnotation == null)
                        continue;
                    if (!actionAnnotation.showInContextMenu())
                        continue;
                    SetJIPipeDocumentation documentationAnnotation = method.getAnnotation(SetJIPipeDocumentation.class);
                    if (documentationAnnotation == null) {
                        documentationAnnotation = new JIPipeDocumentation(method.getName(), "");
                    }
                    URL iconURL;
                    if (UIUtils.DARK_THEME && !StringUtils.isNullOrEmpty(actionAnnotation.iconDarkURL())) {
                        iconURL = actionAnnotation.resourceClass().getResource(actionAnnotation.iconDarkURL());
                    } else {
                        if (!StringUtils.isNullOrEmpty(actionAnnotation.iconURL())) {
                            iconURL = actionAnnotation.resourceClass().getResource(actionAnnotation.iconURL());
                        } else {
                            iconURL = UIUtils.getIconURLFromResources("actions/configure.png");
                        }
                    }
                    if (iconURL == null) {
                        iconURL = UIUtils.getIconURLFromResources("actions/configure.png");
                    }
                    Icon icon = new ImageIcon(iconURL);

                    NodeContextActionWrapperUIContextAction action = new NodeContextActionWrapperUIContextAction(info,
                            documentationAnnotation.name(),
                            DocumentationUtils.getDocumentationDescription(documentationAnnotation),
                            icon,
                            method);
                    nodeSpecificContextActions.add(action);
                }
            }
        }

        List<NodeUIContextAction> actions = new ArrayList<>(Arrays.asList(
                new AddNewNodeUIContextAction(),
                new AddNewParameterSetNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new SelectAllNodeUIContextAction(),
                new InvertSelectionNodeUIContextAction(),
                new AddBookmarkNodeUIContextAction(),
                new RemoveBookmarkNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new AlgorithmGraphCutNodeUIContextAction(),
                new AlgorithmGraphCopyNodeUIContextAction(),
                new AlgorithmGraphPasteNodeUIContextAction(),
                new AlgorithmGraphDuplicateNodeUIContextAction(),
                new AlgorithmGraphDuplicateWithInputConnectionsNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new RunAndShowResultsNodeUIContextAction(),
                new UpdateCacheNodeUIContextAction(),
                new OpenCacheBrowserInWindowUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new RunAndShowIntermediateResultsNodeUIContextAction(),
                new UpdateCacheShowIntermediateNodeUIContextAction(),
                new UpdateCacheOnlyPredecessorsNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new ClearCacheNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new ExportNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new IsolateNodesUIContextAction(),
                new JsonAlgorithmToGroupNodeUIContextAction(),
                new GroupNodeUIContextAction(),
                new CollapseIOInterfaceNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new SetNodeHotkeyContextAction(),
                NodeUIContextAction.SEPARATOR,
                new EnableNodeUIContextAction(),
                new DisableNodeUIContextAction(),
                new EnablePassThroughNodeUIContextAction(),
                new DisablePassThroughNodeUIContextAction(),
                new EnableSaveOutputsNodeUIContextAction(),
                new DisableSaveOutputsNodeUIContextAction(),
//                new EnableVirtualOutputsNodeUIContextAction(),
//                new DisableVirtualOutputNodeUIContextAction(),
                new DeleteNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new SendToForegroundUIContextAction(),
                new RaiseUIContextAction(),
                new LowerUIContextAction(),
                new SendToBackgroundUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new SelectAndMoveNodeHereNodeUIContextAction(),
                new LockNodeLocationSizeUIContextAction(),
                new UnlockNodeLocationSizeUIContextAction()
        ));

        // Custom entries (from registry)
        List<NodeUIContextAction> registeredEntries = JIPipe.getCustomMenus().getRegisteredContextMenuActions().stream()
                .filter(NodeUIContextAction::showInGraphCompartment)
                .sorted(Comparator.comparing(NodeUIContextAction::getName))
                .collect(Collectors.toList());
        if (!registeredEntries.isEmpty()) {
            actions.add(NodeUIContextAction.SEPARATOR);
            actions.addAll(registeredEntries);
        }

        // Node context actions
        if (!nodeSpecificContextActions.isEmpty()) {
            actions = new ArrayList<>(actions);
            actions.add(NodeUIContextAction.SEPARATOR);
            nodeSpecificContextActions.sort(Comparator.comparing(NodeUIContextAction::getName));
            actions.addAll(nodeSpecificContextActions);
        }

        getCanvasUI().setContextActions(actions);
    }

    private void initializeDefaultPanel() {
        defaultPanel = new JPanel(new BorderLayout());

        JSplitPane splitPane = new AutoResizeSplitPane(JSplitPane.VERTICAL_SPLIT, AutoResizeSplitPane.RATIO_1_TO_3);
        defaultPanel.add(splitPane, BorderLayout.CENTER);

        JIPipeDesktopGraphEditorMinimap minimap = new JIPipeDesktopGraphEditorMinimap(this);
        splitPane.setTopComponent(minimap);

        JIPipeDesktopTabPane bottomPanel = new JIPipeDesktopTabPane(false, JIPipeDesktopTabPane.TabPlacement.Right);

        JIPipeDesktopMarkdownReader markdownReader = new JIPipeDesktopMarkdownReader(false);
        markdownReader.setDocument(MarkdownText.fromPluginResource("documentation/algorithm-graph.md", new HashMap<>()));
        bottomPanel.addTab("Quick guide", UIUtils.getIcon32FromResources("actions/help.png"), markdownReader, JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        bottomPanel.addTab("Add nodes", UIUtils.getIcon32FromResources("actions/node-add.png"),
                new NodeToolBox(getDesktopWorkbench(), true), JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        bottomPanel.addTab("Templates", UIUtils.getIcon32FromResources("actions/star.png"),
                new NodeTemplateBox(getDesktopWorkbench(), true, getCanvasUI(), null), JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        bottomPanel.addTab("Bookmarks", UIUtils.getIcon32FromResources("actions/bookmarks.png"),
                new JIPipeDesktopBookmarkListPanel(getDesktopWorkbench(), getGraph(), this, null), JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        bottomPanel.addTab("History",
                UIUtils.getIcon32FromResources("actions/edit-undo-history.png"),
                new JIPipeDesktopHistoryJournalUI(getHistoryJournal()),
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        splitPane.setBottomComponent(bottomPanel);
    }

    @Override
    public void reloadMenuBar() {
        getMenuBar().removeAll();
        getAddableAlgorithms().clear();
        initializeAddNodesMenus(this, getMenuBar(), getAddableAlgorithms());
        initializeCommonActions();
    }

    @Override
    protected void updateSelection() {
        super.updateSelection();
        if (disableUpdateOnSelection)
            return;
        if (getSelection().isEmpty()) {
            setPropertyPanel(defaultPanel, true);
        } else if (getSelection().size() == 1) {
            JIPipeDesktopGraphNodeUI ui = getSelection().iterator().next();
            setPropertyPanel(new JIPipeDesktopPipelineSingleAlgorithmSelectionPanelUI(this, ui.getNode()), true);
        } else {
            setPropertyPanel(new JIPipeDesktopPipelineMultiAlgorithmSelectionPanelUI((JIPipeDesktopProjectWorkbench) getDesktopWorkbench(), getCanvasUI(),
                    getSelection().stream().map(JIPipeDesktopGraphNodeUI::getNode).collect(Collectors.toSet())), true);
        }
    }


    @Override
    public void onDefaultNodeUIActionRequested(JIPipeDesktopGraphNodeUI.DefaultNodeUIActionRequestedEvent event) {
        JIPipeGraphNode node = event.getUi().getNode();
        if (node instanceof JIPipeNodeGroup) {
            if (getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
                JIPipeDesktopNodeGroupUI.openGroupNodeGraph(getDesktopWorkbench(), (JIPipeNodeGroup) node, true);
            }
        } else if (node instanceof JIPipeProjectCompartmentOutput) {
            // Open the compartment
            if (!Objects.equals(getCompartment(), node.getCompartmentUUIDInParentGraph()) && getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
                // This is an input
                JIPipeDesktopProjectWorkbench projectWorkbench = (JIPipeDesktopProjectWorkbench) getDesktopWorkbench();
                UUID uuid = node.getCompartmentUUIDInParentGraph();
                JIPipeProjectCompartment projectCompartment = projectWorkbench.getProject().getCompartments().get(uuid);
                projectWorkbench.getOrOpenPipelineEditorTab(projectCompartment, true);
            } else if (getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
                JIPipeDesktopProjectWorkbench projectWorkbench = (JIPipeDesktopProjectWorkbench) getDesktopWorkbench();
                UUID uuid = node.getCompartmentUUIDInParentGraph();
                JIPipeProjectCompartment projectCompartment = projectWorkbench.getProject().getCompartments().get(uuid);
                JIPipeOutputDataSlot outputSlot = projectCompartment.getFirstOutputSlot();
                JIPipeGraph compartmentGraph = projectWorkbench.getProject().getCompartmentGraph();
                List<JIPipeProjectCompartment> targets = new ArrayList<>();
                for (JIPipeGraphEdge edge : compartmentGraph.getGraph().edgesOf(outputSlot)) {
                    JIPipeGraphNode edgeTarget = compartmentGraph.getGraph().getEdgeTarget(edge).getNode();
                    if (edgeTarget instanceof JIPipeProjectCompartment && edgeTarget != projectCompartment) {
                        targets.add((JIPipeProjectCompartment) edgeTarget);
                    }
                }
                if (targets.size() > 1) {
                    JPopupMenu popupMenu = new JPopupMenu();
                    for (JIPipeProjectCompartment target : targets) {
                        popupMenu.add(UIUtils.createMenuItem("Go to '" + target.getName() + "'", "Open the '" + target.getName() + "' compartment", UIUtils.getIconFromResources("actions/graph-compartment.png"), () -> {
                            projectWorkbench.getOrOpenPipelineEditorTab(target, true);
                        }));
                    }
                    popupMenu.show(event.getUi().getGraphCanvasUI(),
                            event.getUi().getGraphCanvasUI().getLastMousePosition().x,
                            event.getUi().getGraphCanvasUI().getLastMousePosition().y);
                } else if (targets.size() == 1) {
                    projectWorkbench.getOrOpenPipelineEditorTab(targets.get(0), true);
                }
            }
        }
    }

    /**
     * Listens to events of algorithms requesting some action
     *
     * @param event the event
     */
    @Override
    public void onNodeUIActionRequested(JIPipeDesktopGraphNodeUI.NodeUIActionRequestedEvent event) {
        if (event.getAction() instanceof JIPipeDesktopRunAndShowResultsAction) {
            disableUpdateOnSelection = true;
            selectOnly(event.getUi());
            JIPipeDesktopPipelineSingleAlgorithmSelectionPanelUI panel = new JIPipeDesktopPipelineSingleAlgorithmSelectionPanelUI(this,
                    event.getUi().getNode());
            setPropertyPanel(panel, true);
            panel.executeQuickRun(true,
                    false,
                    false,
                    true,
                    ((JIPipeDesktopRunAndShowResultsAction) event.getAction()).isStoreIntermediateResults(),
                    false);
            SwingUtilities.invokeLater(() -> disableUpdateOnSelection = false);
        } else if (event.getAction() instanceof JIPipeDesktopUpdateCacheAction) {
            disableUpdateOnSelection = true;
            selectOnly(event.getUi());
            JIPipeDesktopPipelineSingleAlgorithmSelectionPanelUI panel = new JIPipeDesktopPipelineSingleAlgorithmSelectionPanelUI(this,
                    event.getUi().getNode());
            setPropertyPanel(panel, true);
            panel.executeQuickRun(false,
                    true,
                    false,
                    false,
                    ((JIPipeDesktopUpdateCacheAction) event.getAction()).isStoreIntermediateResults(),
                    ((JIPipeDesktopUpdateCacheAction) event.getAction()).isOnlyPredecessors());
            SwingUtilities.invokeLater(() -> disableUpdateOnSelection = false);
        }
    }

    @Override
    public JIPipeNodeDatabaseRole getNodeDatabaseRole() {
        return JIPipeNodeDatabaseRole.PipelineNode;
    }
}
