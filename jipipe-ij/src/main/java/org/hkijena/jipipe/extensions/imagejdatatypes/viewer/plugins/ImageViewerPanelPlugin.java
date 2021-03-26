package org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanel;
import org.hkijena.jipipe.ui.components.FormPanel;

import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class ImageViewerPanelPlugin {
    private final ImageViewerPanel viewerPanel;

    public ImageViewerPanelPlugin(ImageViewerPanel viewerPanel) {
        this.viewerPanel = viewerPanel;
    }

    public ImageViewerPanel getViewerPanel() {
        return viewerPanel;
    }

    public void uploadSliceToCanvas() {
        viewerPanel.uploadSliceToCanvas();
    }

    public ImageSliceIndex getCurrentSlicePosition() {
        return viewerPanel.getCurrentSlicePosition();
    }

    public ImagePlus getCurrentImage() {
        return viewerPanel.getImage();
    }

    /**
     * Called when the current image is changed
     */
    public void onImageChanged() {

    }

    /**
     * Called when the form panel should be recreated
     * @param formPanel the form panel
     */
    public void createPalettePanel(FormPanel formPanel) {

    }

    /**
     * Draws the image in the processor
     * @param z z position
     * @param c c position
     * @param t t position
     * @param processor the processor
     * @return the modified processor
     */
    public ImageProcessor draw(int z, int c, int t, ImageProcessor processor) {
        return processor;
    }

    /**
     * Called when the current slice is changed
     */
    public void onSliceChanged() {

    }

    /**
     * Called before draw is called
     * @param z z position
     * @param c c position
     * @param t t position
     */
    public void beforeDraw(int z, int c, int t) {

    }

    /**
     * Called after the image has been drawn after the image has been drawn
     * @param graphics2D the graphics
     * @param x x position of the drawn image
     * @param y y position of the drawn image
     * @param w width of the drawn image
     * @param h height of the drawn image
     */
    public void postprocessDraw(Graphics2D graphics2D, int x, int y, int w, int h) {

    }

    /**
     * Called while exporting the image slice.
     * Use this to export items handled by postprocessDraw
     * @param image the image
     * @param sliceIndex the slice
     */
    public void postprocessDrawForExport(BufferedImage image, ImageSliceIndex sliceIndex) {

    }
}
