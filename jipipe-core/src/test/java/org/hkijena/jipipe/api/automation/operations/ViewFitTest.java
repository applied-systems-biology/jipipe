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

package org.hkijena.jipipe.api.automation.operations;

import org.junit.jupiter.api.Test;

import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the pure graph view fit math: zoom selection by the tightest axis, clamping to
 * the zoom limits, scroll centering of the bounds center, the rescaling of bounds that
 * were measured at a fractional zoom, and the degenerate-bounds guard.
 */
class ViewFitTest {

    private static final double EPSILON = 1e-9;

    @Test
    void wideGraphFitsByWidth() {
        // Bounds 1000x100 at zoom 1 into a 500x500 viewport: the width is the tightest axis,
        // so the zoom halves. The vertical scroll centers the 100px-high bounds.
        ViewFit.FitResult result = ViewFit.compute(new Rectangle2D.Double(0, 0, 1000, 100), 1.0, 500, 500, 0.1, 3.0);
        assertEquals(0.5, result.zoom(), EPSILON);
        assertEquals(0, result.scrollX());
        assertEquals(-225, result.scrollY());
    }

    @Test
    void tallGraphFitsByHeight() {
        // Bounds 100x1000 at zoom 1 into a 500x500 viewport: the height is the tightest axis.
        ViewFit.FitResult result = ViewFit.compute(new Rectangle2D.Double(0, 0, 100, 1000), 1.0, 500, 500, 0.1, 3.0);
        assertEquals(0.5, result.zoom(), EPSILON);
        assertEquals(-225, result.scrollX());
        assertEquals(0, result.scrollY());
    }

    @Test
    void smallGraphZoomsInAndClampsAtMax() {
        // Bounds 10x10 at zoom 1 into a 500x500 viewport would zoom to 50, but the result
        // is clamped at the maximum zoom.
        ViewFit.FitResult result = ViewFit.compute(new Rectangle2D.Double(0, 0, 10, 10), 1.0, 500, 500, 0.1, 3.0);
        assertEquals(3.0, result.zoom(), EPSILON);
        assertEquals(-235, result.scrollX());
        assertEquals(-235, result.scrollY());
    }

    @Test
    void hugeGraphClampsAtMinZoom() {
        // Bounds 100000x100000 at zoom 1 into a 500x500 viewport would zoom to 0.005, but
        // the result is clamped at the minimum zoom.
        ViewFit.FitResult result = ViewFit.compute(new Rectangle2D.Double(0, 0, 100000, 100000), 1.0, 500, 500, 0.1, 3.0);
        assertEquals(0.1, result.zoom(), EPSILON);
        assertEquals(4750, result.scrollX());
        assertEquals(4750, result.scrollY());
    }

    @Test
    void scrollCentersTheBounds() {
        // Bounds centered at (400, 300), zoom 1 -> 2, viewport 400x400:
        // scroll = center * 2 - 200 per axis.
        ViewFit.FitResult result = ViewFit.compute(new Rectangle2D.Double(300, 200, 200, 200), 1.0, 400, 400, 0.1, 3.0);
        assertEquals(2.0, result.zoom(), EPSILON);
        assertEquals(600, result.scrollX());
        assertEquals(400, result.scrollY());
    }

    @Test
    void degenerateBoundsYieldFiniteUnitFit() {
        // Zero or negative extents never divide: the zoom stays at the clamped current zoom
        // and the scroll is 0/0, all values finite.
        ViewFit.FitResult zeroWidth = ViewFit.compute(new Rectangle2D.Double(100, 50, 0, 200), 1.0, 400, 400, 0.1, 3.0);
        assertTrue(Double.isFinite(zeroWidth.zoom()));
        assertTrue(Double.isFinite(zeroWidth.scrollX()));
        assertTrue(Double.isFinite(zeroWidth.scrollY()));
        assertEquals(1.0, zeroWidth.zoom(), EPSILON);
        assertEquals(0, zeroWidth.scrollX());
        assertEquals(0, zeroWidth.scrollY());

        ViewFit.FitResult zeroHeight = ViewFit.compute(new Rectangle2D.Double(100, 50, 200, 0), 1.0, 400, 400, 0.1, 3.0);
        assertTrue(Double.isFinite(zeroHeight.zoom()));
        assertEquals(1.0, zeroHeight.zoom(), EPSILON);
        assertEquals(0, zeroHeight.scrollX());
        assertEquals(0, zeroHeight.scrollY());

        ViewFit.FitResult negativeWidth = ViewFit.compute(new Rectangle2D.Double(0, 0, -5, 100), 1.0, 400, 400, 0.1, 3.0);
        assertTrue(Double.isFinite(negativeWidth.zoom()));
        assertEquals(1.0, negativeWidth.zoom(), EPSILON);
        assertEquals(0, negativeWidth.scrollX());
        assertEquals(0, negativeWidth.scrollY());

        // The unchanged zoom is still clamped into the allowed range.
        ViewFit.FitResult clampedHigh = ViewFit.compute(new Rectangle2D.Double(0, 0, 0, 100), 5.0, 400, 400, 0.1, 3.0);
        assertEquals(3.0, clampedHigh.zoom(), EPSILON);
        assertEquals(0, clampedHigh.scrollX());
        assertEquals(0, clampedHigh.scrollY());

        ViewFit.FitResult clampedLow = ViewFit.compute(new Rectangle2D.Double(0, 0, 100, 0), 0.05, 400, 400, 0.1, 3.0);
        assertEquals(0.1, clampedLow.zoom(), EPSILON);
        assertEquals(0, clampedLow.scrollX());
        assertEquals(0, clampedLow.scrollY());
    }

    @Test
    void fractionalCurrentZoomRescalesBounds() {
        // Bounds measured at zoom 0.5: 1600x800 current-zoom pixels are 3200x1600 graph
        // pixels. A viewport of 400x400 fits by width at zoom 0.5 * 0.25 = 0.125, and the
        // scroll must convert the bounds center through zoom 1 space:
        // center (900, 400) / 0.5 * 0.125 - 200 = (25, -100).
        ViewFit.FitResult result = ViewFit.compute(new Rectangle2D.Double(100, 0, 1600, 800), 0.5, 400, 400, 0.1, 3.0);
        assertEquals(0.125, result.zoom(), EPSILON);
        assertEquals(25, result.scrollX());
        assertEquals(-100, result.scrollY());
    }
}
