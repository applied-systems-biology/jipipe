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

import org.hkijena.pipelinej.api.data.ACAQDataSlot;
import org.hkijena.pipelinej.api.data.ACAQExportedDataTable;
import org.hkijena.pipelinej.ui.ACAQProjectWorkbench;
import org.hkijena.pipelinej.ui.registries.ACAQUIDatatypeRegistry;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * Renders row data of an {@link ACAQExportedDataTable}
 */
public class ACAQRowDataTableCellRenderer implements TableCellRenderer {

    private ACAQProjectWorkbench workbenchUI;
    private ACAQDataSlot slot;

    /**
     * @param workbenchUI the workbench
     * @param slot        the data slot
     */
    public ACAQRowDataTableCellRenderer(ACAQProjectWorkbench workbenchUI, ACAQDataSlot slot) {
        this.workbenchUI = workbenchUI;
        this.slot = slot;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof ACAQExportedDataTable.Row) {
            ACAQResultDataSlotCellUI renderer = ACAQUIDatatypeRegistry.getInstance().getCellRendererFor(slot.getAcceptedDataType());
            renderer.render(workbenchUI, slot, (ACAQExportedDataTable.Row) value);
            if (isSelected) {
                renderer.setBackground(new Color(184, 207, 229));
            } else {
                renderer.setBackground(new Color(255, 255, 255));
            }
            return renderer;
        }
        return null;
    }
}
