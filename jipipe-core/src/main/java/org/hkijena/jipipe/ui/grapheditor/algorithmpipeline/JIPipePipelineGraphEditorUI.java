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

package org.hkijena.jipipe.ui.grapheditor.algorithmpipeline;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDefaultDocumentation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeCompartmentOutput;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.grouping.NodeGroup;
import org.hkijena.jipipe.api.history.JIPipeDedicatedGraphHistoryJournal;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.extensions.nodetemplate.NodeTemplateBox;
import org.hkijena.jipipe.extensions.nodetemplate.NodeTemplateMenu;
import org.hkijena.jipipe.extensions.nodetoolboxtool.NodeToolBox;
import org.hkijena.jipipe.extensions.settings.GeneralUISettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.bookmarks.BookmarkListPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.grapheditor.algorithmpipeline.dragdrop.JIPipeCreateNodesFromDraggedDataDragAndDropBehavior;
import org.hkijena.jipipe.ui.grapheditor.algorithmpipeline.actions.RunAndShowResultsAction;
import org.hkijena.jipipe.ui.grapheditor.algorithmpipeline.actions.UpdateCacheAction;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorMinimap;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorUI;
import org.hkijena.jipipe.ui.grapheditor.general.contextmenu.*;
import org.hkijena.jipipe.ui.grapheditor.compartments.contextmenu.clipboard.clipboard.AlgorithmGraphCopyNodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.compartments.contextmenu.clipboard.clipboard.AlgorithmGraphCutNodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.compartments.contextmenu.clipboard.clipboard.AlgorithmGraphDuplicateNodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.compartments.contextmenu.clipboard.clipboard.AlgorithmGraphPasteNodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeNodeUI;
import org.hkijena.jipipe.ui.grapheditor.algorithmpipeline.properties.JIPipePipelineMultiAlgorithmSelectionPanelUI;
import org.hkijena.jipipe.ui.grapheditor.algorithmpipeline.properties.JIPipePipelineSingleAlgorithmSelectionPanelUI;
import org.hkijena.jipipe.ui.grouping.JIPipeNodeGroupUI;
import org.hkijena.jipipe.ui.history.HistoryJournalUI;
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
public class JIPipePipelineGraphEditorUI extends JIPipeGraphEditorUI {

    private JPanel defaultPanel;
    private boolean disableUpdateOnSelection = false;

