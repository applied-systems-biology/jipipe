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

package org.hkijena.jipipe.extensions.parameters.library.primitives;

import java.awt.*;

public enum FontStyleParameter {
    Plain(Font.PLAIN),
    Bold(Font.BOLD),
    Italic(Font.ITALIC),
    BoldItalic(Font.BOLD + Font.ITALIC);

    private final int nativeValue;

    FontStyleParameter(int nativeValue) {

        this.nativeValue = nativeValue;
    }

    @Override
    public String toString() {
        if (this == BoldItalic) {
            return "Bold + Italic";
        }
        return super.toString();
    }

    public Font toFont(FontFamilyParameter family, int size) {
        return family.toFont(nativeValue, size);
    }

    public int getNativeValue() {
        return nativeValue;
    }
}
