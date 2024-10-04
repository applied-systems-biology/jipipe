/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.imagejdatatypes.resultanalysis;

import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableRowInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemReadDataStorage;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.OMEImageData;

import java.nio.file.Path;

public class OMEImageResultImportRun extends AbstractJIPipeRunnable {

    private final JIPipeDataSlot slot;
    private final JIPipeDataTableRowInfo row;
    private final Path rowStorageFolder;
    private final String compartmentName;
    private final String algorithmName;
    private final String displayName;
    private final JIPipeWorkbench workbench;
    private OMEImageData image;

    public OMEImageResultImportRun(JIPipeDataSlot slot, JIPipeDataTableRowInfo row, Path rowStorageFolder, String compartmentName, String algorithmName, String displayName, JIPipeWorkbench workbench) {
        this.slot = slot;
        this.row = row;
        this.rowStorageFolder = rowStorageFolder;
        this.compartmentName = compartmentName;
        this.algorithmName = algorithmName;
        this.displayName = displayName;
        this.workbench = workbench;
    }

    @Override
    public String getTaskLabel() {
        return "Import image";
    }

    @Override
    public void run() {
        JIPipeProgressInfo progressInfo = getProgressInfo();
        progressInfo.setProgress(1, 3);
        progressInfo.log("Importing image from " + rowStorageFolder);
        image = OMEImageData.importData(new JIPipeFileSystemReadDataStorage(progressInfo, rowStorageFolder), getProgressInfo());
        progressInfo.setProgress(3, 3);
    }

    public OMEImageData getImage() {
        return image;
    }

    public JIPipeDataSlot getSlot() {
        return slot;
    }

    public JIPipeDataTableRowInfo getRow() {
        return row;
    }

    public Path getRowStorageFolder() {
        return rowStorageFolder;
    }

    public String getCompartmentName() {
        return compartmentName;
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }
}
