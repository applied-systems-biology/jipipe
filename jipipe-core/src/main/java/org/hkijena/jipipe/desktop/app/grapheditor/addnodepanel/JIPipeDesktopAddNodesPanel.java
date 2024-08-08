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
import org.hkijena.jipipe.api.nodes.database.*;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.AbstractJIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.JIPipeDesktopCompartmentsGraphEditorUI;
import org.hkijena.jipipe.desktop.commons.components.layouts.JIPipeDesktopWrapLayout;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.plugins.settings.JIPipeGraphEditorUIApplicationSettings;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * New and improved node tool box
 */
public class JIPipeDesktopAddNodesPanel extends JIPipeDesktopWorkbenchPanel {

    private final JToolBar toolBar = new JToolBar();
    private final JIPipeNodeDatabase database;
    private final JIPipeRunnableQueue queue = new JIPipeRunnableQueue("Node toolbox");
    private final JIPipeGraphEditorUIApplicationSettings settings;
    private final boolean isCompartmentsEditor;
    private JList<JIPipeNodeDatabaseEntry> algorithmList;
    private JIPipeDesktopSearchTextField searchField;
    private JScrollPane scrollPane;
    private final JPanel mainCategoriesPanel = new JPanel();
    private final AbstractJIPipeDesktopGraphEditorUI graphEditorUI;
    private final List<MainCategoryFilter> mainCategoryFilters = new ArrayList<>();
    private final JCheckBoxMenuItem showNodeDescriptionToggle = new JCheckBoxMenuItem("Show node descriptions", UIUtils.getIconFromResources("actions/help.png"));

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
    }

    private void initializeMainCategoryFilters() {

        // Compartments have no categories
        if(isCompartmentsEditor) {
            return;
        }

        JIPipe.getNodes().getRegisteredCategories().values().stream().sorted(Comparator.comparing(JIPipeNodeTypeCategory::getUIOrder)).forEach(category -> {
            if(category.isVisibleInPipeline()) {
                JToggleButton categoryButton = new JToggleButton(category.getName(), category.getIcon());
                categoryButton.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));

                // Add the old menu as popup menu to the categories
                JPopupMenu popupMenu = UIUtils.addRightClickPopupMenuToButton(categoryButton);
                if(category instanceof DataSourceNodeTypeCategory) {
                    initializeAddDataSourceMenu(graphEditorUI, popupMenu, new HashSet<>());
                }
                else {
                    initializeMenuForCategory(graphEditorUI, popupMenu, category, new HashSet<>());
                }

                MainCategoryFilter currentFilter = new MainCategoryFilter(category, categoryButton);
                categoryButton.addActionListener(e -> {
                    if(categoryButton.isSelected()) {
                        for (MainCategoryFilter filter : mainCategoryFilters) {
                            if(filter.isSelected() && filter != currentFilter) {
                                filter.toggleButton.setSelected(false);
                            }
                        }
                    }
                    updateMainCategoryFilters();
                    reloadAlgorithmList();
                });
               mainCategoryFilters.add(currentFilter);
            }
        });
    }

    private void updateMainCategoryFilters() {

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
        if(!isCompartmentsEditor) {
            initializeMainCategoryPanel();
        }
        initializeAlgorithmList();
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
                if(e.getKeyCode() == KeyEvent.VK_DOWN) {
                    algorithmList.requestFocus();
                }
                else if(e.getKeyCode() == KeyEvent.VK_ENTER) {
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
        showNodeDescriptionToggle.setName("Show node descriptions in the search results, which will take up a bit more vertical space per item");
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
                if(SwingUtilities.isLeftMouseButton(e)) {
                    if(e.getClickCount() == 2) {
                        insertAtCursor(algorithmList.getSelectedValue());
                    }
                }
            }
        });
        algorithmList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {

                if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if(algorithmList.getSelectedValue() != null) {
                        insertAtCursor(algorithmList.getSelectedValue());
                        graphEditorUI.getCanvasUI().requestFocus();
                    }
                }
                else if(e.getKeyCode() == KeyEvent.VK_UP) {
                    if(algorithmList.getSelectedIndex() == 0) {
                        searchField.getTextField().requestFocus();
                    }
                }

                super.keyReleased(e);
            }
        });
    }

    private void insertFirstAtCursor() {
        if(algorithmList.getSelectedValue() != null) {
            insertAtCursor(algorithmList.getSelectedValue());
        }
        else if(algorithmList.getModel().getSize() > 0) {
            insertAtCursor(algorithmList.getModel().getElementAt(0));
        }
    }

    private void insertAtCursor(JIPipeNodeDatabaseEntry entry) {
        JIPipeDesktopGraphNodeUI nodeUI = entry.addToGraph(graphEditorUI.getCanvasUI());
        graphEditorUI.getCanvasUI().selectOnly(nodeUI);
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

    public void focusSearchBar() {
        JXTextField textField = searchField.getTextField();
        textField.selectAll();
        textField.grabFocus();
        textField.requestFocus();
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

            String selectedCategoryId = null;
            for (MainCategoryFilter filter : toolBox.mainCategoryFilters) {
                if(filter.isSelected()) {
                    selectedCategoryId = filter.getCategoryId();
                    break;
                }
            }

            DefaultListModel<JIPipeNodeDatabaseEntry> model = new DefaultListModel<>();
            JIPipeNodeDatabasePipelineVisibility role = toolBox.isCompartmentsEditor ? JIPipeNodeDatabasePipelineVisibility.Compartments : JIPipeNodeDatabasePipelineVisibility.Pipeline;
            for (JIPipeNodeDatabaseEntry entry : toolBox.database.getLegacySearch().query(toolBox.searchField.getText(),
                    role,
                    false,
                    true)) {
                if(selectedCategoryId != null) {
                    if(!entry.getCategoryIds().contains(selectedCategoryId)) {
                        continue;
                    }
                }
                model.addElement(entry);
            }
            toolBox.algorithmList.setModel(model);

            if (!model.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    toolBox.algorithmList.setSelectedIndex(0);
                    toolBox.scrollPane.getVerticalScrollBar().setValue(0);
                });
            }
        }
    }
}
