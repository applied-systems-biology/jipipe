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

package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQExportedDataTable;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;

import javax.swing.*;
import java.nio.file.Path;

/**
 * Renders a {@link ACAQDataSlot} row as table cell
 */
public abstract class ACAQResultDataSlotCellUI extends JLabel {
    /**
     * Creates a new renderer
     */
    public ACAQResultDataSlotCellUI() {
    }

    /**
     * Renders the data slot row
     *
     * @param workbenchUI the workbench
     * @param slot        The data slot
     * @param row         The data slot row
     */
    public abstract void render(ACAQProjectWorkbench workbenchUI, ACAQDataSlot slot, ACAQExportedDataTable.Row row);

    /**
     * Returns the compartment name of the algorithm that generated the data
     *
     * @param workbenchUI The workbench
     * @param slot        The data slot
     * @return The algorithm compartment
     */
    public static String getAlgorithmCompartment(ACAQProjectWorkbench workbenchUI, ACAQDataSlot slot) {
        return workbenchUI.getProject().getCompartments().get(slot.getNode().getCompartment()).getName();
    }

    /**
     * Returns the name of the algorithm that generated the data
     *
     * @param slot The data slot
     * @return The algorithm name
     */
    public static String getAlgorithmName(ACAQDataSlot slot) {
        return slot.getNode().getName();
    }

    /**
     * Returns a name that identifies this row
     *
     * @param workbenchUI The workbench UI
     * @param slot        The data slot
     * @param row         The data slot row
     * @return The display name
     */
    public static String getDisplayName(ACAQProjectWorkbench workbenchUI, ACAQDataSlot slot, ACAQExportedDataTable.Row row) {
        return getAlgorithmCompartment(workbenchUI, slot) + "/" + getAlgorithmName(slot) + "/" + slot.getName() + "/" + row.getLocation();
    }

    /**
     * Returns the folder where the data is stored
     *
     * @param slot The data slot
     * @param row  The data slot row
     * @return The row storage folder
     */
    public static Path getRowStorageFolder(ACAQDataSlot slot, ACAQExportedDataTable.Row row) {
        return slot.getStoragePath().resolve(row.getLocation());
    }
}
