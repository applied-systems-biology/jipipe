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

package org.hkijena.jipipe.extensions.imagejdatatypes.display;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataTableDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.cache.JIPipeCacheDataViewerWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class CachedROIListDataViewerWindow extends CachedImagePlusDataViewerWindow {

    public CachedROIListDataViewerWindow(JIPipeWorkbench workbench, JIPipeDataTableDataSource dataSource, String displayName) {
        super(workbench, dataSource, displayName);
    }

    @Override
    protected void loadData(JIPipeDataItemStore virtualData, JIPipeProgressInfo progressInfo) {
        ROIListData data = JIPipe.getDataTypes().convert(virtualData.getData(progressInfo), ROIListData.class);
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
        getImageViewer().setImage(image);
        getImageViewer().addOverlay(data);

        fitImageToScreenOnce();
    }

}
