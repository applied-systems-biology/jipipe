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
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableMetadataRow;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;

import java.nio.file.Path;

/**
 * A UI that contains various actions for importing/processing output data
 * Please note that the data is not present in the data slot anymore at this point, but instead
 * stored in output files. The folder that contains the files can be accessed via getRowStorageFolder()
 */
public abstract class JIPipeResultDataSlotRowUI extends JIPipeProjectWorkbenchPanel {
    private final JIPipeDataSlot slot;
    private final JIPipeDataTableMetadataRow row;

    /**
     * @param workbenchUI The workbench
     * @param slot        The data slow
     * @param row         The slow row
     */
    public JIPipeResultDataSlotRowUI(JIPipeProjectWorkbench workbenchUI, JIPipeDataSlot slot, JIPipeDataTableMetadataRow row) {
        super(workbenchUI);
        this.slot = slot;
        this.row = row;
    }

    /**
     * Action that is triggered when the user double-clicks on the entry in the table
     */
    public abstract void handleDefaultAction();

    /**
     * Runs the currently set default action for this data.
     * If the data column index is valid, the associated data annotation is displayed instead (using its appropriate standard action)
     *
     * @param dataAnnotationColumn column index of the data column in the data table. if outside the range, silently will run the default data operation instead
     */
    public abstract void handleDefaultActionOrDisplayDataAnnotation(int dataAnnotationColumn);

    /**
     * Returns the data slot
     *
     * @return the data slot
     */
    public JIPipeDataSlot getSlot() {
        return slot;
    }

    /**
     * Returns the data row that is displayed
     *
     * @return the data row that is displayed
     */
    public JIPipeDataTableMetadataRow getRow() {
        return row;
    }

    /**
     * Returns the compartment name of the algorithm that generated the data
     *
     * @return the compartment name of the algorithm that generated the data
     */
    public String getAlgorithmCompartmentName() {
        return getProjectWorkbench().getProject().getCompartments().get(slot.getNode().getCompartmentUUIDInParentGraph()).getName();
    }

    /**
     * Returns the name of the algorithm that generated the data
     *
     * @return the name of the algorithm that generated the data
     */
    public String getAlgorithmName() {
        return slot.getNode().getName();
    }

    /**
     * Returns a name that identifies this row
     *
     * @return a name that identifies this row
     */
    public String getDisplayName() {
        return getAlgorithmCompartmentName() + "/" + getAlgorithmName() + "/" + getSlot().getName() + "/" + getRow().getIndex();
    }

    /**
     * Returns the folder where the data is stored
     *
     * @return the folder where the data is stored
     */
    public Path getRowStorageFolder() {
        return slot.getSlotStoragePath().resolve("" + getRow().getIndex());
    }
}
