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

package org.hkijena.jipipe.extensions.parameters.api.enums;

import javax.swing.*;

/**
 * Default implementation of {@link EnumItemInfo}
 */
public class DefaultEnumItemInfo implements EnumItemInfo {
    @Override
    public Icon getIcon(Object value) {
        return null;
    }

    @Override
    public String getLabel(Object value) {
        if (value instanceof Enum) {
            return value.toString();
        } else {
            return "<None selected>";
        }
    }

    @Override
    public String getTooltip(Object value) {
        return null;
    }
}
