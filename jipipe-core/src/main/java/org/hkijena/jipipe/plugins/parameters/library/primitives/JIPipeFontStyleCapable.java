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

package org.hkijena.jipipe.plugins.parameters.library.primitives;

import java.awt.Font;

/**
 * A {@link JIPipeFontValue} that also carries a {@link Font} style
 * (#1339) — the bit flags {@link Font#PLAIN}, {@link Font#BOLD} and
 * {@link Font#ITALIC}, combined additively.
 */
public interface JIPipeFontStyleCapable {

    /** @return the style bit flags */
    int getStyle();

    /** @param style the style bit flags */
    void setStyle(int style);
}
