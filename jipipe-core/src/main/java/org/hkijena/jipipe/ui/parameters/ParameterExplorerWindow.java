package org.hkijena.jipipe.ui.parameters;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.components.ParameterTreeUI;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Objects;

public class ParameterExplorerWindow extends JFrame {
    private final JIPipeWorkbench workbench;
    private final JIPipeParameterCollection parameterCollection;
    private final JIPipeParameterTree parameterTree;
    private ParameterTreeUI parameterTreeUI;
    private JPanel contentPanel = new JPanel(new BorderLayout());
    private FormPanel formPanel;
    private JTextField nameLabel;
    private JTextField typeLabel;
    private JTextField nameIdLabel;
    private JTextField typeIdLabel;
    private JTextPane typeDescriptionLabel;
    private JIPipeParameterAccess currentValue;
    private JIPipeParameterAccess testerValue;
    private JPanel currentValuePanel = new JPanel(new BorderLayout());
    private JPanel testerValuePanel = new JPanel(new BorderLayout());
    private JTextField currentValueJson;
    private JTextField testerValueJson;

    public ParameterExplorerWindow(JIPipeWorkbench workbench, JIPipeParameterCollection parameterCollection) {
        this.workbench = workbench;
        this.parameterCollection = parameterCollection;
        this.parameterTree = new JIPipeParameterTree(parameterCollection);
        initialize();

        // JSON updater
        parameterCollection.getEventBus().register(new Object() {
            @Subscribe
            public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
                if(currentValue != null && Objects.equals(event.getKey(), currentValue.getKey())) {
                    updateCurrentValueJson();
                }
            }
        });

