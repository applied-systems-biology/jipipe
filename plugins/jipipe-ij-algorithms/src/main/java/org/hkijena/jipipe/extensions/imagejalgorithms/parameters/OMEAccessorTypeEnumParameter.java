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

package org.hkijena.jipipe.extensions.imagejalgorithms.parameters;

import org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsPlugin;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.OMEAccessorTemplate;
import org.hkijena.jipipe.extensions.parameters.library.primitives.DynamicStringEnumParameter;

public class OMEAccessorTypeEnumParameter extends DynamicStringEnumParameter {
    public OMEAccessorTypeEnumParameter() {
        for (String id : ImageJAlgorithmsPlugin.OME_ACCESSOR_STORAGE.getTemplateMap().keySet()) {
            getAllowedValues().add(id);
        }
        if (!getAllowedValues().isEmpty())
            setValue(getAllowedValues().get(0));
    }

    public OMEAccessorTypeEnumParameter(OMEAccessorTypeEnumParameter other) {
        super(other);
    }

    @Override
    public String renderLabel(String value) {
        OMEAccessorTemplate template = ImageJAlgorithmsPlugin.OME_ACCESSOR_STORAGE.getTemplateMap().getOrDefault(value, null);
        if (template != null) {
            return template.getName();
        } else {
            return value;
        }
    }

    @Override
    public String renderTooltip(String value) {
        OMEAccessorTemplate template = ImageJAlgorithmsPlugin.OME_ACCESSOR_STORAGE.getTemplateMap().getOrDefault(value, null);
        if (template != null) {
            return template.getDescription();
        } else {
            return "";
        }
    }
}
