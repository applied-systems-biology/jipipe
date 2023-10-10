package org.hkijena.jipipe.extensions.forms.datatypes;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDefaultDocumentation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.parameters.JIPipeReflectionParameterAccess;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.forms.utils.SingleAnnotationIOSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.awt.*;
import java.util.Collections;

@JIPipeDocumentation(name = "Text input form", description = "A form element that allows the user to input text")
public class StringFormData extends ParameterFormData {

    private String value = "";
    private StringQueryExpression validationExpression = new StringQueryExpression("value != \"\"");
    private SingleAnnotationIOSettings annotationIOSettings = new SingleAnnotationIOSettings();

    public StringFormData() {
    }

    public StringFormData(StringFormData other) {
        super(other);
        this.value = other.value;
        this.validationExpression = new StringQueryExpression(other.validationExpression);
        this.annotationIOSettings = new SingleAnnotationIOSettings(other.annotationIOSettings);
    }

    public static StringFormData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return FormData.importData(storage, StringFormData.class, progressInfo);
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

    @JIPipeDocumentation(name = "Validation expression", description = "Expression that is used to validate the user input. There is a variable 'value' available that " +
            "contains the tested string.")
    @JIPipeParameter("validation-expression")
    public StringQueryExpression getValidationExpression() {
        return validationExpression;
    }

    @JIPipeParameter("validation-expression")
    public void setValidationExpression(StringQueryExpression validationExpression) {
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
        return new StringFormData(this);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        if (!validationExpression.test(value)) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, context, "Invalid value!",
                    String.format("The provided value '%s' does not comply to the test '%s'", value, validationExpression.getExpression()),
                    "Please correct your input"));
        }
    }

    @Override
    public String toString() {
        return String.format("Text form [name=%s, value=%s]", getName(), value);
    }

    @Override
    public void loadData(JIPipeMergingDataBatch dataBatch) {
        if (annotationIOSettings.getInputAnnotation().isEnabled()) {
            JIPipeTextAnnotation annotation =
                    dataBatch.getMergedTextAnnotations().getOrDefault(annotationIOSettings.getInputAnnotation().getContent(),
                            null);
            if (annotation != null) {
                value = annotation.getValue();
            }
        }
    }

    @Override
    public void writeData(JIPipeMergingDataBatch dataBatch) {
        if (annotationIOSettings.getOutputAnnotation().isEnabled()) {
            annotationIOSettings.getAnnotationMergeStrategy().mergeInto(dataBatch.getMergedTextAnnotations(),
                    Collections.singletonList(annotationIOSettings.getOutputAnnotation().createAnnotation(value)));
        }
    }
}
