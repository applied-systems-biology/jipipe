package org.hkijena.jipipe.extensions.imageviewer.utils.viewer2d;

import ij.ImagePlus;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.extensions.imageviewer.utils.ImageViewerLUTEditor;

public class ImageViewer2DLUTEditor extends ImageViewerLUTEditor {
    public ImageViewer2DLUTEditor(JIPipeImageViewer imageViewerPanel, int targetChannel) {
        super(imageViewerPanel, targetChannel);
    }

    @Override
    public void applyLUT() {
        ImagePlus image = getImageViewerPanel().getImagePlus();
        if (image != null && image.getType() != ImagePlus.COLOR_RGB) {
            if (getTargetChannel() < image.getNChannels()) {
//                if (image instanceof CompositeImage) {
//                    CompositeImage compositeImage = (CompositeImage) image;
//                    compositeImage.setChannelLut(getLUT(), targetChannel + 1);
//                }
//                int c = image.getC();
//                if (c == targetChannel + 1) {
//                    image.setLut(getLUT());
//                    imageViewerPanel.uploadSliceToCanvas();
//                }
                getImageViewerPanel().getViewerPanel2D().uploadSliceToCanvas();
            }
        }
    }
}
