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

import java.awt.*;

/**
 * The list-view color swatch for a node, upgraded to carry the node's framework
 * signature pattern.
 * <p>
 * Same 16×50 shape, 1px border, and function-color fill as {@link SolidColorIcon},
 * but additionally paints the framework's node pattern (the same visual
 * "signature" the canvas nodes show) over the fill via {@link NodePatternRenderer}.
 * <p>
 * The pattern uses the exact same gating as a canvas node ({@code JIPipeDesktopGraphNodeUI}):
 * it is drawn only when the theme has {@code showNodeFrameworkPattern} enabled AND the
 * node declares a real framework (a {@link NoFramework} node shows a plain bar). Ink and
 * opacity are read from the current theme style at paint time, so the icon follows
 * Light/Dark theme switches without any extra wiring.
 */
public class NodeFrameworkPatternIcon implements JIPipeDesktopColorIcon {
    private final int imageWidth;
    private final int imageHeight;
    private final Insets insets;
    private Color fillColor;
    private Color borderColor;
    private JIPipeNodeTaxonomyFramework framework;

    /**
     * Creates a 16x16 black icon
     */
    public NodeFrameworkPatternIcon() {
        this(16, 16);
    }

    /**
     * Creates a black icon
     *
     * @param width  icon width
     * @param height icon height
     */
    public NodeFrameworkPatternIcon(int width, int height) {
        this(width, height, Color.black);
    }

    /**
     * Creates an icon with black borders
     *
     * @param imageWidth  icon width
     * @param imageHeight icon height
     * @param fillColor   fill color
     */
    public NodeFrameworkPatternIcon(int imageWidth, int imageHeight, Color fillColor) {
        this(imageWidth, imageHeight, fillColor, Color.BLACK);
    }

    /**
     * @param width       icon width
     * @param height      icon height
     * @param fillColor   fill color
     * @param borderColor border color
     */
    public NodeFrameworkPatternIcon(int width, int height, Color fillColor, Color borderColor) {
        imageWidth = width;
        imageHeight = height;

        this.fillColor = fillColor;
        this.borderColor = borderColor;
        insets = new Insets(1, 1, 1, 1);
    }

    public Color getFillColor() {
        return fillColor;
    }

    public void setFillColor(Color c) {
        fillColor = c;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(Color c) {
        borderColor = c;
    }

    /**
     * @return the framework whose signature pattern is painted over the fill, or
     * {@code null} (or {@link NoFramework}) for a plain color bar
     */
    public JIPipeNodeTaxonomyFramework getFramework() {
        return framework;
    }

    /**
     * Sets the framework whose signature pattern is painted over the fill. A
     * {@code null} or {@link NoFramework} value renders a plain color bar.
     */
    public void setFramework(JIPipeNodeTaxonomyFramework framework) {
        this.framework = framework;
    }

    @Override
    public int getIconWidth() {
        return imageWidth;
    }

    @Override
    public int getIconHeight() {
        return imageHeight;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.setColor(borderColor);
        g.drawRect(x, y, imageWidth - 1, imageHeight - 2);

        int fx = x + insets.left;
        int fy = y + insets.top;
        int fw = imageWidth - insets.left - insets.right;
        int fh = imageHeight - insets.top - insets.bottom - 1;

        g.setColor(fillColor);
        g.fillRect(fx, fy, fw, fh);

        // Framework signature pattern over the fill (same gating as the canvas node).
        // paintNodeScaleCropped: the fill is a small 14×47 sliver, so render the
        // pattern at the canonical node body size and center-crop it - the icon
        // shows the pattern at exactly the cell scale of a real canvas node.
        JIPipeDesktopModernThemeStyle style = ThemeUtils.getCurrentStyle();
        if (isPatternEnabled(style) && fw > 0 && fh > 0) {
            NodePatternRenderer.paintNodeScaleCropped((Graphics2D) g, fx, fy, fw, fh,
                    framework.getPatternStyleId(), framework.getId(),
                    style.getNodePatternInk(), style.getNodePatternOpacity());
        }
    }

    /**
     * Whether the framework pattern is painted for this icon (the theme setting is on
     * and the icon declares a real framework).
     */
    private boolean isPatternEnabled(JIPipeDesktopModernThemeStyle style) {
        return style != null && style.isShowNodeFrameworkPattern()
                && framework != null && !NoFramework.ID.equals(framework.getId());
    }
}
