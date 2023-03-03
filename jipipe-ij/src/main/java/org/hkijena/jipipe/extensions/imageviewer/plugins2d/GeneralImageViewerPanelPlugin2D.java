package org.hkijena.jipipe.extensions.imageviewer.plugins2d;

import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanel2D;
import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanelPlugin2D;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public abstract class GeneralImageViewerPanelPlugin2D extends ImageViewerPanelPlugin2D {
    public GeneralImageViewerPanelPlugin2D(ImageViewerPanel2D viewerPanel) {
        super(viewerPanel);
    }

    @Override
    public String getCategory() {
        return "General";
    }

    @Override
    public Icon getCategoryIcon() {
        return UIUtils.getIconFromResources("actions/configure.png");
    }
}
