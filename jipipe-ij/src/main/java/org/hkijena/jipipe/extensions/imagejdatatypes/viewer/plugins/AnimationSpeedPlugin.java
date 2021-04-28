package org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins;

import com.google.common.eventbus.Subscribe;
import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanel;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanelCanvas;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class AnimationSpeedPlugin extends GeneralImageViewerPanelPlugin {
    public AnimationSpeedPlugin(ImageViewerPanel viewerPanel) {
        super(viewerPanel);
    }

    @Override
    public void createPalettePanel(FormPanel formPanel) {
        ImagePlus image = getCurrentImage();
        if (image != null && (image.getNChannels() > 1 || image.getNSlices() > 1 || image.getNFrames() > 1)) {
            formPanel.addGroupHeader("Animation", UIUtils.getIconFromResources("actions/filmgrain.png"));
            formPanel.addToForm(getViewerPanel().getAnimationSpeedControl(), new JLabel("Speed (ms)"), null);
        }
    }
}
