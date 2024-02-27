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

package org.hkijena.jipipe.extensions.plots.utils;

import org.hkijena.jipipe.extensions.parameters.api.enums.EnumItemInfo;

import javax.swing.*;

public class ColorMapEnumItemInfo implements EnumItemInfo {

    @Override
    public Icon getIcon(Object value) {
        return new ColorMapIcon(32, 16, (ColorMap) value);
    }

    @Override
    public String getLabel(Object value) {
        return value.toString();
    }

    @Override
    public String getTooltip(Object value) {
        return null;
    }
}
