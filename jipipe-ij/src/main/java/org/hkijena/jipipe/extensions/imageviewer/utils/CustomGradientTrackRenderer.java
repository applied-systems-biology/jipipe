/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

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
