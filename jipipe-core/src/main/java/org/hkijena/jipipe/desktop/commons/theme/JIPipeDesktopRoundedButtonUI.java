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

package org.hkijena.jipipe.desktop.commons.theme;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;

public class JIPipeDesktopRoundedButtonUI extends BasicButtonUI {

    private final int cornerSize;

    private final Color backgroundHover;
    private final Color backgroundPressed;

    public JIPipeDesktopRoundedButtonUI(int cornerSize, Color backgroundHover, Color backgroundPressed) {
        this.cornerSize = cornerSize;
        this.backgroundHover = backgroundHover;
        this.backgroundPressed = backgroundPressed;
    }

    @Override
    public void update(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (c.isOpaque()) {
            Color fillColor = c.getBackground();

            AbstractButton button = (AbstractButton) c;
            ButtonModel model = button.getModel();

            if (model.isPressed()) {
                fillColor = backgroundPressed;
            } else if (model.isRollover()) {
                fillColor = backgroundHover;
            }

            g.setColor(fillColor);
            g.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), cornerSize, cornerSize);
        }
        paint(g, c);
    }
}
