package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQDataDeclaration;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotDefinition;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * UI that edits an existing algorithm slot
 */
public class EditAlgorithmSlotPanel extends JPanel {

    private ACAQDataSlot existingSlot;
    private SearchTextField searchField;
    private JList<ACAQDataDeclaration> datatypeList;
    private JComboBox<String> inheritedSlotList;
    private JTextField nameEditor;
    private ACAQDataDeclaration selectedDeclaration;
    private FormPanel formPanel;
    private JButton confirmButton;
    private JDialog dialog;
    private Set<ACAQDataDeclaration> availableTypes;
    private Map<ACAQDataDeclaration, ACAQDataDeclaration> inheritanceConversions = new HashMap<>();


    /**
     * Creates a new instance
     *
     * @param existingSlot the existing slot
     */
    public EditAlgorithmSlotPanel(ACAQDataSlot existingSlot) {
        this.existingSlot = existingSlot;
        initialize();
        initializeAvailableDeclarations();
        reloadTypeList();

        setInitialValues();
    }

    private void setInitialValues() {
        nameEditor.setText(existingSlot.getName());
        datatypeList.setSelectedValue(ACAQDataDeclaration.getInstance(existingSlot.getAcceptedDataType()), true);

    }

    private void initialize() {
        ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) existingSlot.getAlgorithm().getSlotConfiguration();
        setLayout(new BorderLayout());
        initializeToolBar();

        formPanel = new FormPanel(null, FormPanel.NONE);

        datatypeList = new JList<>();
        datatypeList.setCellRenderer(new ACAQDataDeclarationListCellRenderer());
        datatypeList.addListSelectionListener(e -> {
            if (datatypeList.getSelectedValue() != null) {
                setSelectedDeclaration(datatypeList.getSelectedValue());
            }
        });
        JScrollPane scrollPane = new JScrollPane(datatypeList);
        formPanel.addWideToForm(scrollPane, null);

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

