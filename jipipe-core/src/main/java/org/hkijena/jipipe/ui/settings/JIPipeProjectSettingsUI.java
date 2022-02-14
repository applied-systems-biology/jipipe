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

import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParametersUI;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.HashMap;

/**
 * UI around the metadata of an {@link org.hkijena.jipipe.api.JIPipeProject}
 */
public class JIPipeProjectSettingsUI extends JIPipeProjectWorkbenchPanel {

    private JSplitPane splitPane;

    /**
     * @param workbenchUI The workbench UI
     */
    public JIPipeProjectSettingsUI(JIPipeProjectWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JTree tree = new JTree();
        tree.setCellRenderer(new SettingsCategoryNodeRenderer());
        splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT, tree, new JPanel(), AutoResizeSplitPane.RATIO_1_TO_3);
        add(splitPane, BorderLayout.CENTER);

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
        rootNode.add(new SettingsCategoryNode("GENERAL", "General", UIUtils.getIconFromResources("actions/wrench.png")));
        rootNode.add(new SettingsCategoryNode("PERMISSIONS", "Permissions", UIUtils.getIconFromResources("actions/lock.png")));
        rootNode.add(new SettingsCategoryNode("PARAMETERS", "Parameters", UIUtils.getIconFromResources("data-types/parameters.png")));
        rootNode.add(new SettingsCategoryNode("MISC", "Miscellaneous", UIUtils.getIconFromResources("actions/configure.png")));
        tree.setModel(new DefaultTreeModel(rootNode));

        tree.addTreeSelectionListener(e -> {
            if (tree.getLastSelectedPathComponent() instanceof SettingsCategoryNode) {
                selectCategory(((SettingsCategoryNode) tree.getLastSelectedPathComponent()).getId());
            }
        });

        tree.getSelectionModel().setSelectionPath(new TreePath(((DefaultTreeModel) tree.getModel()).getPathToRoot(rootNode.getChildAt(0))));
        UIUtils.expandAllTree(tree);
    }

    private void selectCategory(String id) {
        if (id.equals("GENERAL")) {
            ParameterPanel parameterPanel = new ParameterPanel(getWorkbench(),
                    new JIPipeParameterTree(getProject().getMetadata()),
                    MarkdownDocument.fromPluginResource("documentation/project-settings.md", new HashMap<>()),
                    ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.WITH_SEARCH_BAR);
            parameterPanel.setCustomIsParameterCollectionVisible((tree, collection) -> false);
            parameterPanel.setCustomIsParameterVisible((tree, parameter) -> {
                if (parameter.getKey().contains("template"))
                    return false;
                if (parameter.getKey().equals("update-site-dependencies"))
                    return false;
                return !parameter.isHidden();
            });
            splitPane.setRightComponent(parameterPanel);
        } else if (id.equals("PERMISSIONS")) {
            ParameterPanel parameterPanel = new ParameterPanel(getWorkbench(),
                    new JIPipeParameterTree(getProject().getMetadata().getPermissions()),
                    MarkdownDocument.fromPluginResource("documentation/project-settings.md", new HashMap<>()),
                    ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.WITH_SEARCH_BAR);
            splitPane.setRightComponent(parameterPanel);
        } else if (id.equals("PARAMETERS")) {
            FormPanel parameterUI = new FormPanel(MarkdownDocument.fromPluginResource("documentation/project-settings-parameters.md", new HashMap<>()),
                    FormPanel.WITH_SCROLLING | FormPanel.WITH_DOCUMENTATION);
            GraphNodeParametersUI graphNodeParametersUI = new GraphNodeParametersUI(getWorkbench(), getPipelineParameters().getExportedParameters(), FormPanel.NONE, false);
            graphNodeParametersUI.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            parameterUI.addWideToForm(graphNodeParametersUI, null);
            parameterUI.addVerticalGlue();
            splitPane.setRightComponent(parameterUI);
        } else if (id.equals("MISC")) {
            ParameterPanel parameterPanel = new ParameterPanel(getWorkbench(),
                    new JIPipeParameterTree(getProject().getMetadata()),
                    MarkdownDocument.fromPluginResource("documentation/project-settings.md", new HashMap<>()),
                    ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.WITH_SEARCH_BAR);
            parameterPanel.setCustomIsParameterCollectionVisible((tree, collection) -> false);
            parameterPanel.setCustomIsParameterVisible((tree, parameter) -> {
                if (parameter.getKey().contains("template"))
                    return true;
                if (parameter.getKey().equals("update-site-dependencies"))
                    return true;
                return false;
            });
            splitPane.setRightComponent(parameterPanel);
        }
    }

    private JIPipeProjectInfoParameters getPipelineParameters() {
        Object existing = getProject().getAdditionalMetadata().getOrDefault(JIPipeProjectInfoParameters.METADATA_KEY, null);
        JIPipeProjectInfoParameters result;
        if (existing instanceof JIPipeProjectInfoParameters) {
            result = (JIPipeProjectInfoParameters) existing;
        } else {
            result = new JIPipeProjectInfoParameters();
            getProject().getAdditionalMetadata().put(JIPipeProjectInfoParameters.METADATA_KEY, result);
        }
        result.setProject(getProject());
        return result;
    }

    private static class SettingsCategoryNode extends DefaultMutableTreeNode {
        private final String id;
        private final String label;
        private final Icon icon;

        private SettingsCategoryNode(String id, String label, Icon icon) {
            this.id = id;
            this.label = label;
            this.icon = icon;
        }

        public String getLabel() {
            return label;
        }

        public Icon getIcon() {
            return icon;
        }

        public String getId() {
            return id;
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
