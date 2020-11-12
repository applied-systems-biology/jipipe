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
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.history.JIPipeGraphHistory;
import org.hkijena.jipipe.api.history.SlotConfigurationHistorySnapshot;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * UI that edits an existing algorithm slot
 */
public class EditAlgorithmSlotPanel extends JPanel {

    private final JIPipeGraphHistory graphHistory;
    private JIPipeDataSlot existingSlot;
    private SearchTextField searchField;
    private JList<JIPipeDataInfo> datatypeList;
    private JComboBox<String> inheritedSlotList;
    private JTextField nameEditor;
    private JIPipeDataInfo selectedInfo;
    private JButton confirmButton;
    private JDialog dialog;
    private Set<JIPipeDataInfo> availableTypes;
    private Map<JIPipeDataInfo, JIPipeDataInfo> inheritanceConversions = new HashMap<>();


    /**
     * Creates a new instance
     *
     * @param existingSlot the existing slot
     * @param graphHistory the graph history
     */
    public EditAlgorithmSlotPanel(JIPipeDataSlot existingSlot, JIPipeGraphHistory graphHistory) {
        this.existingSlot = existingSlot;
        this.graphHistory = graphHistory;
        initialize();
        initializeAvailableInfos();
        reloadTypeList();

        setInitialValues();
    }

    private void setInitialValues() {
        nameEditor.setText(existingSlot.getName());
        datatypeList.setSelectedValue(JIPipeDataInfo.getInstance(existingSlot.getAcceptedDataType()), true);
    }

    private void initialize() {
        JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) existingSlot.getNode().getSlotConfiguration();
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

        FormPanel formPanel = new FormPanel(null, FormPanel.NONE);
        FormPanel.GroupHeaderPanel header = formPanel.addGroupHeader("Edit slot '" + existingSlot.getName() + "' in '" + existingSlot.getNode().getName() + "'", JIPipe.getInstance().getNodeRegistry().getIconFor(existingSlot.getNode().getInfo()));
        header.setDescription("You can change the data type of the slot at the left-hand side. Other properties can be changed below. Please note, that existing connections might be removed when editing a slot.");
        nameEditor = new JXTextField();
        nameEditor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (settingsAreValid() && e.getKeyCode() == KeyEvent.VK_ENTER) {
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

        if (existingSlot.getSlotType() == JIPipeSlotType.Output && slotConfiguration.isAllowInheritedOutputSlots()) {
            formPanel.addGroupHeader("Inheritance", UIUtils.getIconFromResources("actions/configure.png"));
            inheritedSlotList = new JComboBox<>();
            inheritedSlotList.setRenderer(new InheritedSlotListCellRenderer(existingSlot.getNode()));
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement("");
            model.addElement("*");
            for (String id : existingSlot.getNode().getInputSlotOrder()) {
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
        if (settingsAreValid()) {
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

        confirmButton = new JButton("Edit", UIUtils.getIconFromResources("actions/edit.png"));
        confirmButton.addActionListener(e -> editSlot());
        confirmButton.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                editSlot();
            }
        });
        buttonPanel.add(confirmButton);

        return buttonPanel;
    }

    private void editSlot() {

        String slotName = nameEditor.getText().trim();
        if (!JIPipeDataSlotInfo.isValidName(slotName)) {
            JOptionPane.showMessageDialog(this, "The name '" + slotName + "' is not a valid slot name. It can only contain alphanumeric characters and following characters: . _ , #");
            return;
        }

        if (!settingsAreValid())
            return;

        // Create a undo snapshot
        graphHistory.addSnapshotBefore(new SlotConfigurationHistorySnapshot(existingSlot.getNode(), "Edit slot '" + existingSlot.getDisplayName() + "'"));

        JIPipeGraphNode algorithm = existingSlot.getNode();
        JIPipeSlotType slotType = existingSlot.getSlotType();
        JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) algorithm.getSlotConfiguration();
        JIPipeDataSlotInfo slotDefinition;
        if (slotType == JIPipeSlotType.Input) {
            slotDefinition = new JIPipeDataSlotInfo(selectedInfo.getDataClass(), slotType, slotName, null);
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

        // Remember connections to the existing slot
        // Remember the slot order
        JIPipeGraph graph = algorithm.getGraph();
        JIPipeDataSlot sourceSlot = null;
        Set<JIPipeDataSlot> targetSlots = null;
        List<String> slotOrder;
        if (slotType == JIPipeSlotType.Input) {
            sourceSlot = graph.getSourceSlot(existingSlot);
            slotOrder = new ArrayList<>(slotConfiguration.getInputSlotOrder());
        } else {
            targetSlots = graph.getTargetSlots(existingSlot);
            slotOrder = new ArrayList<>(slotConfiguration.getOutputSlotOrder());
        }

        // Modify the slot order
        slotOrder.set(slotOrder.indexOf(existingSlot.getName()), slotName);

        // Remove the existing slot
        if (existingSlot.isInput())
            slotConfiguration.removeInputSlot(existingSlot.getName(), true);
        else
            slotConfiguration.removeOutputSlot(existingSlot.getName(), true);

        // Add the new slot and configure it to be placed in the same location
        slotConfiguration.addSlot(slotName, slotDefinition, true);
        if (slotType == JIPipeSlotType.Input) {
            slotConfiguration.trySetInputSlotOrder(slotOrder);
        } else {
            slotConfiguration.trySetOutputSlotOrder(slotOrder);
        }

        JIPipeDataSlot newSlot;
        if (existingSlot.isInput())
            newSlot = algorithm.getInputSlot(slotName);
        else
            newSlot = algorithm.getOutputSlot(slotName);

        // Reconnect the graph
        if (slotType == JIPipeSlotType.Input) {
            if (sourceSlot != null) {
                graph.connect(sourceSlot, newSlot);
            }
        } else {
            for (JIPipeDataSlot targetSlot : targetSlots) {
                graph.connect(newSlot, targetSlot);
            }
        }

        if (dialog != null)
            dialog.setVisible(false);
    }

    private boolean settingsAreValid() {
        if (selectedInfo == null)
            return false;
        String slotName = nameEditor.getText();
        if (slotName == null || slotName.isEmpty()) {
            nameEditor.setToolTipText("Slot name is empty!");
            return false;
        }
        slotName = slotName.trim();
        if (!StringUtils.isFilesystemCompatible(slotName)) {
            nameEditor.setToolTipText("Only alphanumeric names are allowed!");
            return false;
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
        JIPipeGraphNode algorithm = existingSlot.getNode();
        JIPipeSlotType slotType = existingSlot.getSlotType();
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
        Predicate<JIPipeDataInfo> filterFunction = info -> searchField.test(info.getName());
        return availableTypes.stream().filter(filterFunction).sorted(JIPipeDataInfo::compareTo).collect(Collectors.toList());
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

    /**
     * Shows a dialog for adding slots
     *
     * @param parent       parent component
     * @param graphHistory the graph history for undo snapshots
     * @param existingSlot the slot to be edited
     */
    public static void showDialog(Component parent, JIPipeGraphHistory graphHistory, JIPipeDataSlot existingSlot) {
        JDialog dialog = new JDialog();
        EditAlgorithmSlotPanel panel = new EditAlgorithmSlotPanel(existingSlot, graphHistory);
        panel.setDialog(dialog);
        dialog.setContentPane(panel);
        dialog.setTitle("Edit slot '" + existingSlot.getName() + "'");
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(new Dimension(640, 480));
        dialog.setLocationRelativeTo(parent);
        UIUtils.addEscapeListener(dialog);
        dialog.setVisible(true);
    }
}
