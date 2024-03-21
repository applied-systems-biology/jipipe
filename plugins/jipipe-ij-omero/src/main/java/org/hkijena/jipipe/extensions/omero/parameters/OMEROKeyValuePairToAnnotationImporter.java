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

package org.hkijena.jipipe.extensions.omero.parameters;

import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.MetadataFacility;
import omero.gateway.model.DataObject;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.omero.util.OMEROUtils;

import java.util.List;
import java.util.Map;

public class OMEROKeyValuePairToAnnotationImporter extends AbstractJIPipeParameterCollection {
    private boolean enabled = true;
    private JIPipeExpressionParameter filter = new JIPipeExpressionParameter("true");
    private JIPipeExpressionParameter annotationName = new JIPipeExpressionParameter("\"OMERO:KV:\" + key");
    private JIPipeExpressionParameter annotationValue = new JIPipeExpressionParameter("value");

    public OMEROKeyValuePairToAnnotationImporter() {

    }

    public OMEROKeyValuePairToAnnotationImporter(OMEROKeyValuePairToAnnotationImporter other) {
        this.enabled = other.enabled;
        this.filter = new JIPipeExpressionParameter(other.filter);
        this.annotationName = new JIPipeExpressionParameter(other.annotationName);
        this.annotationValue = other.annotationValue;
    }

    @SetJIPipeDocumentation(name = "Enabled", description = "Determined if the import of key-value pairs is enabled")
    @JIPipeParameter(value = "enabled", uiOrder = -100)
    public boolean isEnabled() {
        return enabled;
    }

    @JIPipeParameter("enabled")
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @SetJIPipeDocumentation(name = "Filter", description = "Allows to filter out key-value pairs from being converted into annotations")
    @JIPipeParameter("filter")
    @JIPipeExpressionParameterSettings(hint = "per key-value pair")
    @JIPipeExpressionParameterVariable(name = "Key-value pair key", key = "key", description = "The key of the key-value pair")
    @JIPipeExpressionParameterVariable(name = "Key-value pair value", key = "value", description = "The value of the key-value pair")
    public JIPipeExpressionParameter getFilter() {
        return filter;
    }

    @JIPipeParameter("filter")
    public void setFilter(JIPipeExpressionParameter filter) {
        this.filter = filter;
    }

    @SetJIPipeDocumentation(name = "Annotation name", description = "The name of the generated annotation")
    @JIPipeParameter("annotation-name")
    @JIPipeExpressionParameterSettings(hint = "per key-value pair")
    @JIPipeExpressionParameterVariable(name = "Key-value pair key", key = "key", description = "The key of the key-value pair")
    @JIPipeExpressionParameterVariable(name = "Key-value pair value", key = "value", description = "The value of the key-value pair")
    public JIPipeExpressionParameter getAnnotationName() {
        return annotationName;
    }

    @JIPipeParameter("annotation-name")
    public void setAnnotationName(JIPipeExpressionParameter annotationName) {
        this.annotationName = annotationName;
    }

    @SetJIPipeDocumentation(name = "Annotation value", description = "The value of the generated annotation")
    @JIPipeParameter("annotation-value")
    @JIPipeExpressionParameterSettings(hint = "per key-value pair")
    @JIPipeExpressionParameterVariable(name = "Key-value pair key", key = "key", description = "The key of the key-value pair")
    @JIPipeExpressionParameterVariable(name = "Key-value pair value", key = "value", description = "The value of the key-value pair")
    public JIPipeExpressionParameter getAnnotationValue() {
        return annotationValue;
    }

    @JIPipeParameter("annotation-value")
    public void setAnnotationValue(JIPipeExpressionParameter annotationValue) {
        this.annotationValue = annotationValue;
    }

    public void createAnnotations(List<JIPipeTextAnnotation> target, MetadataFacility metadata, SecurityContext context, DataObject dataObject) throws DSOutOfServiceException, DSAccessException {
        if(enabled) {
            Map<String, String> keyValuePairs = OMEROUtils.getKeyValuePairs(metadata, context, dataObject);
            JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
            for (Map.Entry<String, String> entry : keyValuePairs.entrySet()) {
                variables.set("key", entry.getKey());
                variables.set("value", entry.getValue());
                if (filter.evaluateToBoolean(variables)) {
                    target.add(new JIPipeTextAnnotation(
                            annotationName.evaluateToString(variables),
                            annotationValue.evaluateToString(variables)
                    ));
                }
            }
        }
    }
}
