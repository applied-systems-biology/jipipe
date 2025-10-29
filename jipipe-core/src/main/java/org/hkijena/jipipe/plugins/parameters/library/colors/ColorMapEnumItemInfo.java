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

package org.hkijena.jipipe.plugins.parameters.library.colors;

import org.apache.commons.text.WordUtils;
import org.hkijena.jipipe.plugins.parameters.api.enums.JIPipeEnumItemInfoRenderTarget;
import org.hkijena.jipipe.plugins.parameters.api.enums.JIPipeEnumParameterItemInfo;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link JIPipeEnumParameterItemInfo} implementation for {@link ColorMap}
 */
public class ColorMapEnumItemInfo implements JIPipeEnumParameterItemInfo {

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
    public Icon getIcon(Object value, JIPipeEnumItemInfoRenderTarget renderTarget) {
        return icons.getOrDefault(value, null);
    }

    @Override
    public String getLabel(Object value, JIPipeEnumItemInfoRenderTarget renderTarget) {
        return WordUtils.capitalize("" + value);
    }

    @Override
    public String getTooltip(Object value, JIPipeEnumItemInfoRenderTarget renderTarget) {
        return null;
    }
}
