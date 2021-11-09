package org.hkijena.jipipe.ui.parameters;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.components.ParameterTreeUI;
import org.hkijena.jipipe.ui.components.ReadonlyCopyableTextField;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdaptiveParameterBuilder extends JDialog {
    private final JIPipeWorkbench workbench;
    private final JIPipeParameterCollection parameterCollection;
    private final JIPipeParameterTree parameterTree;
    private ParameterTreeUI parameterTreeUI;
    private final JPanel contentPanel = new JPanel(new BorderLayout());
    private FormPanel listPanel;
    private FormPanel infoPanel;
    private ReadonlyCopyableTextField nameLabel;
    private ReadonlyCopyableTextField typeLabel;
    private ReadonlyCopyableTextField nameIdLabel;
    private ReadonlyCopyableTextField typeIdLabel;
    private JTextPane typeDescriptionLabel;
    private boolean canceled = true;
    private List<ConditionValuePair> conditionValuePairs = new ArrayList<>();
    private JIPipeParameterAccess currentParameterAccess;

    public AdaptiveParameterBuilder(JIPipeWorkbench workbench, JIPipeParameterCollection parameterCollection) {
        this.workbench = workbench;
        this.parameterCollection = parameterCollection;
        this.parameterTree = new JIPipeParameterTree(parameterCollection);
        initialize();

        // Select the first entry
        JTree treeComponent = parameterTreeUI.getTreeComponent();
        DefaultTreeModel model = (DefaultTreeModel) treeComponent.getModel();
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) model.getRoot();
        if (rootNode.getChildCount() > 0) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) rootNode.getChildAt(0);
            treeComponent.setSelectionPath(new TreePath(child.getPath()));
        }
    }

    private void initialize() {
        setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        setTitle((parameterCollection instanceof JIPipeGraphNode) ? "Add adaptive parameter: " + ((JIPipeGraphNode) parameterCollection).getName() : "Add adaptive parameter");

        getContentPane().setLayout(new BorderLayout(8, 8));

        this.parameterTreeUI = new ParameterTreeUI(parameterTree);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                parameterTreeUI,
                contentPanel);
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.33);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.33);
            }
        });
        getContentPane().add(splitPane, BorderLayout.CENTER);

        infoPanel = new FormPanel(null, FormPanel.WITH_SCROLLING | FormPanel.WITH_DOCUMENTATION | FormPanel.DOCUMENTATION_BELOW);
        listPanel = new FormPanel(MarkdownDocument.fromPluginResource("documentation/adaptive-parameter-builder.md", Collections.emptyMap()),
                FormPanel.WITH_SCROLLING | FormPanel.WITH_DOCUMENTATION | FormPanel.DOCUMENTATION_BELOW);

        JPanel listContainer = new JPanel(new BorderLayout());
        listContainer.add(listPanel, BorderLayout.CENTER);

        JToolBar listToolbar = new JToolBar();
        listToolbar.setFloatable(false);
        listToolbar.add(Box.createHorizontalGlue());

        JButton addConditionButton = new JButton("Add condition", UIUtils.getIconFromResources("actions/list-add.png"));
        addConditionButton.addActionListener(e -> addNewConditionValuePair());
        listToolbar.add(addConditionButton);

        listContainer.add(listToolbar, BorderLayout.NORTH);

        DocumentTabPane documentTabPane = new DocumentTabPane();
        documentTabPane.addTab("Conditions", UIUtils.getIconFromResources("actions/dialog-xml-editor.png"), listContainer, DocumentTabPane.CloseMode.withoutCloseButton);
        documentTabPane.addTab("Info", UIUtils.getIconFromResources("actions/help-info.png"), infoPanel, DocumentTabPane.CloseMode.withoutCloseButton);
        contentPanel.add(documentTabPane, BorderLayout.CENTER);

        initializeInfoPanel();
        initializeButtonPanel();

        // Register event
        parameterTreeUI.getTreeComponent().addTreeSelectionListener(e -> updateCurrentValue());
    }

    private void initializeInfoPanel() {
        nameLabel = new ReadonlyCopyableTextField("", false);
        nameIdLabel = new ReadonlyCopyableTextField("", true);
        typeLabel = new ReadonlyCopyableTextField("", false);
        typeIdLabel = new ReadonlyCopyableTextField("", true);
        typeDescriptionLabel = UIUtils.makeReadonlyTextPane("");

        infoPanel.addToForm(nameLabel, new JLabel("Name"), new MarkdownDocument("The name of the parameter"));
        infoPanel.addToForm(nameIdLabel, new JLabel("Unique ID"), new MarkdownDocument("The unique ID of the parameter. This is used for " +
                "creating new parameter data."));
        infoPanel.addToForm(typeLabel, new JLabel("Type name"), new MarkdownDocument("The name of the parameter type"));
        infoPanel.addToForm(typeIdLabel, new JLabel("Unique type ID"), new MarkdownDocument("The unique ID of the data type the parameter is " +
                "based on. On creating parameters, you will have to ensure that this type matches exactly."));
        infoPanel.addToForm(typeDescriptionLabel, new JLabel("Type description"), new MarkdownDocument("A short description of the parameter data type."));
        infoPanel.addVerticalGlue();
    }

    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0,0,8,8));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            canceled = true;
            setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton confirmButton = new JButton("Add adaptive parameter", UIUtils.getIconFromResources("actions/list-add.png"));
        confirmButton.addActionListener(e -> {
            canceled = false;
            setVisible(false);
        });
        buttonPanel.add(confirmButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void updateCurrentValue() {

        nameLabel.setText("<Please select one parameter>");
        nameIdLabel.setText("");
        typeLabel.setText("");
        typeIdLabel.setText("");
        typeDescriptionLabel.setText("");
        conditionValuePairs.clear();
        currentParameterAccess = null;
        rebuildConditionValuePairList();

        TreePath selectionPath = parameterTreeUI.getTreeComponent().getSelectionPath();
        if (selectionPath == null) {
            infoPanel.revalidate();
            infoPanel.repaint();
            return;
        }
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
        if (!(selectedNode.getUserObject() instanceof JIPipeParameterAccess)) {
            infoPanel.revalidate();
            infoPanel.repaint();
            return;
        }

        JIPipeParameterAccess parameterAccess = (JIPipeParameterAccess) selectedNode.getUserObject();
        JIPipeParameterTypeInfo typeInfo = JIPipe.getParameterTypes().getInfoByFieldClass(parameterAccess.getFieldClass());

        infoPanel.getParameterHelp().setDocument(ParameterPanel.generateParameterDocumentation(parameterAccess, parameterTree));

        // Load info
        currentParameterAccess = parameterAccess;
        nameLabel.setText(parameterAccess.getName());
        nameIdLabel.setText(parameterTree.getUniqueKey(parameterAccess));
        typeLabel.setText(typeInfo.getName());
        typeIdLabel.setText(typeInfo.getId());
        typeDescriptionLabel.setText(typeInfo.getDescription());

        infoPanel.revalidate();
        infoPanel.repaint();

        addNewConditionValuePair();
    }

    private void addNewConditionValuePair() {
        if(currentParameterAccess != null) {
            ConditionValuePair pair = new ConditionValuePair();
            pair.setValueType(currentParameterAccess.getFieldClass());
            pair.setValue(JIPipe.getParameterTypes().getInfoByFieldClass(currentParameterAccess.getFieldClass()).newInstance());
            conditionValuePairs.add(pair);
        }
    }

    private void rebuildConditionValuePairList() {

    }

    public boolean isCanceled() {
        return canceled;
    }

    public static class ConditionValuePair {
        private StringQueryExpression condition = new StringQueryExpression();
        private Object value;
        private Class<?> valueType;

        public StringQueryExpression getCondition() {
            return condition;
        }

        public void setCondition(StringQueryExpression condition) {
            this.condition = condition;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public Class<?> getValueType() {
            return valueType;
        }

        public void setValueType(Class<?> valueType) {
            this.valueType = valueType;
        }
    }
}
