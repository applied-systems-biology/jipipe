package org.hkijena.acaq5.extensions.parameters.colors;

import org.apache.commons.lang.WordUtils;
import org.hkijena.acaq5.extensions.parameters.editors.EnumItemInfo;
import org.hkijena.acaq5.utils.StringUtils;

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
        return WordUtils.capitalize( "" + value);
    }
}
