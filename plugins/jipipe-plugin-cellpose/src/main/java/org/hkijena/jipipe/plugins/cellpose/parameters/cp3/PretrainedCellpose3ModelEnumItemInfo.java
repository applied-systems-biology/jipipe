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

package org.hkijena.jipipe.plugins.cellpose.parameters.cp3;

import org.hkijena.jipipe.plugins.parameters.api.enums.EnumItemInfo;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class PretrainedCellpose3ModelEnumItemInfo implements EnumItemInfo {
    @Override
    public Icon getIcon(Object value) {
        return UIUtils.getIconFromResources("apps/cellpose.png");
    }

    @Override
    public String getLabel(Object value) {
        return StringUtils.orElse(value, "<Null>");
    }

    @Override
    public String getTooltip(Object value) {
        return null;
    }
}
