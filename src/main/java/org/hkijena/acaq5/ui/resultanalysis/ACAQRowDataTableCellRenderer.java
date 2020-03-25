package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQExportedDataTable;
import org.hkijena.acaq5.ui.ACAQProjectUI;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class ACAQRowDataTableCellRenderer implements TableCellRenderer {

    private ACAQProjectUI workbenchUI;
    private ACAQDataSlot slot;

    public ACAQRowDataTableCellRenderer(ACAQProjectUI workbenchUI, ACAQDataSlot slot) {
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
