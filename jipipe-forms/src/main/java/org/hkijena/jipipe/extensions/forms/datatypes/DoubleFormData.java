package org.hkijena.jipipe.extensions.forms.datatypes;

import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDefaultDocumentation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.parameters.JIPipeReflectionParameterAccess;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.forms.utils.SingleAnnotationIOSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.awt.*;
import java.util.Collections;

@JIPipeDocumentation(name = "Number input form", description = "A form element that allows the user to input a real number")
public class DoubleFormData extends ParameterFormData {

    private double value = 0;
    private JIPipeExpressionParameter validationExpression = new JIPipeExpressionParameter();
    private SingleAnnotationIOSettings annotationIOSettings = new SingleAnnotationIOSettings();

    public DoubleFormData() {
    }

    public DoubleFormData(DoubleFormData other) {
        super(other);
        this.value = other.value;
        this.validationExpression = new StringQueryExpression(other.validationExpression.getExpression());
        this.annotationIOSettings = new SingleAnnotationIOSettings(other.annotationIOSettings);
    }

    public static DoubleFormData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return FormData.importData(storage, DoubleFormData.class, progressInfo);
    }

    @JIPipeDocumentation(name = "Initial value", description = "The initial value")
    @JIPipeParameter("initial-value")
    public double getValue() {
        return value;
    }

    @JIPipeParameter("initial-value")
    public void setValue(double value) {
        this.value = value;
    }

    @JIPipeDocumentation(name = "Validation expression", description = "Expression that is used to validate the user input. There is a variable 'value' available that tests " +
            "the current number.")
    @JIPipeParameter("validation-expression")
    @JIPipeExpressionParameterSettings(variableSource = NumberQueryExpressionVariablesInfo.class)
    public JIPipeExpressionParameter getValidationExpression() {
        return validationExpression;
    }

    @JIPipeParameter("validation-expression")
    public void setValidationExpression(JIPipeExpressionParameter validationExpression) {
        this.validationExpression = validationExpression;
    }

    @JIPipeDocumentation(name = "Form element I/O", description = "Use following settings to determine how to extract initial values " +
            "from annotations and where to store the user-defined value.")
    @JIPipeParameter("form:io")
    public SingleAnnotationIOSettings getAnnotationIOSettings() {
        return annotationIOSettings;
    }

    @Override
    public Component getEditor(JIPipeWorkbench workbench) {
        JIPipeParameterTree tree = new JIPipeParameterTree(this);
        JIPipeReflectionParameterAccess access = (JIPipeReflectionParameterAccess) tree.getParameters().get("initial-value");
        access.setDocumentation(new JIPipeDefaultDocumentation(getName(), getDescription().getBody()));
        return JIPipe.getParameterTypes().createEditorFor(workbench, new JIPipeParameterTree(access), access);
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new DoubleFormData(this);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        ExpressionVariables variableSet = new ExpressionVariables();
        variableSet.set("value", value);
        if (!validationExpression.test(variableSet)) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, context, "Invalid value!",
                    String.format("The provided value '%s' does not comply to the test '%s'", value, validationExpression.getExpression()),
                    "Please correct your input"));
        }
    }

    @Override
    public String toString() {
        return String.format("Number form [name=%s, value=%s]", getName(), value);
    }

    @Override
    public void loadData(JIPipeMultiIterationStep iterationStep) {
        if (annotationIOSettings.getInputAnnotation().isEnabled()) {
            JIPipeTextAnnotation annotation =
                    iterationStep.getMergedTextAnnotations().getOrDefault(annotationIOSettings.getInputAnnotation().getContent(),
                            null);
            if (annotation != null) {
                if (NumberUtils.isCreatable(annotation.getValue())) {
                    value = NumberUtils.createDouble(annotation.getValue());
                }
            }
        }
    }

    @Override
    public void writeData(JIPipeMultiIterationStep iterationStep) {
        if (annotationIOSettings.getOutputAnnotation().isEnabled()) {
            annotationIOSettings.getAnnotationMergeStrategy().mergeInto(iterationStep.getMergedTextAnnotations(),
                    Collections.singletonList(annotationIOSettings.getOutputAnnotation().createAnnotation("" + value)));
        }
    }
}
