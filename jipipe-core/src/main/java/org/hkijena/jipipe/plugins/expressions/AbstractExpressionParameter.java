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
import org.hkijena.jipipe.api.parameters.JIPipeCustomTextDescriptionParameter;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * A parameter that contains an expression.
 * This allows users to set up filters etc. from within the UI.
 * Use {@link JIPipeExpressionParameterSettings} to control the behavior of this parameter.
 */
public abstract class AbstractExpressionParameter implements JIPipeCustomTextDescriptionParameter {
    private String expression = "";

    public AbstractExpressionParameter() {
    }

    public AbstractExpressionParameter(String expression) {
        this.expression = expression;
    }

    public AbstractExpressionParameter(AbstractExpressionParameter other) {
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
    public boolean test(JIPipeExpressionVariablesMap variables) {
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
    public double evaluateToNumber(JIPipeExpressionVariablesMap variables) {
        return evaluateToDouble(variables);
    }

    /**
     * Runs the expression and returns the numeric result. If no number is returned, an error is thrown.
     *
     * @param variables the variables
     * @return the result
     */
    public int evaluateToInteger(JIPipeExpressionVariablesMap variables) {
        return (int) evaluateToDouble(variables);
    }

    /**
     * Runs the expression and returns the numeric result. If no number is returned, return the default value.
     *
     * @param variables    the variables
     * @param defaultValue the default value
     * @return the result
     */
    public int evaluateToIntegerSafe(JIPipeExpressionVariablesMap variables, int defaultValue) {
        try {
            return evaluateToInteger(variables);
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    /**
     * Runs the expression and returns the numeric result. If no number is returned, an error is thrown.
     *
     * @param variables the variables
     * @return the result
     */
    public double evaluateToDouble(JIPipeExpressionVariablesMap variables) {
        Object result = evaluate(variables);
        if (result instanceof Number) {
            return ((Number) result).doubleValue();
        } else {
            return StringUtils.parseDouble(StringUtils.nullToEmpty(result));
        }
    }

    /**
     * Runs the expression and returns the numeric result. If no number is returned, an error is thrown.
     *
     * @param variables the variables
     * @return the result
     */
    public List<Double> evaluateToDoubleList(JIPipeExpressionVariablesMap variables) {
        Object result = evaluate(variables);
        if (result instanceof Number) {
            return Collections.singletonList(((Number) result).doubleValue());
        } else if (result instanceof Collection) {
            List<Double> list = new ArrayList<>();
            for (Object o : (Collection) result) {
                if (o instanceof Number) {
                    list.add(((Number) o).doubleValue());
                } else {
                    list.add(StringUtils.parseDouble(StringUtils.nullToEmpty(o)));
                }
            }
            return list;
        } else {
            return Collections.singletonList(StringUtils.parseDouble(StringUtils.nullToEmpty(result)));
        }
    }

    /**
     * Runs the expression and returns the numeric result. If no number is returned, an error is thrown.
     *
     * @param variables the variables
     * @return the result
     */
    public float evaluateToFloat(JIPipeExpressionVariablesMap variables) {
        Object result = evaluate(variables);
        if (result instanceof Number) {
            return ((Number) result).floatValue();
        } else {
            return StringUtils.parseFloat(StringUtils.nullToEmpty(result));
        }
    }

    /**
     * Runs the expression and returns the string result.
     *
     * @param variables the variables
     * @return the result
     */
    public String evaluateToString(JIPipeExpressionVariablesMap variables) {
        Object result = evaluate(variables);
        return StringUtils.nullToEmpty(result);
    }

    /**
     * Runs the expression and returns the boolean result.
     *
     * @param variables the variables
     * @return the result
     */
    public boolean evaluateToBoolean(JIPipeExpressionVariablesMap variables) {
        Object result = evaluate(variables);
        return (boolean) result;
    }

    /**
     * Runs the expression and returns a color result.
     * Can handle return value of type {@link Color}, hex string, named colors according to {@link ColorUtils}, Collection of RGB or RGBA values (0-255)
     * Can also handle numeric results that are converted to int, generating a greyscale color (0-255)
     *
     * @param variables the variables
     * @return the result
     */
    public Color evaluateToColor(JIPipeExpressionVariablesMap variables) {
        Object o = evaluate(variables);
        if (o instanceof Number) {
            int rgb = ((Number) o).intValue();
            return new Color(rgb, rgb, rgb);
        } else if (o instanceof Color) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractExpressionParameter that = (AbstractExpressionParameter) o;
        return Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression);
    }

    /**
     * Runs the expression and returns the result.
     *
     * @param variables the variables
     * @return the result
     */
    public Object evaluate(JIPipeExpressionVariablesMap variables) {
        return getEvaluator().evaluate(expression, variables);
    }

    @Override
    public String toString() {
        return "Expression: " + expression;
    }

    @Override
    public String getTextDescription() {
        return expression;
    }
}
