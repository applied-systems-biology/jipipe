package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQExportedDataTable;
import org.hkijena.acaq5.ui.ACAQProjectUI;
import org.hkijena.acaq5.ui.ACAQProjectUIPanel;

import java.nio.file.Path;

/**
 * A UI that contains various actions for importing/processing output data
 * Please note that the data is not present in the data slot anymore at this point, but instead
 * stored in output files. The folder that contains the files can be accessed via getRowStorageFolder()
 */
public abstract class ACAQResultDataSlotRowUI extends ACAQProjectUIPanel {
    private ACAQDataSlot slot;
    private ACAQExportedDataTable.Row row;

    /**
     * @param workbenchUI The workbench
     * @param slot The data slow
     * @param row The slow row
     */
    public ACAQResultDataSlotRowUI(ACAQProjectUI workbenchUI, ACAQDataSlot slot, ACAQExportedDataTable.Row row) {
        super(workbenchUI);
        this.slot = slot;
        this.row = row;
    }

    /**
     * Action that is triggered when the user double-clicks on the entry in the table
     */
    public abstract void handleDefaultAction();

    /**
     * Returns the data slot
     *
     * @return the data slot
     */
    public ACAQDataSlot getSlot() {
        return slot;
    }

    /**
     * Returns the data row that is displayed
     *
     * @return the data row that is displayed
     */
    public ACAQExportedDataTable.Row getRow() {
        return row;
    }

    /**
     * Returns the compartment name of the algorithm that generated the data
     *
     * @return the compartment name of the algorithm that generated the data
     */
    public String getAlgorithmCompartment() {
        return getWorkbenchUI().getProject().getCompartments().get(slot.getAlgorithm().getCompartment()).getName();
    }

    /**
     * Returns the name of the algorithm that generated the data
     *
     * @return the name of the algorithm that generated the data
     */
    public String getAlgorithmName() {
        return slot.getAlgorithm().getName();
    }

    /**
     * Returns a name that identifies this row
     *
     * @return a name that identifies this row
     */
    public String getDisplayName() {
        return getAlgorithmCompartment() + "/" + getAlgorithmName() + "/" + getSlot().getName() + "/" + getRow().getLocation();
    }

    /**
     * Returns the folder where the data is stored
     *
     * @return the folder where the data is stored
     */
    public Path getRowStorageFolder() {
        return slot.getStoragePath().resolve(getRow().getLocation());
    }
}
