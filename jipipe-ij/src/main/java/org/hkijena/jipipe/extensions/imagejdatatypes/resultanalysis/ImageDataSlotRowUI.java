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

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeExportedDataTable;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeDefaultResultDataSlotRowUI;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Result UI for {@link ImagePlus} data
 */
public class ImageDataSlotRowUI extends JIPipeDefaultResultDataSlotRowUI {

    /**
     * @param workbenchUI the workbench
     * @param slot        the data slot
     * @param row         the data row
     */
    public ImageDataSlotRowUI(JIPipeProjectWorkbench workbenchUI, JIPipeDataSlot slot, JIPipeExportedDataTable.Row row) {
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
                    UIUtils.getIconFromResources("apps/imagej.png"), e -> {
                        ImagePlus img = IJ.openImage(imageFile.toString());
                        if (img != null) {
                            img.show();
                            img.setTitle(getDisplayName());
                        }
                    });
        }
    }
}
