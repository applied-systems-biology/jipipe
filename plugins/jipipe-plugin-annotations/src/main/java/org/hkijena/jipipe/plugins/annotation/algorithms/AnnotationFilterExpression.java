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

package org.hkijena.jipipe.plugins.annotation.algorithms;

import org.hkijena.jipipe.api.AddJIPipeDocumentationDescription;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.plugins.expressions.AbstractExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;

import java.util.Collection;

@AddJIPipeDocumentationDescription(description = "The expression result will be converted to a string. All existing annotations are available " +
        "as variables that can be accessed directly, or if they contain special characters or spaces via the $ operator.")
@AddJIPipeExpressionParameterVariable(name = "Annotations map", description = "Map of all annotations (key to value)", key = "all.annotations")
@AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
@AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
public class AnnotationFilterExpression extends JIPipeExpressionParameter {

    public AnnotationFilterExpression() {
    }

    public AnnotationFilterExpression(String expression) {
        super(expression);
    }

    public AnnotationFilterExpression(AbstractExpressionParameter other) {
        super(other);
    }

    /**
     * Generates an annotation value
     *
     * @param annotations existing annotations for the data
     * @param variableSet existing variables
     * @return the annotation value
     */
    public String generateAnnotationValue(Collection<JIPipeTextAnnotation> annotations, JIPipeExpressionVariablesMap variableSet) {
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
     * @param variables   existing variables
     * @return the test results.
     */
    public boolean test(Collection<JIPipeTextAnnotation> annotations, String dataString, JIPipeExpressionVariablesMap variables) {
        for (JIPipeTextAnnotation annotation : annotations) {
            variables.set(annotation.getName(), annotation.getValue());
        }
        variables.set("all.annotations", JIPipeTextAnnotation.annotationListToMap(annotations, JIPipeTextAnnotationMergeMode.Merge));
        variables.set("data_string", dataString);
        return (boolean) evaluate(variables);
    }
}
