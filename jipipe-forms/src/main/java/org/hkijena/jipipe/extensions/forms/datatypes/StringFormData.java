package org.hkijena.jipipe.extensions.forms.datatypes;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.parameters.JIPipeManualParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.parameters.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;

import java.awt.*;
import java.nio.file.Path;

@JIPipeDocumentation(name = "Text input form", description = "A form element that allows the user to input text")
public class StringFormData extends ParameterFormData {

    private String initialValue = "";
    private StringQueryExpression validationExpression = new StringQueryExpression();
    private OptionalAnnotationNameParameter outputAnnotation = new OptionalAnnotationNameParameter("String", true);

    public StringFormData() {

    }

    public StringFormData(StringFormData other) {
        super(other);
        this.initialValue = other.initialValue;
        this.validationExpression = new StringQueryExpression(other.validationExpression);
        this.outputAnnotation = new OptionalAnnotationNameParameter(other.outputAnnotation);
    }

    @JIPipeDocumentation(name = "Initial value", description = "The initial string value")
    @JIPipeParameter("initial-value")
    public String getInitialValue() {
        return initialValue;
    }

    @JIPipeParameter("initial-value")
    public void setInitialValue(String initialValue) {
        this.initialValue = initialValue;
    }

    @JIPipeDocumentation(name = "Validation expression", description = "Expression that is used to validate the user input")
    @JIPipeParameter("validation-expression")
    public StringQueryExpression getValidationExpression() {
        return validationExpression;
    }

    @JIPipeParameter("validation-expression")
    public void setValidationExpression(StringQueryExpression validationExpression) {
        this.validationExpression = validationExpression;
    }

    @JIPipeDocumentation(name = "Output annotation", description = "Determines into which annotation the user input is written.")
    @JIPipeParameter("output-annotation")
    public OptionalAnnotationNameParameter getOutputAnnotation() {
        return outputAnnotation;
    }

    @JIPipeParameter("output-annotation")
    public void setOutputAnnotation(OptionalAnnotationNameParameter outputAnnotation) {
        this.outputAnnotation = outputAnnotation;
    }

    @Override
    public Component getEditor(JIPipeWorkbench workbench) {
       JIPipeParameterTree tree = new JIPipeParameterTree(this);
       return JIPipe.getParameterTypes().createEditorFor(workbench, tree.getParameters().get("initial-value"));
    }

    @Override
    public JIPipeData duplicate() {
        return new StringFormData(this);
    }

    public static StringFormData importFrom(Path rowStorage) {
        return FormData.importFrom(rowStorage, StringFormData.class);
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        if(!validationExpression.test(initialValue)) {
            report.reportIsInvalid("Invalid value!",
                    String.format("The provided value '%s' does not comply to the test '%s'", initialValue, validationExpression.getExpression()),
                    "Please correct your input",
                    this);
        }
    }

    @Override
    public String toString() {
        return String.format("Text form [name=%s, value=%s]", getName(), initialValue);
    }
}
