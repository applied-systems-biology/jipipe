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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.labels;

import ij.process.LUT;
import inra.ijpb.color.ColorMaps;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Icon that renders a color map entry
 */
public class LabelColorMapIcon implements Icon {
    private int imageWidth;
    private int imageHeight;
    private Color borderColor = Color.BLACK;
    private ColorMaps.CommonLabelMaps colorMap;
    private BufferedImage colorMapImage;

    private Insets insets;

    /**
     * Creates a 16x16 black icon
     */
    public LabelColorMapIcon() {
        this(32, 16);
    }

    /**
     * Creates a viridis icon
     *
     * @param width  icon width
     * @param height icon height
     */
    public LabelColorMapIcon(int width, int height) {
        this(width, height, ColorMaps.CommonLabelMaps.MAIN_COLORS);
    }

    /**
     * @param width    icon width
     * @param height   icon height
     * @param colorMap the color map
     */
    public LabelColorMapIcon(int width, int height, ColorMaps.CommonLabelMaps colorMap) {
        imageWidth = width;
        imageHeight = height;
        this.colorMap = colorMap;
        insets = new Insets(1, 1, 1, 1);
    }

    public int getIconWidth() {
        return imageWidth;
    }

    public int getIconHeight() {
        return imageHeight;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.setColor(borderColor);
        g.drawRect(x, y, imageWidth - 1, imageHeight - 2);

        x += insets.left;
        y += insets.top;

        int w = imageWidth - insets.left - insets.right;
        int h = imageHeight - insets.top - insets.bottom - 1;

        if (colorMapImage == null) {
            byte[][] lut = colorMap.computeLut(256, false);
            byte[] red = new byte[256];
            byte[] green = new byte[256];
            byte[] blue = new byte[256];
            for (int i = 0; i < 256; i++) {
                red[i] = lut[i][0];
                green[i] = lut[i][1];
                blue[i] = lut[i][2];
            }
            colorMapImage = ImageJUtils.lutToImage(new LUT(red, green, blue), 256, 1).getBufferedImage();
        }

        g.drawImage(colorMapImage, x, y, w, h, Color.WHITE, null);
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
    }

    public ColorMaps.CommonLabelMaps getColorMap() {
        return colorMap;
    }

    public void setColorMap(ColorMaps.CommonLabelMaps colorMap) {
        this.colorMap = colorMap;
        this.colorMapImage = null;
    }
}