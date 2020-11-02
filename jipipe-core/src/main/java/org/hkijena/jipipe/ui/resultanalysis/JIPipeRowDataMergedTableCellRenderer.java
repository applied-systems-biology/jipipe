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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeExportedDataTable;
import org.hkijena.jipipe.api.data.JIPipeMergedExportedDataTable;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders data in {@link JIPipeMergedExportedDataTable}
 */
public class JIPipeRowDataMergedTableCellRenderer implements TableCellRenderer {

    private JIPipeProjectWorkbench workbenchUI;
    private List<JIPipeResultDataSlotPreviewUI> previewCache = new ArrayList<>();

    /**
     * @param workbenchUI     The workbench
     * @param mergedDataTable the table to be displayed
     */
    public JIPipeRowDataMergedTableCellRenderer(JIPipeProjectWorkbench workbenchUI, JIPipeMergedExportedDataTable mergedDataTable) {
        this.workbenchUI = workbenchUI;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof JIPipeExportedDataTable.Row) {
            JIPipeMergedExportedDataTable model = (JIPipeMergedExportedDataTable) table.getModel();
            JIPipeDataSlot slot = model.getSlot(table.convertRowIndexToModel(row));
            while(row > previewCache.size() - 1) {
                previewCache.add(null);
            }
            JIPipeResultDataSlotPreviewUI preview = previewCache.get(row);
            if (preview == null) {
                preview = JIPipe.getDataTypes().getCellRendererFor(slot.getAcceptedDataType(), table);
                preview.render(workbenchUI, slot, (JIPipeExportedDataTable.Row) value);
                previewCache.set(row, preview);
            }
            if (isSelected) {
                preview.setBackground(UIManager.getColor("List.selectionBackground"));
            } else {
                preview.setBackground(UIManager.getColor("List.background"));
            }
            return preview;
        }
        return null;
    }
}
