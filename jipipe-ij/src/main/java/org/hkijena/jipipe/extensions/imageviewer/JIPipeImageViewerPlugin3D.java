package org.hkijena.jipipe.extensions.imageviewer;

import ij.ImagePlus;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d.universe.CustomImage3DUniverse;

public abstract class JIPipeImageViewerPlugin3D extends JIPipeImageViewerPlugin {

    public JIPipeImageViewerPlugin3D(JIPipeImageViewer viewerPanel) {
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
