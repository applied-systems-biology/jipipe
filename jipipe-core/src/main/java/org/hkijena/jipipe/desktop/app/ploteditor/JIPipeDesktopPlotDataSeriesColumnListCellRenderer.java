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

package org.hkijena.jipipe.desktop.app.ploteditor;

import org.hkijena.jipipe.plugins.tables.datatypes.DoubleArrayTableColumnData;
import org.hkijena.jipipe.plugins.tables.datatypes.StringArrayTableColumnData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Renders entries
 */
public class JIPipeDesktopPlotDataSeriesColumnListCellRenderer extends JLabel implements ListCellRenderer<TableColumnData> {

    /**
     * Creates a new renderer
     */
    public JIPipeDesktopPlotDataSeriesColumnListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends TableColumnData> list, TableColumnData value, int index, boolean isSelected, boolean cellHasFocus) {

        if (value instanceof DoubleArrayTableColumnData) {
            DoubleArrayTableColumnData data = (DoubleArrayTableColumnData) value;
            setText(data.getLabel() + " (" + data.getData().length + " rows)");
            setIcon(UIUtils.getIconFromResources("data-types/results-table.png"));
        } else if (value instanceof StringArrayTableColumnData) {
            StringArrayTableColumnData data = (StringArrayTableColumnData) value;
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
