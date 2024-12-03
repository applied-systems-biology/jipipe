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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.registration;

public enum BUnwarpJMinScaleDeformation {
    VeryCoarse(0),
    Coarse(1),
    Fine(2),
    VeryFine(3);

    private final int nativeValue;

    BUnwarpJMinScaleDeformation(int nativeValue) {
        this.nativeValue = nativeValue;
    }

    public int getNativeValue() {
        return nativeValue;
    }
}
