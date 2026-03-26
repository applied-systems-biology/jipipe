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

package org.hkijena.jipipe.plugins.expressions;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;

/**
 * A parameter collection that allows users to specify multiple annotations/variables
 * that will be joined with a custom delimiter in an expression.
 * <p>
 * Example:
 * - Variables: ["A", "B", "Hello world"]
 * - Delimiter: "_"
 * - Output expression: A + "_" + B + "_" + $"Hello world"
 */
public class JoinedVariablesEntry extends AbstractJIPipeParameterCollection {

    private StringList variables = new StringList();
    private String delimiter = "_";

    /**
     * Creates a new empty instance
     */
    public JoinedVariablesEntry() {
        variables.add("ChangeMe");
    }

    /**
     * Copies the object
     *
     * @param other the original
     */
    public JoinedVariablesEntry(JoinedVariablesEntry other) {
        this.variables = new StringList(other.variables);
        this.delimiter = other.delimiter;
    }

    @SetJIPipeDocumentation(name = "Annotations/Variables", description = "List of annotation or variable names to be joined. " +
            "Names with special characters (spaces, parentheses) will be automatically escaped in the expression.")
    @JIPipeParameter("variables")
    @JsonGetter("variables")
    public StringList getVariables() {
        return variables;
    }

    @JIPipeParameter("variables")
    @JsonSetter("variables")
    public void setVariables(StringList variables) {
        this.variables = variables;
    }

    @SetJIPipeDocumentation(name = "Delimiter", description = "The delimiter string used to join the variables in the output expression.")
    @JIPipeParameter("delimiter")
    @JsonGetter("delimiter")
    public String getDelimiter() {
        return delimiter;
    }

    @JIPipeParameter("delimiter")
    @JsonSetter("delimiter")
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * Generates the expression string for this joined variables entry.
     * <p>
     * For example, with variables ["A", "B", "Hello world"] and delimiter "_",
     * the output will be: A + "_" + B + "_" + $"Hello world"
     *
     * @return the generated expression string
     */
    public String toExpression() {
        if (variables.isEmpty()) {
            return "\"\"";
        }

        StringBuilder sb = new StringBuilder();
        String escapedDelimiter = JIPipeExpressionEvaluator.escapeString(delimiter);

        for (int i = 0; i < variables.size(); i++) {
            String variable = variables.get(i);
            String escapedVariable = JIPipeExpressionEvaluator.escapeVariable(variable);

            if (i > 0) {
                sb.append(" + \"").append(escapedDelimiter).append("\" + ");
            }

            sb.append(escapedVariable);
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        if (variables.isEmpty()) {
            return "(empty)";
        }
        return String.join(", ", variables) + " (delimiter: \"" + delimiter + "\")";
    }
}
