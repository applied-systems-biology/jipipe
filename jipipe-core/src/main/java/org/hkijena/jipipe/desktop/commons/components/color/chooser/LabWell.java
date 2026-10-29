package org.hkijena.jipipe.desktop.commons.components.color.chooser;

import org.hkijena.jipipe.desktop.commons.components.color.JIPipeDesktopColorChooserDialog;
import org.hkijena.jipipe.utils.color.ColorSpaces;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * a/b chromaticity plane at the current L (LAB space, clamped to sRGB).
 */
public class LabWell extends JComponent {
    private final JIPipeDesktopColorChooserDialog jiPipeDesktopColorChooserDialog;
    private BufferedImage bg;
    private double bgL = Double.NaN;
    // The last pointer position in a/b, in *unclamped* LAB coordinates. The
    // marker is drawn here (not from toLab(currentColor)) so it follows the
    // cursor even when the picked color is out of the sRGB gamut and
    // ColorSpaces.fromLab clamped it — the circle no longer "sticks" at the
    // clamped position (#1339).
    private double pointerA = Double.NaN;
    private double pointerB = Double.NaN;

    public LabWell(JIPipeDesktopColorChooserDialog jiPipeDesktopColorChooserDialog) {
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
        double a = (e.getX() / (double) Math.max(1, getWidth()) * 2 - 1) * 128;
        double b = (1 - e.getY() / (double) Math.max(1, getHeight()) * 2 + 1) * 128;
        pointerA = a;
        pointerB = b;
        double L = ColorSpaces.toLab(jiPipeDesktopColorChooserDialog.getCurrentColor())[0];
        jiPipeDesktopColorChooserDialog.setCurrentColor(ColorSpaces.fromLab(L, a, b));
    }

    /**
     * @return the a/b the marker is drawn at (pointer position or color's own).
     */
    public double[] markerAb() {
        if (Double.isNaN(pointerA) || Double.isNaN(pointerB)) {
            double[] lab = ColorSpaces.toLab(jiPipeDesktopColorChooserDialog.getCurrentColor());
            return new double[]{lab[1], lab[2]};
        }
        return new double[]{pointerA, pointerB};
    }

    @Override
    protected void paintComponent(Graphics g) {
        ensureBg();
        int w = getWidth(), h = getHeight();
        g.drawImage(bg, 0, 0, w, h, null);
        // crosshair axes
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(255, 255, 255, 110));
        g2.drawLine(w / 2, 0, w / 2, h);
        g2.drawLine(0, h / 2, w, h / 2);
        g2.setColor(new Color(0, 0, 0, 110));
        g2.drawLine(w / 2 + 1, 0, w / 2 + 1, h);
        g2.drawLine(0, h / 2 + 1, w, h / 2 + 1);
        // corner tags
        g2.setColor(Color.WHITE);
        g2.setFont(getFont().deriveFont(9f));
        g2.drawString("-a", 2, h - 3);
        g2.drawString("+a", w - 16, 10);
        g2.drawString("+b", 2, 10);
        g2.drawString("-b", w - 16, h - 3);
        // marker: prefer the raw pointer position (follows the cursor even
        // when the color was gamut-clamped); fall back to the color's own
        // a/b when there is no pointer yet (e.g. externally-set color).
        double[] lab = ColorSpaces.toLab(jiPipeDesktopColorChooserDialog.getCurrentColor());
        double a = Double.isNaN(pointerA) ? lab[1] : pointerA;
        double b = Double.isNaN(pointerB) ? lab[2] : pointerB;
        int x = (int) ((a + 128) / 256 * (w - 1));
        int y = (int) ((128 - b) / 256 * (h - 1));
        g2.setColor(Color.WHITE);
        g2.drawOval(x - 5, y - 5, 10, 10);
        g2.setColor(Color.BLACK);
        g2.drawOval(x - 4, y - 4, 8, 8);
        g2.dispose();
        g.setColor(new Color(110, 110, 110));
        g.drawRect(0, 0, w - 1, h - 1);
    }

    private void ensureBg() {
        double L = ColorSpaces.toLab(jiPipeDesktopColorChooserDialog.getCurrentColor())[0];
        if (bg == null || bgL != L) {
            int s = 96;
            bg = new BufferedImage(s, s, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < s; y++) {
                double b = (1 - y / (double) (s - 1)) * 256 - 128;
                for (int x = 0; x < s; x++) {
                    double a = x / (double) (s - 1) * 256 - 128;
                    bg.setRGB(x, y, ColorSpaces.fromLab(L, a, b).getRGB());
                }
            }
            bgL = L;
        }
    }
}
