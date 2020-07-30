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

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeExportedDataTable;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.registries.JIPipeUIDatatypeRegistry;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders row data of an {@link JIPipeExportedDataTable}
 */
public class JIPipeRowDataTableCellRenderer implements TableCellRenderer {

    private JIPipeProjectWorkbench workbenchUI;
    private JIPipeDataSlot slot;
    private List<JIPipeResultDataSlotPreviewUI> previewCache = new ArrayList<>();

    /**
     * @param workbenchUI the workbench
     * @param slot        the data slot
     */
    public JIPipeRowDataTableCellRenderer(JIPipeProjectWorkbench workbenchUI, JIPipeDataSlot slot) {
        this.workbenchUI = workbenchUI;
        this.slot = slot;
        for (int i = 0; i < slot.getRowCount(); i++) {
            previewCache.add(null);
        }
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof JIPipeExportedDataTable.Row) {
            JIPipeResultDataSlotPreviewUI preview = previewCache.get(row);
            if(preview == null) {
                preview = JIPipeUIDatatypeRegistry.getInstance().getCellRendererFor(slot.getAcceptedDataType(), table);
                preview.render(workbenchUI, slot, (JIPipeExportedDataTable.Row) value);
                previewCache.set(row, preview);
            }
            if (isSelected) {
                preview.setBackground(new Color(184, 207, 229));
            } else {
                preview.setBackground(new Color(255, 255, 255));
            }
            return preview;
        }
        return null;
    }
}
