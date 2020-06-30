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

import org.hkijena.acaq5.api.data.ACAQDataDeclaration;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.util.Map;
import java.util.Objects;

/**
 * Editor for an inheritance conversion stored in an {@link org.hkijena.acaq5.api.data.ACAQSlotDefinition}
 */
public class InheritanceConversionEditorUI extends JPanel {
    private Map<ACAQDataDeclaration, ACAQDataDeclaration> inheritanceConversions;
    private JList<Map.Entry<ACAQDataDeclaration, ACAQDataDeclaration>> list;

    /**
     * @param inheritanceConversions the inheritance conversion
     */
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
        FormPanel formPanel = new FormPanel(null, FormPanel.NONE);
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
