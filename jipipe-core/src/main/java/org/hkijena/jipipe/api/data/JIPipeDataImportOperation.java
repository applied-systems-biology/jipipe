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

package org.hkijena.jipipe.api.data;

import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * An operation that is executed on showing existing data located in memory/cache
 * The operations must be registered and will appear if the developer does not override the default importer row UI.
 */
public interface JIPipeDataImportOperation extends JIPipeDataOperation{
    /**
     * Checks if the import is possible
     * @param slot the slot that is associated to the data. please note that the slot does not contain any binary data. it allows you to access the algorithm and graph.
     * @param row the row in the output table
     * @param rowStorageFolder the folder the row is storing the data
     * @return if the import is possible
     */
    default boolean canShow(JIPipeDataSlot slot, JIPipeExportedDataTable.Row row, Path rowStorageFolder) {
        return true;
    }

    /**
     * Imports the data and shows it
     * @param slot the slot that is associated to the data. please note that the slot does not contain any binary data. it allows you to access the algorithm and graph.
     * @param row the row in the output table
     * @param rowStorageFolder the folder the row is storing the data
     * @param compartmentName the name of the compartment that generated the data
     * @param algorithmName the name of the algorithm that generated the data
     * @param displayName a unique identifier for the slot
     * @param workbench the workbench that issued the command
     * @return the imported data. null if not successful.
     */
    JIPipeData show(JIPipeDataSlot slot, JIPipeExportedDataTable.Row row, Path rowStorageFolder, String compartmentName, String algorithmName, String displayName, JIPipeWorkbench workbench);
}
