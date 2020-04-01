package org.hkijena.acaq5.extensions.imagejdatatypes.resultanalysis;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQExportedDataTable;
import org.hkijena.acaq5.ui.ACAQProjectUI;
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
    public ImageDataSlotRowUI(ACAQProjectUI workbenchUI, ACAQDataSlot slot, ACAQExportedDataTable.Row row) {
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
