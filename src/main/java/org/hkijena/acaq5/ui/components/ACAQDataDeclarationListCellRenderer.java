package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.api.data.ACAQDataDeclaration;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;

import javax.swing.*;
import java.awt.*;

public class ACAQDataDeclarationListCellRenderer extends JLabel implements ListCellRenderer<ACAQDataDeclaration> {

    public ACAQDataDeclarationListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ACAQDataDeclaration> list, ACAQDataDeclaration value, int index, boolean isSelected, boolean cellHasFocus) {
        if (list.getFont() != null) {
            setFont(list.getFont());
        }
        if (value != null) {
            setText(value.getName());
            setIcon(ACAQUIDatatypeRegistry.getInstance().getIconFor(value.getDataClass()));
        } else {
            setText("<No data type>");
            setIcon(null);
        }

        if (isSelected) {
            setBackground(new Color(184, 207, 229));
        } else {
            setBackground(new Color(255, 255, 255));
        }
        return this;
    }
}
