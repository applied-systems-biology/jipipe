package org.hkijena.jipipe.extensions.imageviewer.utils;

import org.jdesktop.swingx.JXMultiThumbSlider;
import org.jdesktop.swingx.color.GradientTrackRenderer;

import javax.swing.*;
import java.awt.*;

/**
 * A {@link GradientTrackRenderer} that handles the case start=end gracefully
 */
public class CustomGradientTrackRenderer extends GradientTrackRenderer {
    private JXMultiThumbSlider slider;

    @Override
    protected void paintComponent(Graphics gfx) {
        int thumb_width = 12;
        int track_width = slider.getWidth() - thumb_width;
        if (track_width > 0) {
            super.paintComponent(gfx);
        }
    }

    @Override
    public JComponent getRendererComponent(JXMultiThumbSlider slider) {
        this.slider = slider;
        return super.getRendererComponent(slider);
    }
}
