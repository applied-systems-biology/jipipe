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

package org.hkijena.jipipe.extensions.imagejdatatypes.util;

/**
 * Determines how elements of ROI are drawn
 */
public enum ROIElementDrawingMode {
    Always,
    Never,
    IfAvailable;

    @Override
    public String toString() {
        if (this == IfAvailable) {
            return "If available";
        }
        return super.toString();
    }

    public boolean shouldDraw(Object existing, Object alternative) {
        switch (this) {
            case Always:
                return existing != null || alternative != null;
            case Never:
                return false;
            case IfAvailable:
                return existing != null;
            default:
                throw new UnsupportedOperationException();
        }
    }
}
