package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Renders {@link ResultsTableData}
 */
public class ResultsTableDataListCellRenderer extends JLabel implements ListCellRenderer<ResultsTableData> {

    /**
     * Creates new instance
     */
    public ResultsTableDataListCellRenderer() {
        setOpaque(true);
        setIcon(UIUtils.getIconFromResources("data-types/results-table.png"));
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ResultsTableData> list, ResultsTableData value, int index, boolean isSelected, boolean cellHasFocus) {
        setText("" + value);
        if (isSelected) {
            setBackground(new Color(184, 207, 229));
        } else {
            setBackground(new Color(255, 255, 255));
        }
        return this;
    }
}
