package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.traits.ACAQDiscriminator;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;

public class ACAQTraitTableCellRenderer extends JLabel implements TableCellRenderer {

    public ACAQTraitTableCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if(value instanceof ACAQTrait) {
            if(value instanceof ACAQDiscriminator) {
                setText(((ACAQDiscriminator) value).getValue());
            }
            else {
                setText("<html><p style=\"color: green;\">Yes</p></html>");
            }
        }
        else {
            setText("<html><p style=\"color: red;\">No</p></html>");
        }

        if (isSelected) {
            setBackground(new Color(184, 207, 229));
        } else {
            setBackground(new Color(255, 255, 255));
        }

        return this;
    }
}
