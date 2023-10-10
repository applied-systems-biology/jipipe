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

package org.hkijena.jipipe.extensions.imagejdatatypes.resultanalysis;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableMetadataRow;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemReadDataStorage;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.nio.file.Path;

public class ImagePlusResultImportRun implements JIPipeRunnable {

    private final JIPipeDataSlot slot;
    private final JIPipeDataTableMetadataRow row;
    private final Path rowStorageFolder;
    private final String compartmentName;
    private final String algorithmName;
    private final String displayName;
    private final JIPipeWorkbench workbench;
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
    private ImagePlus image;

    public ImagePlusResultImportRun(JIPipeDataSlot slot, JIPipeDataTableMetadataRow row, Path rowStorageFolder, String compartmentName, String algorithmName, String displayName, JIPipeWorkbench workbench) {
        this.slot = slot;
        this.row = row;
        this.rowStorageFolder = rowStorageFolder;
        this.compartmentName = compartmentName;
        this.algorithmName = algorithmName;
        this.displayName = displayName;
        this.workbench = workbench;
    }

    @Override
    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    public void setProgressInfo(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    @Override
    public String getTaskLabel() {
        return "Import image";
    }

    @Override
    public void run() {
        progressInfo.setProgress(1, 3);
        progressInfo.log("Importing image from " + rowStorageFolder);
        ImagePlusData data = ImagePlusData.importData(new JIPipeFileSystemReadDataStorage(progressInfo, rowStorageFolder), progressInfo);
        image = data.getImage();
        progressInfo.setProgress(3, 3);
    }

    public ImagePlus getImage() {
        return image;
    }

    public JIPipeDataSlot getSlot() {
        return slot;
    }

    public JIPipeDataTableMetadataRow getRow() {
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
