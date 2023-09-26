/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.parameters.library.primitives.ranges;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.JIPipeDocumentationDescription;
import org.hkijena.jipipe.api.parameters.JIPipeCustomTextDescriptionParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;

/**
 * Parameter that contains an integer range as string.
 * The format is the following: [range];[range];... with range being an integer or [from]-[to]
 * where from and to are inclusive. Returns the list of integers defined by the string. Empty ranges are ignored.
 * Spaces are ignored. Negative values must be enclosed in brackets
 */
@JIPipeDocumentationDescription(description = "The format is the following: [range];[range];... with [range] either being a single integer or a range [from]-[to] (both inclusive). " +
        "Negative values must be enclosed in parentheses. Example: 0-5;1;(-1)-10. If you want more customization options use the expression mode and functions such as MAKE_SEQUENCE.")
public class IntegerRange implements JIPipeCustomTextDescriptionParameter {

    private String value;
    private boolean useExpression = false;
    private DefaultExpressionParameter expression = new DefaultExpressionParameter("MAKE_SEQUENCE(0, 10)");

    /**
     * Creates a new instance with a null value
     */
    public IntegerRange() {
    }

    /**
     * Creates a new instance and initializes it
     *
     * @param value the value
     */
    public IntegerRange(String value) {
        this.value = value;
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public IntegerRange(IntegerRange other) {
        this.value = other.value;
        this.useExpression = other.useExpression;
        this.expression = new DefaultExpressionParameter(other.expression);
    }

    /**
     * Converts a range string of format [range];[range];... to a list of integers
     *
     * @param value the range string
     * @return the list of integers
     */
    public static List<Integer> getIntegersFromRangeString(String value) {
        String string = StringUtils.orElse(value, "").replace(" ", "");
        List<Integer> integers = new ArrayList<>();
        string = string.replace(',', ';');
        for (String range : string.split(";")) {
            if (StringUtils.isNullOrEmpty(range))
                continue;
            if (range.contains("-")) {
                StringBuilder fromBuilder = new StringBuilder();
                StringBuilder toBuilder = new StringBuilder();
                boolean negative = false;
                boolean writeToFrom = true;
                for (int i = 0; i < range.length(); i++) {
                    char c = range.charAt(i);
                    if (c == '(') {
                        if (negative)
                            throw new NumberFormatException("Cannot nest brackets!");
                        negative = true;
                    } else if (c == ')') {
                        if (!negative)
                            throw new NumberFormatException("Cannot end missing start bracket!");
                        negative = false;
                    } else if (c == '-') {
                        if (negative) {
                            if (writeToFrom)
                                fromBuilder.append(c);
                            else
                                toBuilder.append(c);
                        } else {
                            if (!writeToFrom)
                                throw new RuntimeException("Additional hyphen detected!");
                            writeToFrom = false;
                        }
                    } else {
                        if (writeToFrom)
                            fromBuilder.append(c);
                        else
                            toBuilder.append(c);
                    }
                }

                // Parse borders
                int from = Integer.parseInt(fromBuilder.toString());
                int to = Integer.parseInt(toBuilder.toString());

                if (from <= to) {
                    for (int i = from; i <= to; ++i) {
                        integers.add(i);
                    }
                } else {
                    for (int i = to; i >= to; --i) {
                        integers.add(i);
                    }
                }
            } else {
                integers.add(Integer.parseInt(range));
            }
        }
        return integers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntegerRange that = (IntegerRange) o;
        return useExpression == that.useExpression && Objects.equals(value, that.value) && Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, useExpression, expression);
    }

    @JsonGetter("is-expression")
    public boolean isUseExpression() {
        return useExpression;
    }

    @JsonSetter("is-expression")
    public void setUseExpression(boolean useExpression) {
        this.useExpression = useExpression;
    }

    @JsonGetter("expression")
    public DefaultExpressionParameter getExpression() {
        return expression;
    }

    @JsonSetter("expression")
    public void setExpression(DefaultExpressionParameter expression) {
        this.expression = expression;
    }

    @JsonGetter("value")
    public String getValue() {
        return value;
    }

    @JsonSetter("value")
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Generates the list of integers based on the value. Throws no exceptions.
     *
     * @param min       the min value the integers can have
     * @param max       the max value the integers can have
     * @param variables the variables for the expression-based variant
     * @return null if the format is wrong
     */
    public List<Integer> tryGetIntegers(int min, int max, ExpressionVariables variables) {
        try {
            return getIntegers(min, max, variables);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Generates the list of integers based on the value
     *
     * @param min       the min value the integers can have
     * @param max       the max value the integers can have
     * @param variables the variables for the expression-based variant
     * @return the generated integers
     * @throws NumberFormatException if the format is wrong
     */
    public List<Integer> getIntegers(int min, int max, ExpressionVariables variables) throws NumberFormatException {
        if (isUseExpression()) {
            variables.set("min", min);
            variables.set("max", max);
            Object result = expression.evaluate(variables);
            List<Integer> integers = new ArrayList<>();
            if (result instanceof Number) {
                integers.add(((Number) result).intValue());
            } else if (result instanceof String) {
                try {
                    integers.add((int) Double.parseDouble("" + result));
                } catch (Exception e) {
                    return getIntegersFromRangeString("" + result);
                }
            } else if (result instanceof Collection) {
                for (Object o : (Collection<?>) result) {
                    if (o instanceof Number) {
                        integers.add(((Number) o).intValue());
                    } else if (o instanceof String) {
                        try {
                            integers.add((int) Double.parseDouble("" + o));
                        } catch (Exception e) {
                            integers.addAll(getIntegersFromRangeString("" + o));
                        }
                    } else {
                        throw new UnsupportedOperationException("Invalid expression output for integer range: " + o);
                    }
                }
            } else {
                throw new UnsupportedOperationException("Invalid expression output for integer range: " + result);
            }
            return integers;
        } else {
            return getIntegersFromRangeString(value);
        }
    }

    @Override
    public String toString() {
        return StringUtils.orElse(value, "[Empty]");
    }

    @Override
    public String getTextDescription() {
        return useExpression ? expression.getTextDescription() : value;
    }

    public static class VariableSource implements ExpressionParameterVariableSource {
        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            Set<ExpressionParameterVariable> variables = new HashSet<>();
            variables.add(new ExpressionParameterVariable("Minimum value", "The minimum value the range of the range. Can be any value if not suitable for the parameter.", "min"));
            variables.add(new ExpressionParameterVariable("Maximum value", "The minimum value the range of the range. Can be any value if not suitable for the parameter.", "max"));
            return variables;
        }
    }
}
