package org.hkijena.jipipe.extensions.imageviewer;

import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;

import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class ImageViewerPanelPlugin2D extends ImageViewerPanelPlugin {

    public ImageViewerPanelPlugin2D(JIPipeImageViewerPanel viewerPanel) {
        super(viewerPanel);
    }

    public ImageViewerPanel2D getViewerPanel2D() {
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
