package org.hkijena.acaq5.extensions.imagejdatatypes.compat;

import ij.plugin.frame.RoiManager;
import org.hkijena.acaq5.api.compat.ImageJDatatypeAdapter;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIData;

public class ROIDataImageJAdapter implements ImageJDatatypeAdapter {
    @Override
    public boolean canConvertImageJToACAQ(Object imageJData) {
        return imageJData instanceof RoiManager;
    }

    @Override
    public boolean canConvertACAQToImageJ(ACAQData acaqData) {
        return acaqData instanceof ROIData;
    }

    @Override
    public Class<?> getImageJDatatype() {
        return RoiManager.class;
    }

    @Override
    public Class<? extends ACAQData> getACAQDatatype() {
        return ROIData.class;
    }

    @Override
    public ACAQData convertImageJToACAQ(Object imageJData) {
        return new ROIData((RoiManager) imageJData);
    }

    @Override
    public Object convertACAQToImageJ(ACAQData acaqData, boolean activate) {
        if (activate) {
            RoiManager roiManager = new RoiManager();
            ((ROIData) acaqData).addToRoiManager(roiManager);
            return roiManager;
        } else {
            RoiManager roiManager = new RoiManager(false);
            ((ROIData) acaqData).addToRoiManager(roiManager);
            return roiManager;
        }
    }
}
