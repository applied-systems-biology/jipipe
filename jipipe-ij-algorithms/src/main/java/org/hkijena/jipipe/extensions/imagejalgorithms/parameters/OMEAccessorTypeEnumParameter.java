package org.hkijena.jipipe.extensions.imagejalgorithms.parameters;

import org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.OMEAccessorTemplate;
import org.hkijena.jipipe.extensions.parameters.library.primitives.DynamicStringEnumParameter;

public class OMEAccessorTypeEnumParameter extends DynamicStringEnumParameter {
    public OMEAccessorTypeEnumParameter() {
        for (String id : ImageJAlgorithmsExtension.OME_ACCESSOR_STORAGE.getTemplateMap().keySet()) {
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
        OMEAccessorTemplate template = ImageJAlgorithmsExtension.OME_ACCESSOR_STORAGE.getTemplateMap().getOrDefault(value, null);
        if (template != null) {
            return template.getName();
        } else {
            return value;
        }
    }

    @Override
    public String renderTooltip(String value) {
        OMEAccessorTemplate template = ImageJAlgorithmsExtension.OME_ACCESSOR_STORAGE.getTemplateMap().getOrDefault(value, null);
        if (template != null) {
            return template.getDescription();
        } else {
            return "";
        }
    }
}
