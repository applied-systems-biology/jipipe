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

package org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions;

import org.hkijena.jipipe.plugins.parameters.api.enums.EnumItemInfo;
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
