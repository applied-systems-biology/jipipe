/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.app.resultanalysis.renderers;

import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * Renders {@link org.hkijena.jipipe.api.annotation.JIPipeAnnotation}
 */
public class JIPipeDesktopAnnotationTableCellRenderer extends JLabel implements TableCellRenderer {

    /**
     * Creates a new renderer
     */
    public JIPipeDesktopAnnotationTableCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof JIPipeTextAnnotation) {
            setText(((JIPipeTextAnnotation) value).getValue());
        } else {
            setText("<html><p style=\"color: red;\">NA</p></html>");
        }

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }

        return this;
    }
}
