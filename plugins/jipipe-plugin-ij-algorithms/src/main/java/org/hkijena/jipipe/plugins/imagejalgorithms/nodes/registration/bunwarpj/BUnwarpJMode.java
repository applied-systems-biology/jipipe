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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.registration.bunwarpj;

public enum BUnwarpJMode {
    Fast(0),
    Accurate(1),
    Mono(2);

    private final int nativeValue;

    BUnwarpJMode(int nativeValue) {
        this.nativeValue = nativeValue;
    }

    public int getNativeValue() {
        return nativeValue;
    }


    @Override
    public String toString() {
        switch (this) {
            case Fast:
                return "Fast (Bi-directional)";
            case Accurate:
                return "Accurate (Bi-directional)";
            case Mono:
                return "Mono";
            default:
                return super.toString();
        }

    }
}
