/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.resultanalysis;

import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.*;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * Renders an {@link org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment} cell
 */
public class JIPipeProjectCompartmentTableCellRenderer extends JLabel implements TableCellRenderer {

    /**
     * Creates new instance
     */
    public JIPipeProjectCompartmentTableCellRenderer() {
        setOpaque(true);
        setIcon(UIUtils.getIconFromResources("data-types/graph-compartment.png"));
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        if (value instanceof JIPipeGraphNode) {
            JIPipeGraphNode algorithm = (JIPipeGraphNode) value;
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
