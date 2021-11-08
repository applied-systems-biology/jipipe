package org.hkijena.jipipe.ui.parameters;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeManualParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.components.ParameterTreeUI;
import org.hkijena.jipipe.ui.components.ReadonlyCopyableTextField;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Objects;

public class AdaptiveParameterBuilder extends JDialog {
    private final JIPipeWorkbench workbench;
    private final JIPipeParameterCollection parameterCollection;
    private final JIPipeParameterTree parameterTree;
    private ParameterTreeUI parameterTreeUI;
    private JPanel contentPanel = new JPanel(new BorderLayout());
    private FormPanel formPanel;
    private ReadonlyCopyableTextField nameLabel;
    private ReadonlyCopyableTextField typeLabel;
    private ReadonlyCopyableTextField nameIdLabel;
    private ReadonlyCopyableTextField typeIdLabel;
    private JTextPane typeDescriptionLabel;
    private JPanel currentValuePanel = new JPanel(new BorderLayout());
    private JPanel testerValuePanel = new JPanel(new BorderLayout());
    private boolean canceled = true;

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

        formPanel = new FormPanel(null, FormPanel.WITH_SCROLLING | FormPanel.WITH_DOCUMENTATION | FormPanel.DOCUMENTATION_BELOW);
        DocumentTabPane documentTabPane = new DocumentTabPane();
        documentTabPane.addTab("Info", UIUtils.getIconFromResources("actions/help-info.png"), formPanel, DocumentTabPane.CloseMode.withoutCloseButton);
        contentPanel.add(documentTabPane, BorderLayout.CENTER);

        initializeFormPanel();
        initializeButtonPanel();

        // Register event
        parameterTreeUI.getTreeComponent().addTreeSelectionListener(e -> updateCurrentValue());
    }

    private void initializeFormPanel() {
        nameLabel = new ReadonlyCopyableTextField("", false);
        nameIdLabel = new ReadonlyCopyableTextField("", true);
        typeLabel = new ReadonlyCopyableTextField("", false);
        typeIdLabel = new ReadonlyCopyableTextField("", true);
        typeDescriptionLabel = UIUtils.makeReadonlyTextPane("");

        formPanel.addToForm(nameLabel, new JLabel("Name"), new MarkdownDocument("The name of the parameter"));
        formPanel.addToForm(nameIdLabel, new JLabel("Unique ID"), new MarkdownDocument("The unique ID of the parameter. This is used for " +
                "creating new parameter data."));
        formPanel.addToForm(typeLabel, new JLabel("Type name"), new MarkdownDocument("The name of the parameter type"));
        formPanel.addToForm(typeIdLabel, new JLabel("Unique type ID"), new MarkdownDocument("The unique ID of the data type the parameter is " +
                "based on. On creating parameters, you will have to ensure that this type matches exactly."));
        formPanel.addToForm(typeDescriptionLabel, new JLabel("Type description"), new MarkdownDocument("A short description of the parameter data type."));
        formPanel.addVerticalGlue();
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

        JButton confirmButton = new JButton("Add", UIUtils.getIconFromResources("actions/list-add.png"));
        confirmButton.addActionListener(e -> {
            canceled = false;
            setVisible(false);
        });
        buttonPanel.add(confirmButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void updateCurrentValue() {

        currentValuePanel.removeAll();
        testerValuePanel.removeAll();

        nameLabel.setText("<Please select one parameter>");
        nameIdLabel.setText("");
        typeLabel.setText("");
        typeIdLabel.setText("");
        typeDescriptionLabel.setText("");

        TreePath selectionPath = parameterTreeUI.getTreeComponent().getSelectionPath();
        if (selectionPath == null) {
            formPanel.revalidate();
            formPanel.repaint();
            return;
        }
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
        if (!(selectedNode.getUserObject() instanceof JIPipeParameterAccess)) {
            formPanel.revalidate();
            formPanel.repaint();
            return;
        }

        JIPipeParameterAccess parameterAccess = (JIPipeParameterAccess) selectedNode.getUserObject();
        JIPipeParameterTypeInfo typeInfo = JIPipe.getParameterTypes().getInfoByFieldClass(parameterAccess.getFieldClass());

        formPanel.getParameterHelp().setDocument(ParameterPanel.generateParameterDocumentation(parameterAccess, parameterTree));

        // Load info
        nameLabel.setText(parameterAccess.getName());
        nameIdLabel.setText(parameterTree.getUniqueKey(parameterAccess));
        typeLabel.setText(typeInfo.getName());
        typeIdLabel.setText(typeInfo.getId());
        typeDescriptionLabel.setText(typeInfo.getDescription());

        formPanel.revalidate();
        formPanel.repaint();
    }
}
