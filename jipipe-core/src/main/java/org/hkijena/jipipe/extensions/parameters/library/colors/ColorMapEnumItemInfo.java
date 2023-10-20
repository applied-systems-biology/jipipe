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

package org.hkijena.jipipe.extensions.parameters.library.colors;

import org.apache.commons.text.WordUtils;
import org.hkijena.jipipe.extensions.parameters.api.enums.EnumItemInfo;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link EnumItemInfo} implementation for {@link ColorMap}
 */
public class ColorMapEnumItemInfo implements EnumItemInfo {

    private Map<ColorMap, ColorMapIcon> icons = new HashMap<>();

    /**
     * Creates a new instance
     */
    public ColorMapEnumItemInfo() {
        for (ColorMap value : ColorMap.values()) {
            icons.put(value, new ColorMapIcon(32, 16, value));
        }
    }

    @Override
    public Icon getIcon(Object value) {
        return icons.getOrDefault(value, null);
    }

    @Override
    public String getLabel(Object value) {
        return WordUtils.capitalize("" + value);
    }

    @Override
    public String getTooltip(Object value) {
        return null;
    }
}