    /**
     * Creates a project graph compartment editor
     *
     * @param workbenchUI    The workbench
     * @param algorithmGraph The graph
     * @param compartment    The compartment
     */
    public JIPipePipelineGraphEditorUI(JIPipeWorkbench workbenchUI, JIPipeGraph algorithmGraph, UUID compartment) {
        super(workbenchUI, algorithmGraph, compartment, algorithmGraph.getProject() != null ? algorithmGraph.getProject().getHistoryJournal() : new JIPipeDedicatedGraphHistoryJournal(algorithmGraph));
        initializeDefaultPanel();
        setPropertyPanel(defaultPanel);

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
    public static void initializeAddNodesMenus(JIPipeGraphEditorUI graphEditorUI, JMenuBar menuBar, Set<JIPipeNodeInfo> addedAlgorithms) {

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
        menuBar.add(new NodeTemplateMenu(graphEditorUI.getWorkbench(), graphEditorUI));
    }

    /**
     * Initializes a menu for one algorithm category
     *
     * @param graphEditorUI   the graph editor
     * @param menu            The menu
     * @param category        The algorithm category
     * @param addedAlgorithms added algorithm types are added to this list
     */
    public static void initializeMenuForCategory(JIPipeGraphEditorUI graphEditorUI, JMenu menu, JIPipeNodeTypeCategory category, Set<JIPipeNodeInfo> addedAlgorithms) {
        JIPipeGraph algorithmGraph = graphEditorUI.getAlgorithmGraph();
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
                    if (!JIPipeProjectWorkbench.canAddOrDeleteNodes(graphEditorUI.getWorkbench()))
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
    public static void initializeAddDataSourceMenu(JIPipeGraphEditorUI graphEditorUI, JMenu menu, Set<JIPipeNodeInfo> addedAlgorithms) {
        JIPipeGraph algorithmGraph = graphEditorUI.getAlgorithmGraph();
        JIPipe registryService = JIPipe.getInstance();
        Map<String, Set<Class<? extends JIPipeData>>> dataTypesByMenuPaths = JIPipe.getDataTypes().getDataTypesByMenuPaths();
        Map<String, JMenu> menuTree = UIUtils.createMenuTree(menu, dataTypesByMenuPaths.keySet());

        for (Map.Entry<String, Set<Class<? extends JIPipeData>>> entry : dataTypesByMenuPaths.entrySet()) {
            JMenu subMenu = menuTree.get(entry.getKey());
            for (Class<? extends JIPipeData> dataClass : JIPipeData.getSortedList(entry.getValue())) {
                if (JIPipeData.isHidden(dataClass))
                    continue;
                Set<JIPipeNodeInfo> dataSources = registryService.getNodeRegistry().getDataSourcesFor(dataClass);
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
                        if (!JIPipeProjectWorkbench.canAddOrDeleteNodes(graphEditorUI.getWorkbench()))
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
        if (GeneralUISettings.getInstance().isAddContextActionsToContextMenu()) {
            for (JIPipeNodeInfo info : JIPipe.getNodes().getRegisteredNodeInfos().values()) {
                for (Method method : info.getInstanceClass().getMethods()) {
                    JIPipeContextAction actionAnnotation = method.getAnnotation(JIPipeContextAction.class);
                    if (actionAnnotation == null)
                        continue;
                    if (!actionAnnotation.showInContextMenu())
                        continue;
                    JIPipeDocumentation documentationAnnotation = method.getAnnotation(JIPipeDocumentation.class);
                    if (documentationAnnotation == null) {
                        documentationAnnotation = new JIPipeDefaultDocumentation(method.getName(), "");
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
                new SelectAllNodeUIContextAction(),
                new InvertSelectionNodeUIContextAction(),
                new AddBookmarkNodeUIContextAction(),
                new RemoveBookmarkNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new AlgorithmGraphCutNodeUIContextAction(),
                new AlgorithmGraphCopyNodeUIContextAction(),
                new AlgorithmGraphPasteNodeUIContextAction(),
                new AlgorithmGraphDuplicateNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new RunAndShowResultsNodeUIContextAction(),
                new UpdateCacheNodeUIContextAction(),
                new OpenCacheBrowserInWindowUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new RunAndShowIntermediateResultsNodeUIContextAction(),
                new UpdateCacheShowIntermediateNodeUIContextAction(),
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
                new EnableVirtualOutputsNodeUIContextAction(),
                new DisableVirtualOutputNodeUIContextAction(),
                new DeleteNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new SelectAndMoveNodeHereNodeUIContextAction()
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

        JIPipeGraphEditorMinimap minimap = new JIPipeGraphEditorMinimap(this);
        splitPane.setTopComponent(minimap);

        DocumentTabPane bottomPanel = new DocumentTabPane(false);

        MarkdownReader markdownReader = new MarkdownReader(false);
        markdownReader.setDocument(MarkdownDocument.fromPluginResource("documentation/algorithm-graph.md", new HashMap<>()));
        bottomPanel.addTab("Quick guide", UIUtils.getIconFromResources("actions/help.png"), markdownReader, DocumentTabPane.CloseMode.withoutCloseButton);

        bottomPanel.addTab("Available nodes", UIUtils.getIconFromResources("actions/configuration.png"),
                new NodeToolBox(getWorkbench(), true), DocumentTabPane.CloseMode.withoutCloseButton);

        bottomPanel.addTab("Node templates", UIUtils.getIconFromResources("actions/favorite.png"),
                new NodeTemplateBox(getWorkbench(), true), DocumentTabPane.CloseMode.withoutCloseButton);

        bottomPanel.addTab("Bookmarks", UIUtils.getIconFromResources("actions/bookmarks.png"),
                new BookmarkListPanel(getWorkbench(), getAlgorithmGraph(), this), DocumentTabPane.CloseMode.withoutCloseButton);

        bottomPanel.addTab("Journal",
                UIUtils.getIconFromResources("actions/edit-undo-history.png"),
                new HistoryJournalUI(getHistoryJournal()),
                DocumentTabPane.CloseMode.withoutCloseButton);

        splitPane.setBottomComponent(bottomPanel);
    }

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
            setPropertyPanel(defaultPanel);
        } else if (getSelection().size() == 1) {
            JIPipeNodeUI ui = getSelection().iterator().next();
            setPropertyPanel(new JIPipePipelineSingleAlgorithmSelectionPanelUI(this, ui.getNode()));
        } else {
            setPropertyPanel(new JIPipePipelineMultiAlgorithmSelectionPanelUI((JIPipeProjectWorkbench) getWorkbench(), getCanvasUI(),
                    getSelection().stream().map(JIPipeNodeUI::getNode).collect(Collectors.toSet())));
        }
    }

    @Subscribe
    public void onDefaultActionRequested(JIPipeGraphCanvasUI.DefaultAlgorithmUIActionRequestedEvent event) {
        if (event.getUi().getNode() instanceof NodeGroup) {
            if (event.getUi().getNode() instanceof NodeGroup) {
                if (getWorkbench() instanceof JIPipeProjectWorkbench) {
                    JIPipeNodeGroupUI.openGroupNodeGraph(getWorkbench(), (NodeGroup) event.getUi().getNode(), true);
                }
            }
        }
        else if(event.getUi().getNode() instanceof JIPipeCompartmentOutput) {
            // Open the compartment
            if(!Objects.equals(getCompartment(), event.getUi().getNode().getCompartmentUUIDInParentGraph()) && getWorkbench() instanceof JIPipeProjectWorkbench) {
                JIPipeProjectWorkbench projectWorkbench = (JIPipeProjectWorkbench) getWorkbench();
                UUID uuid = event.getUi().getNode().getCompartmentUUIDInParentGraph();
                JIPipeProjectCompartment projectCompartment = projectWorkbench.getProject().getCompartments().get(uuid);
                projectWorkbench.getOrOpenPipelineEditorTab(projectCompartment, true);
            }
        }
    }

    /**
     * Listens to events of algorithms requesting some action
     *
     * @param event the event
     */
    @Subscribe
    public void onAlgorithmActionRequested(JIPipeGraphCanvasUI.NodeUIActionRequestedEvent event) {
        if (event.getAction() instanceof RunAndShowResultsAction) {
            disableUpdateOnSelection = true;
            selectOnly(event.getUi());
            JIPipePipelineSingleAlgorithmSelectionPanelUI panel = new JIPipePipelineSingleAlgorithmSelectionPanelUI(this,
                    event.getUi().getNode());
            setPropertyPanel(panel);
            panel.executeQuickRun(true,
                    false,
                    false,
                    true,
                    ((RunAndShowResultsAction) event.getAction()).isStoreIntermediateResults(),
                    false);
            SwingUtilities.invokeLater(() -> disableUpdateOnSelection = false);
        } else if (event.getAction() instanceof UpdateCacheAction) {
            disableUpdateOnSelection = true;
            selectOnly(event.getUi());
            JIPipePipelineSingleAlgorithmSelectionPanelUI panel = new JIPipePipelineSingleAlgorithmSelectionPanelUI(this,
                    event.getUi().getNode());
            setPropertyPanel(panel);
            panel.executeQuickRun(false,
                    true,
                    false,
                    false,
                    ((UpdateCacheAction) event.getAction()).isStoreIntermediateResults(),
                    false);
            SwingUtilities.invokeLater(() -> disableUpdateOnSelection = false);
        }
    }
}
