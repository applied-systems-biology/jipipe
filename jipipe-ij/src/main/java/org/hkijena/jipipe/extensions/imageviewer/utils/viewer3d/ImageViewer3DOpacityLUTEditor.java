package org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d;

import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.extensions.imageviewer.utils.ImageViewerOpacityLUTEditor;

public class ImageViewer3DOpacityLUTEditor extends ImageViewerOpacityLUTEditor {
    public ImageViewer3DOpacityLUTEditor(JIPipeImageViewer imageViewerPanel, int targetChannel) {
        super(imageViewerPanel, targetChannel);
    }

    @Override
    public void applyLUT() {
        getImageViewerPanel().getImageViewerPanel3D().scheduleUpdateLutAndCalibration();
    }
}
