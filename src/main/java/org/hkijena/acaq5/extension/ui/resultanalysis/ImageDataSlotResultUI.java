package org.hkijena.acaq5.extension.ui.resultanalysis;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.resultanalysis.ACAQDefaultDataSlotResultUI;
import org.hkijena.acaq5.utils.PathUtils;
import org.hkijena.acaq5.utils.UIUtils;

import java.nio.file.Files;
import java.nio.file.Path;

public class ImageDataSlotResultUI extends ACAQDefaultDataSlotResultUI {

    public ImageDataSlotResultUI(ACAQWorkbenchUI workbenchUI, ACAQDataSlot slot) {
        super(workbenchUI, slot);
    }

    private Path findImageFile() {
        if(getSlot().getStoragePath() != null && Files.isDirectory(getSlot().getStoragePath())) {
            return PathUtils.findFileByExtensionIn(getSlot().getStoragePath(), ".tif");
        }
        return null;
    }

    @Override
    protected void registerActions() {
        super.registerActions();

        Path imageFile = findImageFile();
        if(imageFile != null) {
            registerAction("Import", UIUtils.getIconFromResources("imagej.png"), e -> {
                ImagePlus img = IJ.openImage(imageFile.toString());
                if(img != null) {
                    img.show();
                }
            });
        }
    }
}
