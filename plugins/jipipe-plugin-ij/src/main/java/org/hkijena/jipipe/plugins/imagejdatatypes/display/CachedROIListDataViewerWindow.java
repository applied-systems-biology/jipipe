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

package org.hkijena.jipipe.plugins.imagejdatatypes.display;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.api.data.sources.JIPipeDataTableDataSource;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

import java.awt.*;

public class CachedROIListDataViewerWindow extends CachedImagePlusDataViewerWindow {

    public CachedROIListDataViewerWindow(JIPipeDesktopWorkbench workbench, JIPipeDataTableDataSource dataSource, String displayName) {
        super(workbench, dataSource, displayName);
    }

    @Override
    protected void loadData(JIPipeDataItemStore virtualData, JIPipeProgressInfo progressInfo) {
        ROI2DListData data = JIPipe.getDataTypes().convert(virtualData.getData(progressInfo), ROI2DListData.class, progressInfo);
        int width;
        int height;
        int numZ = 1;
        int numC = 1;
        int numT = 1;

        if (data.isEmpty()) {
            width = 128;
            height = 128;
        } else {
            Rectangle bounds = data.getBounds();
            width = bounds.x + bounds.width;
            height = bounds.y + bounds.height;
            for (Roi roi : data) {
                numZ = Math.max(roi.getZPosition(), numZ);
                numC = Math.max(roi.getCPosition(), numC);
                numT = Math.max(roi.getTPosition(), numT);
            }
        }

        getImageViewer().setError(null);
        ImagePlus image = IJ.createImage("empty", "8-bit", width, height, numC, numZ, numT);
        ImageJUtils.forEachSlice(image, ip -> {
            ip.setColor(0);
            ip.fill();
        }, new JIPipeProgressInfo());
        getImageViewer().clearOverlays();
        getImageViewer().setImageData(new ImagePlusData(image));
        getImageViewer().addOverlay(data);

        fitImageToScreenOnce();
    }

}
