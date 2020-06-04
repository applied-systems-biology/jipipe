package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * Renders {@link ACAQGraphNode} instances in a table cell
 */
public class ACAQAlgorithmTableCellRenderer extends JLabel implements TableCellRenderer {

    /**
     * Creates a new renderer
     */
    public ACAQAlgorithmTableCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        if (value instanceof ACAQGraphNode) {
            ACAQGraphNode algorithm = (ACAQGraphNode) value;
            setIcon(UIUtils.getIconFromColor(UIUtils.getFillColorFor(algorithm.getDeclaration())));
            setText(algorithm.getName());
        }

        if (isSelected) {
            setBackground(new Color(184, 207, 229));
        } else {
            setBackground(new Color(255, 255, 255));
        }

        return this;
    }
}
