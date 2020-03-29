package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQExportedDataTable;
import org.hkijena.acaq5.ui.ACAQProjectUI;

import javax.swing.*;
import java.nio.file.Path;

/**
 * A UI that is rendered in a TableCell.
 */
public abstract class ACAQResultDataSlotCellUI extends JLabel {
    public ACAQResultDataSlotCellUI() {
    }

    /**
     * Returns the compartment name of the algorithm that generated the data
     *
     * @return
     */
    public static String getAlgorithmCompartment(ACAQProjectUI workbenchUI, ACAQDataSlot slot) {
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
    public static String getDisplayName(ACAQProjectUI workbenchUI, ACAQDataSlot slot, ACAQExportedDataTable.Row row) {
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

    public abstract void render(ACAQProjectUI workbenchUI, ACAQDataSlot slot, ACAQExportedDataTable.Row row);
}
