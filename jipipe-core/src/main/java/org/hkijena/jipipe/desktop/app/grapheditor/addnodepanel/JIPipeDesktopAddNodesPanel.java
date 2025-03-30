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

package org.hkijena.jipipe.desktop.app.grapheditor.addnodepanel;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.database.*;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.AbstractJIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.JIPipeDesktopCompartmentsGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.JIPipeDesktopPipelineGraphEditorUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormHelpPanel;
import org.hkijena.jipipe.desktop.commons.components.layouts.JIPipeDesktopWrapLayout;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.plugins.nodetemplate.NodeTemplatePopupMenu;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.JIPipeGraphEditorUIApplicationSettings;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;
import org.jdesktop.swingx.JXTextField;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * New and improved node tool box
 */
public class JIPipeDesktopAddNodesPanel extends JIPipeDesktopWorkbenchPanel {

    private final JToolBar toolBar = new JToolBar();
    private final JIPipeNodeDatabase database;
    private final JIPipeRunnableQueue queue = new JIPipeRunnableQueue("Node toolbox");
    private final JIPipeGraphEditorUIApplicationSettings settings;
    private final boolean isCompartmentsEditor;
    private final JPanel mainCategoriesPanel = new JPanel();
    private final JPanel subCategoryPathPanel = new JPanel();
    private final JPanel subCategorySelectionPanel = new JPanel();
    private final AbstractJIPipeDesktopGraphEditorUI graphEditorUI;
    private final List<MainCategoryFilter> mainCategoryFilters = new ArrayList<>();
    private final JCheckBoxMenuItem showNodeDescriptionToggle = new JCheckBoxMenuItem("Show node descriptions");
    private final JCheckBoxMenuItem showHierarchySelectionToggle = new JCheckBoxMenuItem("Show category browser");
    private final DefaultDirectedGraph<String, DefaultEdge> mainCategoryHierarchy = new DefaultDirectedGraph<>(DefaultEdge.class);
    private JList<JIPipeNodeDatabaseEntry> algorithmList;
    private JIPipeDesktopSearchTextField searchField;
    private JScrollPane scrollPane;
    private String currentHierarchyVertex;

    public JIPipeDesktopAddNodesPanel(JIPipeDesktopWorkbench workbench, AbstractJIPipeDesktopGraphEditorUI graphEditorUI) {
        super(workbench);
        this.database = workbench instanceof JIPipeDesktopProjectWorkbench ?
                ((JIPipeDesktopProjectWorkbench) workbench).getNodeDatabase() : JIPipeNodeDatabase.getInstance();
        this.graphEditorUI = graphEditorUI;
        this.settings = JIPipeGraphEditorUIApplicationSettings.getInstance();
        this.isCompartmentsEditor = graphEditorUI instanceof JIPipeDesktopCompartmentsGraphEditorUI;

        initializeMainCategoryFilters();
        initialize();

        reloadAlgorithmList();
        updateSubCategoryPanels();
    }

    private static void registerMenuComponentsToHierarchyGraph(DefaultDirectedGraph<String, DefaultEdge> graph, JIPipeNodeTypeCategory category) {
        Set<JIPipeNodeInfo> algorithmsOfCategory = JIPipe.getNodes().getNodesOfCategory(category, true);
        for (JIPipeNodeInfo nodeInfo : algorithmsOfCategory) {

            // Main category
            {
                List<String> menuComponents = new ArrayList<>();
                menuComponents.add(nodeInfo.getCategory().getName());
                String menuPath = StringUtils.nullToEmpty(nodeInfo.getMenuPath()).trim();
                if (!StringUtils.isNullOrEmpty(menuPath)) {
                    menuComponents.addAll(Arrays.asList(menuPath.split("\n")));
                }
                registerMenuComponentsToHierarchyGraph(graph, menuComponents);
            }

            // Alias categories
            for (JIPipeNodeMenuLocation alias : nodeInfo.getAliases()) {
                List<String> menuComponents = new ArrayList<>();
                menuComponents.add(alias.getCategory().getName());
                String menuPath = StringUtils.nullToEmpty(alias.getMenuPath()).trim();
                if (!StringUtils.isNullOrEmpty(menuPath)) {
                    menuComponents.addAll(Arrays.asList(menuPath.split("\n")));
                }
                registerMenuComponentsToHierarchyGraph(graph, menuComponents);
            }


        }
    }

