package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQExportedDataTable;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;

import javax.swing.*;
import java.nio.file.Path;

/**
 * A UI that is rendered in a TableCell.
 */
public abstract class ACAQResultDataSlotCellUI extends JLabel {
    public ACAQResultDataSlotCellUI() {
    }

    public abstract void render(ACAQWorkbenchUI workbenchUI, ACAQDataSlot slot, ACAQExportedDataTable.Row row);

    /**
     * Returns the compartment name of the algorithm that generated the data
     *
     * @return
     */
    public static String getAlgorithmCompartment(ACAQWorkbenchUI workbenchUI, ACAQDataSlot slot) {
        return workbenchUI.getProject().getCompartments().get(slot.getAlgorithm().getCompartment()).getName();
    }

    /**
     * Returns the name of the algorithm that generated the data
     *
     * @return
     */
    public static String getAlgorithmName(ACAQDataSlot slot) {
        return slot.getAlgorithm().getName();
    }

    /**
     * Returns a name that identifies this row
     *
     * @return
     */
    public static String getDisplayName(ACAQWorkbenchUI workbenchUI, ACAQDataSlot slot, ACAQExportedDataTable.Row row) {
        return getAlgorithmCompartment(workbenchUI, slot) + "/" + getAlgorithmName(slot) + "/" + slot.getName() + "/" + row.getLocation();
    }

    /**
     * Returns the folder where the data is stored
     *
     * @return
     */
    public static Path getRowStorageFolder(ACAQDataSlot slot, ACAQExportedDataTable.Row row) {
        return slot.getStoragePath().resolve(row.getLocation());
    }
}
