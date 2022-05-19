package org.hkijena.jipipe.extensions.expressions;

import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;

import java.util.HashMap;
import java.util.Map;

/**
 * Object that carries variables for expressions
 */
public class ExpressionVariables extends HashMap<String, Object> {
    public ExpressionVariables() {
    }

    public ExpressionVariables(ExpressionVariables other) {
        putAll(other);
    }

    /**
     * Sets a variable value.
     *
     * @param variableName The variable name
     * @param value        The variable value (null to remove a variable from the set).
     */
    public void set(String variableName, Object value) {
        this.put(variableName, value);
    }

    public void putAnnotations(Map<String, JIPipeTextAnnotation> mergedTextAnnotations) {
        for (Entry<String, JIPipeTextAnnotation> entry : mergedTextAnnotations.entrySet()) {
            put(entry.getKey(), entry.getValue().getValue());
        }
    }
}
