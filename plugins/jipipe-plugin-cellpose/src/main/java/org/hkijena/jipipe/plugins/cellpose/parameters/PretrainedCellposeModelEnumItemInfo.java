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

package org.hkijena.jipipe.plugins.cellpose.parameters;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.plugins.parameters.api.enums.JIPipeEnumItemInfoRenderTarget;
import org.hkijena.jipipe.plugins.parameters.api.enums.JIPipeEnumParameterItemInfo;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;

public class PretrainedCellposeModelEnumItemInfo implements JIPipeEnumParameterItemInfo {
    @Override
    public Icon getIcon(Object value, JIPipeEnumItemInfoRenderTarget renderTarget) {
        return JIPipe.RESOURCES.getIcon16("apps/cellpose.png");
    }

    @Override
    public String getLabel(Object value, JIPipeEnumItemInfoRenderTarget renderTarget) {
        return StringUtils.orElse(value, "<Null>");
    }

    @Override
    public String getTooltip(Object value, JIPipeEnumItemInfoRenderTarget renderTarget) {
        return null;
    }
}
