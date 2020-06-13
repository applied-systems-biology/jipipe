package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.data.ACAQAnnotation;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * Renders {@link ACAQAnnotation}
 */
public class ACAQTraitTableCellRenderer extends JLabel implements TableCellRenderer {

    /**
     * Creates a new renderer
     */
    public ACAQTraitTableCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof ACAQAnnotation) {
            setText(((ACAQAnnotation) value).getValue());
        } else {
            setText("<html><p style=\"color: red;\">NA</p></html>");
        }

        if (isSelected) {
            setBackground(new Color(184, 207, 229));
        } else {
            setBackground(new Color(255, 255, 255));
        }

        return this;
    }
}
