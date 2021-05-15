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

import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Expression for querying strings
 */
@ExpressionParameterSettings(variableSource = StringQueryExpressionVariableSource.class)
public class StringQueryExpression extends DefaultExpressionParameter implements Predicate<String> {

    public static final String DOCUMENTATION_DESCRIPTION = "This parameter is an expression that has two modes: " +
            "(1) Selecting an existing string, and (2) Matching an existing strings by boolean operators<br/>" +
            "<ol><li>Type in the string in double quotes. Example: <pre>\"hello world\"</pre></li>" +
            "<li>The function iterates through all strings. It should return TRUE for one of them. You will have a variable 'value' available within the expression. Example: <pre>value CONTAINS \"hello\"</pre></li></ol>";

    public StringQueryExpression() {
    }

    public StringQueryExpression(String expression) {
        super(expression);
    }

    public StringQueryExpression(ExpressionParameter other) {
        super(other);
    }

    /**
     * Attempts to run the expression. If this fails, the expression itself is returned.
     *
     * @param expressionParameters expression parameters
     * @return expression result or the expression itself
     */
    public String generate(ExpressionParameters expressionParameters) {
        try {
            Object evaluationResult = evaluate(expressionParameters);
            return StringUtils.nullToEmpty(evaluationResult);
        } catch (Exception e) {
        }
        return getExpression();
    }

    /**
     * Queries a string out of the list
     *
     * @param strings              existing annotations for the data
     * @param expressionParameters expression parameters
     * @return the annotation that matches the query or null if none matches
     */
    public String queryFirst(Collection<String> strings, ExpressionParameters expressionParameters) {
        try {
            Object evaluationResult = evaluate(expressionParameters);
            if (evaluationResult instanceof String) {
                String key = (String) evaluationResult;
                if (strings.contains(key))
                    return key;
            }
        } catch (Exception e) {
        }
        for (String string : strings) {
            try {
                expressionParameters.set("value", string);
                Object evaluationResult = evaluate(expressionParameters);
                if (evaluationResult instanceof Boolean && (boolean) evaluationResult)
                    return string;
            } catch (Exception e) {
                if (Objects.equals(string, getExpression()))
                    return string;
            }
        }
        return null;
    }

    /**
     * Generates an annotation value
     *
     * @param strings              existing annotations for the data
     * @param expressionParameters expression parameters
     * @return the annotation that matches the query or null if none matches
     */
    public java.util.List<String> queryAll(Collection<String> strings, ExpressionParameters expressionParameters) {
        java.util.List<String> result = new ArrayList<>();
        try {
            Object evaluationResult = evaluate(expressionParameters);
            if (evaluationResult instanceof String) {
                String key = (String) evaluationResult;
                if (strings.contains(key))
                    result.add(key);
            }
        } catch (Exception e) {
        }
        if (!result.isEmpty())
            return result;
        for (String string : strings) {
            try {
                expressionParameters.set("value", string);
                Object evaluationResult = evaluate(expressionParameters);
                if (evaluationResult instanceof Boolean && (boolean) evaluationResult)
                    result.add(string);
            } catch (Exception e) {
                if (Objects.equals(string, getExpression()))
                    result.add(string);
            }
        }
        return result;
    }

    /**
     * Returns true if the query matches the string
     *
     * @param string the string
     * @return if the query matches
     */
    @Override
    public boolean test(String string) {
        return test(string, new ExpressionParameters());
    }

    /**
     * Returns true if the query matches the string
     *
     * @param string the string
     * @return if the query matches
     */
    public boolean test(String string, ExpressionParameters expressionParameters) {
        if ("true".equals(getExpression()) || getExpression().trim().isEmpty())
            return true;
        expressionParameters.set("value", string);
        try {
            Object evaluationResult = evaluate(expressionParameters);
            if (evaluationResult instanceof String) {
                String key = (String) evaluationResult;
                if (Objects.equals(key, string))
                    return true;
            } else if (evaluationResult instanceof Boolean && (boolean) evaluationResult) {
                return true;
            }
        } catch (Exception e) {
        }
        return Objects.equals(getExpression(), string);
    }

    /**
     * Returns true of one of the strings matches the query.
     *
     * @param strings the strings
     * @return if one string matches
     */
    public boolean testAnyOf(Collection<String> strings) {
        if ("true".equals(getExpression()) || getExpression().trim().isEmpty())
            return true;
        ExpressionParameters variableSet = new ExpressionParameters();
        for (String string : strings) {
            try {
                variableSet.set("value", string);
                Object evaluationResult = evaluate(variableSet);
                if (evaluationResult instanceof String) {
                    String key = (String) evaluationResult;
                    if (Objects.equals(key, string))
                        return true;
                } else if (evaluationResult instanceof Boolean && (boolean) evaluationResult) {
                    return true;
                }
            } catch (Exception e) {
            }
            if (test(variableSet))
                return true;
        }
        return strings.contains(getExpression());
    }
}