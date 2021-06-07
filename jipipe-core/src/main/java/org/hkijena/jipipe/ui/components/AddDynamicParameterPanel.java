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

package org.hkijena.jipipe.ui.components;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeMutableParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * UI that adds slots to an algorithm
 */
public class AddDynamicParameterPanel extends JPanel {

    /**
     * Remember the type selected last for increased usability
     */
    private static JIPipeParameterTypeInfo lastSelectedType = null;
    private JIPipeDynamicParameterCollection parameterCollection;
    private SearchTextField searchField;
    private JList<JIPipeParameterTypeInfo> datatypeList;
    private JTextField keyEditor;
    private JIPipeParameterTypeInfo selectedInfo;
    private JButton confirmButton;
    private JDialog dialog;
    private Set<JIPipeParameterTypeInfo> availableTypes;
    private JTextArea descriptionEditor;
    private JXTextField nameEditor;

    /**
     * @param parameterCollection the parameter collection
     */
    public AddDynamicParameterPanel(JIPipeDynamicParameterCollection parameterCollection) {
        this.parameterCollection = parameterCollection;
        this.availableTypes = parameterCollection.getAllowedTypes().stream().map(x ->
                JIPipe.getParameterTypes().getInfoByFieldClass(x)).collect(Collectors.toSet());
        initialize();
        reloadTypeList();
        setInitialName();
        if (lastSelectedType != null && availableTypes.contains(lastSelectedType)) {
            datatypeList.setSelectedValue(lastSelectedType, true);
            keyEditor.requestFocusInWindow();
            keyEditor.selectAll();
        }
        if (availableTypes.size() == 1) {
            keyEditor.requestFocusInWindow();
            keyEditor.selectAll();
        }
    }

    /**
     * Shows a dialog for adding slots
     *
     * @param parent parent component
     */
    public static void showDialog(Component parent, JIPipeDynamicParameterCollection parameterCollection) {
        JDialog dialog = new JDialog();
        AddDynamicParameterPanel panel = new AddDynamicParameterPanel(parameterCollection);
        panel.setDialog(dialog);
        dialog.setContentPane(panel);
        dialog.setTitle("Add parameter");
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(new Dimension(400, 300));
        dialog.setLocationRelativeTo(parent);
        UIUtils.addEscapeListener(dialog);
        dialog.setVisible(true);
    }

    private void setInitialName() {
        keyEditor.setText(StringUtils.makeUniqueString("Parameter", " ", parameterCollection.getParameters().keySet()));
    }

    private void initialize() {
        setLayout(new BorderLayout());
        initializeToolBar();

        datatypeList = new JList<>();
        datatypeList.setCellRenderer(new JIPipeParameterTypeInfoListCellRenderer());
        datatypeList.addListSelectionListener(e -> {
            if (datatypeList.getSelectedValue() != null) {
                setSelectedInfo(datatypeList.getSelectedValue());
            }
        });
        JScrollPane scrollPane = new JScrollPane(datatypeList);
        add(scrollPane, BorderLayout.CENTER);

        // Create form located at the bottom
        FormPanel formPanel = new FormPanel(null, FormPanel.NONE);
        keyEditor = new JXTextField("Identifier");
        keyEditor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        keyEditor.setToolTipText("A unique identifier for the parameter");
        keyEditor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (canAddParameter() && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    confirmButton.requestFocusInWindow();
                }
            }
        });
        keyEditor.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                checkKeyEditor();
            }
        });
        formPanel.addToForm(keyEditor, new JLabel("Unique identifier"), null);

        nameEditor = new JXTextField("Name");
        formPanel.addToForm(nameEditor, new JLabel("Name"), null);

        descriptionEditor = new JTextArea();
        descriptionEditor.setBorder(BorderFactory.createEtchedBorder());
        formPanel.addToForm(descriptionEditor, new JLabel("Description"), null);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.add(formPanel);
        bottomPanel.add(initializeButtonPanel());
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void checkKeyEditor() {
        if (canAddParameter()) {
            keyEditor.setBorder(BorderFactory.createEtchedBorder());
        } else {
            keyEditor.setBorder(BorderFactory.createLineBorder(Color.RED));
        }
    }

    private JPanel initializeButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            if (dialog != null) {
                dialog.setVisible(false);
            }
        });
        buttonPanel.add(cancelButton);

        confirmButton = new JButton("Add", UIUtils.getIconFromResources("actions/list-add.png"));
        confirmButton.addActionListener(e -> addParameter());
        confirmButton.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                addParameter();
            }
        });
        buttonPanel.add(confirmButton);

        return buttonPanel;
    }

    private void addParameter() {
        if (!canAddParameter())
            return;

        String key = keyEditor.getText().trim();
        String name = StringUtils.orElse(nameEditor.getText(), key);
        String description = descriptionEditor.getText();

        JIPipeMutableParameterAccess access = parameterCollection.addParameter(key, selectedInfo.getFieldClass());
        access.setName(name);
        access.setDescription(description);

        lastSelectedType = selectedInfo;

        if (dialog != null)
            dialog.setVisible(false);
    }

    private boolean canAddParameter() {
        if (selectedInfo == null)
            return false;
        String slotName = keyEditor.getText();
        if (slotName == null || slotName.isEmpty()) {
            keyEditor.setToolTipText("Name is empty!");
            return false;
        }
        keyEditor.setToolTipText(null);
        return true;
    }

    private void initializeToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(Box.createHorizontalStrut(16));
        searchField = new SearchTextField();
        searchField.addActionListener(e -> reloadTypeList());
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (selectedInfo != null && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    keyEditor.requestFocusInWindow();
                    keyEditor.selectAll();
                }
            }
        });
        toolBar.add(searchField);

        add(toolBar, BorderLayout.NORTH);
    }

    private List<JIPipeParameterTypeInfo> getFilteredAndSortedInfos() {
        Predicate<JIPipeParameterTypeInfo> filterFunction = info -> searchField.test(info.getName());

        return availableTypes.stream().filter(filterFunction).sorted(JIPipeParameterTypeInfo::compareTo).collect(Collectors.toList());
    }

    private void reloadTypeList() {
        setSelectedInfo(null);
        List<JIPipeParameterTypeInfo> availableTypes = getFilteredAndSortedInfos();
        DefaultListModel<JIPipeParameterTypeInfo> listModel = new DefaultListModel<>();
        for (JIPipeParameterTypeInfo type : availableTypes) {
            listModel.addElement(type);
        }
        datatypeList.setModel(listModel);
        if (!listModel.isEmpty()) {
            datatypeList.setSelectedIndex(0);
        }
    }

    public JIPipeParameterTypeInfo getSelectedInfo() {
        return selectedInfo;
    }

    public void setSelectedInfo(JIPipeParameterTypeInfo selectedInfo) {
        this.selectedInfo = selectedInfo;
    }

    public JDialog getDialog() {
        return dialog;
    }

    public void setDialog(JDialog dialog) {
        this.dialog = dialog;
    }
}
