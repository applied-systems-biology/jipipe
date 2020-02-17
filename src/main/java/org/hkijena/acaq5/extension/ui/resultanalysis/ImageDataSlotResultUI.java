package org.hkijena.acaq5.extension.ui.resultanalysis;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDataSlot;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.resultanalysis.ACAQDefaultDataSlotResultUI;
import org.hkijena.acaq5.utils.UIUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ImageDataSlotResultUI extends ACAQDefaultDataSlotResultUI {
    public ImageDataSlotResultUI(ACAQWorkbenchUI workbenchUI, ACAQDataSlot<?> slot) {
        super(workbenchUI, slot);
    }

    private Path findImageFile() {
        if(getSlot().getStoragePath() != null && Files.isDirectory(getSlot().getStoragePath())) {
            try {
                return Files.list(getSlot().getStoragePath()).filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".tif")).findFirst().orElse(null);
            } catch (IOException e) {
                return null;
            }
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
