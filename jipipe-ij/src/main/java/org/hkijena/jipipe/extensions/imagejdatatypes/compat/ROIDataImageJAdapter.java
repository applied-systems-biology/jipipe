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

package org.hkijena.jipipe.extensions.imagejdatatypes.compat;

import ij.plugin.frame.RoiManager;
import org.hkijena.jipipe.api.compat.ImageJDatatypeAdapter;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adapter between {@link RoiManager} and {@link ROIListData}
 */
public class ROIDataImageJAdapter implements ImageJDatatypeAdapter {
    @Override
    public boolean canConvertImageJToJIPipe(Object imageJData) {
        return imageJData instanceof RoiManager;
    }

    @Override
    public boolean canConvertJIPipeToImageJ(JIPipeData jipipeData) {
        return jipipeData instanceof ROIListData;
    }

    @Override
    public Class<?> getImageJDatatype() {
        return RoiManager.class;
    }

    @Override
    public Class<? extends JIPipeData> getJIPipeDatatype() {
        return ROIListData.class;
    }

    /**
     * Converts a {@link RoiManager} to {@link ROIListData}.
     * If imageJData is null, the currently active {@link RoiManager} is used.
     *
     * @param imageJData The ImageJ data
     * @return The JIPipe data
     */
    @Override
    public JIPipeData convertImageJToJIPipe(Object imageJData) {
        if (imageJData instanceof RoiManager)
            return new ROIListData((RoiManager) imageJData);
        else
            return new ROIListData(RoiManager.getRoiManager());
    }

    @Override
    public Object convertJIPipeToImageJ(JIPipeData jipipeData, boolean activate, boolean noWindow, String windowName) {
        if (activate) {
            RoiManager roiManager = new RoiManager();
            ((ROIListData) jipipeData).addToRoiManager(roiManager);
            return roiManager;
        } else {
            RoiManager roiManager = new RoiManager(false);
            ((ROIListData) jipipeData).addToRoiManager(roiManager);
            return roiManager;
        }
    }

    @Override
    public List<Object> convertMultipleJIPipeToImageJ(List<JIPipeData> jipipeData, boolean activate, boolean noWindow, String windowName) {
        if (activate) {
            RoiManager roiManager = new RoiManager();
            for (JIPipeData data : jipipeData) {
                ROIListData rois = (ROIListData) data;
                rois.addToRoiManager(roiManager);
            }
            return Collections.singletonList(roiManager);
        } else {
            List<Object> result = new ArrayList<>();
            for (JIPipeData data : jipipeData) {
                result.add(convertJIPipeToImageJ(data, activate, noWindow, windowName));
            }
            return result;
        }
    }

    @Override
    public JIPipeData importFromImageJ(String parameters) {
        return new ROIListData(RoiManager.getRoiManager());
    }
}
