package org.hkijena.jipipe.extensions.omero.parameters;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;

import java.util.Collection;
import java.util.Map;

public class AnnotationsToOMEROKeyValuePairExporter extends AbstractJIPipeParameterCollection {
    private boolean enabled = true;
    private DefaultExpressionParameter filter = new DefaultExpressionParameter("STRING_STARTS_WITH(key, \"OMERO:KV:\")");
    private DefaultExpressionParameter keyValuePairKey = new DefaultExpressionParameter("REPLACE_IN_STRING(key, \"OMERO:KV:\", \"\")");
    private DefaultExpressionParameter keyValuePairValue = new DefaultExpressionParameter("value");

    public AnnotationsToOMEROKeyValuePairExporter() {

    }

    public AnnotationsToOMEROKeyValuePairExporter(AnnotationsToOMEROKeyValuePairExporter other) {
        this.enabled = other.enabled;
        this.filter = new DefaultExpressionParameter(other.filter);
        this.keyValuePairKey = new DefaultExpressionParameter(other.keyValuePairKey);
        this.keyValuePairValue = new DefaultExpressionParameter(other.keyValuePairValue);
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

    @JIPipeDocumentation(name = "Filter annotations", description = "Allows to only target specific annotations to be exported as key-value pair")
    @JIPipeParameter(value = "filter", uiOrder = -90)
    @ExpressionParameterSettings(hint = "per annotation")
    @ExpressionParameterSettingsVariable(name = "Annotation name", description = "The name/key of the annotation", key = "key")
    @ExpressionParameterSettingsVariable(name = "Annotation value", description = "The value of the annotation", key = "value")
    public DefaultExpressionParameter getFilter() {
        return filter;
    }

    @JIPipeParameter("filter")
    public void setFilter(DefaultExpressionParameter filter) {
        this.filter = filter;
    }

    @JIPipeDocumentation(name = "Key-value pair key", description = "Expression that generates the key of the key-value pair")
    @JIPipeParameter(value = "key-value-pair-key", uiOrder = -80)
    @ExpressionParameterSettings(hint = "per annotation")
    @ExpressionParameterSettingsVariable(name = "Annotation name", description = "The name/key of the annotation", key = "key")
    @ExpressionParameterSettingsVariable(name = "Annotation value", description = "The value of the annotation", key = "value")
    public DefaultExpressionParameter getKeyValuePairKey() {
        return keyValuePairKey;
    }

    @JIPipeParameter("key-value-pair-key")
    public void setKeyValuePairKey(DefaultExpressionParameter keyValuePairKey) {
        this.keyValuePairKey = keyValuePairKey;
    }

    @JIPipeDocumentation(name = "Key-value pair value", description = "Expression that generates the value of the key-value pair")
    @JIPipeParameter(value = "key-value-pair-value", uiOrder = -70)
    @ExpressionParameterSettings(hint = "per annotation")
    @ExpressionParameterSettingsVariable(name = "Annotation name", description = "The name/key of the annotation", key = "key")
    @ExpressionParameterSettingsVariable(name = "Annotation value", description = "The value of the annotation", key = "value")
    public DefaultExpressionParameter getKeyValuePairValue() {
        return keyValuePairValue;
    }

    @JIPipeParameter("key-value-pair-value")
    public void setKeyValuePairValue(DefaultExpressionParameter keyValuePairValue) {
        this.keyValuePairValue = keyValuePairValue;
    }

    public void createKeyValuePairs(Map<String, String> target, Collection<JIPipeTextAnnotation> annotations) {
        if(enabled) {
            ExpressionVariables variables = new ExpressionVariables();
            for (JIPipeTextAnnotation annotation : annotations) {
               variables.set("key", annotation.getName());
               variables.set("value", annotation.getValue());
               if(filter.test(variables)) {
                   String key = keyValuePairKey.evaluateToString(variables);
                   String value = keyValuePairValue.evaluateToString(variables);
                   target.put(key, value);
               }
            }
        }
    }
}