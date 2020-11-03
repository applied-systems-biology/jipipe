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

package org.hkijena.jipipe.extensions.parameters.expressions;

import com.fathzer.soft.javaluator.StaticVariableSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Expression for querying strings
 */
@ExpressionParameterSettings(variableSource = StringQueryExpressionVariableSource.class)
public class StringQueryExpression extends DefaultExpressionParameter {

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
     * Queries a string out of the list
     * @param strings existing annotations for the data
     * @return the annotation that matches the query or null if none matches
     */
    public String queryFirst(Collection<String> strings) {
        if(strings.isEmpty())
            return null;
        StaticVariableSet<Object> variableSet = new StaticVariableSet<>();
        try {
            Object evaluationResult = evaluate(variableSet);
            if(evaluationResult instanceof String) {
                String key = (String) evaluationResult;
                if(strings.contains(key))
                    return key;
            }
        }
        catch (Exception e) {
        }
        for (String string : strings) {
            variableSet.set("value", string);
            boolean evaluationResult = test(variableSet);
            if(evaluationResult)
                return string;
        }
        return null;
    }

    /**
     * Generates an annotation value
     * @param strings existing annotations for the data
     * @return the annotation that matches the query or null if none matches
     */
    public List<String> queryAll(Collection<String> strings) {
        List<String> result = new ArrayList<>();
        StaticVariableSet<Object> variableSet = new StaticVariableSet<>();
        try {
            Object evaluationResult = evaluate(variableSet);
            if(evaluationResult instanceof String) {
                String key = (String) evaluationResult;
                if(strings.contains(key))
                    result.add(key);
            }
        }
        catch (Exception e) {
        }
        if(!result.isEmpty())
            return result;
        for (String string : strings) {
            variableSet.set("value", string);
            boolean evaluationResult = test(variableSet);
            if(evaluationResult)
                result.add(string);
        }
        return result;
    }

    /**
     * Returns true if the query matches the string
     * @param string the string
     * @return if the query matches
     */
    public boolean test(String string) {
        if("TRUE".equals(getExpression()))
            return true;
        StaticVariableSet<Object> variableSet = new StaticVariableSet<>();
        try {
            Object evaluationResult = evaluate(variableSet);
            if(evaluationResult instanceof String) {
                String key = (String) evaluationResult;
                if(Objects.equals(key, string))
                    return true;
            }
        }
        catch (Exception e) {
        }
        variableSet.set("value", string);
        return test(variableSet);
    }

    /**
     * Returns true of one of the strings matches the query.
     * @param strings the strings
     * @return if one string matches
     */
    public boolean test(Collection<String> strings) {
        if("TRUE".equals(getExpression()))
            return true;
        StaticVariableSet<Object> variableSet = new StaticVariableSet<>();
        for (String string : strings) {
            try {
                Object evaluationResult = evaluate(variableSet);
                if(evaluationResult instanceof String) {
                    String key = (String) evaluationResult;
                    if(Objects.equals(key, string))
                        return true;
                }
            }
            catch (Exception e) {
            }
            variableSet.set("value", string);
            if(test(variableSet))
                return true;
        }
        return false;
    }
}
