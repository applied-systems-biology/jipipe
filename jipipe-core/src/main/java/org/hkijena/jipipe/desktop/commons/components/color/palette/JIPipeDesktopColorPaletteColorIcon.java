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

package org.hkijena.jipipe.desktop.commons.components.color.palette;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopColorPaletteColorIcon implements Icon {
    private static final Stroke STROKE_SELECTED = new BasicStroke(3);
    private final int size;
    private final JIPipeDesktopColorPaletteColor color;
    private final boolean isUserColor;
    private final JIPipeDesktopColorPaletteUI paletteUI;
    private final Icon userColorIcon = JIPipe.RESOURCES.getIcon16Inverted("actions/user.png");

    public JIPipeDesktopColorPaletteColorIcon(int size, JIPipeDesktopColorPaletteColor color, boolean isUserColor, JIPipeDesktopColorPaletteUI paletteUI) {
        this.size = size;
        this.color = color;
        this.isUserColor = isUserColor;
        this.paletteUI = paletteUI;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g;
        final int m = 2;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color.getBackground());
        g.fillOval(x + m,y + m,size - 2*m, size - 2*m);
        if(paletteUI == null || paletteUI.isEnableBackgroundColorSelection()) {
            g.setColor(color.getForeground());
            g.fillArc(x + m,y + m,size - 2*m,size-2*m,45,180);
        }
        if(paletteUI != null && paletteUI.getSelectedColor() == color) {
            g.setColor(ThemeUtils.getCurrentStyle().getButtonToggled());
            g2d.setStroke(STROKE_SELECTED);
            int dotSize = 6;
            g.drawOval(x + m,y + m ,size - 2*m,size -2*m);
//            g.setColor(ThemeUtils.getCurrentStyle().getTextForeground());
            g.setColor(ThemeUtils.getCurrentStyle().getButtonToggled().darker());
            g.fillOval(x + size / 2 - (dotSize / 2),y + size / 2 - (dotSize / 2) ,dotSize,dotSize);
        }
        if(isUserColor) {
            userColorIcon.paintIcon(c, g2d, x + (size / 2) - userColorIcon.getIconWidth() / 2, y+ (size / 2) - userColorIcon.getIconWidth() / 2);
        }
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }
}
