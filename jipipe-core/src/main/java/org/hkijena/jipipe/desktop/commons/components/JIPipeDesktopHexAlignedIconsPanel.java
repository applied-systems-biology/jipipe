package org.hkijena.jipipe.desktop.commons.components;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JIPipeDesktopHexAlignedIconsPanel extends JPanel {

    private List<Icon> icons = new ArrayList<>();
    private int rows = 5;
    private int hGap = 8;
    private int vGap = 8;
    private int fadeWidth = 24;

    public JIPipeDesktopHexAlignedIconsPanel() {
    }

    public JIPipeDesktopHexAlignedIconsPanel(List<Icon> icons, int rows) {
        setOpaque(false);
        setIcons(icons);
        setRows(rows);
        // Default outer padding is 0; you can customize via setBorder(new EmptyBorder(...))
    }

    public void setIcons(List<Icon> icons) {
        this.icons = Objects.requireNonNull(icons, "icons must not be null");
        repaint();
    }

    public void setRows(int rows) {
        this.rows = Math.max(1, rows);
        repaint();
    }

    public void setHorizontalGap(int gap) {
        this.hGap = Math.max(0, gap);
        repaint();
    }

    public void setVerticalGap(int gap) {
        this.vGap = Math.max(0, gap);
        repaint();
    }

    public void setFadeWidth(int fadeWidth) {
        this.fadeWidth = Math.max(0, fadeWidth);
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        // Sensible default; you can size the panel however you like
        return new Dimension(480, 180);
    }

    @Override
    protected void paintComponent(Graphics g) {

        if(icons.isEmpty()) {
            return;
        }

        // We’ll render everything into an ARGB buffer, apply left/right alpha fades, then blit.
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0 || icons == null || icons.isEmpty()) {
            return;
        }

        BufferedImage layer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = layer.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Insets in = getInsets();
        int availW = Math.max(0, w - in.left - in.right);
        int availH = Math.max(0, h - in.top - in.bottom);

        Icon base = icons.getFirst();
        int iw = base.getIconWidth();
        int ih = base.getIconHeight();

        if (availW > 0 && availH > 0 && iw > 0 && ih > 0) {
            // Compute Y positions so exactly `rows` rows span the full drawable height.
            // Topmost and bottommost images sit within the insets; rows==1 centers the row vertically.
            double innerH = Math.max(ih, availH); // ensure non-negative spacing
            double stepY = (rows == 1) ? 0.0 : (innerH - ih) / (rows - 1.0);

            // Horizontal step and odd-row offset for a hex-ish (staggered) layout.
            int stepX = iw + hGap;
            int oddOffsetX = stepX / 2;

            int iconIndex = 0;

            for (int r = 0; r < rows; r++) {
                int y = in.top + (int) Math.round(r * stepY);

                // Optional extra breathing room (doesn't change row count, just lowers apparent density).
                y = Math.max(in.top, Math.min(h - in.bottom - ih, y));

                int startX = in.left + ((r & 1) == 1 ? oddOffsetX : 0);

                // Draw across the width; start slightly before and end slightly after to ensure smooth fades.
                int x = startX;
                // If the offset causes a gap on the left, back up one step so fade looks continuous.
                while (x - stepX >= in.left - iw) {
                    x -= stepX;
                }

                for (; x <= w - in.right; x += stepX) {
                    Icon icon = icons.get(iconIndex % icons.size());
                    icon.paintIcon(this, g2, x, y);
                    iconIndex++;
                }
            }
        }

        // Apply left/right fade using DST_IN to mask alpha.
        if (fadeWidth > 0) {
            Graphics2D mask = layer.createGraphics();
            mask.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_IN));

            // Left fade: transparent at x=0 -> opaque at x=fadeWidth
            Paint left = new GradientPaint(0, 0,
                    new Color(0, 0, 0, 0),
                    Math.min(fadeWidth, w), 0,
                    new Color(0, 0, 0, 255), true);
            mask.setPaint(left);
            mask.fillRect(0, 0, Math.min(fadeWidth, w), h);

            // Right fade: opaque at x=w-fadeWidth -> transparent at x=w
            Paint right = new GradientPaint(Math.max(0, w - fadeWidth), 0,
                    new Color(0, 0, 0, 255),
                    w, 0,
                    new Color(0, 0, 0, 0), true);
            mask.setPaint(right);
            mask.fillRect(Math.max(0, w - fadeWidth), 0, Math.min(fadeWidth, w), h);

            mask.dispose();
        }

        // Draw the composed layer
        g.drawImage(layer, 0, 0, null);
        g2.dispose();
    }

    // --- convenience builder-ish setters ---

    public JIPipeDesktopHexAlignedIconsPanel withGaps(int horizontal, int vertical) {
        setHorizontalGap(horizontal);
        setVerticalGap(vertical);
        return this;
    }

    public JIPipeDesktopHexAlignedIconsPanel withFadeWidth(int pixels) {
        setFadeWidth(pixels);
        return this;
    }

    public List<Icon> getIcons() {
        return icons;
    }

    public int getRows() {
        return rows;
    }

    public int gethGap() {
        return hGap;
    }

    public int getvGap() {
        return vGap;
    }

    public int getFadeWidth() {
        return fadeWidth;
    }
}
