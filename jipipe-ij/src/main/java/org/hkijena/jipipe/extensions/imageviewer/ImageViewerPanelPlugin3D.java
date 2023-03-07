package org.hkijena.jipipe.extensions.imageviewer;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d.CustomImage3DUniverse;

import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class ImageViewerPanelPlugin3D extends ImageViewerPanelPlugin {

    public ImageViewerPanelPlugin3D(ImageViewerPanel viewerPanel) {
        super(viewerPanel);
    }

    public ImageViewerPanel3D getViewerPanel3D() {
        return getViewerPanel().getImageViewerPanel3D();
    }

    public CustomImage3DUniverse getUniverse() {
        return getViewerPanel3D().getUniverse();
    }

    public ImagePlus process(ImagePlus imagePlus) {
        return imagePlus;
    }
}
