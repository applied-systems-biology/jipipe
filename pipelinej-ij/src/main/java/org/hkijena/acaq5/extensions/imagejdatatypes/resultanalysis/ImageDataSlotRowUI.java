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

package org.hkijena.acaq5.extensions.imagejdatatypes.resultanalysis;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQExportedDataTable;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.resultanalysis.ACAQDefaultResultDataSlotRowUI;
import org.hkijena.acaq5.utils.PathUtils;
import org.hkijena.acaq5.utils.UIUtils;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Result UI for {@link ImagePlus} data
 */
public class ImageDataSlotRowUI extends ACAQDefaultResultDataSlotRowUI {

    /**
     * @param workbenchUI the workbench
     * @param slot        the data slot
     * @param row         the data row
     */
    public ImageDataSlotRowUI(ACAQProjectWorkbench workbenchUI, ACAQDataSlot slot, ACAQExportedDataTable.Row row) {
        super(workbenchUI, slot, row);
    }

    private Path findImageFile() {
        if (getRowStorageFolder() != null && Files.isDirectory(getRowStorageFolder())) {
            return PathUtils.findFileByExtensionIn(getRowStorageFolder(), ".tif");
        }
        return null;
    }

    @Override
    protected void registerActions() {
        super.registerActions();

        Path imageFile = findImageFile();
        if (imageFile != null) {
            registerAction("Import", "Imports the image '" + imageFile + "' into ImageJ",
                    UIUtils.getIconFromResources("imagej.png"), e -> {
                        ImagePlus img = IJ.openImage(imageFile.toString());
                        if (img != null) {
                            img.show();
                            img.setTitle(getDisplayName());
                        }
                    });
        }
    }
}
