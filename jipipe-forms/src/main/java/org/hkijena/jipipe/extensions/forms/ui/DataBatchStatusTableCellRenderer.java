package org.hkijena.jipipe.extensions.forms.ui;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;

public class DataBatchStatusTableCellRenderer extends JLabel implements TableCellRenderer {

    public static final Color COLOR_UNVISITED = new Color(0xefd78e);
    public static final Color COLOR_INVALID = new Color(0xef8e8e);
    public static final Color COLOR_INVALID_SELECTED = new Color(0xb57979);
    private final List<DataBatchStatus> dataBatchStatuses;

    public DataBatchStatusTableCellRenderer(List<DataBatchStatus> dataBatchStatuses) {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        this.dataBatchStatuses = dataBatchStatuses;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setText("" + value);
        DataBatchStatus status = dataBatchStatuses.get(table.convertRowIndexToModel(row));
        if (isSelected) {
            if (status == DataBatchStatus.Invalid)
                setBackground(COLOR_INVALID_SELECTED);
            else
                setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            if (status == DataBatchStatus.Invalid)
                setBackground(COLOR_INVALID);
            else if (status == DataBatchStatus.Unvisited)
                setBackground(COLOR_UNVISITED);
            else
                setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }
}
