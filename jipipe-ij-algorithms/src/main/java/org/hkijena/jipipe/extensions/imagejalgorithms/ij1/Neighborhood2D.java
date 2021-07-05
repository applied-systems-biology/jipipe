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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1;

public enum Neighborhood2D {
    FourConnected,
    EightConnected;

    public int getNativeValue() {
        return this == FourConnected ? 4 : 8;
    }

    @Override
    public String toString() {
        switch (this) {
            case FourConnected:
                return "4-connected";
            case EightConnected:
                return "8-connected";
            default:
                return super.toString();
        }
    }
}
