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

package org.hkijena.jipipe.extensions.ijfilaments.display;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.api.data.JIPipeDataTableDataSource;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewerCacheDataViewerWindow;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.awt.*;

public class CachedFilamentsDataViewerWindow extends JIPipeImageViewerCacheDataViewerWindow {

    public CachedFilamentsDataViewerWindow(JIPipeWorkbench workbench, JIPipeDataTableDataSource dataSource, String displayName) {
        super(workbench, dataSource, displayName);
    }

    @Override
    protected void loadData(JIPipeDataItemStore virtualData, JIPipeProgressInfo progressInfo) {
        Filaments3DData data = JIPipe.getDataTypes().convert(virtualData.getData(progressInfo), Filaments3DData.class);
        ROIListData rois = data.toRoi(false, true, true, true);
        int width;
        int height;
        int numZ = 1;
        int numC = 1;
        int numT = 1;

        if (data.isEmpty()) {
            width = 128;
            height = 128;
        } else {
            Rectangle bounds = rois.getBounds();
            width = bounds.x + bounds.width;
            height = bounds.y + bounds.height;
            for (Roi roi : rois) {
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
        getImageViewer().setImagePlus(image);
        getImageViewer().addOverlay(rois);
        fitImageToScreenOnce();
    }

}
