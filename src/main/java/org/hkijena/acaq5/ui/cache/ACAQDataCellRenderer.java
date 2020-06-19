package org.hkijena.acaq5.ui.cache;

import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;

/**
 * Renders {@link ACAQData}
 */
public class ACAQDataCellRenderer extends JLabel implements TableCellRenderer {

    /**
     * Create new instance
     */
    public ACAQDataCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof ACAQData) {
            ACAQData data = (ACAQData) value;
            setIcon(ACAQUIDatatypeRegistry.getInstance().getIconFor(data.getClass()));
            setText(ACAQData.getNameOf(data.getClass()));
        }
        if (isSelected) {
            setBackground(new Color(184, 207, 229));
        } else {
            setBackground(new Color(255, 255, 255));
        }
        return this;
    }
}
