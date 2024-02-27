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

package org.hkijena.jipipe.extensions.plots.utils;

import org.jfree.chart.plot.DefaultDrawingSupplier;

import java.awt.*;

/**
 * Provides a color palette
 */
public class ColorMapSupplier extends DefaultDrawingSupplier {
    private final Paint[] colors;
    public int paintIndex;
    public int fillPaintIndex;

    public ColorMapSupplier(Paint[] colors) {
        this.colors = colors;
    }

    @Override
    public Paint getNextPaint() {
        Paint result = colors[paintIndex % colors.length];
        paintIndex++;
        return result;
    }

    @Override
    public Paint getNextFillPaint() {
        Paint result = colors[fillPaintIndex % colors.length];
        fillPaintIndex++;
        return result;
    }
}
