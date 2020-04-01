package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQExportedDataTable;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;

import javax.swing.*;

/**
 * Renders a {@link ACAQDataSlot} row as table cell
 */
public class ACAQDefaultResultDataSlotCellUI extends ACAQResultDataSlotCellUI {

    /**
     * Creates a new renderer
     */
    public ACAQDefaultResultDataSlotCellUI() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public void render(ACAQProjectWorkbench workbenchUI, ACAQDataSlot slot, ACAQExportedDataTable.Row row) {
        setIcon(ACAQUIDatatypeRegistry.getInstance().getIconFor(slot.getAcceptedDataType()));
        setText(ACAQData.getNameOf(slot.getAcceptedDataType()));
    }
}
