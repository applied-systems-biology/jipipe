package org.hkijena.jipipe.desktop.commons.components.filechoosernext;

import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.nio.file.Path;

public class JIPipeDesktopFileChooserNextSizeTableCellRenderer extends JLabel implements TableCellRenderer {

    public JIPipeDesktopFileChooserNextSizeTableCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        setHorizontalAlignment(JLabel.RIGHT);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        if(value instanceof Number) {
            long size = ((Number) value).longValue();
            if(size == 0) {
                setText("");
            }
            else if(size < 0) {
                setText((-size) + " elements");
            }
            else {
                setText(StringUtils.formatSize(size));
            }
        }
        else {
            setText("");
        }

        if(isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        }
        else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }
}
