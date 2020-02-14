package org.hkijena.acaq5.extension.api.datatypes;

import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import org.hkijena.acaq5.api.ACAQData;
import org.hkijena.acaq5.api.ACAQDocumentation;

import java.nio.file.Path;

@ACAQDocumentation(name = "ROI", description = "Collection of ROI")
public class ACAQROIData implements ACAQData {
    private Roi roi;

    public ACAQROIData(Roi roi) {
        this.roi = roi;
    }

    public Roi getROI() {
        return roi;
    }

    @Override
    public void saveTo(Path storageFilePath, String name) {
        throw new RuntimeException("Not implemented yet");
    }
}
