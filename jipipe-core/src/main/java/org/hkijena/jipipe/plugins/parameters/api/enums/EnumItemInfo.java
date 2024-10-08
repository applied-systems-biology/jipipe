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

package org.hkijena.jipipe.plugins.parameters.api.enums;

import javax.swing.*;

/**
 * Used by {@link EnumDesktopParameterEditorUI} to extract icons and other information for its enum value
 */
public interface EnumItemInfo {

    /**
     * Extracts an icon for the enum value
     *
     * @param value the enum value
     * @return the icon of the enum value
     */
    Icon getIcon(Object value);

    /**
     * Extracts a custom label for the enum value
     *
     * @param value the enum value
     * @return custom label
     */
    String getLabel(Object value);

    /**
     * Extracts a custom tooltip for the enum value
     *
     * @param value the enum value
     * @return custom tooltip
     */
    String getTooltip(Object value);
}
