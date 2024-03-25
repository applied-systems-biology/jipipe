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

package org.hkijena.jipipe.plugins.parameters.library.roi;

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

    /**
     * Places the inner rectangle into the outer rectangle according to the anchor
     *
     * @param inner the inner (x and y are used to shift the rectangle; ignored on center-center); relative x and y
     * @param outer the outer rectangle; absolute x and y
     * @return the new inner rectangle. width and height are unchanged
     */
    public Rectangle placeInside(Rectangle inner, Rectangle outer) {
        switch (this) {
            case TopLeft:
                return new Rectangle(inner.x + outer.x, inner.y + outer.y, inner.width, inner.height);
            case TopRight:
                return new Rectangle(outer.x + outer.width - inner.width - inner.x, inner.y + outer.y, inner.width, inner.height);
            case TopCenter:
                return new Rectangle(outer.x + outer.width / 2 - inner.width / 2, inner.y + outer.y, inner.width, inner.height);
            case BottomLeft:
                return new Rectangle(inner.x + outer.x, outer.y + outer.height - inner.height - inner.y, inner.width, inner.height);
            case BottomRight:
                return new Rectangle(outer.x + outer.width - inner.width - inner.x, outer.y + outer.height - inner.height - inner.y, inner.width, inner.height);
            case BottomCenter:
                return new Rectangle(outer.x + outer.width / 2 - inner.width / 2, outer.y + outer.height - inner.height - inner.y, inner.width, inner.height);
            case CenterLeft:
                return new Rectangle(outer.x + inner.x, outer.y + outer.height / 2 - inner.height / 2, inner.width, inner.height);
            case CenterRight:
                return new Rectangle(outer.x + outer.width - inner.width - inner.x, outer.y + outer.height / 2 - inner.height / 2, inner.width, inner.height);
            case CenterCenter:
                return new Rectangle(outer.x + outer.width / 2 - inner.width / 2, outer.y + outer.height / 2 - inner.height / 2, inner.width, inner.height);
            default:
                throw new UnsupportedOperationException();
        }
    }

    public double getRelativeX() {
        return relativeX;
    }

    public double getRelativeY() {
        return relativeY;
    }
}
