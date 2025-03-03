package org.hkijena.jipipe.desktop.commons.components.filechoosernext;

import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.time.LocalDateTime;

public class JIPipeDesktopFileChooserNextDateTableCellRenderer extends JLabel implements TableCellRenderer {

    public JIPipeDesktopFileChooserNextDateTableCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        setHorizontalAlignment(JLabel.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        if(value instanceof LocalDateTime) {
          setText(StringUtils.formatDateTime((LocalDateTime) value));
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
