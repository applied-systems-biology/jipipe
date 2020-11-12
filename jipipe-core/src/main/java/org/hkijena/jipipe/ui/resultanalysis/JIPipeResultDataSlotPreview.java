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
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import java.awt.BorderLayout;
import java.nio.file.Path;

/**
 * Renders a {@link JIPipeDataSlot} row as table cell.
 * The component should default to a light render workload (e.g. a status text).
 * Run renderPreview() to run any kind of heavy workload
 */
public abstract class JIPipeResultDataSlotPreview extends JIPipeProjectWorkbenchPanel {

    private final JTable table;
    private final JIPipeDataSlot slot;
    private final JIPipeExportedDataTable.Row row;

    /**
     * Creates a new renderer
     *
     * @param workbench the workbench
     * @param table     the table where the data is rendered in
     * @param slot      the data slot
     * @param row       the row
     */
    public JIPipeResultDataSlotPreview(JIPipeProjectWorkbench workbench, JTable table, JIPipeDataSlot slot, JIPipeExportedDataTable.Row row) {
        super(workbench);
        this.slot = slot;
        this.row = row;
        setLayout(new BorderLayout());
        setOpaque(false);
        this.table = table;
    }

    /**
     * This function is called from outside to render the preview.
     * Please not that it is called everytime the cell is rendered, meaning that you should prevent reloading the data all the time.
     */
    public abstract void renderPreview();

    /**
     * @return the table where the data is rendered in
     */
    public JTable getTable() {
        return table;
    }

    public JIPipeDataSlot getSlot() {
        return slot;
    }

    public JIPipeExportedDataTable.Row getRow() {
        return row;
    }

    /**
     * Rebuilds the table to show the updated preview
     */
    public void refreshTable() {
        if (getTable() != null) {
            if (getTable() instanceof JXTable)
                ((JXTable) getTable()).packAll();
            getTable().repaint();
        }
    }

    /**
     * Returns the compartment name of the algorithm that generated the data
     *
     * @param workbenchUI The workbench
     * @param slot        The data slot
     * @return The algorithm compartment
     */
    public static String getAlgorithmCompartment(JIPipeProjectWorkbench workbenchUI, JIPipeDataSlot slot) {
        return workbenchUI.getProject().getCompartments().get(slot.getNode().getCompartment()).getName();
    }

    /**
     * Returns the name of the algorithm that generated the data
     *
     * @param slot The data slot
     * @return The algorithm name
     */
    public static String getAlgorithmName(JIPipeDataSlot slot) {
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
    public static String getDisplayName(JIPipeProjectWorkbench workbenchUI, JIPipeDataSlot slot, JIPipeExportedDataTable.Row row) {
        return getAlgorithmCompartment(workbenchUI, slot) + "/" + getAlgorithmName(slot) + "/" + slot.getName() + "/" + row.getIndex();
    }

    /**
     * Returns the folder where the data is stored
     *
     * @param slot The data slot
     * @param row  The data slot row
     * @return The row storage folder
     */
    public static Path getRowStorageFolder(JIPipeDataSlot slot, JIPipeExportedDataTable.Row row) {
        return slot.getStoragePath().resolve("" + row.getIndex());
    }
}
