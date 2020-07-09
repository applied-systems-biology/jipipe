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

package org.hkijena.acaq5.ui.plotbuilder;

import org.hkijena.acaq5.extensions.tables.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.StringArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.TableColumn;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;

/**
 * Renders entries
 */
public class PlotDataSeriesColumnListCellRenderer extends JLabel implements ListCellRenderer<TableColumn> {

    /**
     * Creates a new renderer
     */
    public PlotDataSeriesColumnListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends TableColumn> list, TableColumn value, int index, boolean isSelected, boolean cellHasFocus) {

        if (value instanceof DoubleArrayTableColumn) {
            DoubleArrayTableColumn data = (DoubleArrayTableColumn) value;
            setText(data.getLabel() + " (" + data.getData().length + " rows)");
            setIcon(UIUtils.getIconFromResources("table.png"));
        } else if (value instanceof StringArrayTableColumn) {
            StringArrayTableColumn data = (StringArrayTableColumn) value;
            setText(data.getLabel() + " (" + data.getData().length + " rows)");
            setIcon(UIUtils.getIconFromResources("table.png"));
        } else if (value != null) {
            setText(value.getLabel());
            setIcon(UIUtils.getIconFromResources("cog.png"));
        } else {
            setText("None selected");
            setIcon(UIUtils.getIconFromResources("error.png"));
        }

        if (isSelected) {
            setBackground(new Color(184, 207, 229));
        } else {
            setBackground(new Color(255, 255, 255));
        }

        return this;
    }
}
