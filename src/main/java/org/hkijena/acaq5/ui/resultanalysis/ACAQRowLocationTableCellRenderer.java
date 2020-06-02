package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.nio.file.Path;

/**
 * Renders the location of of {@link org.hkijena.acaq5.api.data.ACAQExportedDataTable} and {@link org.hkijena.acaq5.api.data.ACAQMergedExportedDataTable}
 */
public class ACAQRowLocationTableCellRenderer extends JLabel implements TableCellRenderer {

    /**
     * Creates new renderer
     */
    public ACAQRowLocationTableCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        setIcon(UIUtils.getIconFromResources("database.png"));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        if (value instanceof Path) {
            setText(value.toString());
        }

        if (isSelected) {
            setBackground(new Color(184, 207, 229));
        } else {
            setBackground(new Color(255, 255, 255));
        }

        return this;
    }
}
