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

import java.awt.geom.Rectangle2D;

/**
 * Pure math for fitting the graph view to a set of nodes. Picks the zoom so the bounds
 * fit into the viewport (limited by the tightest axis and the zoom limits) and computes
 * the scroll position that centers the bounds. Has no UI dependencies; UI-facing
 * operations translate the returned zoom and scroll into view state.
 */
final class ViewFit {

    private ViewFit() {
    }

    /**
     * Computes the zoom and scroll position that fit the given bounds into the viewport.
     * <p>
     * The new zoom is the current zoom scaled by the tightest axis ratio, clamped into
     * {@code [minZoom, maxZoom]}. The scroll positions center the bounds center, converted
     * from current-zoom pixels into new-zoom pixels, minus half the viewport. Degenerate
     * bounds (width or height at most 0) keep the clamped current zoom and scroll 0/0.
     * All returned values are finite.
     * </p>
     *
     * @param boundsAtCurrentZoom the bounds to fit, measured at the current zoom
     * @param currentZoom         the current zoom level
     * @param viewportWidth       the viewport width in pixels
     * @param viewportHeight      the viewport height in pixels
     * @param minZoom             the lowest allowed zoom level
     * @param maxZoom             the highest allowed zoom level
     * @return the fitted zoom and the scroll position that centers the bounds
     */
    static FitResult compute(Rectangle2D boundsAtCurrentZoom, double currentZoom, int viewportWidth, int viewportHeight, double minZoom, double maxZoom) {
        double boundsWidth = boundsAtCurrentZoom.getWidth();
        double boundsHeight = boundsAtCurrentZoom.getHeight();
        if (boundsWidth <= 0 || boundsHeight <= 0) {
            return new FitResult(clamp(currentZoom, minZoom, maxZoom), 0, 0);
        }
        double scale = Math.min(viewportWidth / Math.max(boundsWidth, 1), viewportHeight / Math.max(boundsHeight, 1));
        double zoom = clamp(currentZoom * scale, minZoom, maxZoom);
        double centerX = boundsAtCurrentZoom.getCenterX();
        double centerY = boundsAtCurrentZoom.getCenterY();
        int scrollX = (int) Math.round(centerX / currentZoom * zoom - viewportWidth / 2.0);
        int scrollY = (int) Math.round(centerY / currentZoom * zoom - viewportHeight / 2.0);
        return new FitResult(zoom, scrollX, scrollY);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    /**
     * The fitted zoom level and the scroll position that centers the fitted bounds.
     */
    record FitResult(double zoom, int scrollX, int scrollY) {
    }
}
