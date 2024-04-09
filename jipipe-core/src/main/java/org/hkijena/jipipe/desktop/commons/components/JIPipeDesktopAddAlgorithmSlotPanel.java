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

package org.hkijena.jipipe.desktop.commons.components;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.history.JIPipeHistoryJournal;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeIOSlotConfiguration;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeDesktopDataInfoListCellRenderer;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * UI that adds slots to an algorithm
 */
public class JIPipeDesktopAddAlgorithmSlotPanel extends JPanel {

    /**
     * Remember the type selected last for increased usability
     */
    private static JIPipeDataInfo lastSelectedType = null;
    private final JIPipeHistoryJournal historyJournal;
    private JIPipeGraphNode algorithm;
    private JIPipeSlotType slotType;
    private JIPipeDesktopSearchTextField searchField;
    private JList<JIPipeDataInfo> datatypeList;
    private JTextField nameEditor;
    private JIPipeDataInfo selectedInfo;
    private JButton confirmButton;
    private JDialog dialog;
    private Set<JIPipeDataInfo> availableTypes;
    private JCheckBox optionalInputEditor = new JCheckBox();

    private JScrollPane listScrollPane;
    private JXTextField descriptionEditor;

    private List<JIPipeDataSlot> addedSlots = new ArrayList<>();

    /**
     * @param algorithm      the target algorithm
     * @param slotType       the slot type to be created
     * @param historyJournal the history journal. can be null.
     */
    public JIPipeDesktopAddAlgorithmSlotPanel(JIPipeGraphNode algorithm, JIPipeSlotType slotType, JIPipeHistoryJournal historyJournal) {
        this.algorithm = algorithm;
        this.slotType = slotType;
        this.historyJournal = historyJournal;
        initialize();
        initializeAvailableInfos();
        reloadTypeList();
        setInitialName();
        if (lastSelectedType != null && availableTypes.contains(lastSelectedType)) {
            datatypeList.setSelectedValue(lastSelectedType, true);
        }
        SwingUtilities.invokeLater(() -> {
            nameEditor.requestFocusInWindow();
            nameEditor.selectAll();
        });
    }

    /**
     * Shows a dialog for adding slots
     *
     * @param parent         parent component
     * @param historyJournal the graph history
     * @param algorithm      target algorithm
     * @param slotType       slot type to be created
     * @return the panel
     */
    public static JIPipeDesktopAddAlgorithmSlotPanel showDialog(Component parent, JIPipeHistoryJournal historyJournal, JIPipeGraphNode algorithm, JIPipeSlotType slotType) {
        JDialog dialog = new JDialog();
        JIPipeDesktopAddAlgorithmSlotPanel panel = new JIPipeDesktopAddAlgorithmSlotPanel(algorithm, slotType, historyJournal);
        panel.setDialog(dialog);
        dialog.setContentPane(panel);
        dialog.setTitle("Add slot");
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(new Dimension(640, 480));
        dialog.setLocationRelativeTo(parent);
        UIUtils.addEscapeListener(dialog);
        dialog.setVisible(true);
        return panel;
    }

    public JList<JIPipeDataInfo> getDatatypeList() {
        return datatypeList;
    }

    public List<JIPipeDataSlot> getAddedSlots() {
        return addedSlots;
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
        setLayout(new BorderLayout());

        JPanel listPanel = new JPanel(new BorderLayout());

        initializeToolBar(listPanel);

        datatypeList = new JList<>();
        datatypeList.setCellRenderer(new JIPipeDesktopDataInfoListCellRenderer());
        datatypeList.addListSelectionListener(e -> {
            if (datatypeList.getSelectedValue() != null) {
                setSelectedInfo(datatypeList.getSelectedValue());
            }
        });
        listScrollPane = new JScrollPane(datatypeList);
        listPanel.add(listScrollPane, BorderLayout.CENTER);
        add(listPanel, BorderLayout.WEST);

        // Create form located at the bottom
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.NONE);
        JIPipeDesktopFormPanel.GroupHeaderPanel header = formPanel.addGroupHeader("Add slot to '" + algorithm.getName() + "'", JIPipe.getInstance().getNodeRegistry().getIconFor(algorithm.getInfo()));
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
        nameEditor.getDocument().addDocumentListener(new JIPipeDesktopDocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                checkNameEditor();
            }
        });
        formPanel.addToForm(nameEditor, new JLabel("Slot name"), null);

        // Description editor
        descriptionEditor = new JXTextField();
        formPanel.addToForm(descriptionEditor, new JLabel("Description"));

        if (slotType == JIPipeSlotType.Input) {
            optionalInputEditor.setText("Optional input");
            optionalInputEditor.setToolTipText("If enabled, the input slot does not require an incoming edge. The node then will receive an empty data table.");
            formPanel.addWideToForm(optionalInputEditor, null);
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
            nameEditor.setBorder(UIUtils.createControlBorder());
        } else {
            nameEditor.setBorder(UIUtils.createControlErrorBorder());
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
            slotDefinition = new JIPipeDataSlotInfo(selectedInfo.getDataClass(), slotType, slotName, StringUtils.nullToEmpty(descriptionEditor.getText()));
            slotDefinition.setOptional(optionalInputEditor.isSelected());
        } else if (slotType == JIPipeSlotType.Output) {
            slotDefinition = new JIPipeDataSlotInfo(selectedInfo.getDataClass(), slotType, slotName, StringUtils.nullToEmpty(descriptionEditor.getText()));
        } else {
            throw new UnsupportedOperationException();
        }

        if (getHistoryJournal() != null) {
            getHistoryJournal().snapshotBeforeAddSlot(algorithm, slotDefinition, algorithm.getCompartmentUUIDInParentGraph());
        }

        slotConfiguration.addSlot(slotName, slotDefinition, true);
        if (slotType == JIPipeSlotType.Input) {
            addedSlots.add(algorithm.getInputSlot(slotName));
        } else {
            addedSlots.add(algorithm.getOutputSlot(slotName));
        }
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
        searchField = new JIPipeDesktopSearchTextField();
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

    public Set<JIPipeDataInfo> getAvailableTypes() {
        return availableTypes;
    }

    public void setAvailableTypes(Set<JIPipeDataInfo> availableTypes) {
        this.availableTypes = availableTypes;
        reloadTypeList();
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
        if (lastSelectedType != null && availableTypes.contains(lastSelectedType)) {
            listModel.addElement(lastSelectedType);
        }
        for (JIPipeDataInfo type : availableTypes) {
            if (lastSelectedType != null && availableTypes.contains(lastSelectedType) && type == lastSelectedType) {
                continue;
            }
            listModel.addElement(type);
        }
        datatypeList.setModel(listModel);
        if (!listModel.isEmpty()) {
            datatypeList.setSelectedIndex(0);
        }
        UIUtils.invokeScrollToTop(listScrollPane);
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
}
