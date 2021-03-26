package org.hkijena.jipipe.extensions.imagejdatatypes.util;

import org.hkijena.jipipe.extensions.parameters.primitives.EnumItemInfo;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class OptionalBitDepthEnumItemInfo implements EnumItemInfo {
    @Override
    public Icon getIcon(Object value) {
        if (value instanceof OptionalBitDepth) {
            switch ((OptionalBitDepth) value) {
                case Grayscale32f:
                    return UIUtils.getIconFromResources("data-types/imgplus-greyscale-32f.png");
                case Grayscale16u:
                    return UIUtils.getIconFromResources("data-types/imgplus-greyscale-16u.png");
                case Grayscale8u:
                    return UIUtils.getIconFromResources("data-types/imgplus-greyscale-8u.png");
                case ColorRGB:
                    return UIUtils.getIconFromResources("data-types/imgplus-color-rgb.png");
                case None:
                    return UIUtils.getIconFromResources("data-types/imgplus.png");
            }
        }
        return null;
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
