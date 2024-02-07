package org.hkijena.jipipe.extensions.omero.parameters;

import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.MetadataFacility;
import omero.gateway.model.DataObject;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
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

    @JIPipeDocumentation(name = "Enabled", description = "Determined if the import of key-value pairs is enabled")
    @JIPipeParameter(value = "enabled", uiOrder = -100)
    public boolean isEnabled() {
        return enabled;
    }

    @JIPipeParameter("enabled")
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @JIPipeDocumentation(name = "Filter", description = "Allows to filter out key-value pairs from being converted into annotations")
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

    @JIPipeDocumentation(name = "Annotation name", description = "The name of the generated annotation")
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

    @JIPipeDocumentation(name = "Annotation value", description = "The value of the generated annotation")
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
            ExpressionVariables variables = new ExpressionVariables();
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
