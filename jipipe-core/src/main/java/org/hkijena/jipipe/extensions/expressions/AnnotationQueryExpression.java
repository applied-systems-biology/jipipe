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

import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * An expression that is used to query annotations
 */
@ExpressionParameterSettings(variableSource = AnnotationQueryExpressionVariableSource.class)
public class AnnotationQueryExpression extends DefaultExpressionParameter {

    public static final String DOCUMENTATION_DESCRIPTION = "This parameter is an expression that has two modes: " +
            "(1) Selecting an existing annotation by its name, and (2) Matching an existing annotation by boolean operators<br/>" +
            "<ol><li>Type in the name of the existing annotation. Put the name in double quotes (optional, but recommended). Example: <pre>\"#Dataset\"</pre></li>" +
            "<li>The function iterates through all annotations. It should return TRUE for one of them. You will have 'key' and 'value' available within the expression. Example: <pre>key == \"#Dataset\"</pre></li></ol>";

    public AnnotationQueryExpression() {
    }

    public AnnotationQueryExpression(String expression) {
        super(expression);
    }

    public AnnotationQueryExpression(ExpressionParameter other) {
        super(other);
    }

    /**
     * Generates an annotation value
     *
     * @param annotations existing annotations for the data
     * @return the annotation that matches the query or null if none matches
     */
    public JIPipeTextAnnotation queryFirst(Collection<JIPipeTextAnnotation> annotations) {
        ExpressionVariables variableSet = new ExpressionVariables();
        try {
            Object evaluationResult = evaluate(variableSet);
            if (evaluationResult instanceof String) {
                String key = (String) evaluationResult;
                for (JIPipeTextAnnotation annotation : annotations) {
                    if (Objects.equals(annotation.getName(), key))
                        return annotation;
                }
            }
        } catch (Exception e) {
        }
        try {
            for (JIPipeTextAnnotation annotation : annotations) {
                variableSet.set("key", annotation.getName());
                variableSet.set("value", annotation.getValue());
                boolean evaluationResult = test(variableSet);
                if (evaluationResult)
                    return annotation;
            }
        } catch (Exception e) {
        }
        for (JIPipeTextAnnotation annotation : annotations) {
            if (Objects.equals(annotation.getName(), getExpression()))
                return annotation;
        }
        return null;
    }

    /**
     * Returns all annotations that match the query
     *
     * @param annotations existing annotations for the data
     * @return the annotation that matches the query or null if none matches
     */
    public java.util.List<JIPipeTextAnnotation> queryAll(Collection<JIPipeTextAnnotation> annotations) {
        java.util.List<JIPipeTextAnnotation> result = new ArrayList<>();
        ExpressionVariables variableSet = new ExpressionVariables();
        try {
            Object evaluationResult = evaluate(variableSet);
            if (evaluationResult instanceof String) {
                String key = (String) evaluationResult;
                for (JIPipeTextAnnotation annotation : annotations) {
                    if (Objects.equals(annotation.getName(), key)) {
                        result.add(annotation);
                    }
                }
            }
        } catch (Exception e) {
        }
        if (!result.isEmpty())
            return result;
        for (JIPipeTextAnnotation annotation : annotations) {
            variableSet.set("key", annotation.getName());
            variableSet.set("value", annotation.getValue());
            boolean evaluationResult = test(variableSet);
            if (evaluationResult)
                result.add(annotation);
        }
        return result;
    }
}
