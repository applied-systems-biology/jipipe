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

import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableInfo;
import org.hkijena.jipipe.api.data.serialization.JIPipeMergedDataTableInfo;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * Renders the column header in merged {@link JIPipeDataTableInfo} instances
 */
public class JIPipeDesktopMergedDataSlotTableColumnHeaderRenderer implements TableCellRenderer {
    private JIPipeMergedDataTableInfo dataTable;

    /**
     * @param dataTable The table
     */
    public JIPipeDesktopMergedDataSlotTableColumnHeaderRenderer(JIPipeMergedDataTableInfo dataTable) {
        this.dataTable = dataTable;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        TableCellRenderer defaultRenderer = table.getTableHeader().getDefaultRenderer();
        if (column < 5) {
            return defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        } else if (dataTable.toDataAnnotationColumnIndex(column) != -1) {
            String info = dataTable.getDataAnnotationColumns().get(dataTable.toDataAnnotationColumnIndex(column));
            String html = String.format("<html><table><tr><td><img src=\"%s\"/></td><td>%s</tr>",
                    UIUtils.getIconFromResources("data-types/data-annotation.png"),
                    info);
            return defaultRenderer.getTableCellRendererComponent(table, html, isSelected, hasFocus, row, column);
        } else {
            String info = dataTable.getAnnotationColumns().get(dataTable.toAnnotationColumnIndex(column));
            String html = String.format("<html><table><tr><td><img src=\"%s\"/></td><td>%s</tr>",
                    UIUtils.getIconFromResources("data-types/annotation.png"),
                    info);
            return defaultRenderer.getTableCellRendererComponent(table, html, isSelected, hasFocus, row, column);
        }
    }
}
