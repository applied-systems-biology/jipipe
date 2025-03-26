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

package org.hkijena.jipipe.plugins.imageviewer.legacy.api;

import ij.process.ImageProcessor;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.imageviewer.legacy.JIPipeDesktopLegacyImageViewer;
import org.hkijena.jipipe.plugins.imageviewer.legacy.impl.JIPipeDesktopLegacyImageViewerPanel2D;

import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class JIPipeDesktopLegacyImageViewerPlugin2D extends JIPipeDesktopLegacyImageViewerPlugin {

    public JIPipeDesktopLegacyImageViewerPlugin2D(JIPipeDesktopLegacyImageViewer viewerPanel) {
        super(viewerPanel);
    }

    public JIPipeDesktopLegacyImageViewerPanel2D getViewerPanel2D() {
        return getViewerPanel().getViewerPanel2D();
    }

    public void uploadSliceToCanvas() {
        getViewerPanel2D().uploadSliceToCanvas();
    }

    public ImageSliceIndex getCurrentSlicePosition() {
        return getViewerPanel2D().getCurrentSliceIndex();
    }

    public ImageProcessor getCurrentSlice() {
        return getViewerPanel2D().getCurrentSlice();
    }


    /**
     * Draws the image in the processor
     *
     * @param c         c position
     * @param z         z position
     * @param t         t position
     * @param processor the processor
     * @return the modified processor
     */
    public ImageProcessor draw(int c, int z, int t, ImageProcessor processor) {
        return processor;
    }

    /**
     * Called when the current slice is changed
     *
     * @param deferUploadSlice if true, uploading the slice to the canvas should be deferred
     */
    public void onSliceChanged(boolean deferUploadSlice) {

    }

    /**
     * Called before draw is called
     *
     * @param c c position
     * @param z z position
     * @param t t position
     */
    public void beforeDraw(int c, int z, int t) {

    }

    /**
     * Called after the image has been drawn after the image has been drawn
     *
     * @param graphics2D the graphics
     * @param renderArea the render area
     * @param sliceIndex the index of the slice
     */
    public void postprocessDraw(Graphics2D graphics2D, Rectangle renderArea, ImageSliceIndex sliceIndex) {

    }

    /**
     * Called while exporting the image slice.
     * Use this to export items handled by postprocessDraw
     *
     * @param image         the image
     * @param sliceIndex    the slice
     * @param magnification the magnification
     */
    public void postprocessDrawForExport(BufferedImage image, ImageSliceIndex sliceIndex, double magnification) {

    }
}
