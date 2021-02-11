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

package org.hkijena.jipipe.extensions.parameters.roi;

import java.awt.*;

/**
 * An anchor
 */
public enum Anchor {
    TopLeft(Margin.PARAM_LEFT | Margin.PARAM_TOP | Margin.PARAM_WIDTH | Margin.PARAM_HEIGHT, 0, 0),
    TopCenter(Margin.PARAM_LEFT | Margin.PARAM_TOP | Margin.PARAM_RIGHT | Margin.PARAM_HEIGHT, 0.5, 0),
    TopRight(Margin.PARAM_RIGHT | Margin.PARAM_TOP | Margin.PARAM_WIDTH | Margin.PARAM_HEIGHT, 1, 0),
    BottomLeft(Margin.PARAM_LEFT | Margin.PARAM_BOTTOM | Margin.PARAM_HEIGHT | Margin.PARAM_WIDTH, 0, 1),
    BottomCenter(Margin.PARAM_LEFT | Margin.PARAM_BOTTOM | Margin.PARAM_HEIGHT | Margin.PARAM_RIGHT, 0.5, 1),
    BottomRight(Margin.PARAM_WIDTH | Margin.PARAM_BOTTOM | Margin.PARAM_HEIGHT | Margin.PARAM_RIGHT, 1, 1),
    CenterLeft(Margin.PARAM_TOP | Margin.PARAM_BOTTOM | Margin.PARAM_WIDTH | Margin.PARAM_LEFT, 0, 0.5),
    CenterRight(Margin.PARAM_TOP | Margin.PARAM_BOTTOM | Margin.PARAM_WIDTH | Margin.PARAM_RIGHT, 1, 0.5),
    CenterCenter(Margin.PARAM_LEFT | Margin.PARAM_TOP | Margin.PARAM_RIGHT | Margin.PARAM_BOTTOM, 0.5, 0.5);

    private final int relevantParameters;
    private final double relativeX;
    private final double relativeY;

    Anchor(int relevantParameters, double relativeX, double relativeY) {
        this.relevantParameters = relevantParameters;
        this.relativeX = relativeX;
        this.relativeY = relativeY;
    }

    public int getRelevantParameters() {
        return relevantParameters;
    }

    /**
     * Reconstructs the rectangle from a location anchored inside
     *
     * @param width  the width
     * @param height the height
     * @return the rectangle
     */
    public Rectangle getRectangle(Point location, int width, int height) {
        int x = (int) (location.x - relativeX * (width - 1));
        int y = (int) (location.y - relativeY * (height - 1));
        return new Rectangle(x, y, width, height);
    }

    /**
     * Gets the coordinate inside given rectangle
     *
     * @param rectangle the rectangle
     * @return the coordinate inside the rectangle according to the anchor
     */
    public Point getRectangleCoordinates(Rectangle rectangle) {
        return new Point((int) (rectangle.x + relativeX * (rectangle.width - 1)),
                (int) (rectangle.y + relativeY * (rectangle.height - 1)));
    }

    public double getRelativeX() {
        return relativeX;
    }

    public double getRelativeY() {
        return relativeY;
    }
}
