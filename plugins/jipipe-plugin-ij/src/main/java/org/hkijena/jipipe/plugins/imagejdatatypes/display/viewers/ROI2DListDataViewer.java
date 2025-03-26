/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.imagejdatatypes.display.viewers;

import ij.ImagePlus;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewerWindow;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.BitDepth;

public class ROI2DListDataViewer extends ImagePlusDataViewer {
    public ROI2DListDataViewer(JIPipeDesktopDataViewerWindow dataViewerWindow) {
        super(dataViewerWindow);
    }

    @Override
    protected void loadDataIntoLegacyViewer(JIPipeData data) {
        getLegacyImageViewer().clearOverlays();
        if (data instanceof ROI2DListData) {
            ROI2DListData listData = (ROI2DListData) data;
            ImagePlus canvas = listData.createBlankCanvas("ROI", BitDepth.Grayscale8u);
            getLegacyImageViewer().setImagePlus(canvas);
            getLegacyImageViewer().addOverlay(data);
        }
    }
}
