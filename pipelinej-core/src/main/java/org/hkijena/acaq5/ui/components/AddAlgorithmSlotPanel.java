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

package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.algorithm.ACAQIOSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQDataDeclaration;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotDefinition;
import org.hkijena.acaq5.api.data.ACAQSlotType;
import org.hkijena.acaq5.api.history.ACAQGraphHistory;
import org.hkijena.acaq5.api.history.SlotConfigurationHistorySnapshot;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;
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
    private static ACAQDataDeclaration lastSelectedType = null;
    private final ACAQGraphHistory graphHistory;
    private ACAQGraphNode algorithm;
    private ACAQSlotType slotType;
    private SearchTextField searchField;
    private JList<ACAQDataDeclaration> datatypeList;
    private JComboBox<String> inheritedSlotList;
    private JTextField nameEditor;
    private ACAQDataDeclaration selectedDeclaration;
    private JButton confirmButton;
    private JDialog dialog;
    private Set<ACAQDataDeclaration> availableTypes;
    private Map<ACAQDataDeclaration, ACAQDataDeclaration> inheritanceConversions = new HashMap<>();

    /**
     * @param algorithm    the target algorithm
     * @param slotType     the slot type to be created
     * @param graphHistory the graph history
     */
    public AddAlgorithmSlotPanel(ACAQGraphNode algorithm, ACAQSlotType slotType, ACAQGraphHistory graphHistory) {
        this.algorithm = algorithm;
        this.slotType = slotType;
        this.graphHistory = graphHistory;
        initialize();
        initializeAvailableDeclarations();
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
        if (algorithm.getSlotConfiguration() instanceof ACAQIOSlotConfiguration) {
            initialValue = "Data";
        }

        if (slotType == ACAQSlotType.Input)
            initialValue = StringUtils.makeUniqueString(initialValue, " ", s -> algorithm.getInputSlotMap().containsKey(s));
        else
            initialValue = StringUtils.makeUniqueString(initialValue, " ", s -> algorithm.getOutputSlotMap().containsKey(s));

        nameEditor.setText(initialValue);
    }

    private void initialize() {
        ACAQDefaultMutableSlotConfiguration slotConfiguration = (ACAQDefaultMutableSlotConfiguration) algorithm.getSlotConfiguration();
        setLayout(new BorderLayout());
        initializeToolBar();

        datatypeList = new JList<>();
        datatypeList.setCellRenderer(new ACAQDataDeclarationListCellRenderer());
        datatypeList.addListSelectionListener(e -> {
            if (datatypeList.getSelectedValue() != null) {
                setSelectedDeclaration(datatypeList.getSelectedValue());
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

        if (slotType == ACAQSlotType.Output && slotConfiguration.isAllowInheritedOutputSlots()) {
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

        ACAQDefaultMutableSlotConfiguration slotConfiguration = (ACAQDefaultMutableSlotConfiguration) algorithm.getSlotConfiguration();
        ACAQSlotDefinition slotDefinition;
        if (slotType == ACAQSlotType.Input) {
            slotDefinition = new ACAQSlotDefinition(selectedDeclaration.getDataClass(), slotType, slotName, null);
        } else if (slotType == ACAQSlotType.Output) {
            String inheritedSlot = null;
            if (inheritedSlotList != null && inheritedSlotList.getSelectedItem() != null) {
                inheritedSlot = inheritedSlotList.getSelectedItem().toString();
            }

            slotDefinition = new ACAQSlotDefinition(selectedDeclaration.getDataClass(), slotType, slotName, inheritedSlot);
        } else {
            throw new UnsupportedOperationException();
        }

        slotDefinition.setInheritanceConversions(inheritanceConversions);
        slotConfiguration.addSlot(slotName, slotDefinition, true);
        lastSelectedType = selectedDeclaration;

        if (dialog != null)
            dialog.setVisible(false);
    }

    private boolean canAddSlot() {
        if (selectedDeclaration == null)
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
        if (slotType == ACAQSlotType.Input) {
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

        JLabel algorithmNameLabel = new JLabel(algorithm.getName(), new ColorIcon(16, 16, UIUtils.getFillColorFor(algorithm.getDeclaration())), JLabel.LEFT);
        algorithmNameLabel.setToolTipText(TooltipUtils.getAlgorithmTooltip(algorithm.getDeclaration()));
        toolBar.add(algorithmNameLabel);
        toolBar.add(Box.createHorizontalStrut(5));

        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(Box.createHorizontalStrut(16));
        searchField = new SearchTextField();
        searchField.addActionListener(e -> reloadTypeList());
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (selectedDeclaration != null && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    nameEditor.requestFocusInWindow();
                    nameEditor.selectAll();
                }
            }
        });
        toolBar.add(searchField);

        add(toolBar, BorderLayout.NORTH);
    }

    private void initializeAvailableDeclarations() {
        ACAQDefaultMutableSlotConfiguration slotConfiguration = (ACAQDefaultMutableSlotConfiguration) algorithm.getSlotConfiguration();
        if (slotType == ACAQSlotType.Input) {
            availableTypes = slotConfiguration.getAllowedInputSlotTypes()
                    .stream().map(ACAQDataDeclaration::getInstance).collect(Collectors.toSet());
        } else if (slotType == ACAQSlotType.Output) {
            availableTypes = slotConfiguration.getAllowedOutputSlotTypes()
                    .stream().map(ACAQDataDeclaration::getInstance).collect(Collectors.toSet());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private List<ACAQDataDeclaration> getFilteredAndSortedDeclarations() {
        Predicate<ACAQDataDeclaration> filterFunction = declaration -> searchField.test(declaration.getName());
        return availableTypes.stream().filter(filterFunction).sorted(ACAQDataDeclaration::compareTo).collect(Collectors.toList());
    }

    private void reloadTypeList() {
        setSelectedDeclaration(null);
        List<ACAQDataDeclaration> availableTypes = getFilteredAndSortedDeclarations();
        DefaultListModel<ACAQDataDeclaration> listModel = new DefaultListModel<>();
        for (ACAQDataDeclaration type : availableTypes) {
            listModel.addElement(type);
        }
        datatypeList.setModel(listModel);
        if (!listModel.isEmpty()) {
            datatypeList.setSelectedIndex(0);
        }
    }

    public ACAQDataDeclaration getSelectedDeclaration() {
        return selectedDeclaration;
    }

    public void setSelectedDeclaration(ACAQDataDeclaration selectedDeclaration) {
        this.selectedDeclaration = selectedDeclaration;
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
    public static void showDialog(Component parent, ACAQGraphHistory graphHistory, ACAQGraphNode algorithm, ACAQSlotType slotType) {
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
