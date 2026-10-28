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

import java.awt.Color;

/**
 * A {@link JIPipeFontValue} that also carries the {@link Color} to draw the
 * text with (#1339) — a {@link java.awt.Font} does not hold a color, so it
 * is a separate capability. The color uses the mapper's standard
 * hex-string codec, like the {@code color} parameter type.
 */
public interface JIPipeFontColorCapable {

    /** @return the text color (never null) */
    Color getColor();

    /** @param color the text color (null resets to white) */
    void setColor(Color color);
}