        // Select the first entry
        JTree treeComponent = parameterTreeUI.getTreeComponent();
        DefaultTreeModel model = (DefaultTreeModel) treeComponent.getModel();
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) model.getRoot();
        if(rootNode.getChildCount() > 0) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) rootNode.getChildAt(0);
            treeComponent.setSelectionPath(new TreePath(child.getPath()));
        }
    }

    private void initialize() {
        setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        setTitle((parameterCollection instanceof JIPipeGraphNode) ? "Parameter explorer: " + ((JIPipeGraphNode) parameterCollection).getName() : "Parameter explorer");

        getContentPane().setLayout(new BorderLayout(8,8));

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
        contentPanel.add(formPanel, BorderLayout.CENTER);

        initializeFormPanel();

        // Register event
        parameterTreeUI.getTreeComponent().addTreeSelectionListener(e -> updateCurrentValue());
    }

    private void initializeFormPanel() {
        nameLabel = UIUtils.makeReadonlyTextField("");
        nameIdLabel = UIUtils.makeReadonlyTextField("");
        nameIdLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        typeLabel = UIUtils.makeReadonlyTextField("");
        typeIdLabel = UIUtils.makeReadonlyTextField("");
        typeIdLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        typeDescriptionLabel = UIUtils.makeReadonlyTextPane("");

        formPanel.addGroupHeader("General info", UIUtils.getIconFromResources("actions/help-info.png"));
        formPanel.addToForm(nameLabel, new JLabel("Name"), new MarkdownDocument("The name of the parameter"));
        formPanel.addToForm(nameIdLabel, new JLabel("Unique ID"), new MarkdownDocument("The unique ID of the parameter. This is used for " +
                "creating new parameter data."));
        formPanel.addToForm(typeLabel, new JLabel("Type name"), new MarkdownDocument("The name of the parameter type"));
        formPanel.addToForm(typeIdLabel, new JLabel("Unique type ID"), new MarkdownDocument("The unique ID of the data type the parameter is " +
                "based on. On creating parameters, you will have to ensure that this type matches exactly."));
        formPanel.addToForm(typeDescriptionLabel, new JLabel("Type description"), new MarkdownDocument("A short description of the parameter data type."));

        // Current value
        FormPanel.GroupHeaderPanel editorHeader = formPanel.addGroupHeader("Current value", UIUtils.getIconFromResources("actions/edit.png"));

        JButton resetEditorButton = new JButton(UIUtils.getIconFromResources("actions/rabbitvcs-reset.png"));
        resetEditorButton.setToolTipText("Reset value");
        resetEditorButton.addActionListener(e -> resetCurrentValue());
        editorHeader.addColumn(resetEditorButton);

        JButton pasteCurrentValueJsonButton = new JButton(UIUtils.getIconFromResources("actions/edit-paste.png"));
        pasteCurrentValueJsonButton.setToolTipText("Paste JSON data. This will set the current parameter according to the pasted JSON data.");
        pasteCurrentValueJsonButton.addActionListener(e -> pasteCurrentValueJson());
        editorHeader.addColumn(pasteCurrentValueJsonButton);

        formPanel.addWideToForm(currentValuePanel, null);

        currentValueJson = UIUtils.makeReadonlyTextField("");
        currentValueJson.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JPanel currentValueJsonPanel = new JPanel(new BorderLayout());
        currentValueJsonPanel.add(currentValueJson, BorderLayout.CENTER);
        JButton copyCurrentValueJsonButton = new JButton(UIUtils.getIconFromResources("actions/edit-copy.png"));
        copyCurrentValueJsonButton.setToolTipText("Copy current value");
        copyCurrentValueJsonButton.addActionListener(e -> copyCurrentValueJson());
        currentValueJsonPanel.add(copyCurrentValueJsonButton, BorderLayout.EAST);
        formPanel.addToForm(currentValueJsonPanel, new JLabel("As JSON"), new MarkdownDocument("The current parameter value in JSON format. " +
                "This can be directly converted into parameter data."));

        // Value tester
        FormPanel.GroupHeaderPanel valueTesterHeader = formPanel.addGroupHeader("Value tester", UIUtils.getIconFromResources("actions/testbench.png"));

        JButton resetTesterButton = new JButton(UIUtils.getIconFromResources("actions/rabbitvcs-reset.png"));
        resetTesterButton.setToolTipText("Reset value");
        resetTesterButton.addActionListener(e -> resetTesterValue());
        valueTesterHeader.addColumn(resetTesterButton);

        JButton pasteTesterValueJsonButton = new JButton(UIUtils.getIconFromResources("actions/edit-paste.png"));
        pasteTesterValueJsonButton.setToolTipText("Paste JSON data. This will set the tester value according to the pasted JSON data.");
        pasteTesterValueJsonButton.addActionListener(e -> pasteTesterValueJson());
        valueTesterHeader.addColumn(pasteTesterValueJsonButton);

        JButton copyCurrentValueButton = new JButton(UIUtils.getIconFromResources("actions/down.png"));
        copyCurrentValueButton.addActionListener(e -> copyCurrentValueIntoTester());
        copyCurrentValueButton.setToolTipText("Copy current value into the tester");
        valueTesterHeader.addColumn(copyCurrentValueButton);

        JButton writeCurrentValueButton = new JButton(UIUtils.getIconFromResources("actions/up.png"));
        writeCurrentValueButton.addActionListener(e -> writeTesterValueIntoCurrent());
        writeCurrentValueButton.setToolTipText("Write into current value");
        valueTesterHeader.addColumn(writeCurrentValueButton);

        formPanel.addWideToForm(testerValuePanel, null);

        testerValueJson = UIUtils.makeReadonlyTextField("");
        testerValueJson.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JPanel testerValueJsonPanel = new JPanel(new BorderLayout());
        testerValueJsonPanel.add(testerValueJson, BorderLayout.CENTER);
        JButton copyTesterValueJsonButton = new JButton(UIUtils.getIconFromResources("actions/edit-copy.png"));
        copyTesterValueJsonButton.setToolTipText("Copy current value");
        copyTesterValueJsonButton.addActionListener(e -> copyTesterValueJson());
        testerValueJsonPanel.add(copyTesterValueJsonButton, BorderLayout.EAST);
        formPanel.addToForm(testerValueJsonPanel, new JLabel("As JSON"), new MarkdownDocument("The current tester value in JSON format. " +
                "This can be directly converted into parameter data."));

        formPanel.addVerticalGlue();
    }

    private void pasteTesterValueJson() {
        String clipboard = UIUtils.getStringFromClipboard();
        if(StringUtils.isNullOrEmpty(clipboard)) {
            JOptionPane.showMessageDialog(this, "The clipboard is empty!", "Paste JSON", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            Object data = JsonUtils.getObjectMapper().readerFor(currentValue.getFieldClass()).readValue(clipboard);
            testerValue.set(data);
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(this, "The pasted data is not valid for this parameter!\n"
                    +e.getMessage(), "Paste JSON", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void pasteCurrentValueJson() {
        String clipboard = UIUtils.getStringFromClipboard();
        if(StringUtils.isNullOrEmpty(clipboard)) {
            JOptionPane.showMessageDialog(this, "The clipboard is empty!", "Paste JSON", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            Object data = JsonUtils.getObjectMapper().readerFor(currentValue.getFieldClass()).readValue(clipboard);
            currentValue.set(data);
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(this, "The pasted data is not valid for this parameter!\n"
                    +e.getMessage(), "Paste JSON", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void copyTesterValueJson() {
        StringSelection selection = new StringSelection(testerValueJson.getText());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }

    private void copyCurrentValueJson() {
        StringSelection selection = new StringSelection(currentValueJson.getText());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }

    private void updateCurrentValueJson() {
        try {
            String valueAsString = JsonUtils.getObjectMapper().writeValueAsString(currentValue.get(Object.class));
            currentValueJson.setText(valueAsString);
        }
        catch (Exception e) {

        }
    }

    private void updateTesterValueJson() {
        try {
            String valueAsString = JsonUtils.getObjectMapper().writeValueAsString(testerValue.get(Object.class));
            testerValueJson.setText(valueAsString);
        }
        catch (Exception e) {

        }
    }

    private void writeTesterValueIntoCurrent() {
        if(currentValue == null)
            return;
        JIPipeParameterTypeInfo typeInfo = JIPipe.getParameterTypes().getInfoByFieldClass(currentValue.getFieldClass());
        currentValue.set(typeInfo.duplicate(testerValue.get(Object.class)));
    }

    private void copyCurrentValueIntoTester() {
        if(currentValue == null)
            return;
        JIPipeParameterTypeInfo typeInfo = JIPipe.getParameterTypes().getInfoByFieldClass(currentValue.getFieldClass());
        testerValue.set(typeInfo.duplicate(currentValue.get(Object.class)));
    }

    private void resetTesterValue() {
        if(currentValue == null)
            return;
        JIPipeParameterTypeInfo typeInfo = JIPipe.getParameterTypes().getInfoByFieldClass(currentValue.getFieldClass());
        testerValue.set(typeInfo.newInstance());
    }

    private void resetCurrentValue() {
        if(currentValue == null)
            return;
        if(parameterCollection instanceof JIPipeGraphNode) {
            JIPipeGraphNode newNode = JIPipe.createNode(((JIPipeGraphNode)parameterCollection).getClass());
            JIPipeParameterTree tempTree = new JIPipeParameterTree(newNode);
            currentValue.set(tempTree.getParameters().get(parameterTree.getUniqueKey(currentValue)).get(Object.class));
        }
        else {
            JIPipeParameterTypeInfo typeInfo = JIPipe.getParameterTypes().getInfoByFieldClass(currentValue.getFieldClass());
            currentValue.set(typeInfo.newInstance());
        }
    }

    private void updateCurrentValue() {

        currentValuePanel.removeAll();
        testerValuePanel.removeAll();

        currentValue = null;
        nameLabel.setText("<Please select one parameter>");
        typeLabel.setText("");

        TreePath selectionPath = parameterTreeUI.getTreeComponent().getSelectionPath();
        if(selectionPath == null) {
            formPanel.revalidate();
            formPanel.repaint();
            return;
        }
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
        if(!(selectedNode.getUserObject() instanceof JIPipeParameterAccess)) {
            formPanel.revalidate();
            formPanel.repaint();
            return;
        }

        JIPipeParameterAccess parameterAccess = (JIPipeParameterAccess) selectedNode.getUserObject();
        JIPipeParameterTypeInfo typeInfo = JIPipe.getParameterTypes().getInfoByFieldClass(parameterAccess.getFieldClass());

        formPanel.getParameterHelp().setDocument(ParameterPanel.generateParameterDocumentation(parameterAccess));

        // Set current value & tester
        currentValue = parameterAccess;
        testerValue = JIPipeManualParameterAccess.builder()
                .dummyAccess(typeInfo.getFieldClass())
                .setName(parameterAccess.getName())
                .build();
        testerValue.set(typeInfo.newInstance());

        // JSON updater
        testerValue.getSource().getEventBus().register(new Object() {
            @Subscribe
            public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
                if(testerValue != null) {
                    updateTesterValueJson();
                }
            }
        });

        // Set editors
        currentValuePanel.add(JIPipe.getParameterTypes().createEditorFor(workbench, parameterAccess), BorderLayout.CENTER);
        testerValuePanel.add(JIPipe.getParameterTypes().createEditorFor(workbench, testerValue), BorderLayout.CENTER);

       // Load info
        nameLabel.setText(parameterAccess.getName());
        nameIdLabel.setText(parameterTree.getUniqueKey(parameterAccess));
        typeLabel.setText(typeInfo.getName());
        typeIdLabel.setText(typeInfo.getId());
        typeDescriptionLabel.setText(typeInfo.getDescription());

        updateCurrentValueJson();
        updateTesterValueJson();

        formPanel.revalidate();
        formPanel.repaint();
    }
}
