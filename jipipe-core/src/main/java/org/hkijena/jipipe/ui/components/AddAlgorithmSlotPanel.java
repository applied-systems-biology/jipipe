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
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.history.JIPipeHistoryJournal;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeIOSlotConfiguration;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * UI that adds slots to an algorithm
 */
public class AddAlgorithmSlotPanel extends JPanel {

    /**
     * Remember the type selected last for increased usability
     */
    private static JIPipeDataInfo lastSelectedType = null;
    private final JIPipeHistoryJournal historyJournal;
    private JIPipeGraphNode algorithm;
    private JIPipeSlotType slotType;
    private SearchTextField searchField;
    private JList<JIPipeDataInfo> datatypeList;
    private JComboBox<String> inheritedSlotList;
    private JTextField nameEditor;
    private JIPipeDataInfo selectedInfo;
    private JButton confirmButton;
    private JDialog dialog;
    private Set<JIPipeDataInfo> availableTypes;
    private Map<JIPipeDataInfo, JIPipeDataInfo> inheritanceConversions = new HashMap<>();
    private JCheckBox optionalInputEditor = new JCheckBox();

    /**
     * @param algorithm    the target algorithm
     * @param slotType     the slot type to be created
     * @param historyJournal the history journal. can be null.
     */
    public AddAlgorithmSlotPanel(JIPipeGraphNode algorithm, JIPipeSlotType slotType, JIPipeHistoryJournal historyJournal) {
        this.algorithm = algorithm;
        this.slotType = slotType;
        this.historyJournal = historyJournal;
        initialize();
        initializeAvailableInfos();
        reloadTypeList();
        setInitialName();
        if (lastSelectedType != null && availableTypes.contains(lastSelectedType)) {
            datatypeList.setSelectedValue(lastSelectedType, true);
            nameEditor.requestFocusInWindow();
            nameEditor.selectAll();
        }
        if (availableTypes.size() == 1) {
            nameEditor.requestFocusInWindow();
            nameEditor.selectAll();
        }
    }

    private void setInitialName() {
        String initialValue = slotType.toString();

        // This is general
        if (algorithm.getSlotConfiguration() instanceof JIPipeIOSlotConfiguration) {
            initialValue = "Data";
        }

        if (slotType == JIPipeSlotType.Input)
            initialValue = StringUtils.makeUniqueString(initialValue, " ", s -> algorithm.getInputSlotMap().containsKey(s));
        else
            initialValue = StringUtils.makeUniqueString(initialValue, " ", s -> algorithm.getOutputSlotMap().containsKey(s));

        nameEditor.setText(initialValue);
    }

    private void initialize() {
        JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) algorithm.getSlotConfiguration();
        setLayout(new BorderLayout());

        JPanel listPanel = new JPanel(new BorderLayout());

        initializeToolBar(listPanel);

        datatypeList = new JList<>();
        datatypeList.setCellRenderer(new JIPipeDataInfoListCellRenderer());
        datatypeList.addListSelectionListener(e -> {
            if (datatypeList.getSelectedValue() != null) {
                setSelectedInfo(datatypeList.getSelectedValue());
            }
        });
        JScrollPane scrollPane = new JScrollPane(datatypeList);
        listPanel.add(scrollPane, BorderLayout.CENTER);
        add(listPanel, BorderLayout.WEST);

