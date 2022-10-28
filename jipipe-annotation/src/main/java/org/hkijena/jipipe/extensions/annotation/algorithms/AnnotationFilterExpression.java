package org.hkijena.jipipe.extensions.annotation.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentationDescription;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;

import java.util.Collection;

@JIPipeDocumentationDescription(description = "The expression result will be converted to a string. All existing annotations are available " +
        "as variables that can be accessed directly, or if they contain special characters or spaces via the $ operator.")
@ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
@ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
@ExpressionParameterSettingsVariable(name = "Annotations map", description = "Map of all annotations (key to value)", key = "all.annotations")
@ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
public class AnnotationFilterExpression extends DefaultExpressionParameter {

    public AnnotationFilterExpression() {
    }

    public AnnotationFilterExpression(String expression) {
        super(expression);
    }

    public AnnotationFilterExpression(ExpressionParameter other) {
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
     * @param variables   existing variables
     * @return the test results.
     */
    public boolean test(Collection<JIPipeTextAnnotation> annotations, String dataString, ExpressionVariables variables) {
        for (JIPipeTextAnnotation annotation : annotations) {
            variables.set(annotation.getName(), annotation.getValue());
        }
        variables.set("all.annotations", JIPipeTextAnnotation.annotationListToMap(annotations, JIPipeTextAnnotationMergeMode.Merge));
        variables.set("data_string", dataString);
        return (boolean) evaluate(variables);
    }
}
