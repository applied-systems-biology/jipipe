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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * Moved out of the ImageJ algorithms extension
 */
public enum RoiLabel {
    None,
    Index,
    Name,
    Centroid,
    Metadata;

    /**
     * Draws a label on an image processor
     *
     * @param imp            the image
     * @param ip             the target processor
     * @param roi            the ROI
     * @param roiIndex       the index of the ROI
     * @param r              Rectangle where x and y are the centroid (width, height are zero)
     * @param foreground     foreground color
     * @param background     background color
     * @param font           the font
     * @param drawBackground if a background should be drawn
     */
    public void draw(ImagePlus imp, ImageProcessor ip, Roi roi, int roiIndex, Rectangle r, Color foreground, Color background, Font font, boolean drawBackground) {
        if (this == None)
            return;
        String label = createLabel(roi, roiIndex);
        ImageJUtils.drawCenteredStringLabel(imp, ip, label, r, foreground, background, font, drawBackground);
    }

    /**
     * Generates a string label based on the ROI
     *
     * @param roi      the roi
     * @param roiIndex the index of the roi
     * @return the label or null if no label should be generated
     */
    public String createLabel(Roi roi, int roiIndex) {
        String label = null;
        if (this == Name) {
            label = roi.getName();
        }
        if (this == Centroid) {
            Point centroid = ROIListData.getCentroid(roi);
            label = centroid.x + ", " + centroid.y;
        }
        if (this == Metadata) {
            label = roi.getProperties();
        }
        if (label == null) {
            label = "" + roiIndex;
        }
        return label;
    }

    public void draw(Graphics2D graphics2D, Roi roi, int roiIndex, Point2D center, double magnification, Color foreground, Color background, Font font, boolean drawBackground) {
        if (this == None)
            return;
        String label = createLabel(roi, roiIndex);
        graphics2D.setFont(font);
        FontMetrics fontMetrics = graphics2D.getFontMetrics();
        int width = fontMetrics.stringWidth(label);
        int height = fontMetrics.getHeight();
        int x = (int) (center.getX() * magnification - width / 2);
        int y = (int) (center.getY() * magnification - height / 2);
        if (drawBackground) {
            graphics2D.setColor(background);
            graphics2D.fillRect(x - 1, y - 1, width + 2, height + 2);
        }
        graphics2D.setColor(foreground);
        graphics2D.drawString(label, x, y + fontMetrics.getAscent());
    }
}
