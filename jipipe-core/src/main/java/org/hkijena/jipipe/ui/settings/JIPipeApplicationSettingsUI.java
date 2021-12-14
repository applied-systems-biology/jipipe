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

package org.hkijena.jipipe.ui.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDefaultDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.registries.JIPipeSettingsRegistry;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UI for {@link org.hkijena.jipipe.api.registries.JIPipeSettingsRegistry}
 */
public class JIPipeApplicationSettingsUI extends JIPipeWorkbenchPanel {

    private final JTree tree = new JTree();
    private final Map<String, TreeNode> nodePathMap = new HashMap<>();

    /**
     * Creates a new instance
     *
     * @param workbench the workbench
     */
    public JIPipeApplicationSettingsUI(JIPipeWorkbench workbench) {
        super(workbench);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        tree.setCellRenderer(new SettingsCategoryNodeRenderer());
        JSplitPane splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT, tree, new JPanel(), AutoResizeSplitPane.RATIO_1_TO_3);
        add(splitPane, BorderLayout.CENTER);

        Map<String, List<JIPipeSettingsRegistry.Sheet>> byCategory =
                JIPipe.getSettings().getRegisteredSheets().values().stream().collect(Collectors.groupingBy(JIPipeSettingsRegistry.Sheet::getCategory));
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
            for (JIPipeSettingsRegistry.Sheet sheet : byCategory.get(category)) {
                if (categoryIcon == null)
                    categoryIcon = sheet.getCategoryIcon();
            }
            SettingsCategoryNode node = new SettingsCategoryNode(byCategory.get(category), category, categoryIcon);
            for (JIPipeSettingsRegistry.Sheet sheet : byCategory.get(category).stream().sorted(Comparator.comparing(JIPipeSettingsRegistry.Sheet::getName)).collect(Collectors.toList())) {
                SettingsCategoryNode subCategoryNode = new SettingsCategoryNode(Collections.singletonList(sheet), sheet.getName(), sheet.getIcon());
                node.add(subCategoryNode);
                nodePathMap.put("/" + category + "/" + sheet.getName(), subCategoryNode);
            }
            rootNode.add(node);
            nodes.add(node);
            nodePathMap.put("/" + category, node);
        }
        tree.setModel(new DefaultTreeModel(rootNode));
        tree.addTreeSelectionListener(e -> {
            if (tree.getLastSelectedPathComponent() instanceof SettingsCategoryNode) {
                SettingsCategoryNode node = (SettingsCategoryNode) tree.getLastSelectedPathComponent();
                JIPipeParameterTree traversedParameterCollection = new JIPipeParameterTree();
                for (JIPipeSettingsRegistry.Sheet sheet : node.sheets) {
                    traversedParameterCollection.add(sheet.getParameterCollection(), sheet.getName(), null);
                    traversedParameterCollection.setSourceDocumentation(sheet.getParameterCollection(), new JIPipeDefaultDocumentation(sheet.getName(), null));
                }

                ParameterPanel parameterPanel = new ParameterPanel(getWorkbench(),
                        traversedParameterCollection,
                        MarkdownDocument.fromPluginResource("documentation/application-settings.md", new HashMap<>()),
                        ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.WITH_SEARCH_BAR);
                splitPane.setRightComponent(parameterPanel);
            }
        });
        if (!nodes.isEmpty()) {
            SettingsCategoryNode node = nodes.get(0);
            tree.getSelectionModel().setSelectionPath(new TreePath(((DefaultTreeModel) tree.getModel()).getPathToRoot(node)));
        }
        UIUtils.expandAllTree(tree);
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
        private final List<JIPipeSettingsRegistry.Sheet> sheets;
        private final String label;
        private final Icon icon;

        private SettingsCategoryNode(List<JIPipeSettingsRegistry.Sheet> sheets, String label, Icon icon) {
            this.sheets = sheets;
            this.label = label;
            this.icon = icon;
        }

        public List<JIPipeSettingsRegistry.Sheet> getSheets() {
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
