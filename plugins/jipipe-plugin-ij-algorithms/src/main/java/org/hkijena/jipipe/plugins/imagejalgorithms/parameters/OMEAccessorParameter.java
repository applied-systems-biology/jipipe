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

package org.hkijena.jipipe.plugins.imagejalgorithms.parameters;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import ome.units.quantity.Quantity;
import ome.xml.meta.MetadataRetrieve;
import ome.xml.model.primitives.PrimitiveNumber;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeMutableParameterAccess;
import org.hkijena.jipipe.plugins.imagejalgorithms.ImageJAlgorithmsPlugin;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.OMEAccessorTemplate;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

public class OMEAccessorParameter {
    private String accessorId;
    private JIPipeDynamicParameterCollection parameters = new JIPipeDynamicParameterCollection(false);

    public OMEAccessorParameter() {
        Set<String> keys = ImageJAlgorithmsPlugin.OME_ACCESSOR_STORAGE.getTemplateMap().keySet();
        if (!keys.isEmpty()) {
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
        if (parameters.getParameters().isEmpty()) {
            // Load appropriate parameters in
            resetParameters();

        }
    }

    public void resetParameters() {
        if (accessorId != null) {
            OMEAccessorTemplate template = ImageJAlgorithmsPlugin.OME_ACCESSOR_STORAGE.getTemplateMap().getOrDefault(accessorId, null);
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

    public Object evaluate(MetadataRetrieve metadataRetrieve) {
        OMEAccessorTemplate template = ImageJAlgorithmsPlugin.OME_ACCESSOR_STORAGE.getTemplateMap().getOrDefault(accessorId, null);
        if (template != null) {
            Object[] parameters = new Object[template.getParameterIds().size()];
            List<String> parameterIds = template.getParameterIds();
            for (int i = 0; i < parameterIds.size(); i++) {
                String parameterId = parameterIds.get(i);
                JIPipeMutableParameterAccess parameterAccess = this.parameters.getParameter(parameterId);
                if (parameterAccess != null) {
                    parameters[i] = parameterAccess.get(Object.class);
                } else {
                    System.err.println("Resetting parameter map of " + this);
                    resetParameters();
                    return null;
                }
            }
            try {
                return template.getMethod().invoke(metadataRetrieve, parameters);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }

    public String evaluateToString(MetadataRetrieve metadataRetrieve) {
        Object object = evaluate(metadataRetrieve);
        if (object == null) {
            return "";
        } else if (object instanceof Quantity) {
            Quantity quantity = (Quantity) object;
            return quantity.value() + " " + quantity.unit().getSymbol();
        } else if (object.getClass().isArray()) {
            return JsonUtils.toJsonString(object);
        } else if (object instanceof PrimitiveNumber) {
            return StringUtils.nullToEmpty(((PrimitiveNumber) object).getNumberValue());
        }
        return StringUtils.nullToEmpty(object);
    }
}
