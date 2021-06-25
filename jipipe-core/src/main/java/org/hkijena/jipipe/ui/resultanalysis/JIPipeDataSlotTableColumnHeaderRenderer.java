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

import org.hkijena.jipipe.api.data.JIPipeExportedDataTable;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;

/**
 * Renders additional {@link String} columns in a table
 */
public class JIPipeDataSlotTableColumnHeaderRenderer implements TableCellRenderer {
    private JIPipeExportedDataTable dataTable;

    /**
     * Creates a new instance
     *
     * @param dataTable The table
     */
    public JIPipeDataSlotTableColumnHeaderRenderer(JIPipeExportedDataTable dataTable) {
        this.dataTable = dataTable;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        TableCellRenderer defaultRenderer = table.getTableHeader().getDefaultRenderer();
        int modelColumn = table.convertColumnIndexToModel(column);
        if (modelColumn < 3) {
            return defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        } else if(modelColumn < dataTable.getDataAnnotationColumns().size() + 3) {
            String info = dataTable.getDataAnnotationColumns().get(modelColumn - 3);
            String html = String.format("<html><table><tr><td><img src=\"%s\"/></td><td>%s</tr>",
                    UIUtils.getIconFromResources("data-types/data-annotation.png"),
                    info);
            return defaultRenderer.getTableCellRendererComponent(table, html, isSelected, hasFocus, row, column);
        }else {
            String info = dataTable.getAnnotationColumns().get(modelColumn - dataTable.getDataAnnotationColumns().size() - 3);
            String html = String.format("<html><table><tr><td><img src=\"%s\"/></td><td>%s</tr>",
                    UIUtils.getIconFromResources("data-types/annotation.png"),
                    info);
            return defaultRenderer.getTableCellRendererComponent(table, html, isSelected, hasFocus, row, column);
        }
    }
}
