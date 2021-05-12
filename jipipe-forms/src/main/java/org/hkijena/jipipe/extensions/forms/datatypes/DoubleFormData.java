package org.hkijena.jipipe.extensions.forms.datatypes;

import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDefaultDocumentation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.parameters.JIPipeReflectionParameterAccess;
import org.hkijena.jipipe.extensions.forms.utils.SingleAnnotationIOSettings;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.awt.*;
import java.nio.file.Path;
import java.util.Collections;

@JIPipeDocumentation(name = "Number input form", description = "A form element that allows the user to input a real number")
public class DoubleFormData extends ParameterFormData {

    private double value = 0;
    private DefaultExpressionParameter validationExpression = new DefaultExpressionParameter();
    private SingleAnnotationIOSettings annotationIOSettings = new SingleAnnotationIOSettings();

    public DoubleFormData() {
        annotationIOSettings.getEventBus().register(this);
    }

    public DoubleFormData(DoubleFormData other) {
        super(other);
        this.value = other.value;
        this.validationExpression = new StringQueryExpression(other.validationExpression);
        this.annotationIOSettings = new SingleAnnotationIOSettings(other.annotationIOSettings);
        annotationIOSettings.getEventBus().register(this);
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
    @ExpressionParameterSettings(variableSource = NumberQueryExpressionVariableSource.class)
    public DefaultExpressionParameter getValidationExpression() {
        return validationExpression;
    }

    @JIPipeParameter("validation-expression")
    public void setValidationExpression(DefaultExpressionParameter validationExpression) {
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
        return JIPipe.getParameterTypes().createEditorFor(workbench, access);
    }

    @Override
    public JIPipeData duplicate() {
        return new DoubleFormData(this);
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        ExpressionParameters variableSet = new ExpressionParameters();
        variableSet.set("value", value);
        if (!validationExpression.test(variableSet)) {
            report.reportIsInvalid("Invalid value!",
                    String.format("The provided value '%s' does not comply to the test '%s'", value, validationExpression.getExpression()),
                    "Please correct your input",
                    this);
        }
    }

    @Override
    public String toString() {
        return String.format("Number form [name=%s, value=%s]", getName(), value);
    }

    @Override
    public void loadData(JIPipeMergingDataBatch dataBatch) {
        if (annotationIOSettings.getInputAnnotation().isEnabled()) {
            JIPipeAnnotation annotation =
                    dataBatch.getAnnotations().getOrDefault(annotationIOSettings.getInputAnnotation().getContent(),
                            null);
            if (annotation != null) {
                if (NumberUtils.isCreatable(annotation.getValue())) {
                    value = NumberUtils.createDouble(annotation.getValue());
                }
            }
        }
    }

    @Override
    public void writeData(JIPipeMergingDataBatch dataBatch) {
        if (annotationIOSettings.getOutputAnnotation().isEnabled()) {
            annotationIOSettings.getAnnotationMergeStrategy().mergeInto(dataBatch.getAnnotations(),
                    Collections.singletonList(annotationIOSettings.getOutputAnnotation().createAnnotation("" + value)));
        }
    }

    public static DoubleFormData importFrom(Path rowStorage) {
        return FormData.importFrom(rowStorage, DoubleFormData.class);
    }
}
