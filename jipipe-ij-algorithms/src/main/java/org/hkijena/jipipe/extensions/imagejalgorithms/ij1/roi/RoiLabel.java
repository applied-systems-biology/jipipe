package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.DrawUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

import java.awt.*;

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
