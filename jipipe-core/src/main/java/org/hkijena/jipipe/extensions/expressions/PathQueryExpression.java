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

import org.hkijena.jipipe.extensions.expressions.variables.PathFilterExpressionParameterVariablesInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * Expression for querying strings
 */
@JIPipeExpressionParameterSettings(variableSource = PathFilterExpressionParameterVariablesInfo.class)
public class PathQueryExpression extends JIPipeExpressionParameter implements Predicate<Path> {

    public PathQueryExpression() {
    }

    public PathQueryExpression(String expression) {
        super(expression);
    }

    public PathQueryExpression(AbstractExpressionParameter other) {
        super(other);
    }

    /**
     * Queries a string out of the list
     *
     * @param strings             existing annotations for the data
     * @param expressionVariables expression parameters
     * @return the annotation that matches the query or null if none matches
     */
    public Path queryFirst(Collection<Path> strings, ExpressionVariables expressionVariables) {
        for (Path string : strings) {
            PathFilterExpressionParameterVariablesInfo.buildFor(string, expressionVariables);
            if (test(expressionVariables))
                return string;
        }
        return null;
    }

    /**
     * Generates an annotation value
     *
     * @param strings             existing annotations for the data
     * @param expressionVariables expression parameters
     * @return the annotation that matches the query or null if none matches
     */
    public java.util.List<Path> queryAll(Collection<Path> strings, ExpressionVariables expressionVariables) {
        java.util.List<Path> result = new ArrayList<>();
        for (Path string : strings) {
            PathFilterExpressionParameterVariablesInfo.buildFor(string, expressionVariables);
            if (test(expressionVariables))
                result.add(string);
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
    public boolean test(Path string) {
        return test(string, new ExpressionVariables());
    }

    /**
     * Returns true if the query matches the string
     *
     * @param string the string
     * @return if the query matches
     */
    public boolean test(Path string, ExpressionVariables expressionVariables) {
        if ("true".equals(getExpression()) || getExpression().trim().isEmpty())
            return true;
        PathFilterExpressionParameterVariablesInfo.buildFor(string, expressionVariables);
        return test(expressionVariables);
    }

    /**
     * Returns true of one of the strings matches the query.
     *
     * @param strings             the strings
     * @param expressionVariables expression variables
     * @return if one string matches
     */
    public boolean testAnyOf(Collection<Path> strings, ExpressionVariables expressionVariables) {
        if ("true".equals(getExpression()) || getExpression().trim().isEmpty())
            return true;
        for (Path string : strings) {
            PathFilterExpressionParameterVariablesInfo.buildFor(string, expressionVariables);
            if (test(expressionVariables))
                return true;
        }
        return false;
    }
}
