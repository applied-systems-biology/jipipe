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

package org.hkijena.jipipe.desktop.app.customizer;

import org.hkijena.jipipe.desktop.commons.components.icons.JIPipeDesktopColorIcon;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Random;

/**
 * Icon that displays a mosaic effect using colors from a JIPipeDesktopModernThemeStyle
 */
public class JIPipeDesktopModernThemeStyleIcon implements JIPipeDesktopColorIcon {

    private static final int DEFAULT_SIZE = 32;
    private final JIPipeDesktopModernThemeStyle themeStyle;
    private int iconWidth = DEFAULT_SIZE;
    private int iconHeight = DEFAULT_SIZE;
    private Color fillColor;
    private Color borderColor;

    /**
     * Creates a theme style icon with default size (32x32)
     *
     * @param themeStyle the theme style to use for colors
     */
    public JIPipeDesktopModernThemeStyleIcon(JIPipeDesktopModernThemeStyle themeStyle) {
        this(themeStyle, DEFAULT_SIZE, DEFAULT_SIZE);
    }

    /**
     * Creates a theme style icon with specified size
     *
     * @param themeStyle the theme style to use for colors
     * @param width      icon width
     * @param height     icon height
     */
    public JIPipeDesktopModernThemeStyleIcon(JIPipeDesktopModernThemeStyle themeStyle, int width, int height) {
        if (themeStyle == null) {
            throw new IllegalArgumentException("Theme style cannot be null");
        }
        this.themeStyle = themeStyle;
        this.iconWidth = width;
        this.iconHeight = height;
        this.fillColor = themeStyle.getPrimaryColor();
        this.borderColor = themeStyle.getTextForeground();
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g.create();
        
        // Enable antialiasing for better rendering quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        // Create mosaic pattern
        createMosaicPattern(g2d, x, y, iconWidth, iconHeight);
        
        g2d.dispose();
    }

    /**
     * Creates a mosaic pattern using theme colors
     *
     * @param g2d    the graphics context
     * @param x      x position
     * @param y      y position
     * @param width  width of the icon
     * @param height height of the icon
     */
    private void createMosaicPattern(Graphics2D g2d, int x, int y, int width, int height) {
        // Define the colors to use in the mosaic
        Color[] mosaicColors = {
            themeStyle.getPrimaryColor(),
            themeStyle.getSuccessColor(),
            themeStyle.getWindowBackground(),
            themeStyle.getPanelBackground(),
            themeStyle.getTabSelectedHighlight(),
            themeStyle.getTabSelectedBackground(),
            themeStyle.getTextForeground(),
            themeStyle.getTextMuted(),
            themeStyle.getFormBackground()
        };

        // Calculate mosaic piece size based on icon size
        int pieceSize = Math.max(4, Math.min(width, height) / 6);
        
        // Create a random number generator with a fixed seed for consistent appearance
        Random random = new Random(42); // Fixed seed for consistent mosaic pattern
        
        // Draw mosaic pieces
        for (int row = 0; row < height / pieceSize + 1; row++) {
            for (int col = 0; col < width / pieceSize + 1; col++) {
                int pieceX = x + col * pieceSize;
                int pieceY = y + row * pieceSize;
                int pieceWidth = Math.min(pieceSize, width - pieceX + x);
                int pieceHeight = Math.min(pieceSize, height - pieceY + y);
                
                if (pieceWidth > 0 && pieceHeight > 0) {
                    // Select color based on position and some randomness
                    int colorIndex = (row * 3 + col + random.nextInt(3)) % mosaicColors.length;
                    Color color = mosaicColors[colorIndex];
                    
                    // Draw the mosaic piece
                    g2d.setColor(color);
                    g2d.fill(new Rectangle2D.Double(pieceX, pieceY, pieceWidth, pieceHeight));
                    
                    // Add a subtle border to separate pieces
                    g2d.setColor(themeStyle.getTextMuted());
                    g2d.draw(new Rectangle2D.Double(pieceX, pieceY, pieceWidth, pieceHeight));
                }
            }
        }
        
        // Draw a border around the entire icon
        g2d.setColor(borderColor);
        g2d.drawRect(x, y, width - 1, height - 1);
    }

    @Override
    public int getIconWidth() {
        return iconWidth;
    }

    @Override
    public int getIconHeight() {
        return iconHeight;
    }

    /**
     * Sets the icon size
     *
     * @param width  new width
     * @param height new height
     */
    public void setIconSize(int width, int height) {
        this.iconWidth = width;
        this.iconHeight = height;
    }

    /**
     * Gets the theme style used by this icon
     *
     * @return the theme style
     */
    public JIPipeDesktopModernThemeStyle getThemeStyle() {
        return themeStyle;
    }

    @Override
    public Color getFillColor() {
        return fillColor;
    }

    @Override
    public void setFillColor(Color c) {
        this.fillColor = c;
    }

    @Override
    public Color getBorderColor() {
        return borderColor;
    }

    @Override
    public void setBorderColor(Color c) {
        this.borderColor = c;
    }
}