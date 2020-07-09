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

package org.hkijena.pipelinej.ui.resultanalysis;

import org.hkijena.pipelinej.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.nio.file.Path;

/**
 * Renders the location of of {@link org.hkijena.pipelinej.api.data.ACAQExportedDataTable} and {@link org.hkijena.pipelinej.api.data.ACAQMergedExportedDataTable}
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
