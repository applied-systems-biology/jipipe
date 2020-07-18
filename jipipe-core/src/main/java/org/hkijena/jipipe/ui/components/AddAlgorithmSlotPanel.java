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

import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
import org.hkijena.jipipe.api.algorithm.JIPipeIOSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.history.JIPipeGraphHistory;
import org.hkijena.jipipe.api.history.SlotConfigurationHistorySnapshot;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
    private final JIPipeGraphHistory graphHistory;
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

    /**
     * @param algorithm    the target algorithm
     * @param slotType     the slot type to be created
     * @param graphHistory the graph history
     */
    public AddAlgorithmSlotPanel(JIPipeGraphNode algorithm, JIPipeSlotType slotType, JIPipeGraphHistory graphHistory) {
        this.algorithm = algorithm;
        this.slotType = slotType;
        this.graphHistory = graphHistory;
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
        String initialValue = slotType + " data";

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
        initializeToolBar();

        datatypeList = new JList<>();
        datatypeList.setCellRenderer(new JIPipeDataInfoListCellRenderer());
        datatypeList.addListSelectionListener(e -> {
            if (datatypeList.getSelectedValue() != null) {
                setSelectedInfo(datatypeList.getSelectedValue());
            }
        });
        JScrollPane scrollPane = new JScrollPane(datatypeList);
        add(scrollPane, BorderLayout.CENTER);

        // Create form located at the bottom
        FormPanel formPanel = new FormPanel(null, FormPanel.NONE);
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

        if (slotType == JIPipeSlotType.Output && slotConfiguration.isAllowInheritedOutputSlots()) {
            formPanel.addGroupHeader("Inheritance", UIUtils.getIconFromResources("cog.png"));
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

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.add(formPanel);
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

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("remove.png"));
        cancelButton.addActionListener(e -> {
            if (dialog != null) {
                dialog.setVisible(false);
            }
        });
        buttonPanel.add(cancelButton);

        confirmButton = new JButton("Add", UIUtils.getIconFromResources("add.png"));
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
        graphHistory.addSnapshotBefore(new SlotConfigurationHistorySnapshot(algorithm, "Add slot '" + slotName + "'"));

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
        if (!StringUtils.isFilesystemCompatible(slotName)) {
            nameEditor.setToolTipText("Only alphanumeric names are allowed!");
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

    private void initializeToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JLabel algorithmNameLabel = new JLabel(algorithm.getName(), new ColorIcon(16, 16, UIUtils.getFillColorFor(algorithm.getInfo())), JLabel.LEFT);
        algorithmNameLabel.setToolTipText(TooltipUtils.getAlgorithmTooltip(algorithm.getInfo()));
        toolBar.add(algorithmNameLabel);
        toolBar.add(Box.createHorizontalStrut(5));

        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(Box.createHorizontalStrut(16));
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

        add(toolBar, BorderLayout.NORTH);
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
     * @param graphHistory the graph history
     * @param algorithm    target algorithm
     * @param slotType     slot type to be created
     */
    public static void showDialog(Component parent, JIPipeGraphHistory graphHistory, JIPipeGraphNode algorithm, JIPipeSlotType slotType) {
        JDialog dialog = new JDialog();
        AddAlgorithmSlotPanel panel = new AddAlgorithmSlotPanel(algorithm, slotType, graphHistory);
        panel.setDialog(dialog);
        dialog.setContentPane(panel);
        dialog.setTitle("Add slot");
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(new Dimension(500, 600));
        dialog.setLocationRelativeTo(parent);
        UIUtils.addEscapeListener(dialog);
        dialog.setVisible(true);
    }
}
