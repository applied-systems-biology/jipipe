package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.data.ACAQExportedDataTable;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.ui.registries.ACAQUITraitRegistry;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class ACAQDataSlotTableColumnHeaderRenderer implements TableCellRenderer {
    private ACAQExportedDataTable dataTable;

    public ACAQDataSlotTableColumnHeaderRenderer(ACAQExportedDataTable dataTable) {
        this.dataTable = dataTable;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        TableCellRenderer defaultRenderer = table.getTableHeader().getDefaultRenderer();
        if (column < 2) {
            return defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        } else {
            ACAQTraitDeclaration declaration = dataTable.getTraitColumns().get(column - 2);
            String html = String.format("<html><table><tr><td><img src=\"%s\"/></td><td>%s</tr>",
                    ACAQUITraitRegistry.getInstance().getIconURLFor(declaration).toString(),
                    declaration.getName());
            return defaultRenderer.getTableCellRendererComponent(table, html, isSelected, hasFocus, row, column);
        }
    }
}
