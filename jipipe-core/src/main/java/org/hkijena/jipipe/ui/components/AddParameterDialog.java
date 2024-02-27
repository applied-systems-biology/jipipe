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

package org.hkijena.jipipe.ui.components;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.renderers.JIPipeParameterTypeInfoListCellRenderer;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
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
public class AddParameterDialog extends JIPipeWorkbenchPanel {

    /**
     * Remember the type selected last for increased usability
     */
    private static JIPipeParameterTypeInfo lastSelectedType = null;
    private final JIPipeDynamicParameterCollection parameterCollection;
    private final Set<JIPipeParameterTypeInfo> availableTypes;
    private final Settings currentSettings = new Settings();
    private SearchTextField searchField;
    private JList<JIPipeParameterTypeInfo> datatypeList;
    private JIPipeParameterTypeInfo selectedInfo;
    private JDialog dialog;

    /**
     * @param parameterCollection the parameter collection
     */
    public AddParameterDialog(JIPipeWorkbench workbench, JIPipeDynamicParameterCollection parameterCollection) {
        super(workbench);
        this.parameterCollection = parameterCollection;
        this.availableTypes = parameterCollection.getAllowedTypes().stream().map(x ->
                JIPipe.getParameterTypes().getInfoByFieldClass(x)).collect(Collectors.toSet());
        setInitialName();
        initialize();
        reloadTypeList();
        if (lastSelectedType != null) {
            datatypeList.setSelectedValue(lastSelectedType, true);
        }
    }

    /**
     * Shows a dialog for adding slots
     *
     * @param parent parent component
     */
    public static void showDialog(JIPipeWorkbench workbench, Component parent, JIPipeDynamicParameterCollection parameterCollection) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent));
        AddParameterDialog panel = new AddParameterDialog(workbench, parameterCollection);
        panel.setDialog(dialog);
        dialog.setContentPane(panel);
        dialog.setTitle("Add parameter");
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(new Dimension(800, 600));
        dialog.setLocationRelativeTo(parent);
        UIUtils.addEscapeListener(dialog);
        dialog.setVisible(true);
    }

    private void setInitialName() {
        currentSettings.setId(StringUtils.makeUniqueString("parameter", "-", parameterCollection.getParameters().keySet()));
    }

    private void initialize() {
        setLayout(new BorderLayout());

        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(AutoResizeSplitPane.LEFT_RIGHT, AutoResizeSplitPane.RATIO_1_TO_3);
        initializeDataTypeSelection(splitPane);
        initializeFormPanel(splitPane);
        initializeButtonPanel();
        add(splitPane, BorderLayout.CENTER);
    }

    private void initializeFormPanel(AutoResizeSplitPane splitPane) {
        // Create form located at the bottom
        final MarkdownDocument documentation = new MarkdownDocument("# Creating parameters\n\n" +
                "Please select the parameter type on the left-hand list and at least provide a unique identifier. Optionally, you can also input a name and a description.");
        ParameterPanel parameterPanel = new ParameterPanel(getWorkbench(), currentSettings, documentation, FormPanel.WITH_SCROLLING | FormPanel.WITH_DOCUMENTATION | FormPanel.DOCUMENTATION_BELOW);
        splitPane.setRightComponent(parameterPanel);

    }

    private void initializeDataTypeSelection(AutoResizeSplitPane splitPane) {
        JPanel dataTypeSelectionPanel = new JPanel(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(Box.createHorizontalStrut(16));
        searchField = new SearchTextField();
        searchField.addActionListener(e -> reloadTypeList());
        toolBar.add(searchField);

        dataTypeSelectionPanel.add(toolBar, BorderLayout.NORTH);

        datatypeList = new JList<>();
        datatypeList.setCellRenderer(new JIPipeParameterTypeInfoListCellRenderer());
        datatypeList.addListSelectionListener(e -> {
            if (datatypeList.getSelectedValue() != null) {
                setSelectedInfo(datatypeList.getSelectedValue());
            }
        });
        JScrollPane scrollPane = new JScrollPane(datatypeList);
        dataTypeSelectionPanel.add(scrollPane, BorderLayout.CENTER);

        splitPane.setLeftComponent(dataTypeSelectionPanel);
    }

    private void initializeButtonPanel() {
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

        JButton confirmButton = new JButton("Add", UIUtils.getIconFromResources("actions/list-add.png"));
        confirmButton.addActionListener(e -> addParameter());
        confirmButton.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                addParameter();
            }
        });
        buttonPanel.add(confirmButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void addParameter() {
        if (!canAddParameter())
            return;

        String key = currentSettings.id.trim();
        String name = StringUtils.orElse(currentSettings.name, key);

        JIPipeMutableParameterAccess access = parameterCollection.addParameter(key, selectedInfo.getFieldClass());
        access.setName(name);
        access.setDescription(currentSettings.description.getBody());

        lastSelectedType = selectedInfo;

        if (dialog != null)
            dialog.setVisible(false);
    }

    private boolean canAddParameter() {
        if (selectedInfo == null)
            return false;
        String id = currentSettings.getId();
        if (id == null || id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "The unique key cannot be empty!", "Add parameter", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
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

    public static class Settings extends AbstractJIPipeParameterCollection {
        private String id;
        private String name;
        private HTMLText description = new HTMLText();

        @SetJIPipeDocumentation(name = "Unique identifier", description = "The unique identifier of the parameter. Cannot be empty.")
        @JIPipeParameter(value = "id", important = true, uiOrder = -100)
        public String getId() {
            return id;
        }

        @JIPipeParameter("id")
        public void setId(String id) {
            this.id = id;
        }

        @SetJIPipeDocumentation(name = "Name", description = "Optional name of the parameter that will be displayed in the GUI.")
        @JIPipeParameter(value = "name", uiOrder = -90)
        public String getName() {
            return name;
        }

        @JIPipeParameter("name")
        public void setName(String name) {
            this.name = name;
        }

        @SetJIPipeDocumentation(name = "Description", description = "Optional description of the parameter.")
        @JIPipeParameter(value = "description", uiOrder = -80)
        public HTMLText getDescription() {
            return description;
        }

        @JIPipeParameter("description")
        public void setDescription(HTMLText description) {
            this.description = description;
        }
    }
}
