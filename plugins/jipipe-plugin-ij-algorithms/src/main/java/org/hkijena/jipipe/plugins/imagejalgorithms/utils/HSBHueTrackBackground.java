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

package org.hkijena.jipipe.plugins.imagejalgorithms.utils;

import org.hkijena.jipipe.plugins.parameters.library.ranges.PaintGenerator;

import java.awt.*;

public class HSBHueTrackBackground implements PaintGenerator {

    private static float[] FRACTIONS = new float[255];
    private static Color[] COLORS = new Color[255];

    static {
        for (int i = 0; i < 255; i++) {
            FRACTIONS[i] = i * (1.0f / 255.0f);
            COLORS[i] = new Color(Color.HSBtoRGB(FRACTIONS[i], 1.0f, 1.0f));
        }
    }

    @Override
    public Paint generate(int x, int y, int width, int height) {
        return new LinearGradientPaint(x, y, x + width, y, FRACTIONS, COLORS);
    }
}
