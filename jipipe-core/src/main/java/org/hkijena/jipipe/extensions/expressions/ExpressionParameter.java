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

package org.hkijena.jipipe.extensions.expressions;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.util.Collection;
import java.util.Iterator;

/**
 * A parameter that contains an expression.
 * This allows users to set up filters etc. from within the UI.
 * Use {@link ExpressionParameterSettings} to control the behavior of this parameter.
 */
public abstract class ExpressionParameter {
    private String expression = "";

    public ExpressionParameter() {
    }

    public ExpressionParameter(String expression) {
        this.expression = expression;
    }

    public ExpressionParameter(ExpressionParameter other) {
        this.expression = other.expression;
    }

    @JsonGetter("expression")
    public String getExpression() {
        return expression;
    }

    @JsonSetter("expression")
    public void setExpression(String expression) {
        this.expression = expression;
    }

    /**
     * Returns the evaluator instance that should evaluate this expression type
     *
     * @return the evaluator
     */
    public abstract ExpressionEvaluator getEvaluator();

    /**
     * Runs the expression and returns the boolean result. If no boolean is returned, an error is thrown.
     *
     * @param variables the variables
     * @return the result
     */
    public boolean test(ExpressionVariables variables) {
        return getEvaluator().test(expression, variables);
    }

    /**
     * Returns true if the expression is empty
     *
     * @return true if the expression is empty
     */
    public boolean isEmpty() {
        return StringUtils.nullToEmpty(getExpression()).trim().length() == 0;
    }

    /**
     * Runs the expression and returns the numeric result. If no number is returned, an error is thrown.
     *
     * @param variables the variables
     * @return the result
     */
    public double evaluateToNumber(ExpressionVariables variables) {
        Object result = evaluate(variables);
        return ((Number) result).doubleValue();
    }

    /**
     * Runs the expression and returns the numeric result. If no number is returned, an error is thrown.
     *
     * @param variables the variables
     * @return the result
     */
    public int evaluateToInteger(ExpressionVariables variables) {
        Object result = evaluate(variables);
        return ((Number) result).intValue();
    }

    /**
     * Runs the expression and returns the numeric result. If no number is returned, an error is thrown.
     *
     * @param variables the variables
     * @return the result
     */
    public double evaluateToDouble(ExpressionVariables variables) {
        Object result = evaluate(variables);
        return ((Number) result).doubleValue();
    }

    /**
     * Runs the expression and returns the numeric result. If no number is returned, an error is thrown.
     *
     * @param variables the variables
     * @return the result
     */
    public float evaluateToFloat(ExpressionVariables variables) {
        Object result = evaluate(variables);
        return ((Number) result).floatValue();
    }

    /**
     * Runs the expression and returns the string result.
     *
     * @param variables the variables
     * @return the result
     */
    public String evaluateToString(ExpressionVariables variables) {
        Object result = evaluate(variables);
        return StringUtils.nullToEmpty(result);
    }

    /**
     * Runs the expression and returns the boolean result.
     *
     * @param variables the variables
     * @return the result
     */
    public boolean evaluateToBoolean(ExpressionVariables variables) {
        Object result = evaluate(variables);
        return (boolean) result;
    }

    /**
     * Runs the expression and returns a color result.
     * Can handle return value of type {@link Color}, hex string, named colors according to {@link ColorUtils}, Collection of RGB or RGBA values (0-255)
     *
     * @param variables the variables
     * @return the result
     */
    public Color evaluateToColor(ExpressionVariables variables) {
        Object o = evaluate(variables);
        if (o instanceof Color) {
            return (Color) o;
        } else if (o instanceof String) {
            return ColorUtils.parseColor((String) o);
        } else if (o instanceof Collection) {
            Collection<?> collection = (Collection<?>) o;
            Iterator<?> iterator = collection.iterator();
            int red = ((Number) iterator.next()).intValue();
            int green = ((Number) iterator.next()).intValue();
            int blue = ((Number) iterator.next()).intValue();
            int alpha = 255;
            if (iterator.hasNext()) {
                alpha = ((Number) iterator.next()).intValue();
            }
            return new Color(red, green, blue, alpha);
        } else {
            return null;
        }
    }

    /**
     * Runs the expression and returns the result.
     *
     * @param variables the variables
     * @return the result
     */
    public Object evaluate(ExpressionVariables variables) {
        return getEvaluator().evaluate(expression, variables);
    }

    @Override
    public String toString() {
        return "Expression: " + expression;
    }
}
