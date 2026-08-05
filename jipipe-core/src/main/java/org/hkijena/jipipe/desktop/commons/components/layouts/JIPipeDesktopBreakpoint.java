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

package org.hkijena.jipipe.desktop.commons.components.layouts;

public enum JIPipeDesktopBreakpoint {
    XS(0),
    SM(576),
    MD(768),
    LG(992),
    XL(1200);

    private final int minWidth;

    JIPipeDesktopBreakpoint(int minWidth) {
        this.minWidth = minWidth;
    }

    public int getMinWidth() {
        return minWidth;
    }

    public static JIPipeDesktopBreakpoint fromWidth(int width) {
        JIPipeDesktopBreakpoint result = XS;
        for (JIPipeDesktopBreakpoint bp : values()) {
            if (width >= bp.minWidth) {
                result = bp;
            }
        }
        return result;
    }
}
