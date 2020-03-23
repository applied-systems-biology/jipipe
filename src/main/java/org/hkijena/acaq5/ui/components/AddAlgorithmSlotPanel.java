package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQIOSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQDataDeclaration;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
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
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AddAlgorithmSlotPanel extends JPanel {
    private ACAQAlgorithm algorithm;
    private ACAQDataSlot.SlotType slotType;
    private JXTextField searchField;
    private JList<ACAQDataDeclaration> datatypeList;
    private JComboBox<String> inheritedSlotList;
    private JTextField nameEditor;
    private ACAQDataDeclaration selectedDeclaration;
    private FormPanel formPanel;
    private JButton confirmButton;
    private JDialog dialog;
    private Set<ACAQDataDeclaration> availableTypes;

    public AddAlgorithmSlotPanel(ACAQAlgorithm algorithm, ACAQDataSlot.SlotType slotType) {
        this.algorithm = algorithm;
        this.slotType = slotType;
        initialize();
        initializeAvailableDeclarations();
        reloadTypeList();
        setInitialName();
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

        initialValue = StringUtils.makeUniqueString(initialValue, " ", s -> algorithm.getSlots().containsKey(s));
        nameEditor.setText(initialValue);
    }

    private void initialize() {
        ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration();
        setLayout(new BorderLayout());
        initializeToolBar();

        datatypeList = new JList<>();
        datatypeList.setCellRenderer(new ACAQDataDeclarationListCellRenderer());
        datatypeList.addListSelectionListener(e -> {
            if (datatypeList.getSelectedValue() != null) {
                setSelectedDeclaration(datatypeList.getSelectedValue());
            }
        });
        add(new JScrollPane(datatypeList), BorderLayout.CENTER);

        formPanel = new FormPanel(null, false, false);
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

        if (slotType == ACAQDataSlot.SlotType.Output && slotConfiguration.isAllowInheritedOutputSlots()) {
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
        }



        add(formPanel, BorderLayout.SOUTH);
        initializeButtonPanel();
    }

    private void checkNameEditor() {
        if (canAddSlot()) {
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

        confirmButton = new JButton("Add", UIUtils.getIconFromResources("add.png"));
        confirmButton.addActionListener(e -> addSlot());
        confirmButton.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                addSlot();
            }
        });
        buttonPanel.add(confirmButton);

        formPanel.addToForm(buttonPanel, null);
    }

    private void addSlot() {
        if (!canAddSlot())
            return;
        String slotName = nameEditor.getText().trim();
        ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration();
        if (slotType == ACAQDataSlot.SlotType.Input) {
            slotConfiguration.addInputSlot(slotName, selectedDeclaration.getDataClass());
        } else if (slotType == ACAQDataSlot.SlotType.Output) {
            String inheritedSlot = null;
            if (inheritedSlotList != null && inheritedSlotList.getSelectedItem() != null) {
                inheritedSlot = inheritedSlotList.getSelectedItem().toString();
            }
            slotConfiguration.addOutputSlot(slotName, inheritedSlot, selectedDeclaration.getDataClass());
        } else {
            throw new UnsupportedOperationException();
        }
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
        if (algorithm.getSlots().containsKey(slotName)) {
            nameEditor.setToolTipText("The slot name already exists!");
            return false;
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
        searchField = new JXTextField("Search ...");
        searchField.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                reloadTypeList();
            }
        });
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

        JButton clearSearchButton = new JButton(UIUtils.getIconFromResources("clear.png"));
        clearSearchButton.addActionListener(e -> searchField.setText(null));
        toolBar.add(clearSearchButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void initializeAvailableDeclarations() {
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
        String[] searchStrings = getSearchStrings();
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

    private String[] getSearchStrings() {
        String[] searchStrings = null;
        if (searchField.getText() != null) {
            String str = searchField.getText().trim();
            if (!str.isEmpty()) {
                searchStrings = str.split(" ");
            }
        }
        return searchStrings;
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

    public static void showDialog(Component parent, ACAQAlgorithm algorithm, ACAQDataSlot.SlotType slotType) {
        JDialog dialog = new JDialog();
        AddAlgorithmSlotPanel panel = new AddAlgorithmSlotPanel(algorithm, slotType);
        panel.setDialog(dialog);
        dialog.setContentPane(panel);
        dialog.setTitle("Add slot");
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(new Dimension(500, 400));
        dialog.setLocationRelativeTo(parent);
        UIUtils.addEscapeListener(dialog);
        dialog.setVisible(true);
    }
}
