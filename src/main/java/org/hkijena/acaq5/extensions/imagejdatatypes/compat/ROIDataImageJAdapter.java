package org.hkijena.acaq5.extensions.imagejdatatypes.compat;

import ij.plugin.frame.RoiManager;
import org.hkijena.acaq5.api.compat.ImageJDatatypeAdapter;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIListData;

/**
 * Adapter between {@link RoiManager} and {@link ROIListData}
 */
public class ROIDataImageJAdapter implements ImageJDatatypeAdapter {
    @Override
    public boolean canConvertImageJToACAQ(Object imageJData) {
        return imageJData instanceof RoiManager;
    }

    @Override
    public boolean canConvertACAQToImageJ(ACAQData acaqData) {
        return acaqData instanceof ROIListData;
    }

    @Override
    public Class<?> getImageJDatatype() {
        return RoiManager.class;
    }

    @Override
    public Class<? extends ACAQData> getACAQDatatype() {
        return ROIListData.class;
    }

    /**
     * Converts a {@link RoiManager} to {@link ROIListData}.
     * If imageJData is null, the currently active {@link RoiManager} is used.
     *
     * @param imageJData The ImageJ data
     * @return The ACAQ data
     */
    @Override
    public ACAQData convertImageJToACAQ(Object imageJData) {
        if (imageJData instanceof RoiManager)
            return new ROIListData((RoiManager) imageJData);
        else
            return new ROIListData(RoiManager.getRoiManager());
    }

    @Override
    public Object convertACAQToImageJ(ACAQData acaqData, boolean activate, boolean noWindow, String windowName) {
        if (activate) {
            RoiManager roiManager = new RoiManager();
            ((ROIListData) acaqData).addToRoiManager(roiManager);
            return roiManager;
        } else {
            RoiManager roiManager = new RoiManager(false);
            ((ROIListData) acaqData).addToRoiManager(roiManager);
            return roiManager;
        }
    }

    @Override
    public ACAQData importFromImageJ(String windowName) {
        return new ROIListData(RoiManager.getRoiManager());
    }
}
