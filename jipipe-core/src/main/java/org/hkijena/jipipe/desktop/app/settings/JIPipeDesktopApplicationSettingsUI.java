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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.registries.JIPipeApplicationSettingsRegistry;
import org.hkijena.jipipe.api.settings.JIPipeApplicationSettingsSheet;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
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
 * UI for {@link JIPipeApplicationSettingsRegistry}
 */
public class JIPipeDesktopApplicationSettingsUI extends JIPipeDesktopWorkbenchPanel {

    private final JTree tree = new JTree();
    private final Map<String, DefaultMutableTreeNode> nodePathMap = new HashMap<>();

    private final Map<JIPipeApplicationSettingsSheet, DefaultMutableTreeNode> sheetToNodeMap = new HashMap<>();

    /**
     * Creates a new instance
     *
     * @param workbench the workbench
     */
    public JIPipeDesktopApplicationSettingsUI(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        tree.setCellRenderer(new SettingsCategoryNodeRenderer());
        JSplitPane splitPane = new JIPipeDesktopSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tree), new JPanel(), JIPipeDesktopSplitPane.RATIO_1_TO_3);
        add(splitPane, BorderLayout.CENTER);

        Map<String, List<JIPipeApplicationSettingsSheet>> byCategory =
                JIPipe.getSettings().getRegisteredSheets().values().stream().collect(Collectors.groupingBy(JIPipeApplicationSettingsSheet::getCategory));
        List<String> categories = byCategory.keySet().stream().sorted().collect(Collectors.toList());
        if (categories.contains("General")) {
            categories.remove("General");
            categories.add(0, "General");
        }
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
        nodePathMap.put("/", rootNode);
        List<SettingsCategoryNode> nodes = new ArrayList<>();
        for (String category : categories) {
            Icon categoryIcon = null;
            for (JIPipeApplicationSettingsSheet sheet : byCategory.get(category)) {
                if (categoryIcon == null)
                    categoryIcon = sheet.getCategoryIcon();
            }
            SettingsCategoryNode node = new SettingsCategoryNode(byCategory.get(category), category, categoryIcon);
            for (JIPipeApplicationSettingsSheet sheet : byCategory.get(category).stream().sorted(Comparator.comparing(JIPipeApplicationSettingsSheet::getName)).collect(Collectors.toList())) {
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
            onTreeNodeSelected(splitPane);
        });
        if (!nodes.isEmpty()) {
            SettingsCategoryNode node = nodes.get(0);
            tree.getSelectionModel().setSelectionPath(new TreePath(((DefaultTreeModel) tree.getModel()).getPathToRoot(node)));
        }
        UIUtils.expandAllTree(tree);
    }

    private void onTreeNodeSelected(JSplitPane splitPane) {
        if (tree.getLastSelectedPathComponent() instanceof SettingsCategoryNode) {
            SettingsCategoryNode node = (SettingsCategoryNode) tree.getLastSelectedPathComponent();

            if (node.sheets.isEmpty()) {
                splitPane.setRightComponent(UIUtils.createInfoLabel("No settings available", "There are no settings within the category '" + node.label + "'"));
            } else if (node.sheets.size() == 1) {
                JIPipeApplicationSettingsSheet sheet = node.sheets.get(0);
                JIPipeDesktopParameterFormPanel parameterPanel = new JIPipeDesktopParameterFormPanel(getDesktopWorkbench(),
                        sheet,
                        MarkdownText.fromPluginResource("documentation/application-settings.md", new HashMap<>()),
                        JIPipeDesktopParameterFormPanel.WITH_SCROLLING | JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION | JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR);
                splitPane.setRightComponent(parameterPanel);
            } else {
                JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
                formPanel.addWideToForm(new JLabel("<html><h1>" + node.label + "</h1></html>", UIUtils.getIcon32FromResources("actions/configure.png"), SwingConstants.LEFT));
                node.sheets.stream().sorted(Comparator.comparing(JIPipeApplicationSettingsSheet::getName)).forEach(sheet -> {
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
                splitPane.setRightComponent(formPanel);
            }

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
        private final List<JIPipeApplicationSettingsSheet> sheets;
        private final String label;
        private final Icon icon;

        private SettingsCategoryNode(List<JIPipeApplicationSettingsSheet> sheets, String label, Icon icon) {
            this.sheets = sheets;
            this.label = label;
            this.icon = icon;
        }

        public List<JIPipeApplicationSettingsSheet> getSheets() {
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
}