        if (existingSlot.getSlotType() == ACAQDataSlot.SlotType.Output && slotConfiguration.isAllowInheritedOutputSlots()) {
            formPanel.addGroupHeader("Inheritance", UIUtils.getIconFromResources("cog.png"));
            inheritedSlotList = new JComboBox<>();
            inheritedSlotList.setRenderer(new InheritedSlotListCellRenderer(existingSlot.getAlgorithm()));
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement("");
            model.addElement("*");
            for (String id : existingSlot.getAlgorithm().getInputSlotOrder()) {
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
        initializeButtonPanel();
    }

    private void checkNameEditor() {
        if (settingsAreValid()) {
            nameEditor.setBorder(BorderFactory.createEtchedBorder());
        } else {
            nameEditor.setBorder(BorderFactory.createLineBorder(Color.RED));
        }
    }

    private void initializeButtonPanel() {
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

        confirmButton = new JButton("Edit", UIUtils.getIconFromResources("edit.png"));
        confirmButton.addActionListener(e -> editSlot());
        confirmButton.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                editSlot();
            }
        });
        buttonPanel.add(confirmButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void editSlot() {
        if (!settingsAreValid())
            return;
        String slotName = nameEditor.getText().trim();
        ACAQAlgorithm algorithm = existingSlot.getAlgorithm();
        ACAQDataSlot.SlotType slotType = existingSlot.getSlotType();
        ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration();
        ACAQSlotDefinition slotDefinition;
        if (slotType == ACAQDataSlot.SlotType.Input) {
            slotDefinition = new ACAQSlotDefinition(selectedDeclaration.getDataClass(), slotType, slotName, null);
        } else if (slotType == ACAQDataSlot.SlotType.Output) {
            String inheritedSlot = null;
            if (inheritedSlotList != null && inheritedSlotList.getSelectedItem() != null) {
                inheritedSlot = inheritedSlotList.getSelectedItem().toString();
            }

            slotDefinition = new ACAQSlotDefinition(selectedDeclaration.getDataClass(), slotType, slotName, inheritedSlot);
        } else {
            throw new UnsupportedOperationException();
        }

        slotDefinition.setInheritanceConversions(inheritanceConversions);

        // Remember connections to the existing slot
        // Remember the slot order
        ACAQAlgorithmGraph graph = algorithm.getGraph();
        ACAQDataSlot sourceSlot = null;
        Set<ACAQDataSlot> targetSlots = null;
        List<String> slotOrder;
        if (slotType == ACAQDataSlot.SlotType.Input) {
            sourceSlot = graph.getSourceSlot(existingSlot);
            slotOrder = new ArrayList<>(slotConfiguration.getInputSlotOrder());
        } else {
            targetSlots = graph.getTargetSlots(existingSlot);
            slotOrder = new ArrayList<>(slotConfiguration.getOutputSlotOrder());
        }

        // Modify the slot order
        slotOrder.set(slotOrder.indexOf(existingSlot.getName()), slotName);

        // Remove the existing slot
        slotConfiguration.removeSlot(existingSlot.getName(), true);

        // Add the new slot and configure it to be placed in the same location
        slotConfiguration.addSlot(slotName, slotDefinition, true);
        if (slotType == ACAQDataSlot.SlotType.Input) {
            slotConfiguration.trySetInputSlotOrder(slotOrder);
        } else {
            slotConfiguration.trySetOutputSlotOrder(slotOrder);
        }

        ACAQDataSlot newSlot = algorithm.getSlots().get(slotName);

        // Reconnect the graph
        if (slotType == ACAQDataSlot.SlotType.Input) {
            if (sourceSlot != null) {
                graph.connect(sourceSlot, newSlot);
            }
        } else {
            for (ACAQDataSlot targetSlot : targetSlots) {
                graph.connect(newSlot, targetSlot);
            }
        }

        if (dialog != null)
            dialog.setVisible(false);
    }

    private boolean settingsAreValid() {
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
        nameEditor.setToolTipText(null);
        return true;
    }

    private void initializeToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        ACAQAlgorithm algorithm = existingSlot.getAlgorithm();
        JLabel algorithmNameLabel = new JLabel(algorithm.getName(),
                new ColorIcon(16, 16, UIUtils.getFillColorFor(algorithm.getDeclaration())), JLabel.LEFT);
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
        ACAQAlgorithm algorithm = existingSlot.getAlgorithm();
        ACAQDataSlot.SlotType slotType = existingSlot.getSlotType();
        ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration();
        if (slotType == ACAQDataSlot.SlotType.Input) {
            availableTypes = slotConfiguration.getAllowedInputSlotTypes()
                    .stream().map(ACAQDataDeclaration::getInstance).collect(Collectors.toSet());
        } else if (slotType == ACAQDataSlot.SlotType.Output) {
            availableTypes = slotConfiguration.getAllowedOutputSlotTypes()
                    .stream().map(ACAQDataDeclaration::getInstance).collect(Collectors.toSet());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private List<ACAQDataDeclaration> getFilteredAndSortedDeclarations() {
        String[] searchStrings = searchField.getSearchStrings();
        Predicate<ACAQDataDeclaration> filterFunction = declaration -> {
            if (searchStrings != null && searchStrings.length > 0) {
                boolean matches = true;
                String name = declaration.getName();
                for (String searchString : searchStrings) {
                    if (!name.toLowerCase().contains(searchString.toLowerCase())) {
                        matches = false;
                        break;
                    }
                }
                return matches;
            } else {
                return true;
            }
        };

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
     * @param existingSlot the slot to be edited
     */
    public static void showDialog(Component parent, ACAQDataSlot existingSlot) {
        JDialog dialog = new JDialog();
        EditAlgorithmSlotPanel panel = new EditAlgorithmSlotPanel(existingSlot);
        panel.setDialog(dialog);
        dialog.setContentPane(panel);
        dialog.setTitle("Edit slot '" + existingSlot.getName() + "'");
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(new Dimension(500, 600));
        dialog.setLocationRelativeTo(parent);
        UIUtils.addEscapeListener(dialog);
        dialog.setVisible(true);
    }
}
