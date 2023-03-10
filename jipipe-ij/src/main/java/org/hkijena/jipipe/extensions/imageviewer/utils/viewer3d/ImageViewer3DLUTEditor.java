package org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d;

import ij.ImagePlus;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.extensions.imageviewer.utils.ImageViewerLUTEditor;

public class ImageViewer3DLUTEditor extends ImageViewerLUTEditor {
    public ImageViewer3DLUTEditor(JIPipeImageViewer imageViewerPanel, int targetChannel) {
        super(imageViewerPanel, targetChannel);
    }

    @Override
    public void applyLUT() {
        ImagePlus image = getImageViewerPanel().getImagePlus();
        if (image != null && image.getType() != ImagePlus.COLOR_RGB) {
            if (getTargetChannel() < image.getNChannels()) {
                getImageViewerPanel().getImageViewerPanel3D().updateLutAndCalibration();
            }
        }
    }
}
