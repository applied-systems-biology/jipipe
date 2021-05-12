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

import org.hkijena.jipipe.extensions.expressions.variables.PathFilterExpressionParameterVariableSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * Expression for querying strings
 */
@ExpressionParameterSettings(variableSource = PathFilterExpressionParameterVariableSource.class)
public class PathQueryExpression extends DefaultExpressionParameter implements Predicate<Path> {

    public PathQueryExpression() {
    }

    public PathQueryExpression(String expression) {
        super(expression);
    }

    public PathQueryExpression(ExpressionParameter other) {
        super(other);
    }

    /**
     * Queries a string out of the list
     *
     * @param strings              existing annotations for the data
     * @param expressionParameters expression parameters
     * @return the annotation that matches the query or null if none matches
     */
    public Path queryFirst(Collection<Path> strings, ExpressionParameters expressionParameters) {
        for (Path string : strings) {
            PathFilterExpressionParameterVariableSource.buildFor(string, expressionParameters);
            if (test(expressionParameters))
                return string;
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
    public java.util.List<Path> queryAll(Collection<Path> strings, ExpressionParameters expressionParameters) {
        java.util.List<Path> result = new ArrayList<>();
        for (Path string : strings) {
            PathFilterExpressionParameterVariableSource.buildFor(string, expressionParameters);
            if (test(expressionParameters))
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
        return test(string, new ExpressionParameters());
    }

    /**
     * Returns true if the query matches the string
     *
     * @param string the string
     * @return if the query matches
     */
    public boolean test(Path string, ExpressionParameters expressionParameters) {
        if ("true".equals(getExpression()) || getExpression().trim().isEmpty())
            return true;
        PathFilterExpressionParameterVariableSource.buildFor(string, expressionParameters);
        return test(expressionParameters);
    }

    /**
     * Returns true of one of the strings matches the query.
     *
     * @param strings              the strings
     * @param expressionParameters
     * @return if one string matches
     */
    public boolean testAnyOf(Collection<Path> strings, ExpressionParameters expressionParameters) {
        if ("true".equals(getExpression()) || getExpression().trim().isEmpty())
            return true;
        for (Path string : strings) {
            PathFilterExpressionParameterVariableSource.buildFor(string, expressionParameters);
            if (test(expressionParameters))
                return true;
        }
        return false;
    }
}
