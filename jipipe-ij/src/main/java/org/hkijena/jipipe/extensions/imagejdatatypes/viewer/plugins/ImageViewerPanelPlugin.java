package org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanel;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchAccess;
import org.hkijena.jipipe.ui.components.FormPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class ImageViewerPanelPlugin implements JIPipeWorkbenchAccess {
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
        return viewerPanel.getCurrentSliceIndex();
    }

    public ImagePlus getCurrentImage() {
        return viewerPanel.getImage();
    }

    public ImageProcessor getCurrentSlice() {
        return viewerPanel.getCurrentSlice();
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return getViewerPanel().getWorkbench();
    }

    /**
     * Called when the current image is changed
     */
    public void onImageChanged() {

    }

    /**
     * Called when the form panel should be recreated
     *
     * @param formPanel the form panel
     */
    public void createPalettePanel(FormPanel formPanel) {

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
     * @param x          x position of the drawn image
     * @param y          y position of the drawn image
     * @param w          width of the drawn image
     * @param h          height of the drawn image
     */
    public void postprocessDraw(Graphics2D graphics2D, int x, int y, int w, int h) {

    }

    /**
     * Called while exporting the image slice.
     * Use this to export items handled by postprocessDraw
     *
     * @param image      the image
     * @param sliceIndex the slice
     */
    public void postprocessDrawForExport(BufferedImage image, ImageSliceIndex sliceIndex) {

    }

    /**
     * The tool panel category where this tool is shown
     *
     * @return the category
     */
    public abstract String getCategory();

    /**
     * The icon for the category if a new one must be created
     *
     * @return the icon
     */
    public abstract Icon getCategoryIcon();
}
