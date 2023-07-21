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

package org.hkijena.jipipe.extensions.imagejalgorithms.parameters;

public enum Neighborhood2D3D {
    NoDiagonals,
    IncludingDiagonals;

    public int getNativeValue2D() {
        return this == NoDiagonals ? 4 : 8;
    }

    public int getNativeValue3D() {
        return this == NoDiagonals ? 6 : 26;
    }

    @Override
    public String toString() {
        switch (this) {
            case NoDiagonals:
                return "No diagonals (2D: 4, 3D: 6)";
            case IncludingDiagonals:
                return "Including diagonals (2D: 8, 3D: 26)";
            default:
                return super.toString();
        }
    }
}
