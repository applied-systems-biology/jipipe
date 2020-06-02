package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQExportedDataTable;
import org.hkijena.acaq5.api.data.ACAQMergedExportedDataTable;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;

/**
 * Renders data in {@link ACAQMergedExportedDataTable}
 */
public class ACAQRowDataMergedTableCellRenderer implements TableCellRenderer {

    private ACAQProjectWorkbench workbenchUI;

    /**
     * @param workbenchUI The workbench
     */
    public ACAQRowDataMergedTableCellRenderer(ACAQProjectWorkbench workbenchUI) {
        this.workbenchUI = workbenchUI;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof ACAQExportedDataTable.Row) {
            ACAQMergedExportedDataTable model = (ACAQMergedExportedDataTable) table.getModel();
            ACAQDataSlot slot = model.getSlot(row);
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
