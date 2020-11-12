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
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDefaultDocumentation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.grouping.NodeGroup;
import org.hkijena.jipipe.api.history.AddNodeGraphHistorySnapshot;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.extensions.settings.GeneralUISettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.components.MarkdownReader;
import org.hkijena.jipipe.ui.events.AlgorithmUIActionRequestedEvent;
import org.hkijena.jipipe.ui.events.DefaultAlgorithmUIActionRequestedEvent;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.*;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.clipboard.AlgorithmGraphCopyNodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.clipboard.AlgorithmGraphCutNodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.clipboard.AlgorithmGraphPasteNodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.settings.JIPipeMultiAlgorithmSelectionPanelUI;
import org.hkijena.jipipe.ui.grapheditor.settings.JIPipeSingleAlgorithmSelectionPanelUI;
import org.hkijena.jipipe.ui.grouping.JIPipeNodeGroupUI;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Editor for a project graph compartment
 */
public class JIPipeGraphCompartmentUI extends JIPipeGraphEditorUI {

    private JPanel defaultPanel;
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
        initializeDefaultPanel();
        setPropertyPanel(defaultPanel);

        // Set D&D and Copy&Paste behavior
        getCanvasUI().setDragAndDropBehavior(new JIPipeStandardDragAndDropBehavior());
        List<NodeUIContextAction> nodeSpecificContextActions = new ArrayList<>();
        if (GeneralUISettings.getInstance().isAddContextActionsToContextMenu()) {
            for (JIPipeNodeInfo info : JIPipe.getNodes().getRegisteredNodeInfos().values()) {
                for (Method method : info.getInstanceClass().getMethods()) {
                    JIPipeContextAction actionAnnotation = method.getAnnotation(JIPipeContextAction.class);
                    if (actionAnnotation == null)
                        continue;
                    JIPipeDocumentation documentationAnnotation = method.getAnnotation(JIPipeDocumentation.class);
                    if (documentationAnnotation == null) {
                        documentationAnnotation = new JIPipeDefaultDocumentation(method.getName(), "");
                    }
                    URL iconURL = null;
                    if (!StringUtils.isNullOrEmpty(actionAnnotation.iconURL())) {
                        iconURL = ResourceUtils.class.getResource(actionAnnotation.iconURL());
                    } else {
                        iconURL = UIUtils.getIconURLFromResources("actions/configure.png");
                    }
                    Icon icon = new ImageIcon(iconURL);

                    NodeContextActionWrapperUIContextAction action = new NodeContextActionWrapperUIContextAction(info, documentationAnnotation.name(), documentationAnnotation.description(), icon, method);
                    nodeSpecificContextActions.add(action);
                }
            }
        }

        List<NodeUIContextAction> actions = Arrays.asList(
                new SelectAllNodeUIContextAction(),
                new InvertSelectionNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new AlgorithmGraphCutNodeUIContextAction(),
                new AlgorithmGraphCopyNodeUIContextAction(),
                new AlgorithmGraphPasteNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new RunAndShowResultsNodeUIContextAction(),
                new UpdateCacheNodeUIContextAction(),
                new OpenCacheBrowserInWindowUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new ExportNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new IsolateNodesUIContextAction(),
                new JsonAlgorithmToGroupNodeUIContextAction(),
                new GroupNodeUIContextAction(),
                new CollapseIOInterfaceNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new EnableNodeUIContextAction(),
                new DisableNodeUIContextAction(),
                new EnablePassThroughNodeUIContextAction(),
                new DisablePassThroughNodeUIContextAction(),
                new EnableSaveOutputsNodeUIContextAction(),
                new DisableSaveOutputsNodeUIContextAction(),
                new DeleteNodeUIContextAction(),
                NodeUIContextAction.SEPARATOR,
                new SelectAndMoveNodeHereNodeUIContextAction()
        );

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

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.33);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.33);
            }
        });
        defaultPanel.add(splitPane, BorderLayout.CENTER);

        JIPipeGraphEditorMinimap minimap = new JIPipeGraphEditorMinimap(this);
        splitPane.setTopComponent(minimap);

        MarkdownReader markdownReader = new MarkdownReader(false);
        markdownReader.setDocument(MarkdownDocument.fromPluginResource("documentation/algorithm-graph.md"));
        splitPane.setBottomComponent(markdownReader);
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
            setPropertyPanel(defaultPanel);
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
                    updateCache, false, !updateCache, false);
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
        Set<JIPipeNodeInfo> algorithmsOfCategory = registryService.getNodeRegistry().getNodesOfCategory(category);
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
                JMenuItem addItem = new JMenuItem(info.getName(), JIPipe.getNodes().getIconFor(info));
                addItem.setToolTipText(TooltipUtils.getAlgorithmTooltip(info));
                addItem.addActionListener(e -> {
                    JIPipeGraphNode node = info.newInstance();
                    graphEditorUI.getCanvasUI().getGraphHistory().addSnapshotBefore(new AddNodeGraphHistorySnapshot(algorithmGraph, Collections.singleton(node)));
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
                        JIPipeGraphNode node = info.newInstance();
                        graphEditorUI.getCanvasUI().getGraphHistory().addSnapshotBefore(new AddNodeGraphHistorySnapshot(algorithmGraph, Collections.singleton(node)));
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
}
