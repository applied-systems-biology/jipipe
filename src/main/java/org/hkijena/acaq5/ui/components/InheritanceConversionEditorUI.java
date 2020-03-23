package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.api.data.ACAQDataDeclaration;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.Objects;

public class InheritanceConversionEditorUI extends JPanel {
    private Map<ACAQDataDeclaration, ACAQDataDeclaration> inheritanceConversions;
    private JList<Map.Entry<ACAQDataDeclaration, ACAQDataDeclaration>> list;

    public InheritanceConversionEditorUI(Map<ACAQDataDeclaration, ACAQDataDeclaration> inheritanceConversions) {
        this.inheritanceConversions = inheritanceConversions;
        initialize();
        reload();
    }

    private void reload() {
        DefaultListModel<Map.Entry<ACAQDataDeclaration, ACAQDataDeclaration>> model = new DefaultListModel<>();
        for (Map.Entry<ACAQDataDeclaration, ACAQDataDeclaration> entry : inheritanceConversions.entrySet()) {
            model.addElement(entry);
        }
        list.setModel(model);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        list = new JList<>();
        list.setCellRenderer(new ACAQInheritanceConversionListCellRenderer());
        add(list, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JButton addButton = new JButton("Add", UIUtils.getIconFromResources("add.png"));
        addButton.addActionListener(e -> addEntry());
        toolBar.add(addButton);

        JButton removeButton = new JButton(UIUtils.getIconFromResources("delete.png"));
        removeButton.addActionListener(e -> removeSelectedEntries());
        toolBar.add(removeButton);
        add(toolBar, BorderLayout.NORTH);
    }

    private void removeSelectedEntries() {
        for (Map.Entry<ACAQDataDeclaration, ACAQDataDeclaration> entry :
                list.getSelectedValuesList()) {
            inheritanceConversions.remove(entry.getKey());
        }
        reload();
    }

    private void addEntry() {
        FormPanel formPanel = new FormPanel(null, false, false, false);
        ACAQDataDeclaration[] available = ACAQDatatypeRegistry.getInstance().getUnhiddenRegisteredDataTypes().values()
                .stream().map(ACAQDataDeclaration::getInstance).toArray(ACAQDataDeclaration[]::new);
        JComboBox<ACAQDataDeclaration> from = new JComboBox<>(available);
        from.setRenderer(new ACAQDataDeclarationListCellRenderer());
        JComboBox<ACAQDataDeclaration> to = new JComboBox<>(available);
        to.setRenderer(new ACAQDataDeclarationListCellRenderer());
        formPanel.addToForm(from, new JLabel("From"), null);
        formPanel.addToForm(to, new JLabel("To"), null);

        if (JOptionPane.showConfirmDialog(this,
                formPanel,
                "Add conversion",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            ACAQDataDeclaration selectedFrom = (ACAQDataDeclaration) from.getSelectedItem();
            ACAQDataDeclaration selectedTo = (ACAQDataDeclaration) to.getSelectedItem();
            if (!Objects.equals(selectedFrom, selectedTo)) {
                inheritanceConversions.put(selectedFrom, selectedTo);
                reload();
            }
        }
    }
}
