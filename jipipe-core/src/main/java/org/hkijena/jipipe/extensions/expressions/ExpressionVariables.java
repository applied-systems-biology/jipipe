package org.hkijena.jipipe.extensions.expressions;

import java.util.HashMap;

/**
 * Object that carries variables for expressions
 */
public class ExpressionVariables extends HashMap<String, Object> {
    public ExpressionVariables() {
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

}
