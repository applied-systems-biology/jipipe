package org.hkijena.jipipe.extensions.forms.datatypes;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.parameters.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.nio.file.Path;
import java.util.Collections;

@JIPipeDocumentation(name = "Text input form", description = "A form element that allows the user to input text")
public class StringFormData extends ParameterFormData {

    private String value = "";
    private StringQueryExpression validationExpression = new StringQueryExpression();
    private OptionalAnnotationNameParameter outputAnnotation = new OptionalAnnotationNameParameter("Text", true);
    private OptionalAnnotationNameParameter inputAnnotation = new OptionalAnnotationNameParameter("", false);
    private JIPipeAnnotationMergeStrategy annotationMergeStrategy = JIPipeAnnotationMergeStrategy.OverwriteExisting;

    public StringFormData() {

    }

    public StringFormData(StringFormData other) {
        super(other);
        this.value = other.value;
        this.validationExpression = new StringQueryExpression(other.validationExpression);
        this.outputAnnotation = new OptionalAnnotationNameParameter(other.outputAnnotation);
        this.inputAnnotation = new OptionalAnnotationNameParameter(other.inputAnnotation);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
    }

    @JIPipeDocumentation(name = "Initial value", description = "The initial string value")
    @JIPipeParameter("initial-value")
    public String getValue() {
        return value;
    }

    @JIPipeParameter("initial-value")
    public void setValue(String value) {
        this.value = value;
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

    @JIPipeDocumentation(name = "Input annotation", description = "The annotation used to override the initial value. If the annotation does not exist, " +
            "the standard initial value is used.")
    @JIPipeParameter("input-annotation")
    public OptionalAnnotationNameParameter getInputAnnotation() {
        return inputAnnotation;
    }

    @JIPipeParameter("input-annotation")
    public void setInputAnnotation(OptionalAnnotationNameParameter inputAnnotation) {
        this.inputAnnotation = inputAnnotation;
    }

    @JIPipeDocumentation(name = "Merge output annotation", description = "Determines how the output annotation is merged with existing values.")
    @JIPipeParameter("merge-strategy")
    public JIPipeAnnotationMergeStrategy getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeAnnotationMergeStrategy annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
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
        if(!validationExpression.test(value)) {
            report.reportIsInvalid("Invalid value!",
                    String.format("The provided value '%s' does not comply to the test '%s'", value, validationExpression.getExpression()),
                    "Please correct your input",
                    this);
        }
    }

    @Override
    public String toString() {
        return String.format("Text form [name=%s, value=%s]", getName(), value);
    }

    @Override
    public void loadData(JIPipeMergingDataBatch dataBatch) {
        if(inputAnnotation.isEnabled()) {
            JIPipeAnnotation annotation = dataBatch.getAnnotations().getOrDefault(inputAnnotation.getContent(), null);
            if(annotation != null) {
                value = annotation.getValue();
            }
        }
    }

    @Override
    public void writeData(JIPipeMergingDataBatch dataBatch) {
        if(outputAnnotation.isEnabled()) {
            annotationMergeStrategy.mergeInto(dataBatch.getAnnotations(), Collections.singletonList(outputAnnotation.createAnnotation(value)));
        }
    }
}
