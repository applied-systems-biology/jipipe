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

package org.hkijena.jipipe.desktop.app.settings;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.settings.JIPipeDefaultProjectSettingsSheet;
import org.hkijena.jipipe.api.settings.JIPipeDefaultProjectSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeProjectSettingsSheet;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.JIPipeDesktopSplitPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides components for the project settings.
 * This is not a panel, as the project overview UI splits the tree and the content
 */
public class JIPipeDesktopProjectSettingsUI extends JPanel {

    private final JIPipeProject project;
    private final JIPipeDesktopWorkbench workbench;
    private final JTree tree = new JTree();
    private final Map<String, DefaultMutableTreeNode> nodePathMap = new HashMap<>();
    private final Map<JIPipeProjectSettingsSheet, DefaultMutableTreeNode> sheetToNodeMap = new HashMap<>();

    private final JPanel treePanel = new JPanel(new BorderLayout());
    private final JPanel contentPanel = new JPanel(new BorderLayout());


    /**
     * Creates a new instance
     *
     * @param workbench the workbench
     */
    public JIPipeDesktopProjectSettingsUI(JIPipeProject project, JIPipeDesktopWorkbench workbench) {
        this.project = project;
        this.workbench = workbench;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        treePanel.add(new JScrollPane(tree), BorderLayout.CENTER);
        tree.setCellRenderer(new SettingsCategoryNodeRenderer());

        List<JIPipeProjectSettingsSheet> settingsSheets = new ArrayList<>(project.getSettingsSheets().values());

        // Introduce legacy settings as sheets
        settingsSheets.add(new ProjectMetadataSettingsSheetWrapper(project));
        settingsSheets.add(new ProjectParametersSettingsSheetWrapper(project));
        settingsSheets.add(new ProjectDirectoriesSettingsSheetWrapper(project));

        // Continue with sheets
        Map<String, List<JIPipeProjectSettingsSheet>> byCategory =
                settingsSheets.stream().collect(Collectors.groupingBy(JIPipeProjectSettingsSheet::getCategory));
        List<String> categories = byCategory.keySet().stream().sorted().collect(Collectors.toList());
        if (categories.contains("General")) {
            categories.remove("General");
            categories.add(0, "General");
        }
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
        nodePathMap.put("/", rootNode);
        List<SettingsCategoryNode> nodes = new ArrayList<>();
        for (String category : categories) {
            Icon categoryIcon = JIPipeDefaultProjectSettingsSheetCategory.General.getIcon();
            List<JIPipeProjectSettingsSheet> sheetList = byCategory.getOrDefault(category, Collections.emptyList());
            for (JIPipeProjectSettingsSheet sheet : sheetList) {
                if (categoryIcon == null)
                    categoryIcon = sheet.getCategoryIcon();
            }
            SettingsCategoryNode node = new SettingsCategoryNode(sheetList, category, categoryIcon);

            for (JIPipeProjectSettingsSheet sheet : sheetList.stream().sorted(Comparator.comparing(JIPipeProjectSettingsSheet::getName)).collect(Collectors.toList())) {
                SettingsCategoryNode subCategoryNode = new SettingsCategoryNode(Collections.singletonList(sheet), sheet.getName(), sheet.getIcon());
                node.add(subCategoryNode);
                nodePathMap.put("/" + category + "/" + sheet.getName(), subCategoryNode);
                sheetToNodeMap.put(sheet, subCategoryNode);
            }
            rootNode.add(node);
            nodes.add(node);
            nodePathMap.put("/" + category, node);
        }
        tree.setModel(new DefaultTreeModel(rootNode));
        tree.addTreeSelectionListener(e -> {
            onTreeNodeSelected();
        });
        if (!nodes.isEmpty()) {
            SettingsCategoryNode node = nodes.get(0);
            tree.getSelectionModel().setSelectionPath(new TreePath(((DefaultTreeModel) tree.getModel()).getPathToRoot(node)));
        }
        UIUtils.expandAllTree(tree);

        JSplitPane splitPane = new JIPipeDesktopSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePanel, contentPanel, JIPipeDesktopSplitPane.RATIO_1_TO_3);
        add(splitPane, BorderLayout.CENTER);
    }

    private void onTreeNodeSelected() {
        if (tree.getLastSelectedPathComponent() instanceof SettingsCategoryNode) {
            SettingsCategoryNode node = (SettingsCategoryNode) tree.getLastSelectedPathComponent();
            contentPanel.removeAll();
            if (node.sheets.isEmpty()) {
                contentPanel.add(UIUtils.createInfoLabel("No settings available", "There are no settings within the category '" + node.label + "'"), BorderLayout.CENTER);
            } else if (node.sheets.size() == 1) {
                JIPipeProjectSettingsSheet sheet = node.sheets.get(0);
                JIPipeDesktopParameterFormPanel parameterPanel = new JIPipeDesktopParameterFormPanel(workbench,
                        sheet,
                        MarkdownText.fromPluginResource("documentation/project-settings.md", new HashMap<>()),
                        JIPipeDesktopParameterFormPanel.WITH_SCROLLING | JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION | JIPipeDesktopParameterFormPanel.DOCUMENTATION_NO_UI | JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR);
                contentPanel.add(parameterPanel, BorderLayout.CENTER);
            } else {
                JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
                formPanel.addWideToForm(new JLabel("<html><h1>" + node.label + "</h1></html>", UIUtils.getIcon32FromResources("actions/configure.png"), SwingConstants.LEFT));
                node.sheets.stream().sorted(Comparator.comparing(JIPipeProjectSettingsSheet::getName)).forEach(sheet -> {
                    JButton goToCategoryButton = new JButton("<html><span style=\"font-size: 16px;\">" + sheet.getName() + "</span><br/>" + sheet.getDescription() + "</html>", sheet.getIcon());
                    goToCategoryButton.setHorizontalAlignment(SwingConstants.LEFT);
//                    goToCategoryButton.setVerticalAlignment(SwingConstants.TOP);
//                    goToCategoryButton.setVerticalTextPosition(SwingConstants.TOP);
                    goToCategoryButton.addActionListener(e2 -> {
                        tree.getSelectionModel().setSelectionPath(new TreePath(sheetToNodeMap.get(sheet).getPath()));
                    });
                    formPanel.addWideToForm(goToCategoryButton);
                });
                formPanel.addVerticalGlue();
                contentPanel.add(formPanel, BorderLayout.CENTER);
            }

            contentPanel.revalidate();
            contentPanel.repaint();

        }
    }

    public void selectNode(String path) {
        TreeNode node = nodePathMap.getOrDefault(path, null);
        if (node != null) {
            tree.getSelectionModel().setSelectionPath(new TreePath(((DefaultTreeModel) tree.getModel()).getPathToRoot(node)));
        }
    }

    public Map<String, TreeNode> getNodePathMap() {
        return Collections.unmodifiableMap(nodePathMap);
    }

    private static class SettingsCategoryNode extends DefaultMutableTreeNode {
        private final List<JIPipeProjectSettingsSheet> sheets;
        private final String label;
        private final Icon icon;

        private SettingsCategoryNode(List<JIPipeProjectSettingsSheet> sheets, String label, Icon icon) {
            this.sheets = sheets;
            this.label = label;
            this.icon = icon;
        }

        public List<JIPipeProjectSettingsSheet> getSheets() {
            return sheets;
        }

        public String getLabel() {
            return label;
        }

        public Icon getIcon() {
            return icon;
        }
    }

    private static class SettingsCategoryNodeRenderer extends JLabel implements TreeCellRenderer {

        public SettingsCategoryNodeRenderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            if (value instanceof SettingsCategoryNode) {
                setText(((SettingsCategoryNode) value).getLabel());
                setIcon(((SettingsCategoryNode) value).getIcon());
            } else {
                setText("");
                setIcon(null);
            }
            if (selected) {
                setBackground(UIManager.getColor("List.selectionBackground"));
            } else {
                setBackground(UIManager.getColor("List.background"));
            }
            return this;
        }
    }

    public abstract static class SettingsSheetWrapper extends JIPipeDefaultProjectSettingsSheet {
        private final JIPipeParameterCollection wrapped;

        private SettingsSheetWrapper(JIPipeParameterCollection wrapped) {
            this.wrapped = wrapped;
        }

        @SetJIPipeDocumentation(name = "General")
        @JIPipeParameter("wrapped")
        public JIPipeParameterCollection getWrapped() {
            return wrapped;
        }
    }

    public static class ProjectDirectoriesSettingsSheetWrapper extends SettingsSheetWrapper {
        private final JIPipeProject project;

        public ProjectDirectoriesSettingsSheetWrapper(JIPipeProject project) {
            super(project.getMetadata().getDirectories());
            this.project = project;
        }

        @Override
        public JIPipeDefaultProjectSettingsSheetCategory getDefaultCategory() {
            return JIPipeDefaultProjectSettingsSheetCategory.General;
        }

        @Override
        public String getId() {
            return "jipipe:project-directories";
        }

        @Override
        public Icon getIcon() {
            return UIUtils.getIconFromResources("actions/folder.png");
        }

        @Override
        public String getName() {
            return "Project-wide directories";
        }

        @Override
        public String getDescription() {
            return "Allows to setup project-wide paths and directories";
        }

        @Override
        public void getEnvironmentDependencies(List<JIPipeEnvironment> target) {

        }
    }

    public static class ProjectMetadataSettingsSheetWrapper extends SettingsSheetWrapper {
        private final JIPipeProject project;

        public ProjectMetadataSettingsSheetWrapper(JIPipeProject project) {
            super(project.getMetadata());
            this.project = project;
        }

        @Override
        public String getId() {
            return "jipipe:project-metadata";
        }

        @Override
        public Icon getIcon() {
            return UIUtils.getIconFromResources("actions/circle-info.png");
        }

        @Override
        public String getName() {
            return "Project metadata";
        }

        @Override
        public String getDescription() {
            return "General project metadata";
        }

        @Override
        public JIPipeDefaultProjectSettingsSheetCategory getDefaultCategory() {
            return JIPipeDefaultProjectSettingsSheetCategory.General;
        }

        @Override
        public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterCollection subParameter) {
            if (subParameter == project.getMetadata().getPermissions()) {
                return false;
            }
            if (subParameter == project.getMetadata().getDirectories()) {
                return false;
            }
            if (subParameter == project.getMetadata().getGlobalParameters()) {
                return false;
            }
            return super.isParameterUIVisible(tree, subParameter);
        }

        @Override
        public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
            if ("template-description".equals(access.getKey())) {
                return false;
            }
            if ("node-templates".equals(access.getKey())) {
                return false;
            }
            return super.isParameterUIVisible(tree, access);
        }

        @Override
        public void getEnvironmentDependencies(List<JIPipeEnvironment> target) {

        }
    }

    public static class ProjectPermissionsSettingsSheetWrapper extends SettingsSheetWrapper {
        private final JIPipeProject project;

        public ProjectPermissionsSettingsSheetWrapper(JIPipeProject project) {
            super(project.getMetadata());
            this.project = project;
        }

        @Override
        public String getId() {
            return "jipipe:project-permissions";
        }

        @Override
        public Icon getIcon() {
            return UIUtils.getIconFromResources("actions/database-lock.png");
        }

        @Override
        public String getName() {
            return "Project permissions";
        }

        @Override
        public String getDescription() {
            return "Allows to lock certain edit options";
        }

        @Override
        public JIPipeDefaultProjectSettingsSheetCategory getDefaultCategory() {
            return JIPipeDefaultProjectSettingsSheetCategory.General;
        }

        @Override
        public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterCollection subParameter) {
            if (subParameter == project.getMetadata().getPermissions()) {
                return true;
            }
            return false;
        }

        @Override
        public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
            return super.isParameterUIVisible(tree, access);
        }

        @Override
        public void getEnvironmentDependencies(List<JIPipeEnvironment> target) {

        }
    }

    public static class ProjectParametersSettingsSheetWrapper extends SettingsSheetWrapper {
        private final JIPipeProject project;

        public ProjectParametersSettingsSheetWrapper(JIPipeProject project) {
            super(project.getMetadata());
            this.project = project;
        }

        @Override
        public String getId() {
            return "jipipe:project-global-parameters";
        }

        @Override
        public Icon getIcon() {
            return UIUtils.getIconFromResources("actions/configure_toolbars.png");
        }

        @Override
        public String getName() {
            return "Project-wide parameters";
        }

        @Override
        public String getDescription() {
            return "Allows to configure parameters that are accessible project-wide in expressions and nodes";
        }

        @Override
        public JIPipeDefaultProjectSettingsSheetCategory getDefaultCategory() {
            return JIPipeDefaultProjectSettingsSheetCategory.General;
        }

        @Override
        public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterCollection subParameter) {
            if (subParameter == project.getMetadata().getGlobalParameters()) {
                return true;
            }
            return false;
        }

        @Override
        public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
            return super.isParameterUIVisible(tree, access);
        }

        @Override
        public void getEnvironmentDependencies(List<JIPipeEnvironment> target) {

        }
    }

}
