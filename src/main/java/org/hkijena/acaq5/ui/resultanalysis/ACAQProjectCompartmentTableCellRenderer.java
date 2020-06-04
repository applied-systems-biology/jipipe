package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * Renders an {@link org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment} cell
 */
public class ACAQProjectCompartmentTableCellRenderer extends JLabel implements TableCellRenderer {

    /**
     * Creates new instance
     */
    public ACAQProjectCompartmentTableCellRenderer() {
        setOpaque(true);
        setIcon(UIUtils.getIconFromResources("graph-compartment.png"));
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        if (value instanceof ACAQGraphNode) {
            ACAQGraphNode algorithm = (ACAQGraphNode) value;
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
