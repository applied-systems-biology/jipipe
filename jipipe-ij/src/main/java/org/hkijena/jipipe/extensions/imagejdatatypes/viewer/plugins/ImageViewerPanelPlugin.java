package org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanel;
import org.hkijena.jipipe.ui.components.FormPanel;

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

    public void onImageChanged() {

    }

    public void createPalettePanel(FormPanel formPanel) {

    }

    public ImageProcessor draw(int z, int c, int t, ImageProcessor processor) {
        return processor;
    }

    public void onSliceChanged() {

    }

    public void beforeDraw(int z, int c, int t) {

    }
}
