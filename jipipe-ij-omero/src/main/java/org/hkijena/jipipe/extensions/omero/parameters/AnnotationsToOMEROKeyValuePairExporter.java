package org.hkijena.jipipe.extensions.omero.parameters;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;

import java.util.Collection;
import java.util.Map;

public class AnnotationsToOMEROKeyValuePairExporter extends AbstractJIPipeParameterCollection {
    private boolean enabled = true;
    private JIPipeExpressionParameter filter = new JIPipeExpressionParameter("STRING_STARTS_WITH(key, \"OMERO:KV:\")");
    private JIPipeExpressionParameter keyValuePairKey = new JIPipeExpressionParameter("REPLACE_IN_STRING(key, \"OMERO:KV:\", \"\")");
    private JIPipeExpressionParameter keyValuePairValue = new JIPipeExpressionParameter("value");

    public AnnotationsToOMEROKeyValuePairExporter() {

    }

    public AnnotationsToOMEROKeyValuePairExporter(AnnotationsToOMEROKeyValuePairExporter other) {
        this.enabled = other.enabled;
        this.filter = new JIPipeExpressionParameter(other.filter);
        this.keyValuePairKey = new JIPipeExpressionParameter(other.keyValuePairKey);
        this.keyValuePairValue = new JIPipeExpressionParameter(other.keyValuePairValue);
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

    @SetJIPipeDocumentation(name = "Filter annotations", description = "Allows to only target specific annotations to be exported as key-value pair")
    @JIPipeParameter(value = "filter", uiOrder = -90)
    @JIPipeExpressionParameterSettings(hint = "per annotation")
    @JIPipeExpressionParameterVariable(name = "Annotation name", description = "The name/key of the annotation", key = "key")
    @JIPipeExpressionParameterVariable(name = "Annotation value", description = "The value of the annotation", key = "value")
    public JIPipeExpressionParameter getFilter() {
        return filter;
    }

    @JIPipeParameter("filter")
    public void setFilter(JIPipeExpressionParameter filter) {
        this.filter = filter;
    }

    @SetJIPipeDocumentation(name = "Key-value pair key", description = "Expression that generates the key of the key-value pair")
    @JIPipeParameter(value = "key-value-pair-key", uiOrder = -80)
    @JIPipeExpressionParameterSettings(hint = "per annotation")
    @JIPipeExpressionParameterVariable(name = "Annotation name", description = "The name/key of the annotation", key = "key")
    @JIPipeExpressionParameterVariable(name = "Annotation value", description = "The value of the annotation", key = "value")
    public JIPipeExpressionParameter getKeyValuePairKey() {
        return keyValuePairKey;
    }

    @JIPipeParameter("key-value-pair-key")
    public void setKeyValuePairKey(JIPipeExpressionParameter keyValuePairKey) {
        this.keyValuePairKey = keyValuePairKey;
    }

    @SetJIPipeDocumentation(name = "Key-value pair value", description = "Expression that generates the value of the key-value pair")
    @JIPipeParameter(value = "key-value-pair-value", uiOrder = -70)
    @JIPipeExpressionParameterSettings(hint = "per annotation")
    @JIPipeExpressionParameterVariable(name = "Annotation name", description = "The name/key of the annotation", key = "key")
    @JIPipeExpressionParameterVariable(name = "Annotation value", description = "The value of the annotation", key = "value")
    public JIPipeExpressionParameter getKeyValuePairValue() {
        return keyValuePairValue;
    }

    @JIPipeParameter("key-value-pair-value")
    public void setKeyValuePairValue(JIPipeExpressionParameter keyValuePairValue) {
        this.keyValuePairValue = keyValuePairValue;
    }

    public void createKeyValuePairs(Map<String, String> target, Collection<JIPipeTextAnnotation> annotations) {
        if(enabled) {
            JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
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
