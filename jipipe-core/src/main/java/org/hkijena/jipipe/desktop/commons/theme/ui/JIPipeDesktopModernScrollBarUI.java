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

package org.hkijena.jipipe.desktop.commons.theme.ui;

import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public class JIPipeDesktopModernScrollBarUI extends BasicScrollBarUI {
    public static ComponentUI createUI(JComponent c) {
        return new JIPipeDesktopModernScrollBarUI();
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        try {
            if (UIUtils.currentThemeIsModern())
                return createZeroButton();
            else
                return super.createDecreaseButton(orientation);
        } catch (NullPointerException e) {
            return super.createDecreaseButton(orientation);
        }
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        try {
            if (UIUtils.currentThemeIsModern())
                return createZeroButton();
            else
                return super.createIncreaseButton(orientation);
        } catch (NullPointerException e) {
            e.printStackTrace();
            return super.createIncreaseButton(orientation);
        }
    }

    private JButton createZeroButton() {
        JButton jbutton = new JButton();
        jbutton.setPreferredSize(new Dimension(0, 0));
        jbutton.setMinimumSize(new Dimension(0, 0));
        jbutton.setMaximumSize(new Dimension(0, 0));
        return jbutton;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds)
    {
        Graphics2D g2d = (Graphics2D) g;

        if(thumbBounds.isEmpty() || !scrollbar.isEnabled())     {
            return;
        }

        int w = thumbBounds.width;
        int h = thumbBounds.height;

        g.translate(thumbBounds.x, thumbBounds.y);

        g.setColor(thumbColor);
        Object oldAntialiasing = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.fillRoundRect(1, 0, w - 3, h - 1, 3, 3);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiasing);

        g.translate(-thumbBounds.x, -thumbBounds.y);
    }
}
