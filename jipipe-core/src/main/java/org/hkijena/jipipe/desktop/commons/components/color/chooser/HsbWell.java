package org.hkijena.jipipe.desktop.commons.components.color.chooser;

import org.hkijena.jipipe.desktop.commons.components.color.JIPipeDesktopColorChooserDialog;
import org.hkijena.jipipe.utils.color.ColorSpaces;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * Saturation × brightness well at the current hue (HSB space).
 */
public class HsbWell extends JComponent {
    private final JIPipeDesktopColorChooserDialog jiPipeDesktopColorChooserDialog;
    private BufferedImage bg;
    private double bgHue = Double.NaN;

    public HsbWell(JIPipeDesktopColorChooserDialog jiPipeDesktopColorChooserDialog) {
        this.jiPipeDesktopColorChooserDialog = jiPipeDesktopColorChooserDialog;
        setPreferredSize(new Dimension(160, 160));
        setMinimumSize(new Dimension(120, 120));
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                update(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                update(e);
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    private void update(MouseEvent e) {
        double sat = e.getX() / (double) Math.max(1, getWidth()) * 100;
        double bri = (1.0 - e.getY() / (double) Math.max(1, getHeight())) * 100;
        double hue = ColorSpaces.toHsb(jiPipeDesktopColorChooserDialog.getCurrentColor())[0];
        jiPipeDesktopColorChooserDialog.setCurrentColor(ColorSpaces.fromHsb(hue, sat, bri));
    }

    @Override
    protected void paintComponent(Graphics g) {
        ensureBg();
        int w = getWidth(), h = getHeight();
        g.drawImage(bg, 0, 0, w, h, null);
        double[] hsb = ColorSpaces.toHsb(jiPipeDesktopColorChooserDialog.getCurrentColor());
        int x = (int) (hsb[1] / 100 * (w - 1));
        int y = (int) ((1 - hsb[2] / 100) * (h - 1));
        g.setColor(Color.WHITE);
        g.drawOval(x - 5, y - 5, 10, 10);
        g.setColor(Color.BLACK);
        g.drawOval(x - 4, y - 4, 8, 8);
        g.setColor(new Color(110, 110, 110));
        g.drawRect(0, 0, w - 1, h - 1);
    }

    private void ensureBg() {
        double hue = ColorSpaces.toHsb(jiPipeDesktopColorChooserDialog.getCurrentColor())[0];
        if (bg == null || bgHue != hue) {
            int s = 96;
            bg = new BufferedImage(s, s, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < s; y++) {
                double bri = (1 - y / (double) (s - 1)) * 100;
                for (int x = 0; x < s; x++) {
                    double sat = x / (double) (s - 1) * 100;
                    bg.setRGB(x, y, ColorSpaces.fromHsb(hue, sat, bri).getRGB());
                }
            }
            bgHue = hue;
        }
    }
}
