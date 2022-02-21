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