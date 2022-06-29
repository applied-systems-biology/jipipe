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
 *
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.DrawUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

import java.awt.*;

/**
 * Moved out of the ImageJ algorithms extension
 */
public enum RoiLabel {
    None,
    Index,
    Name,
    Centroid;

    public void draw(ImagePlus imp, ImageProcessor ip, Roi roi, int roiIndex, Rectangle r, Color foreground, Color background, Font font, boolean drawBackground) {
        if (this == None)
            return;
        String label = null;
        if (this == Name) {
            label = roi.getName();
        }
        if (this == Centroid) {
            Point centroid = ROIListData.getCentroid(roi);
            label = centroid.x + ", " + centroid.y;
        }
        if (label == null) {
            label = "" + roiIndex;
        }
        DrawUtils.drawStringLabel(imp, ip, label, r, foreground, background, font, drawBackground);
    }
}
