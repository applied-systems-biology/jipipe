/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.parameters.library.colors;

import ij.process.LUT;
import org.hkijena.jipipe.extensions.parameters.api.enums.EnumParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * All available color maps
 */
@EnumParameterSettings(itemInfo = ColorMapEnumItemInfo.class, searchable = true)
public enum ColorMap {
    viridis,
    plasma,
    inferno,
    magma,
    cividis,
    Greys,
    Purples,
    Blues,
    Greens,
    Oranges,
    Reds,
    YlOrBr,
    YlOrRd,
    OrRd,
    PuRd,
    RdPu,
    BuPu,
    GnBu,
    PuBu,
    YlGnBu,
    PuBuGn,
    BuGn,
    YlGn,
    binary,
    gist_yarg,
    gist_gray,
    gray,
    bone,
    pink,
    spring,
    summer,
    autumn,
    winter,
    cool,
    Wistia,
    hot,
    afmhot,
    gist_heat,
    copper,
    PiYG,
    PRGn,
    BrBG,
    PuOr,
    RdGy,
    RdBu,
    RdYlBu,
    RdYlGn,
    Spectral,
    coolwarm,
    bwr,
    seismic,
    twilight,
    twilight_shifted,
    hsv;

    private final BufferedImage mapImage;

    ColorMap() {
        try {
            this.mapImage = ImageIO.read(ResourceUtils.getPluginResourceAsStream("colormaps/" + name() + ".png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public BufferedImage getMapImage() {
        return mapImage;
    }

    /**
     * Generates a color according to the color map
     *
     * @param value the value. should be between 0 and 1 for reasonable output. Automatically clamped to 0 and 1.
     * @return the color
     */
    public Color apply(double value) {
        int pixel = Math.max(0, Math.min(511, (int) (value * 512)));
        return new Color(mapImage.getRGB(pixel, 0));
    }

    /**
     * Converts the color map into an ImageJ {@link LUT}
     *
     * @return the LUT
     */
    public LUT toLUT() {
        byte[] rLut = new byte[256];
        byte[] gLut = new byte[256];
        byte[] bLut = new byte[256];

        for (int i = 0; i < 256; i++) {
            int lutIndex = i * 2;
            Color color = new Color(mapImage.getRGB(lutIndex, 0));
            rLut[i] = (byte) color.getRed();
            gLut[i] = (byte) color.getGreen();
            bLut[i] = (byte) color.getBlue();
        }
        return new LUT(rLut, gLut, bLut);
    }
}
