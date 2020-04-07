package org.hkijena.acaq5.ui.grapheditor;

import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.MarkdownReader;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQMultiAlgorithmSelectionPanelUI;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQSingleAlgorithmSelectionPanelUI;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Editor for a project graph compartment
 */
public class ACAQAlgorithmGraphCompartmentUI extends ACAQAlgorithmGraphEditorUI {

    private MarkdownReader documentationPanel;

    /**
     * Creates a project graph compartment editor
     *
     * @param workbenchUI    The workbench
     * @param algorithmGraph The graph
     * @param compartment    The compartment
     */
    public ACAQAlgorithmGraphCompartmentUI(ACAQProjectWorkbench workbenchUI, ACAQAlgorithmGraph algorithmGraph, String compartment) {
        super(workbenchUI, algorithmGraph, compartment);
        documentationPanel = new MarkdownReader(false);
        documentationPanel.setDocument(MarkdownDocument.fromPluginResource("documentation/algorithm-graph.md"));
        setPropertyPanel(documentationPanel);
    }


    @Override
    public void reloadMenuBar() {
        getMenuBar().removeAll();
        initializeAddNodesMenus(getMenuBar(), getAlgorithmGraph(), getCompartment());
        initializeCommonActions();
    }

    @Override
    protected void updateSelection() {
        if (getSelection().isEmpty()) {
            setPropertyPanel(documentationPanel);
        } else if (getSelection().size() == 1) {
            ACAQAlgorithmUI ui = getSelection().iterator().next();
            setPropertyPanel(new ACAQSingleAlgorithmSelectionPanelUI((ACAQProjectWorkbench) getWorkbench(), getAlgorithmGraph(), ui.getAlgorithm()));
        } else {
            setPropertyPanel(new ACAQMultiAlgorithmSelectionPanelUI((ACAQProjectWorkbench) getWorkbench(), getAlgorithmGraph(),
                    getSelection().stream().map(ACAQAlgorithmUI::getAlgorithm).collect(Collectors.toSet())));
        }
    }

    /**
     * Initializes the "Add nodes" menus
     *
     * @param menuBar        The menu bar where the items are created
     * @param algorithmGraph the algorithm graph where nodes are put
     * @param compartment    the graph compartment where nodes are put
     */
    public static void initializeAddNodesMenus(JMenuBar menuBar, ACAQAlgorithmGraph algorithmGraph, String compartment) {
        JMenu addDataSourceMenu = new JMenu("Add data");
        addDataSourceMenu.setIcon(UIUtils.getIconFromResources("database.png"));
        initializeAddDataSourceMenu(addDataSourceMenu, algorithmGraph, compartment);
        menuBar.add(addDataSourceMenu);

        JMenu addFilesystemMenu = new JMenu("Filesystem");
        addFilesystemMenu.setIcon(UIUtils.getIconFromResources("tree.png"));
        initializeMenuForCategory(addFilesystemMenu, ACAQAlgorithmCategory.FileSystem, algorithmGraph, compartment);
        menuBar.add(addFilesystemMenu);

        JMenu addAnnotationMenu = new JMenu("Annotation");
        addAnnotationMenu.setIcon(UIUtils.getIconFromResources("label.png"));
        initializeMenuForCategory(addAnnotationMenu, ACAQAlgorithmCategory.Annotation, algorithmGraph, compartment);
        menuBar.add(addAnnotationMenu);

        JMenu addEnhancerMenu = new JMenu("Enhance");
        addEnhancerMenu.setIcon(UIUtils.getIconFromResources("magic.png"));
        initializeMenuForCategory(addEnhancerMenu, ACAQAlgorithmCategory.Enhancer, algorithmGraph, compartment);
        menuBar.add(addEnhancerMenu);

        JMenu addSegmenterMenu = new JMenu("Segment");
        addSegmenterMenu.setIcon(UIUtils.getIconFromResources("segment.png"));
        initializeMenuForCategory(addSegmenterMenu, ACAQAlgorithmCategory.Segmentation, algorithmGraph, compartment);
        menuBar.add(addSegmenterMenu);

        JMenu addConverterMenu = new JMenu("Convert");
        addConverterMenu.setIcon(UIUtils.getIconFromResources("convert.png"));
        initializeMenuForCategory(addConverterMenu, ACAQAlgorithmCategory.Converter, algorithmGraph, compartment);
        menuBar.add(addConverterMenu);

        JMenu addQuantifierMenu = new JMenu("Quantify");
        addQuantifierMenu.setIcon(UIUtils.getIconFromResources("statistics.png"));
        initializeMenuForCategory(addQuantifierMenu, ACAQAlgorithmCategory.Quantifier, algorithmGraph, compartment);
        menuBar.add(addQuantifierMenu);

        JMenu addMiscMenu = new JMenu("Miscellaneous");
        addMiscMenu.setIcon(UIUtils.getIconFromResources("module.png"));
        initializeMenuForCategory(addMiscMenu, ACAQAlgorithmCategory.Miscellaneous, algorithmGraph, compartment);
        menuBar.add(addMiscMenu);
    }

    /**
     * Initializes a menu for one algorithm category
     *
     * @param menu           The menu
     * @param category       The algorithm category
     * @param algorithmGraph the algorithm graph where nodes are added
     * @param compartment    the graph compartment where nodes are put
     */
    public static void initializeMenuForCategory(JMenu menu, ACAQAlgorithmCategory category, ACAQAlgorithmGraph algorithmGraph, String compartment) {
        ACAQDefaultRegistry registryService = ACAQDefaultRegistry.getInstance();
        Set<ACAQAlgorithmDeclaration> algorithmsOfCategory = registryService.getAlgorithmRegistry().getAlgorithmsOfCategory(category);
        if (algorithmsOfCategory.isEmpty()) {
            menu.setVisible(false);
            return;
        }

        Icon icon = new ColorIcon(16, 16, UIUtils.getFillColorFor(category));
        Map<String, Set<ACAQAlgorithmDeclaration>> byMenuPath = ACAQAlgorithmDeclaration.groupByMenuPaths(algorithmsOfCategory);
        Map<String, JMenu> menuTree = UIUtils.createMenuTree(menu, byMenuPath.keySet());

        for (Map.Entry<String, Set<ACAQAlgorithmDeclaration>> entry : byMenuPath.entrySet()) {
            JMenu subMenu = menuTree.get(entry.getKey());
            for (ACAQAlgorithmDeclaration declaration : ACAQAlgorithmDeclaration.getSortedList(entry.getValue())) {
                JMenuItem addItem = new JMenuItem(declaration.getName(), icon);
                addItem.setToolTipText(TooltipUtils.getAlgorithmTooltip(declaration));
                addItem.addActionListener(e -> algorithmGraph.insertNode(declaration.newInstance(), compartment));
                subMenu.add(addItem);
            }
        }
    }

    /**
     * Initializes a menu that adds data sources
     *
     * @param menu           the target menu
     * @param algorithmGraph the algorithm graph where nodes are put
     * @param compartment    the compartment where nodes are put
     */
    public static void initializeAddDataSourceMenu(JMenu menu, ACAQAlgorithmGraph algorithmGraph, String compartment) {
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
                    JMenuItem addItem = new JMenuItem(declaration.getName(), icon);
                    addItem.setToolTipText(TooltipUtils.getAlgorithmTooltip(declaration));
                    addItem.addActionListener(e -> algorithmGraph.insertNode(declaration.newInstance(), compartment));
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
