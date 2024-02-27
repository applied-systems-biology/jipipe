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

package org.hkijena.jipipe.ui.grapheditor.general.connections;

/**
 * Contains coordinates of a line that is rectangular
 * This abstracts away x and y for horizontal and vertical implementations
 */
public class RectangularLine {
    public int a0;
    public int a1;
    public int b0;
    public int b1;

    /**
     * @param a0 the first major coordinate (equivalent to x0 in horizontal)
     * @param a1 the second major coordinate (equivalent to x1 in horizontal).
     * @param b0 the first minor coordinate (equivalent to y0 in horizontal)
     * @param b1 the second minor coordinate (equivalent to y1 in horizontal)
     */
    public RectangularLine(int a0, int a1, int b0, int b1) {
        this.a0 = a0;
        this.a1 = a1;
        this.b0 = b0;
        this.b1 = b1;
    }

    public int getA1() {
        return a1;
    }

    public int getB1() {
        return b1;
    }

    public int getSmallerMajor() {
        return Math.min(a0, a1);
    }

    public int getLargerMajor() {
        return Math.max(a0, a1);
    }

    public int getSmallerMinor() {
        return Math.min(b0, b1);
    }

    public int getLargerMinor() {
        return Math.max(b0, b1);
    }

    /**
     * Returns number of pixels that intersect with another line
     *
     * @param other the other line
     * @return how many pixels intersect. Can be negative
     */
    public int intersect(RectangularLine other) {
        if (other.b1 == b1) {
            return Math.min(other.getLargerMajor(), getLargerMajor()) - Math.max(other.getSmallerMajor(), getSmallerMajor());
        } else if (other.a1 == a1) {
            return Math.min(other.getLargerMinor(), getLargerMinor()) - Math.max(other.getSmallerMinor(), getSmallerMinor());
        }
        return 0;
    }
}
