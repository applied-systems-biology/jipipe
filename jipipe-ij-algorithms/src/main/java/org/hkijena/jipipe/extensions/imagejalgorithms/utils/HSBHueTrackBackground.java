package org.hkijena.jipipe.extensions.imagejalgorithms.utils;

import org.hkijena.jipipe.extensions.parameters.library.ranges.PaintGenerator;

import java.awt.Color;
import java.awt.LinearGradientPaint;
import java.awt.Paint;

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
