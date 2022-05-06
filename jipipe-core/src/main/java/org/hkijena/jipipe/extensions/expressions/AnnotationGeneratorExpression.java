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

import org.hkijena.jipipe.api.JIPipeDocumentationDescription;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;

import java.util.Collection;

/**
 * An expression that is used to generate annotations for data
 */
@ExpressionParameterSettings(variableSource = AnnotationGeneratorExpressionVariableSource.class)
@JIPipeDocumentationDescription(description = "The expression result will be converted to a string. All existing annotations are available " +
        "as variables that can be accessed directly, or if they contain special characters or spaces via the $ operator.")
public class AnnotationGeneratorExpression extends DefaultExpressionParameter {

    public AnnotationGeneratorExpression() {
    }

    public AnnotationGeneratorExpression(String expression) {
        super(expression);
    }

    public AnnotationGeneratorExpression(ExpressionParameter other) {
        super(other);
    }

    /**
     * Generates an annotation value
     *
     * @param annotations existing annotations for the data
     * @param variableSet existing variables
     * @return the annotation value
     */
    public String generateAnnotationValue(Collection<JIPipeTextAnnotation> annotations, ExpressionVariables variableSet) {
        for (JIPipeTextAnnotation annotation : annotations) {
            if (!variableSet.containsKey(annotation.getName()))
                variableSet.set(annotation.getName(), annotation.getValue());
        }
        return "" + evaluate(variableSet);
    }

    /**
     * Evaluates the expression as boolean
     *
     * @param annotations existing annotations for the data
     * @param dataString  the data as string
     * @return the test results.
     */
    public boolean test(Collection<JIPipeTextAnnotation> annotations, String dataString) {
        ExpressionVariables variableSet = new ExpressionVariables();
        for (JIPipeTextAnnotation annotation : annotations) {
            variableSet.set(annotation.getName(), annotation.getValue());
        }
        variableSet.set("data_string", dataString);
        return (boolean) evaluate(variableSet);
    }
}
