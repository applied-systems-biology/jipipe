package org.hkijena.jipipe.extensions.imagejalgorithms.utils;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.process.ImageProcessor;

import java.awt.*;

public class DrawUtils {
    private DrawUtils() {

    }

    public static void drawStringLabel(ImagePlus imp, ImageProcessor ip, String label, Rectangle r, Color foreground, Color background, Font font, boolean drawBackground) {
        int size = font.getSize();
        ImageCanvas ic = imp.getCanvas();
        if (ic != null) {
            double mag = ic.getMagnification();
            if (mag < 1.0)
                size /= mag;
        }
        if (size == 9 && r.width > 50 && r.height > 50)
            size = 12;
        ip.setFont(new Font(font.getName(), font.getStyle(), size));
        int w = ip.getStringWidth(label);
        int x = r.x + r.width / 2 - w / 2;
        int y = r.y + r.height / 2 + Math.max(size / 2, 6);
        FontMetrics metrics = ip.getFontMetrics();
        int h = metrics.getHeight();
        if (drawBackground) {
            ip.setColor(background);
            ip.setRoi(x - 1, y - h + 2, w + 1, h - 3);
            ip.fill();
            ip.resetRoi();
        }
        ip.setColor(foreground);
        ip.drawString(label, x, y);
    }
}
