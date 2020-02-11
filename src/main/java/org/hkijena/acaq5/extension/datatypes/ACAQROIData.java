package org.hkijena.acaq5.extension.datatypes;

import ij.gui.Roi;
import org.hkijena.acaq5.api.ACAQData;
import org.hkijena.acaq5.api.ACAQDocumentation;

@ACAQDocumentation(name = "ROI", description = "Collection of ROI")
public class ACAQROIData implements ACAQData {
    private Roi roi;

    public ACAQROIData(Roi roi) {
        this.roi = roi;
    }

    public Roi getROI() {
        return roi;
    }
}
