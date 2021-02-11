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

package org.hkijena.jipipe.ui.plotbuilder;

import org.hkijena.jipipe.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.StringArrayTableColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

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
            setIcon(UIUtils.getIconFromResources("data-types/results-table.png"));
        } else if (value instanceof StringArrayTableColumn) {
            StringArrayTableColumn data = (StringArrayTableColumn) value;
            setText(data.getLabel() + " (" + data.getData().length + " rows)");
            setIcon(UIUtils.getIconFromResources("data-types/results-table.png"));
        } else if (value != null) {
            setText(value.getLabel());
            setIcon(UIUtils.getIconFromResources("actions/configure.png"));
        } else {
            setText("None selected");
            setIcon(UIUtils.getIconFromResources("emblems/vcs-conflicting.png"));
        }

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }

        return this;
    }
}
