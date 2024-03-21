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

package org.hkijena.jipipe.extensions.imagejalgorithms.utils;

import org.hkijena.jipipe.extensions.parameters.library.ranges.PaintGenerator;

import java.awt.*;

public class LABGreenRedTrackBackground implements PaintGenerator {

    private static float[] FRACTIONS = new float[2];
    private static Color[] COLORS = new Color[2];

    static {
        FRACTIONS[1] = 1.0f;
        COLORS[0] = Color.GREEN;
        COLORS[1] = Color.RED;
    }

    @Override
    public Paint generate(int x, int y, int width, int height) {
        return new LinearGradientPaint(x, y, x + width, y, FRACTIONS, COLORS);
    }
}
