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

import java.awt.*;

/**
 * The common contract of the font parameter family (#1339): every member
 * carries a font family and a size, exposes the concrete {@link Font} to
 * draw with, and supports a copy operation. Capability-specific fields live
 * on the marker interfaces {@link JIPipeFontStyleCapable} (style flags) and
 * {@link JIPipeFontColorCapable} (text color), so a single adaptive editor
 * can gate its sections by {@code instanceof}.
 * <p>
 * All members keep their fields non-null - missing JSON keys fall back to
 * defaults on deserialization, which keeps old projects loadable.
 */
public interface JIPipeFontValue {

    /**
     * @return the font family name (never null)
     */
    String getFamily();

    /**
     * @param family the font family name (null resets to the default family)
     */
    void setFamily(String family);

    /**
     * @return the font size in points
     */
    int getSize();

    /**
     * @param size the font size in points
     */
    void setSize(int size);

    /**
     * @return the font as an {@link Font} (color, if present, is <em>not</em> part of a {@link Font})
     */
    Font toFont();

    /**
     * @return an independent copy of this value
     */
    JIPipeFontValue copy();
}