        // Create form located at the bottom
        FormPanel formPanel = new FormPanel(null, FormPanel.NONE);
        FormPanel.GroupHeaderPanel header = formPanel.addGroupHeader("Add slot to '" + algorithm.getName() + "'", JIPipe.getInstance().getNodeRegistry().getIconFor(algorithm.getInfo()));
        header.setDescription("Select the slot type in the list at the left-hand side. Then set the name and other properties of the newly created slot. You can later change the name or attach a custom label.");
        nameEditor = new JXTextField();
        nameEditor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (canAddSlot() && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    confirmButton.requestFocusInWindow();
                }
            }
        });
        nameEditor.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                checkNameEditor();
            }
        });
        formPanel.addToForm(nameEditor, new JLabel("Slot name"), null);

        if (slotType == JIPipeSlotType.Input) {
            optionalInputEditor.setText("Optional input");
            optionalInputEditor.setToolTipText("If enabled, the input slot does not require an incoming edge. The node then will receive an empty data table.");
            formPanel.addWideToForm(optionalInputEditor, null);
        }
        if (slotType == JIPipeSlotType.Output && slotConfiguration.isAllowInheritedOutputSlots()) {
            formPanel.addGroupHeader("Inheritance", UIUtils.getIconFromResources("actions/configure.png"));
            inheritedSlotList = new JComboBox<>();
            inheritedSlotList.setRenderer(new InheritedSlotListCellRenderer(algorithm));
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement("");
            model.addElement("*");
            for (String id : algorithm.getInputSlotOrder()) {
                model.addElement(id);
            }
            inheritedSlotList.setModel(model);
            inheritedSlotList.setSelectedIndex(0);
            formPanel.addToForm(inheritedSlotList, new JLabel("Inherited slot"), null);
            inheritedSlotList.setToolTipText("Inherits the slot type from an input slot. This will adapt to which data is currently connected.");

            InheritanceConversionEditorUI inheritanceConversionEditorUI
                    = new InheritanceConversionEditorUI(inheritanceConversions);
            inheritanceConversionEditorUI.setBorder(BorderFactory.createEtchedBorder());
            formPanel.addToForm(inheritanceConversionEditorUI, new JLabel("Inheritance conversions"), null);
        }
        formPanel.addVerticalGlue();
        add(formPanel, BorderLayout.CENTER);


        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.add(initializeButtonPanel());
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void checkNameEditor() {
        if (canAddSlot()) {
            nameEditor.setBorder(BorderFactory.createEtchedBorder());
        } else {
            nameEditor.setBorder(BorderFactory.createLineBorder(Color.RED));
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
        confirmButton.addActionListener(e -> addSlot());
        confirmButton.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                addSlot();
            }
        });
        buttonPanel.add(confirmButton);

        return buttonPanel;
    }

    private void addSlot() {
        if (!canAddSlot())
            return;

        String slotName = nameEditor.getText().trim();
        if (!JIPipeDataSlotInfo.isValidName(slotName)) {
            JOptionPane.showMessageDialog(this, "The name '" + slotName + "' is not a valid slot name. It can only contain alphanumeric characters and following characters: . _ , #");
            return;
        }

        JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) algorithm.getSlotConfiguration();
        JIPipeDataSlotInfo slotDefinition;
        if (slotType == JIPipeSlotType.Input) {
            slotDefinition = new JIPipeDataSlotInfo(selectedInfo.getDataClass(), slotType, slotName, null);
            slotDefinition.setOptional(optionalInputEditor.isSelected());
        } else if (slotType == JIPipeSlotType.Output) {
            String inheritedSlot = null;
            if (inheritedSlotList != null && inheritedSlotList.getSelectedItem() != null) {
                inheritedSlot = inheritedSlotList.getSelectedItem().toString();
            }

            slotDefinition = new JIPipeDataSlotInfo(selectedInfo.getDataClass(), slotType, slotName, inheritedSlot);
        } else {
            throw new UnsupportedOperationException();
        }
        slotDefinition.setInheritanceConversions(inheritanceConversions);

        if(getHistoryJournal() != null) {
            getHistoryJournal().snapshotBeforeAddSlot(algorithm, slotDefinition, algorithm.getCompartmentUUIDInGraph());
        }

        slotConfiguration.addSlot(slotName, slotDefinition, true);
        lastSelectedType = selectedInfo;

        if (dialog != null)
            dialog.setVisible(false);
    }

    private boolean canAddSlot() {
        if (selectedInfo == null)
            return false;
        String slotName = nameEditor.getText();
        if (slotName == null || slotName.isEmpty()) {
            nameEditor.setToolTipText("Slot name is empty!");
            return false;
        }
        slotName = slotName.trim();
        if (!JIPipeDataSlotInfo.isValidName(slotName)) {
            nameEditor.setToolTipText("Invalid name: It can only contain alphanumeric characters and following characters: . _ , #");
            return false;
        }
        if (slotType == JIPipeSlotType.Input) {
            if (algorithm.getInputSlotMap().containsKey(slotName)) {
                nameEditor.setToolTipText("The slot name already exists!");
                return false;
            }
        } else {
            if (algorithm.getOutputSlotMap().containsKey(slotName)) {
                nameEditor.setToolTipText("The slot name already exists!");
                return false;
            }
        }
        nameEditor.setToolTipText(null);
        return true;
    }

    private void initializeToolBar(JPanel listPanel) {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        searchField = new SearchTextField();
        searchField.addActionListener(e -> reloadTypeList());
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (selectedInfo != null && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    nameEditor.requestFocusInWindow();
                    nameEditor.selectAll();
                }
            }
        });
        toolBar.add(searchField);

        listPanel.add(toolBar, BorderLayout.NORTH);
    }

    private void initializeAvailableInfos() {
        JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) algorithm.getSlotConfiguration();
        if (slotType == JIPipeSlotType.Input) {
            availableTypes = slotConfiguration.getAllowedInputSlotTypes()
                    .stream().map(JIPipeDataInfo::getInstance).collect(Collectors.toSet());
        } else if (slotType == JIPipeSlotType.Output) {
            availableTypes = slotConfiguration.getAllowedOutputSlotTypes()
                    .stream().map(JIPipeDataInfo::getInstance).collect(Collectors.toSet());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private List<JIPipeDataInfo> getFilteredAndSortedInfos() {
        if (searchField.getSearchStrings() == null || searchField.getSearchStrings().length == 0) {
            return availableTypes.stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
        } else {
            Predicate<JIPipeDataInfo> filterFunction = info -> searchField.test(info.getName());
            return availableTypes.stream().filter(filterFunction).sorted(Comparator.comparing((JIPipeDataInfo di) -> di.getName().length()).thenComparing(Comparator.naturalOrder())).collect(Collectors.toList());
        }
    }

    private void reloadTypeList() {
        setSelectedInfo(null);
        List<JIPipeDataInfo> availableTypes = getFilteredAndSortedInfos();
        DefaultListModel<JIPipeDataInfo> listModel = new DefaultListModel<>();
        for (JIPipeDataInfo type : availableTypes) {
            listModel.addElement(type);
        }
        datatypeList.setModel(listModel);
        if (!listModel.isEmpty()) {
            datatypeList.setSelectedIndex(0);
        }
    }

    public JIPipeDataInfo getSelectedInfo() {
        return selectedInfo;
    }

    public void setSelectedInfo(JIPipeDataInfo selectedInfo) {
        this.selectedInfo = selectedInfo;
    }

    public JDialog getDialog() {
        return dialog;
    }

    public void setDialog(JDialog dialog) {
        this.dialog = dialog;
    }

    public JIPipeHistoryJournal getHistoryJournal() {
        return historyJournal;
    }

    /**
     * Shows a dialog for adding slots
     *
     * @param parent       parent component
     * @param historyJournal the graph history
     * @param algorithm    target algorithm
     * @param slotType     slot type to be created
     */
    public static void showDialog(Component parent, JIPipeHistoryJournal historyJournal, JIPipeGraphNode algorithm, JIPipeSlotType slotType) {
        JDialog dialog = new JDialog();
        AddAlgorithmSlotPanel panel = new AddAlgorithmSlotPanel(algorithm, slotType, historyJournal);
        panel.setDialog(dialog);
        dialog.setContentPane(panel);
        dialog.setTitle("Add slot");
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(new Dimension(640, 480));
        dialog.setLocationRelativeTo(parent);
        UIUtils.addEscapeListener(dialog);
        dialog.setVisible(true);
    }
}
