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

package org.hkijena.jipipe.desktop.commons.components.icons;

import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.NoFramework;
import org.hkijena.jipipe.desktop.commons.nodepatterns.NodePatternRenderer;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;
import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * The unified list-view icon for a node entry: a rounded, function-colored bar
 * carrying the node's framework signature pattern, with the node icon drawn in
 * the upper region.
 * <p>
 * Width is {@link #WIDTH} (22px — a little more than the 16px node icons, so
 * the icon sits with spacing in the bar); the height is set by the caller
 * (the row height: ~50px in the node browser, the full card height in the
 * find-node dialog). The framework pattern is painted at node scale (a crop of
 * a real node's background, see {@link NodePatternRenderer#paintNodeScaleCropped})
 * with the exact same gating as a canvas node: only when the theme has
 * {@code showNodeFrameworkPattern} enabled and the entry declares a real
 * framework. The node icon is centered in a fixed-height upper region and
 * scaled DOWN (aspect-preserving) only if it is larger than the region.
 */
public class NodeListEntryIcon implements Icon {
    /** The bar width (a little more than the 16px node icons). */
    public static final int WIDTH = 22;
    /** The upper region reserved for the node icon (its height). */
    private static final int ICON_REGION = 22;
    /** The top margin above the node icon region. */
    private static final int TOP_MARGIN = 3;
    private static final int ARC = 6;

    private final int height;
    private Color background;
    private JIPipeNodeTaxonomyFramework framework;
    private Icon nodeIcon;

    /**
     * @param height the icon height (the row height the icon should cover)
     */
    public NodeListEntryIcon(int height) {
        this.height = height;
    }

    public void setFillColor(Color c) {
        this.background = c;
    }

    public Color getFillColor() {
        return background;
    }

    /**
     * @return the framework whose signature pattern is painted, or
     * {@code null} / {@link NoFramework} for a plain color bar
     */
    public JIPipeNodeTaxonomyFramework getFramework() {
        return framework;
    }

    /**
     * Sets the framework whose signature pattern is painted. A {@code null} or
     * {@link NoFramework} value renders a plain color bar.
     */
    public void setFramework(JIPipeNodeTaxonomyFramework framework) {
        this.framework = framework;
    }

    public Icon getNodeIcon() {
        return nodeIcon;
    }

    /**
     * Sets the node icon drawn in the upper region (expected 16×16; larger
     * icons are scaled down aspect-preserving to fit the region).
     */
    public void setNodeIcon(Icon nodeIcon) {
        this.nodeIcon = nodeIcon;
    }

    @Override
    public int getIconWidth() {
        return WIDTH;
    }

    @Override
    public int getIconHeight() {
        return height;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (background == null) {
            background = Color.GRAY;
        }
        // Standard Icon contract: paint at the given (x,y) at the icon's
        // natural size. The host component (JLabel / the list cell) decides
        // where (x,y) is and how much room to give — it centers a stretched
        // label itself. The icon must NOT re-center from c.getSize() (that
        // would double-center and shift the icon).
        Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            // Function-colored rounded bar (the node fill color).
            Shape bar = new RoundRectangle2D.Float(x, y, WIDTH, height, ARC, ARC);
            g2d.setColor(background);
            g2d.fill(bar);

            // Framework signature pattern over the fill, clipped to the bar —
            // same gating as a canvas node.
            JIPipeDesktopModernThemeStyle style = ThemeUtils.getCurrentStyle();
            if (isPatternEnabled(style)) {
                g2d.clip(bar);
                NodePatternRenderer.paintNodeScaleCropped(g2d, x, y, WIDTH, height,
                        framework.getPatternStyleId(), framework.getId(),
                        style.getNodePatternInk(), style.getNodePatternOpacity());
                g2d.setClip(null);
            }

            // The node icon in the upper region, centered, scaled down only if
            // it does not fit (never upscaled).
            if (nodeIcon != null) {
                int iw = nodeIcon.getIconWidth();
                int ih = nodeIcon.getIconHeight();
                float scale = Math.min(1f, Math.min((float) ICON_REGION / iw, (float) ICON_REGION / ih));
                int sw = Math.max(1, Math.round(iw * scale));
                int sh = Math.max(1, Math.round(ih * scale));
                int ix = x + (WIDTH - sw) / 2;
                int iy = y + TOP_MARGIN + (ICON_REGION - sh) / 2;
                BufferedImage buf = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
                Graphics2D bg = buf.createGraphics();
                nodeIcon.paintIcon(c, bg, 0, 0);
                bg.dispose();
                if (scale < 1f) {
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                }
                g2d.drawImage(buf, ix, iy, sw, sh, null);
            }
        } finally {
            g2d.dispose();
        }
    }

    /**
     * Whether the framework pattern is painted (the theme setting is on and the
     * entry declares a real framework).
     */
    private boolean isPatternEnabled(JIPipeDesktopModernThemeStyle style) {
        return style != null && style.isShowNodeFrameworkPattern()
                && framework != null && !NoFramework.ID.equals(framework.getId());
    }
}
