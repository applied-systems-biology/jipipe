package org.hkijena.jipipe.ui.parameters;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeAdaptiveParameterSettings;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeDummyParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeManualParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.components.ParameterTreeUI;
import org.hkijena.jipipe.ui.components.ReadonlyCopyableTextField;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.hkijena.jipipe.utils.scripting.MacroUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdaptiveParameterBuilder extends JDialog {
    private final JIPipeWorkbench workbench;
    private final JIPipeParameterCollection parameterCollection;
    private final JIPipeParameterTree parameterTree;
    private final JPanel contentPanel = new JPanel(new BorderLayout());
    private ParameterTreeUI parameterTreeUI;
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

    public JIPipeParameterAccess getCurrentParameterAccess() {
        return currentParameterAccess;
    }

    public JIPipeParameterTree getParameterTree() {
        return parameterTree;
    }

    private void initialize() {
        setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        setTitle((parameterCollection instanceof JIPipeGraphNode) ? "Add adaptive parameter: " + ((JIPipeGraphNode) parameterCollection).getName() : "Add adaptive parameter");

        getContentPane().setLayout(new BorderLayout(8, 8));

        this.parameterTreeUI = new ParameterTreeUI(parameterTree);

        JSplitPane splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                parameterTreeUI,
                contentPanel, AutoResizeSplitPane.RATIO_1_TO_3);
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
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 8));
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
        if (currentParameterAccess != null) {
            ConditionValuePair pair = new ConditionValuePair();
            pair.setOriginalAccess(currentParameterAccess);
            pair.setCondition(new DefaultExpressionParameter("true"));
            pair.setValueType(currentParameterAccess.getFieldClass());
            pair.setValue(JIPipe.getParameterTypes().getInfoByFieldClass(currentParameterAccess.getFieldClass()).newInstance());
            conditionValuePairs.add(pair);
            rebuildConditionValuePairList();
        }
    }

    private void rebuildConditionValuePairList() {
        listPanel.clear();
        for (ConditionValuePair conditionValuePair : conditionValuePairs) {
            listPanel.addWideToForm(new ConditionValuePairUI(this, conditionValuePair), null);
        }
        listPanel.addVerticalGlue();
        revalidate();
        repaint();
    }

    private void moveUp(ConditionValuePairUI conditionValuePairUI) {
        int i = conditionValuePairs.indexOf(conditionValuePairUI.conditionValuePair);
        if (i > 0) {
            ConditionValuePair other = conditionValuePairs.get(i - 1);
            conditionValuePairs.set(i, other);
            conditionValuePairs.set(i - 1, conditionValuePairUI.conditionValuePair);
            rebuildConditionValuePairList();
        }
    }

    private void delete(ConditionValuePairUI conditionValuePairUI) {
        conditionValuePairs.remove(conditionValuePairUI.conditionValuePair);
        rebuildConditionValuePairList();
    }

    private void moveDown(ConditionValuePairUI conditionValuePairUI) {
        int i = conditionValuePairs.indexOf(conditionValuePairUI.conditionValuePair);
        if (i >= 0 && i < (conditionValuePairs.size() - 1)) {
            ConditionValuePair other = conditionValuePairs.get(i + 1);
            conditionValuePairs.set(i, other);
            conditionValuePairs.set(i + 1, conditionValuePairUI.conditionValuePair);
            rebuildConditionValuePairList();
        }
    }

    public DefaultExpressionParameter build() {
        StringBuilder builder = new StringBuilder();
        if (conditionValuePairs.isEmpty()) {
            builder.append("default");
        } else {
            builder.append("SWITCH(");
            for (int i = 0; i < conditionValuePairs.size(); i++) {
                ConditionValuePair conditionValuePair = conditionValuePairs.get(i);
                if (i != 0) {
                    builder.append(", ");
                }
                builder.append("CASE(");
                builder.append(conditionValuePair.condition.getExpression());
                builder.append(", ");
                builder.append(MacroUtils.escapeString(JsonUtils.toJsonString(conditionValuePair.value)));
                builder.append(")");
            }
            builder.append(", ");
            builder.append("CASE(true, default)");
            builder.append(")");
        }
        return new DefaultExpressionParameter(builder.toString());
    }

    public boolean isCanceled() {
        return canceled;
    }

    public static class ConditionValuePair {
        private JIPipeParameterAccess originalAccess;
        private DefaultExpressionParameter condition = new StringQueryExpression();
        private Object value;
        private Class<?> valueType;

        public DefaultExpressionParameter getCondition() {
            return condition;
        }

        public void setCondition(DefaultExpressionParameter condition) {
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

        public JIPipeParameterAccess getOriginalAccess() {
            return originalAccess;
        }

        public void setOriginalAccess(JIPipeParameterAccess originalAccess) {
            this.originalAccess = originalAccess;
        }
    }

    public static class ConditionValuePairUI extends JPanel {
        private final AdaptiveParameterBuilder adaptiveParameterBuilder;
        private final ConditionValuePair conditionValuePair;

        public ConditionValuePairUI(AdaptiveParameterBuilder adaptiveParameterBuilder, ConditionValuePair conditionValuePair) {
            this.adaptiveParameterBuilder = adaptiveParameterBuilder;
            this.conditionValuePair = conditionValuePair;
            initialize();
        }

        private void initialize() {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createLineBorder(Color.GRAY));
            JToolBar toolBar = new JToolBar();
            toolBar.setFloatable(false);

            JButton moveUpButton = new JButton(UIUtils.getIconFromResources("actions/arrow-up.png"));
            UIUtils.makeFlat25x25(moveUpButton);
            moveUpButton.addActionListener(e -> moveUp());
            toolBar.add(moveUpButton);

            JButton moveDownButton = new JButton(UIUtils.getIconFromResources("actions/arrow-down.png"));
            UIUtils.makeFlat25x25(moveDownButton);
            moveDownButton.addActionListener(e -> moveDown());
            toolBar.add(moveDownButton);

            toolBar.add(Box.createHorizontalGlue());

            JButton deleteButton = new JButton(UIUtils.getIconFromResources("actions/close-tab.png"));
            UIUtils.makeFlat25x25(deleteButton);
            deleteButton.addActionListener(e -> delete());
            toolBar.add(deleteButton);

            add(toolBar, BorderLayout.NORTH);

            FormPanel formPanel = new FormPanel(null, FormPanel.NONE);

            JIPipeManualParameterAccess conditionAccess = JIPipeManualParameterAccess.builder().setSource(new JIPipeDummyParameterCollection())
                    .setFieldClass(DefaultExpressionParameter.class)
                    .addAnnotation(new ExpressionParameterSettings() {

                        @Override
                        public Class<? extends Annotation> annotationType() {
                            return ExpressionParameterSettings.class;
                        }

                        @Override
                        public Class<? extends ExpressionParameterVariableSource> variableSource() {
                            return JIPipeAdaptiveParameterSettings.VariableSource.class;
                        }
                    })
                    .setKey("condition")
                    .setGetter(conditionValuePair::getCondition)
                    .setSetter(conditionValuePair::setCondition).build();
            JIPipeManualParameterAccess.Builder builder = JIPipeManualParameterAccess.builder().setSource(new JIPipeDummyParameterCollection())
                    .setFieldClass(conditionValuePair.getValueType())
                    .setKey("value")
                    .setGetter(conditionValuePair::getValue)
                    .setSetter(conditionValuePair::setValue);
            if (conditionValuePair.getOriginalAccess() != null) {
                for (Annotation annotation : conditionValuePair.getOriginalAccess().getAnnotations()) {
                    builder.addAnnotation(annotation);
                }
            }
            JIPipeManualParameterAccess valueAccess = builder.build();
            formPanel.addToForm(JIPipe.getParameterTypes().createEditorFor(adaptiveParameterBuilder.workbench, conditionAccess),
                    new JLabel("Condition"), null);
            formPanel.addToForm(JIPipe.getParameterTypes().createEditorFor(adaptiveParameterBuilder.workbench, valueAccess),
                    new JLabel("Value"), null);

            add(formPanel, BorderLayout.CENTER);
        }

        private void moveDown() {
            adaptiveParameterBuilder.moveDown(this);
        }

        private void delete() {
            adaptiveParameterBuilder.delete(this);
        }

        private void moveUp() {
            adaptiveParameterBuilder.moveUp(this);
        }
    }
}
