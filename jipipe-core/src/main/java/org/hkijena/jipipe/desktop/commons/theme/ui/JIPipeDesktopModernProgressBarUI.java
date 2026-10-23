package org.hkijena.jipipe.desktop.commons.theme.ui;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;

public class JIPipeDesktopModernProgressBarUI extends BasicProgressBarUI {

    @Override
    protected void paintIndeterminate(Graphics g, JComponent c) {
        if (ThemeUtils.isReducedMotion()) {
            JProgressBar bar = (JProgressBar) c;
            Image hourglass = JIPipe.RESOURCES.getIcon16("actions/hourglass-half.png").getImage();
            int w = bar.getWidth();
            int h = bar.getHeight();
            int iconSize = Math.min(w, h);
            int x = (w - iconSize) / 2;
            int y = (h - iconSize) / 2;
            g.drawImage(hourglass, x, y, iconSize, iconSize, null);
            return;
        }
        super.paintIndeterminate(g, c);
    }
}
