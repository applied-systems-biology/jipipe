package org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d;

import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.extensions.imageviewer.utils.ImageViewerGrayscaleLUTEditor;

public class ImageViewer3DGreyscaleLUTEditor extends ImageViewerGrayscaleLUTEditor {
    public ImageViewer3DGreyscaleLUTEditor(JIPipeImageViewer imageViewerPanel, int targetChannel) {
        super(imageViewerPanel, targetChannel);
    }

    @Override
    public void applyLUT() {
        getImageViewerPanel().getImageViewerPanel3D().updateLutAndCalibration();
    }
}
