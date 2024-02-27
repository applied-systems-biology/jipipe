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

package org.hkijena.jipipe.extensions.imagejalgorithms.parameters;

public enum Neighborhood3D {
    SixConnected,
    TwentySixConnected;

    public int getNativeValue() {
        return this == SixConnected ? 6 : 26;
    }

    @Override
    public String toString() {
        switch (this) {
            case SixConnected:
                return "6-connected";
            case TwentySixConnected:
                return "26-connected";
            default:
                return super.toString();
        }
    }
}
