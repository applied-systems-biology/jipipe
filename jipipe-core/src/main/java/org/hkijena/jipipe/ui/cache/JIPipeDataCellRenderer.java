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

package org.hkijena.jipipe.ui.cache;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeData;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;

/**
 * Renders {@link JIPipeData}
 */
public class JIPipeDataCellRenderer extends JLabel implements TableCellRenderer {

    /**
     * Create new instance
     */
    public JIPipeDataCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof JIPipeData) {
            JIPipeData data = (JIPipeData) value;
            setIcon(JIPipe.getDataTypes().getIconFor(data.getClass()));
            setText(JIPipeData.getNameOf(data.getClass()));
        }
        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }
}
