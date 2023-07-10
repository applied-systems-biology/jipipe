package org.hkijena.jipipe.extensions.imagejalgorithms.parameters;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.OMEAccessorTemplate;

import java.util.Set;

public class OMEAccessorParameter {
    private String accessorId;
    private JIPipeDynamicParameterCollection parameters = new JIPipeDynamicParameterCollection(false);

    public OMEAccessorParameter() {
        Set<String> keys = ImageJAlgorithmsExtension.OME_ACCESSOR_STORAGE.getTemplateMap().keySet();
        if(!keys.isEmpty()) {
            accessorId = keys.iterator().next();
        }
        resetParameters();
    }

    public OMEAccessorParameter(OMEAccessorParameter other) {
        this.accessorId = other.accessorId;
        this.parameters = new JIPipeDynamicParameterCollection(other.parameters);
    }

    @JsonGetter("accessor-id")
    public String getAccessorId() {
        return accessorId;
    }

    @JsonSetter("accessor-id")
    public void setAccessorId(String accessorId) {
        this.accessorId = accessorId;
        if(parameters.getParameters().isEmpty()) {
            // Load appropriate parameters in
            resetParameters();
        }
    }

    public void resetParameters() {
        if(accessorId != null) {
            OMEAccessorTemplate template = ImageJAlgorithmsExtension.OME_ACCESSOR_STORAGE.getTemplateMap().getOrDefault(accessorId, null);
            parameters = new JIPipeDynamicParameterCollection(template.getParameterCollection());
        }
    }

    @JsonGetter("parameters")
    public JIPipeDynamicParameterCollection getParameters() {
        return parameters;
    }

    @JsonSetter("parameters")
    public void setParameters(JIPipeDynamicParameterCollection parameters) {
        this.parameters = parameters;
    }
}
