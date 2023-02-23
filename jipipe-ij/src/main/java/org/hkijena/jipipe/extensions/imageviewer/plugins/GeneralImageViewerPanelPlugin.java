package org.hkijena.jipipe.extensions.imageviewer.plugins;

import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public abstract class GeneralImageViewerPanelPlugin extends ImageViewerPanelPlugin {
    public GeneralImageViewerPanelPlugin(ImageViewerPanel viewerPanel) {
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