    private static void registerMenuComponentsToHierarchyGraph(DefaultDirectedGraph<String, DefaultEdge> graph, List<String> menuComponents) {
        String next = null;
        for (int end = menuComponents.size() - 1; end >= 0; end--) {
            String locationId = String.join("\n", menuComponents.subList(0, end + 1));
            if (!graph.containsVertex(locationId)) {
                graph.addVertex(locationId);
            }
            if (next != null) {
                graph.addEdge(locationId, next);
            }
            next = locationId;
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
    public static void initializeMenuForCategory(AbstractJIPipeDesktopGraphEditorUI graphEditorUI, JPopupMenu menu, JIPipeNodeTypeCategory category, Set<JIPipeNodeInfo> addedAlgorithms) {
        JIPipeGraph algorithmGraph = graphEditorUI.getGraph();
        JIPipe registryService = JIPipe.getInstance();
        Set<JIPipeNodeInfo> algorithmsOfCategory = registryService.getNodeRegistry().getNodesOfCategory(category, true);
        if (algorithmsOfCategory.isEmpty()) {
            menu.setVisible(false);
            return;
        }

        Map<String, Set<JIPipeNodeInfo>> byMenuPath = JIPipeNodeInfo.groupByMenuPaths(category, algorithmsOfCategory);
        Map<String, JComponent> menuTree = UIUtils.createMenuTree(menu, byMenuPath.keySet());

        for (Map.Entry<String, Set<JIPipeNodeInfo>> entry : byMenuPath.entrySet()) {
            JComponent subMenu = menuTree.get(entry.getKey());

//            subMenu.addMouseListener(new MouseAdapter() {
//                @Override
//                public void mouseClicked(MouseEvent e) {
//                    System.out.println("clicked " + entry.getKey());
//                }
//            });

            for (JIPipeNodeInfo info : JIPipeNodeInfo.getSortedList(entry.getValue())) {
                if (info.isHidden())
                    continue;
                if (UIUtils.getMenuItemCount(subMenu) >= 30) {
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
    public static void initializeAddDataSourceMenu(AbstractJIPipeDesktopGraphEditorUI graphEditorUI, JPopupMenu menu, Set<JIPipeNodeInfo> addedAlgorithms) {
        JIPipeGraph algorithmGraph = graphEditorUI.getGraph();
        JIPipe registryService = JIPipe.getInstance();
        Map<String, Set<Class<? extends JIPipeData>>> dataTypesByMenuPaths = JIPipe.getDataTypes().getDataTypesByMenuPaths();
        Map<String, JComponent> menuTree = UIUtils.createMenuTree(menu, dataTypesByMenuPaths.keySet());

        for (Map.Entry<String, Set<Class<? extends JIPipeData>>> entry : dataTypesByMenuPaths.entrySet()) {
            JComponent subMenu = menuTree.get(entry.getKey());
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
        Set<JComponent> invisible = new HashSet<>();
        do {
            changed = false;
            for (JComponent item : menuTree.values()) {
                if (invisible.contains(item)) {
                    continue;
                }
                boolean hasVisible = false;
                for (int i = 0; i < UIUtils.getMenuItemCount(item); i++) {
                    if (UIUtils.getMenuItem(item, i).isVisible()) {
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

    public Set<String> getPinnedNodeDatabaseEntries() {
        return new HashSet<>(settings.getNodeSearchSettings().getPinnedNodes());
    }

    private void initializeMainCategoryFilters() {

        // Compartments have no categories
        if (isCompartmentsEditor) {
            return;
        }

        List<JIPipeNodeTypeCategory> categories = JIPipe.getNodes().getRegisteredCategories().values().stream().sorted(Comparator.comparing(JIPipeNodeTypeCategory::getUIOrder)).collect(Collectors.toList());
        categories.add(new TemplatesDummyNodeTypeCategory());
        categories.add(new PinnedDummyNodeTypeCategory());

        // Inject special categories here
        for (JIPipeNodeTypeCategory category : categories) {
            if (category.isVisibleInPipeline()) {
                JToggleButton categoryButton = new JToggleButton(category.getName(), category.getIcon());
                categoryButton.setBorder(UIUtils.createControlBorder());
                categoryButton.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));

                // Add the old menu as popup menu to the categories
                if (category instanceof TemplatesDummyNodeTypeCategory) {
                    NodeTemplatePopupMenu popupMenu = new NodeTemplatePopupMenu(getDesktopWorkbench(), graphEditorUI);
                    UIUtils.addReloadableRightClickPopupMenuToButton(categoryButton, popupMenu, () -> {
                    });
                } else if (category instanceof PinnedDummyNodeTypeCategory) {
                    JPopupMenu popupMenu = new JPopupMenu();
                    UIUtils.addReloadableRightClickPopupMenuToButton(categoryButton, popupMenu, () -> {
                        popupMenu.removeAll();
                        initializeMenuForPinned(popupMenu);
                    });
                } else {
                    JPopupMenu popupMenu = UIUtils.addRightClickPopupMenuToButton(categoryButton);
                    if (category instanceof DataSourceNodeTypeCategory) {

                        initializeAddDataSourceMenu(graphEditorUI, popupMenu, new HashSet<>());
                    } else {
                        registerMenuComponentsToHierarchyGraph(mainCategoryHierarchy, category);
                        initializeMenuForCategory(graphEditorUI, popupMenu, category, new HashSet<>());
                    }
                }

                MainCategoryFilter currentFilter = new MainCategoryFilter(category, categoryButton);
                categoryButton.addActionListener(e -> {
                    if (categoryButton.isSelected()) {
                        for (MainCategoryFilter filter : mainCategoryFilters) {
                            if (filter.isSelected() && filter != currentFilter) {
                                filter.toggleButton.setSelected(false);
                            }
                        }
                    }
                    resetSubCategory();
                    reloadAlgorithmList();
                });
                mainCategoryFilters.add(currentFilter);
            }
        }
    }

    private void initializeMenuForPinned(JPopupMenu popupMenu) {
        Set<String> pinnedNodeDatabaseEntries = getPinnedNodeDatabaseEntries();
        for (JIPipeNodeDatabaseEntry entry : database.getLegacySearch().query("", isCompartmentsEditor ? JIPipeNodeDatabasePipelineVisibility.Compartments : JIPipeNodeDatabasePipelineVisibility.Pipeline,
                false, true, new HashSet<>(settings.getNodeSearchSettings().getPinnedNodes()))) {
            if (pinnedNodeDatabaseEntries.contains(entry.getId())) {
                if (entry instanceof CreateNewNodeByInfoDatabaseEntry) {
                    popupMenu.add(UIUtils.createMenuItem(entry.getName(), TooltipUtils.getAlgorithmTooltip(((CreateNewNodeByInfoDatabaseEntry) entry).getNodeInfo(), true),
                            entry.getIcon(), () -> insertAtCursor(entry)));
                } else if (entry instanceof CreateNewNodeByInfoAliasDatabaseEntry) {
                    popupMenu.add(UIUtils.createMenuItem(entry.getName(), TooltipUtils.getAlgorithmTooltip(((CreateNewNodeByInfoAliasDatabaseEntry) entry).getNodeInfo(), true),
                            entry.getIcon(), () -> insertAtCursor(entry)));
                } else if (entry instanceof CreateNewNodesByTemplateDatabaseEntry) {
                    popupMenu.add(UIUtils.createMenuItem(entry.getName(), TooltipUtils.getAlgorithmTooltip(((CreateNewNodesByTemplateDatabaseEntry) entry).getTemplate(), true),
                            entry.getIcon(), () -> insertAtCursor(entry)));
                } else if (entry instanceof CreateNewNodeByExampleDatabaseEntry) {
                    popupMenu.add(UIUtils.createMenuItem(entry.getName(), TooltipUtils.getAlgorithmTooltip(((CreateNewNodeByExampleDatabaseEntry) entry).getExample().getNodeInfo(), true),
                            entry.getIcon(), () -> insertAtCursor(entry)));
                }
            }
        }

//        popupMenu.removeAll();
    }

    private void resetSubCategory() {
        MainCategoryFilter selectedMainCategory = getSelectedMainCategory();
        if (selectedMainCategory != null) {
            if (selectedMainCategory.getCategoryId().contains("dummy")) {
                currentHierarchyVertex = null;
            } else {
                currentHierarchyVertex = selectedMainCategory.getCategory().getName();
            }
        } else {
            currentHierarchyVertex = null;
        }
        updateSubCategoryPanels();
    }

    private void reloadAlgorithmList() {
        queue.cancelAll();
        queue.enqueue(new ReloadListRun(this));
    }

    public JToolBar getToolBar() {
        return toolBar;
    }

    private void initialize() {
        setLayout(new GridBagLayout());

        initializeToolbar();
        if (!isCompartmentsEditor) {
            initializeMainCategoryPanel();
            initializeSubCategoryPanel();
        }
        initializeAlgorithmList();
        initializeAlgorithmListContextMenu();
    }

    private void updateSubCategoryPanels() {
        if (isCompartmentsEditor) {
            return;
        }
        subCategorySelectionPanel.removeAll();
        subCategoryPathPanel.removeAll();

        MainCategoryFilter selectedMainCategory = getSelectedMainCategory();
        if (selectedMainCategory != null && currentHierarchyVertex != null && showHierarchySelectionToggle.isSelected() && mainCategoryHierarchy.containsVertex(currentHierarchyVertex)) {
            subCategoryPathPanel.setVisible(true);
            subCategorySelectionPanel.setVisible(true);


            // Update current hierarchy
            String[] currentHierarchyPathComponents = currentHierarchyVertex.split("\n");
            subCategoryPathPanel.removeAll();
            for (int i = 0; i < currentHierarchyPathComponents.length; i++) {
                String component = currentHierarchyPathComponents[i];
                String fullComponent = String.join("\n", Arrays.asList(currentHierarchyPathComponents).subList(0, i + 1));
                JButton navigateButton = new JButton(component);
                navigateButton.setBorder(null);
                navigateButton.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
                navigateButton.addActionListener(e -> {
                    if (mainCategoryHierarchy.containsVertex(fullComponent)) {
                        currentHierarchyVertex = fullComponent;
                        updateSubCategoryPanels();
                        reloadAlgorithmList();
                    }
                });
                subCategoryPathPanel.add(navigateButton);

                JButton listButton = new JButton(UIUtils.getIcon8FromResources("caret-right.png"));
                listButton.setBorder(UIUtils.createEmptyBorder(3));
                subCategoryPathPanel.add(listButton);
                JPopupMenu goToMenu = UIUtils.addPopupMenuToButton(listButton);
                List<String> successors = Graphs.successorListOf(mainCategoryHierarchy, fullComponent).stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
                for (String successor : successors) {
                    String[] successorPathComponents = successor.split("\n");
                    goToMenu.add(UIUtils.createMenuItem(successorPathComponents[successorPathComponents.length - 1], "Go to sub category",
                            UIUtils.getIconFromResources("actions/tag.png"), () -> {
                                if (mainCategoryHierarchy.containsVertex(successor)) {
                                    currentHierarchyVertex = successor;
                                    updateSubCategoryPanels();
                                    reloadAlgorithmList();
                                }
                            }));
                }

            }

            // Add selection
            List<String> currentSuccessors = Graphs.successorListOf(mainCategoryHierarchy, currentHierarchyVertex).stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
            for (String successor : currentSuccessors) {
                String[] successorPathComponents = successor.split("\n");
                JButton button = new JButton(successorPathComponents[successorPathComponents.length - 1]);
                button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIUtils.getControlBorderColor()),
                        UIUtils.createEmptyBorder(1)));
                button.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
                button.addActionListener(e -> {
                    if (mainCategoryHierarchy.containsVertex(successor)) {
                        currentHierarchyVertex = successor;
                        updateSubCategoryPanels();
                        reloadAlgorithmList();
                    }
                });
                subCategorySelectionPanel.add(button);
            }

        } else {
            subCategoryPathPanel.setVisible(false);
            subCategorySelectionPanel.setVisible(false);
        }
        revalidate();
        repaint();
    }

    private void initializeSubCategoryPanel() {

        subCategoryPathPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, UIUtils.getControlBorderColor()));
        subCategorySelectionPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtils.getControlBorderColor()));

        subCategoryPathPanel.setLayout(new JIPipeDesktopWrapLayout(FlowLayout.LEFT));
        add(subCategoryPathPanel, new GridBagConstraints(0,
                2,
                1,
                1,
                1,
                0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(2, 8, 2, 8),
                0,
                0));
        subCategorySelectionPanel.setLayout(new JIPipeDesktopWrapLayout(FlowLayout.LEFT));
        add(subCategorySelectionPanel, new GridBagConstraints(0,
                3,
                1,
                1,
                1,
                0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(2, 8, 8, 8),
                0,
                0));
    }

    private void initializeAlgorithmListContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        UIUtils.addRightClickPopupMenuToJList(algorithmList, contextMenu, () -> {
            contextMenu.removeAll();

            Set<String> pinnedNodeDatabaseEntries = getPinnedNodeDatabaseEntries();
            List<JIPipeNodeDatabaseEntry> selectedValues = algorithmList.getSelectedValuesList();
            if (!selectedValues.isEmpty()) {
                contextMenu.add(UIUtils.createMenuItem("Insert at cursor", "Inserts the node at the cursor", UIUtils.getIconFromResources("actions/add.png"), () -> {
                    insertAtCursor(selectedValues.get(0));
                }));
                UIUtils.addSeparatorIfNeeded(contextMenu);
                if (selectedValues.stream().anyMatch(entry -> !pinnedNodeDatabaseEntries.contains(entry.getId()))) {
                    contextMenu.add(UIUtils.createMenuItem("Pin", "Adds the item to the list of pinned items", UIUtils.getIconFromResources("actions/window-pin.png"), () -> {
                        pinNodes(selectedValues);
                    }));
                }
                if (selectedValues.stream().anyMatch(entry -> pinnedNodeDatabaseEntries.contains(entry.getId()))) {
                    contextMenu.add(UIUtils.createMenuItem("Unpin", "Removes the item from the list of pinned items", UIUtils.getIconFromResources("actions/window-unpin.png"), () -> {
                        unpinNodes(selectedValues);
                    }));
                }
                contextMenu.addSeparator();
                contextMenu.add(UIUtils.createMenuItem("Show documentation", "Displays the documentation of this node", UIUtils.getIconFromResources("actions/help.png"), () -> {
                    showDocumentation(selectedValues.get(0));
                }));
            }
        });
    }

    private void showDocumentation(JIPipeNodeDatabaseEntry entry) {
        // Create/open the documentation panel
        JIPipeDesktopFormHelpPanel helpPanel = graphEditorUI.getDockPanel().getPanelComponent(JIPipeDesktopPipelineGraphEditorUI.DOCK_NODE_CONTEXT_HELP, JIPipeDesktopFormHelpPanel.class);
        if (helpPanel == null) {
            helpPanel = new JIPipeDesktopFormHelpPanel();
            graphEditorUI.getDockPanel().addDockPanel(JIPipeDesktopPipelineGraphEditorUI.DOCK_NODE_CONTEXT_HELP,
                    "Documentation",
                    UIUtils.getIcon32FromResources("actions/help-question.png"),
                    JIPipeDesktopDockPanel.PanelLocation.BottomRight,
                    true,
                    0, helpPanel);
        }
        graphEditorUI.getDockPanel().activatePanel(JIPipeDesktopPipelineGraphEditorUI.DOCK_NODE_CONTEXT_HELP, false);
        if (entry instanceof CreateNewNodeByInfoDatabaseEntry) {
            helpPanel.showContent(entry.getName(), TooltipUtils.getAlgorithmDocumentation(((CreateNewNodeByInfoDatabaseEntry) entry).getNodeInfo()));
        } else if (entry instanceof CreateNewNodeByInfoAliasDatabaseEntry) {
            helpPanel.showContent(entry.getName(), TooltipUtils.getAlgorithmDocumentation(((CreateNewNodeByInfoAliasDatabaseEntry) entry).getNodeInfo()));
        } else if (entry instanceof CreateNewNodeByExampleDatabaseEntry) {
            helpPanel.showContent(entry.getName(), TooltipUtils.getAlgorithmDocumentation(((CreateNewNodeByExampleDatabaseEntry) entry).getExample().getNodeInfo()));
        } else if (entry instanceof CreateNewCompartmentNodeDatabaseEntry) {
            helpPanel.showContent("Graph compartment", TooltipUtils.getAlgorithmDocumentation(JIPipe.getNodes().getInfoById("jipipe:project-compartment")));
        } else {
            helpPanel.showContent(entry.getName(), new MarkdownText("# " + entry.getName() + "\n\n" + entry.getDescription().getBody()));
        }
    }

    private void pinNodes(List<JIPipeNodeDatabaseEntry> selectedValues) {
        for (JIPipeNodeDatabaseEntry entry : selectedValues) {
            settings.getNodeSearchSettings().getPinnedNodes().add(entry.getId());
        }
        settings.getNodeSearchSettings().getPinnedNodes().makeUnique();
        JIPipe.getSettings().save();
        reloadAlgorithmList();
    }

    private void unpinNodes(List<JIPipeNodeDatabaseEntry> selectedValues) {
        for (JIPipeNodeDatabaseEntry entry : selectedValues) {
            settings.getNodeSearchSettings().getPinnedNodes().add(entry.getId());
        }
        settings.getNodeSearchSettings().getPinnedNodes().makeUnique();
        JIPipe.getSettings().save();
        reloadAlgorithmList();
    }

    private void initializeMainCategoryPanel() {
        mainCategoriesPanel.setLayout(new JIPipeDesktopWrapLayout(FlowLayout.LEFT));
        for (MainCategoryFilter filter : mainCategoryFilters) {
            mainCategoriesPanel.add(filter.toggleButton);
        }
        add(mainCategoriesPanel, new GridBagConstraints(0,
                1,
                1,
                1,
                1,
                0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(8, 8, 8, 8),
                0,
                0));
    }

    private void initializeToolbar() {
        add(toolBar, new GridBagConstraints(0,
                0,
                1,
                1,
                1,
                0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0),
                0,
                0));

        searchField = new JIPipeDesktopSearchTextField();
        searchField.addActionListener(e -> reloadAlgorithmList());
        searchField.getTextField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    algorithmList.requestFocus();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    insertFirstAtCursor();
                    graphEditorUI.getCanvasUI().requestFocus();
                }
            }
        });
        toolBar.add(searchField);

        JButton menuButton = new JButton(UIUtils.getIconFromResources("actions/hamburger-menu.png"));
        UIUtils.makeButtonFlat25x25(menuButton);
        JPopupMenu menu = UIUtils.addPopupMenuToButton(menuButton);
        initializeToolbarMenu(menu);
        toolBar.add(menuButton);
    }

    private void initializeToolbarMenu(JPopupMenu menu) {
        showHierarchySelectionToggle.setToolTipText("Show a panel where the node hierarchy can be browsed");
        showHierarchySelectionToggle.setSelected(settings.getNodeSearchSettings().isShowHierarchySelection());
        showHierarchySelectionToggle.addActionListener(e -> {
            updateSubCategoryPanels();
            settings.getNodeSearchSettings().setShowHierarchySelection(showHierarchySelectionToggle.isSelected());
            JIPipe.getSettings().save();
        });
        menu.add(showHierarchySelectionToggle);

        showNodeDescriptionToggle.setToolTipText("Show node descriptions in the search results, which will take up a bit more vertical space per item");
        showNodeDescriptionToggle.setSelected(settings.getNodeSearchSettings().isShowDescriptions());
        showNodeDescriptionToggle.addActionListener(e -> {
            reloadAlgorithmList();
            settings.getNodeSearchSettings().setShowDescriptions(showNodeDescriptionToggle.isSelected());
            JIPipe.getSettings().save();
        });
        menu.add(showNodeDescriptionToggle);
    }

    public boolean isShowDescriptions() {
        return showNodeDescriptionToggle.isSelected();
    }

    private void initializeAlgorithmList() {
        algorithmList = new JList<>();
        algorithmList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        algorithmList.setBorder(UIUtils.createEmptyBorder(8));
        algorithmList.setOpaque(false);
        algorithmList.setModel(new DefaultListModel<>());
        algorithmList.setDragEnabled(true);
        algorithmList.setTransferHandler(new JIPipeDesktopAddNodeTransferHandler());
        scrollPane = new JScrollPane(algorithmList);
        algorithmList.setCellRenderer(new JIPipeDesktopAddNodePanelEntryListCellRenderer(scrollPane, this));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, new GridBagConstraints(0,
                5,
                1,
                1,
                1,
                1,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.BOTH,
                new Insets(8, 0, 8, 8),
                0,
                0));

        algorithmList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() == 2) {
                        insertAtCursor(algorithmList.getSelectedValue());
                    }
                }
            }
        });
        algorithmList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {

                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (algorithmList.getSelectedValue() != null) {
                        insertAtCursor(algorithmList.getSelectedValue());
                        graphEditorUI.getCanvasUI().requestFocus();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (algorithmList.getSelectedIndex() == 0) {
                        searchField.getTextField().requestFocus();
                    }
                }

                super.keyReleased(e);
            }
        });
    }

    private void insertFirstAtCursor() {
        if (algorithmList.getSelectedValue() != null) {
            insertAtCursor(algorithmList.getSelectedValue());
        } else if (algorithmList.getModel().getSize() > 0) {
            insertAtCursor(algorithmList.getModel().getElementAt(0));
        }
    }

    private void insertAtCursor(JIPipeNodeDatabaseEntry entry) {
        Set<JIPipeDesktopGraphNodeUI> nodeUIs = entry.addToGraph(graphEditorUI.getCanvasUI());
        graphEditorUI.getCanvasUI().setSelection(nodeUIs);
    }

    public void focusSearchBar() {
        JTextField textField = searchField.getTextField();
        textField.selectAll();
        textField.grabFocus();
        textField.requestFocus();
        searchField.grabAttentionAnimation();
    }

    private MainCategoryFilter getSelectedMainCategory() {
        for (MainCategoryFilter mainCategoryFilter : mainCategoryFilters) {
            if (mainCategoryFilter.toggleButton.isSelected()) {
                return mainCategoryFilter;
            }
        }
        return null;
    }

    public static class MainCategoryFilter {
        private final JIPipeNodeTypeCategory category;
        private final JToggleButton toggleButton;

        public MainCategoryFilter(JIPipeNodeTypeCategory category, JToggleButton toggleButton) {
            this.category = category;
            this.toggleButton = toggleButton;
        }

        public boolean isSelected() {
            return toggleButton.isSelected();
        }

        public JIPipeNodeTypeCategory getCategory() {
            return category;
        }

        public String getCategoryId() {
            return category.getId();
        }
    }

    public static class ReloadListRun extends AbstractJIPipeRunnable {

        private final JIPipeDesktopAddNodesPanel toolBox;

        public ReloadListRun(JIPipeDesktopAddNodesPanel toolBox) {
            this.toolBox = toolBox;
        }

        @Override
        public String getTaskLabel() {
            return "Reload list";
        }

        @Override
        public void run() {

            MainCategoryFilter selectedCategory = toolBox.getSelectedMainCategory();
            String selectedCategoryId = selectedCategory != null ? selectedCategory.getCategoryId() : null;

            Set<String> pinnedNodeDatabaseEntries = toolBox.getPinnedNodeDatabaseEntries();
            DefaultListModel<JIPipeNodeDatabaseEntry> model = new DefaultListModel<>();
            JIPipeNodeDatabasePipelineVisibility role = toolBox.isCompartmentsEditor ? JIPipeNodeDatabasePipelineVisibility.Compartments : JIPipeNodeDatabasePipelineVisibility.Pipeline;
            List<JIPipeNodeDatabaseEntry> queryResult = toolBox.database.getLegacySearch().query(toolBox.searchField.getText(),
                    role,
                    false,
                    true,
                    pinnedNodeDatabaseEntries);

            for (JIPipeNodeDatabaseEntry entry : queryResult) {
                if ("jipipe:dummy:templates".equals(selectedCategoryId)) {
                    if (!(entry instanceof CreateNewNodesByTemplateDatabaseEntry)) {
                        continue;
                    }
                } else if ("jipipe:dummy:pinned".equals(selectedCategoryId)) {
                    if (!pinnedNodeDatabaseEntries.contains(entry.getId())) {
                        continue;
                    }
                } else if (selectedCategoryId != null) {
                    if (!entry.getCategoryIds().contains(selectedCategoryId)) {
                        continue;
                    }
                }

                if (toolBox.currentHierarchyVertex != null) {
                    boolean found = false;
                    for (String locationInfo : entry.getLocationInfos()) {
                        if (locationInfo.startsWith(toolBox.currentHierarchyVertex)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        continue;
                    }
                }

                model.addElement(entry);
            }

            try {
                SwingUtilities.invokeAndWait(() -> toolBox.algorithmList.setModel(model));
            } catch (InterruptedException | InvocationTargetException ignored) {
                return;
            }
            if (!model.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    toolBox.algorithmList.setSelectedIndex(0);
                    toolBox.scrollPane.getVerticalScrollBar().setValue(0);
                });
            }
        }
    }

    public static class TemplatesDummyNodeTypeCategory extends MiscellaneousNodeTypeCategory {
        @Override
        public String getId() {
            return "jipipe:dummy:templates";
        }

        @Override
        public String getName() {
            return "Templates";
        }

        @Override
        public String getDescription() {
            return "Node templates";
        }

        @Override
        public Icon getIcon() {
            return UIUtils.getIconFromResources("actions/star.png");
        }

        @Override
        public boolean isVisibleInPipeline() {
            return true;
        }

        @Override
        public boolean isVisibleInCompartments() {
            return true;
        }
    }

    public static class PinnedDummyNodeTypeCategory extends MiscellaneousNodeTypeCategory {
        @Override
        public String getId() {
            return "jipipe:dummy:pinned";
        }

        @Override
        public String getName() {
            return "";
        }

        @Override
        public String getDescription() {
            return "Pinned nodes";
        }

        @Override
        public Icon getIcon() {
            return UIUtils.getIconFromResources("actions/window-pin.png");
        }

        @Override
        public boolean isVisibleInPipeline() {
            return true;
        }

        @Override
        public boolean isVisibleInCompartments() {
            return true;
        }
    }
}
