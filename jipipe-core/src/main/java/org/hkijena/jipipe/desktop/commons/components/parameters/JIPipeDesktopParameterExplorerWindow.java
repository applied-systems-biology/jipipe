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

package org.hkijena.jipipe.desktop.commons.components.parameters;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterAccessTreeUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopReadonlyCopyableTextField;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.JIPipeDesktopSplitPane;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Objects;

public class JIPipeDesktopParameterExplorerWindow extends JFrame implements JIPipeParameterCollection.ParameterChangedEventListener {
    private final JIPipeDesktopWorkbench workbench;
    private final JIPipeParameterCollection parameterCollection;
    private final JIPipeParameterTree parameterTree;
    private final JPanel contentPanel = new JPanel(new BorderLayout());
    private final JPanel currentValuePanel = new JPanel(new BorderLayout());
    private final JPanel testerValuePanel = new JPanel(new BorderLayout());
    private JIPipeDesktopParameterAccessTreeUI parameterTreeUI;
    private JIPipeDesktopFormPanel formPanel;
    private JIPipeDesktopReadonlyCopyableTextField nameLabel;
    private JIPipeDesktopReadonlyCopyableTextField typeLabel;
    private JIPipeDesktopReadonlyCopyableTextField nameIdLabel;
    private JIPipeDesktopReadonlyCopyableTextField typeIdLabel;
    private JTextPane typeDescriptionLabel;
    private JIPipeParameterAccess currentValue;
    private JIPipeParameterAccess testerValue;
    private JIPipeDesktopReadonlyCopyableTextField currentValueJson;
    private JIPipeDesktopReadonlyCopyableTextField testerValueJson;

    public JIPipeDesktopParameterExplorerWindow(JIPipeDesktopWorkbench workbench, JIPipeParameterCollection parameterCollection) {
        this.workbench = workbench;
        this.parameterCollection = parameterCollection;
        this.parameterTree = new JIPipeParameterTree(parameterCollection);
        initialize();

        // JSON updater
        parameterCollection.getParameterChangedEventEmitter().subscribeWeak(this);

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
        setIconImage(UIUtils.getJIPipeIcon128());
        setTitle((parameterCollection instanceof JIPipeGraphNode) ? "Parameter explorer: " + ((JIPipeGraphNode) parameterCollection).getName() : "Parameter explorer");

        getContentPane().setLayout(new BorderLayout(8, 8));

        this.parameterTreeUI = new JIPipeDesktopParameterAccessTreeUI(parameterTree);

        JSplitPane splitPane = new JIPipeDesktopSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                parameterTreeUI,
                contentPanel, JIPipeDesktopSplitPane.RATIO_1_TO_3);
        getContentPane().add(splitPane, BorderLayout.CENTER);

        formPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.WITH_SCROLLING | JIPipeDesktopFormPanel.WITH_DOCUMENTATION | JIPipeDesktopFormPanel.DOCUMENTATION_BELOW);
        contentPanel.add(formPanel, BorderLayout.CENTER);

        initializeFormPanel();

        // Register event
        parameterTreeUI.getTreeComponent().addTreeSelectionListener(e -> updateCurrentValue());
    }

    private void initializeFormPanel() {
        nameLabel = new JIPipeDesktopReadonlyCopyableTextField("", false);
        nameIdLabel = new JIPipeDesktopReadonlyCopyableTextField("", true);
        typeLabel = new JIPipeDesktopReadonlyCopyableTextField("", false);
        typeIdLabel = new JIPipeDesktopReadonlyCopyableTextField("", true);
        typeDescriptionLabel = UIUtils.createReadonlyTextPane("");

        formPanel.addGroupHeader("General info", UIUtils.getIconFromResources("actions/help-info.png"));
        formPanel.addToForm(nameLabel, new JLabel("Name"), new MarkdownText("The name of the parameter"));
        formPanel.addToForm(nameIdLabel, new JLabel("Unique ID"), new MarkdownText("The unique ID of the parameter. This is used for " +
                "creating new parameter data."));
        formPanel.addToForm(typeLabel, new JLabel("Type name"), new MarkdownText("The name of the parameter type"));
        formPanel.addToForm(typeIdLabel, new JLabel("Unique type ID"), new MarkdownText("The unique ID of the data type the parameter is " +
                "based on. On creating parameters, you will have to ensure that this type matches exactly."));
        formPanel.addToForm(typeDescriptionLabel, new JLabel("Type description"), new MarkdownText("A short description of the parameter data type."));

        // Current value
        JIPipeDesktopFormPanel.GroupHeaderPanel editorHeader = formPanel.addGroupHeader("Current value", UIUtils.getIconFromResources("actions/edit.png"));

        JButton resetEditorButton = new JButton(UIUtils.getIconFromResources("actions/rabbitvcs-reset.png"));
        resetEditorButton.setToolTipText("Reset value");
        resetEditorButton.addActionListener(e -> resetCurrentValue());
        editorHeader.addToTitlePanel(resetEditorButton);

        JButton pasteCurrentValueJsonButton = new JButton(UIUtils.getIconFromResources("actions/edit-paste.png"));
        pasteCurrentValueJsonButton.setToolTipText("Paste JSON data. This will set the current parameter according to the pasted JSON data.");
        pasteCurrentValueJsonButton.addActionListener(e -> pasteCurrentValueJson());
        editorHeader.addToTitlePanel(pasteCurrentValueJsonButton);

        formPanel.addWideToForm(currentValuePanel, null);

        currentValueJson = new JIPipeDesktopReadonlyCopyableTextField("", true);
        formPanel.addToForm(currentValueJson, new JLabel("As JSON"), new MarkdownText("The current parameter value in JSON format. " +
                "This can be directly converted into parameter data."));

        // Value tester
        JIPipeDesktopFormPanel.GroupHeaderPanel valueTesterHeader = formPanel.addGroupHeader("Value tester", UIUtils.getIconFromResources("actions/testbench.png"));

        JButton resetTesterButton = new JButton(UIUtils.getIconFromResources("actions/rabbitvcs-reset.png"));
        resetTesterButton.setToolTipText("Reset value");
        resetTesterButton.addActionListener(e -> resetTesterValue());
        valueTesterHeader.addToTitlePanel(resetTesterButton);

        JButton pasteTesterValueJsonButton = new JButton(UIUtils.getIconFromResources("actions/edit-paste.png"));
        pasteTesterValueJsonButton.setToolTipText("Paste JSON data. This will set the tester value according to the pasted JSON data.");
        pasteTesterValueJsonButton.addActionListener(e -> pasteTesterValueJson());
        valueTesterHeader.addToTitlePanel(pasteTesterValueJsonButton);

        JButton copyCurrentValueButton = new JButton(UIUtils.getIconFromResources("actions/down.png"));
        copyCurrentValueButton.addActionListener(e -> copyCurrentValueIntoTester());
        copyCurrentValueButton.setToolTipText("Copy current value into the tester");
        valueTesterHeader.addToTitlePanel(copyCurrentValueButton);

        JButton writeCurrentValueButton = new JButton(UIUtils.getIconFromResources("actions/up.png"));
        writeCurrentValueButton.addActionListener(e -> writeTesterValueIntoCurrent());
        writeCurrentValueButton.setToolTipText("Write into current value");
        valueTesterHeader.addToTitlePanel(writeCurrentValueButton);

        formPanel.addWideToForm(testerValuePanel, null);

        testerValueJson = new JIPipeDesktopReadonlyCopyableTextField("", true);
        formPanel.addToForm(testerValueJson, new JLabel("As JSON"), new MarkdownText("The current tester value in JSON format. " +
                "This can be directly converted into parameter data."));

        formPanel.addVerticalGlue();
    }

    private void pasteTesterValueJson() {
        String clipboard = UIUtils.getStringFromClipboard();
        if (StringUtils.isNullOrEmpty(clipboard)) {
            JOptionPane.showMessageDialog(this, "The clipboard is empty!", "Paste JSON", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            Object data = JsonUtils.getObjectMapper().readerFor(currentValue.getFieldClass()).readValue(clipboard);
            testerValue.set(data);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "The pasted data is not valid for this parameter!\n"
                    + e.getMessage(), "Paste JSON", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void pasteCurrentValueJson() {
        String clipboard = UIUtils.getStringFromClipboard();
        if (StringUtils.isNullOrEmpty(clipboard)) {
            JOptionPane.showMessageDialog(this, "The clipboard is empty!", "Paste JSON", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            Object data = JsonUtils.getObjectMapper().readerFor(currentValue.getFieldClass()).readValue(clipboard);
            currentValue.set(data);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "The pasted data is not valid for this parameter!\n"
                    + e.getMessage(), "Paste JSON", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void updateCurrentValueJson() {
        try {
            String valueAsString = JsonUtils.getObjectMapper().writeValueAsString(currentValue.get(Object.class));
            currentValueJson.setText(valueAsString);
        } catch (Exception e) {
        }
    }

    private void updateTesterValueJson() {
        try {
            String valueAsString = JsonUtils.getObjectMapper().writeValueAsString(testerValue.get(Object.class));
            testerValueJson.setText(valueAsString);
        } catch (Exception e) {
        }
    }

    private void writeTesterValueIntoCurrent() {
        if (currentValue == null)
            return;
        JIPipeParameterTypeInfo typeInfo = JIPipe.getParameterTypes().getInfoByFieldClass(currentValue.getFieldClass());
        currentValue.set(typeInfo.duplicate(testerValue.get(Object.class)));
    }

    private void copyCurrentValueIntoTester() {
        if (currentValue == null)
            return;
        JIPipeParameterTypeInfo typeInfo = JIPipe.getParameterTypes().getInfoByFieldClass(currentValue.getFieldClass());
        testerValue.set(typeInfo.duplicate(currentValue.get(Object.class)));
    }

    private void resetTesterValue() {
        if (currentValue == null)
            return;
        JIPipeParameterTypeInfo typeInfo = JIPipe.getParameterTypes().getInfoByFieldClass(currentValue.getFieldClass());
        testerValue.set(typeInfo.newInstance());
    }

    private void resetCurrentValue() {
        if (currentValue == null)
            return;
        if (parameterCollection instanceof JIPipeGraphNode) {
            JIPipeGraphNode newNode = JIPipe.createNode(((JIPipeGraphNode) parameterCollection).getClass());
            JIPipeParameterTree tempTree = new JIPipeParameterTree(newNode);
            currentValue.set(tempTree.getParameters().get(parameterTree.getUniqueKey(currentValue)).get(Object.class));
        } else {
            JIPipeParameterTypeInfo typeInfo = JIPipe.getParameterTypes().getInfoByFieldClass(currentValue.getFieldClass());
            currentValue.set(typeInfo.newInstance());
        }
    }

    private void updateCurrentValue() {

        currentValuePanel.removeAll();
        testerValuePanel.removeAll();

        currentValue = null;
        nameLabel.setText("<Please select one parameter>");
        nameIdLabel.setText("");
        typeLabel.setText("");
        typeIdLabel.setText("");
        typeDescriptionLabel.setText("");
        currentValueJson.setText("");
        testerValueJson.setText("");

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

        formPanel.getHelpPanel().showContent(JIPipeDesktopParameterFormPanel.generateParameterDocumentation(parameterAccess, parameterTree));

        // Set current value & tester
        currentValue = parameterAccess;
        testerValue = JIPipeManualParameterAccess.builder()
                .dummyAccess(typeInfo.getFieldClass())
                .setAnnotationSupplier(parameterAccess::getAnnotationOfType)
                .setName(parameterAccess.getName())
                .build();
        testerValue.set(typeInfo.newInstance());

        // JSON updater
        testerValue.getSource().getParameterChangedEventEmitter().subscribeWeak(this);

        // Set editors
        currentValuePanel.add(JIPipe.getParameterTypes().createEditorFor(workbench, parameterTree, parameterAccess), BorderLayout.CENTER);
        testerValuePanel.add(JIPipe.getParameterTypes().createEditorFor(workbench, parameterTree, testerValue), BorderLayout.CENTER);

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

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if (event.getEmitter() == parameterCollection.getParameterChangedEventEmitter()) {
            if (currentValue != null && Objects.equals(event.getKey(), currentValue.getKey())) {
                updateCurrentValueJson();
            }
        } else if (event.getEmitter() == testerValue.getSource().getParameterChangedEventEmitter()) {
            if (testerValue != null) {
                updateTesterValueJson();
            }
        }
    }
}
