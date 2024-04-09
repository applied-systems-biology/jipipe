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

package org.hkijena.jipipe.plugins.imageviewer.utils;

import org.hkijena.jipipe.utils.ColorUtils;
import org.jdesktop.swingx.JXMultiThumbSlider;
import org.jdesktop.swingx.color.ColorUtil;
import org.jdesktop.swingx.multislider.ThumbRenderer;

import javax.swing.*;
import java.awt.*;

public class CustomGradientThumbRenderer extends JComponent implements ThumbRenderer {

    public static final Stroke STROKE_SELECTED = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{2}, 0);
    private boolean selected;

    public CustomGradientThumbRenderer() {
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D graphics2D = (Graphics2D) g;
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        JComponent thumb = this;
        int w = thumb.getWidth();
        g.setColor(getForeground());
        if (selected) {
            g.fillRect(0, 0, w - 1, w - 1);
            graphics2D.setStroke(STROKE_SELECTED);
            g.setColor(ColorUtils.invertRGB(getForeground()));
            g.drawRect(0, 0, w - 1, w - 1);
        } else {
            g.fillOval(2, 2, w - 3, w - 3);
        }
    }

    public JComponent getThumbRendererComponent(JXMultiThumbSlider slider, int index, boolean selected) {
        Color c = (Color) slider.getModel().getThumbAt(index).getObject();
        c = ColorUtil.removeAlpha(c);
        this.setForeground(c);
        this.selected = selected;
        return this;
    }
}
