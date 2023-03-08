package org.hkijena.jipipe.extensions.imageviewer.plugins2d;

import ij.ImagePlus;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewerPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class AnimationSpeedPlugin2D extends GeneralImageViewerPanelPlugin2D {
    public AnimationSpeedPlugin2D(JIPipeImageViewerPanel viewerPanel) {
        super(viewerPanel);
    }

    @Override
    public void initializeSettingsPanel(FormPanel formPanel) {
        ImagePlus image = getCurrentImage();
        if (image != null && (image.getNChannels() > 1 || image.getNSlices() > 1 || image.getNFrames() > 1)) {
            formPanel.addGroupHeader("Animation", UIUtils.getIconFromResources("actions/filmgrain.png"));
            formPanel.addToForm(getViewerPanel2D().getAnimationSpeedControl(), new JLabel("Speed (ms)"), null);
        }
    }
}
