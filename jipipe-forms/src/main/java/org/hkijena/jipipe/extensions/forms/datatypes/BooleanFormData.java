package org.hkijena.jipipe.extensions.forms.datatypes;

import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDefaultDocumentation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.parameters.JIPipeReflectionParameterAccess;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryCause;
import org.hkijena.jipipe.extensions.forms.utils.SingleAnnotationIOSettings;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalStringParameter;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.util.Collections;

@JIPipeDocumentation(name = "Boolean input form", description = "A form element that allows the user to input a boolean (true/false) value")
public class BooleanFormData extends ParameterFormData {

    private boolean value = true;
    private SingleAnnotationIOSettings annotationIOSettings = new SingleAnnotationIOSettings();
    private OptionalStringParameter trueString = new OptionalStringParameter("true", true);
    private OptionalStringParameter falseString = new OptionalStringParameter("false", true);

    public BooleanFormData() {
    }

    public BooleanFormData(BooleanFormData other) {
        super(other);
        this.value = other.value;
        this.annotationIOSettings = new SingleAnnotationIOSettings(other.annotationIOSettings);
        this.trueString = other.trueString;
        this.falseString = other.falseString;
    }

    public static BooleanFormData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return FormData.importData(storage, BooleanFormData.class, progressInfo);
    }

    @JIPipeDocumentation(name = "Initial value", description = "The initial value")
    @JIPipeParameter("initial-value")
    public boolean getValue() {
        return value;
    }

    @JIPipeParameter("initial-value")
    public void setValue(boolean value) {
        this.value = value;
    }

    @JIPipeDocumentation(name = "Form element I/O", description = "Use following settings to determine how to extract initial values " +
            "from annotations and where to store the user-defined value.")
    @JIPipeParameter("form:io")
    public SingleAnnotationIOSettings getAnnotationIOSettings() {
        return annotationIOSettings;
    }

    @JIPipeDocumentation(name = "Value for 'TRUE'", description = "The annotation value that is written if the form value is TRUE")
    @JIPipeParameter("true-string")
    public OptionalStringParameter getTrueString() {
        return trueString;
    }

    @JIPipeParameter("true-string")
    public void setTrueString(OptionalStringParameter trueString) {
        this.trueString = trueString;
    }

    @JIPipeDocumentation(name = "Value for 'FALSE'", description = "The annotation value that is written if the form value is FALSE")
    @JIPipeParameter("false-string")
    public OptionalStringParameter getFalseString() {
        return falseString;
    }

    @JIPipeParameter("false-string")
    public void setFalseString(OptionalStringParameter falseString) {
        this.falseString = falseString;
    }

    @Override
    public Component getEditor(JIPipeWorkbench workbench) {
        JIPipeParameterTree tree = new JIPipeParameterTree(this);
        JIPipeReflectionParameterAccess access = (JIPipeReflectionParameterAccess) tree.getParameters().get("initial-value");
        access.setDocumentation(new JIPipeDefaultDocumentation(getName(), getDescription().getBody()));
        return JIPipe.getParameterTypes().createEditorFor(workbench, access);
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new BooleanFormData(this);
    }

    @Override
    public void reportValidity(JIPipeValidationReportEntryCause parentCause, JIPipeValidationReport report) {
    }

    @Override
    public String toString() {
        return String.format("Boolean form [name=%s, value=%s]", getName(), value);
    }

    @Override
    public void loadData(JIPipeMergingDataBatch dataBatch) {
        if (annotationIOSettings.getInputAnnotation().isEnabled()) {
            JIPipeTextAnnotation annotation =
                    dataBatch.getMergedTextAnnotations().getOrDefault(annotationIOSettings.getInputAnnotation().getContent(),
                            null);
            if (annotation != null) {
                String value = StringUtils.nullToEmpty(annotation.getValue());
                if (trueString.isEnabled() && value.equals(trueString.getContent()))
                    this.value = true;
                else if (falseString.isEnabled() && value.equals(falseString.getContent()))
                    this.value = false;
                else if (value.toLowerCase().startsWith("t"))
                    this.value = true;
                else if (value.toLowerCase().startsWith("f"))
                    this.value = false;
                else if (NumberUtils.isCreatable(annotation.getValue())) {
                    if (NumberUtils.createInteger(annotation.getValue()) > 0)
                        this.value = true;
                }
            }
        }
    }

    @Override
    public void writeData(JIPipeMergingDataBatch dataBatch) {
        if (annotationIOSettings.getOutputAnnotation().isEnabled()) {
            if (value) {
                if (trueString.isEnabled()) {
                    annotationIOSettings.getAnnotationMergeStrategy().mergeInto(dataBatch.getMergedTextAnnotations(),
                            Collections.singletonList(annotationIOSettings.getOutputAnnotation().createAnnotation(trueString.getContent())));
                }
            } else {
                if (falseString.isEnabled()) {
                    annotationIOSettings.getAnnotationMergeStrategy().mergeInto(dataBatch.getMergedTextAnnotations(),
                            Collections.singletonList(annotationIOSettings.getOutputAnnotation().createAnnotation(falseString.getContent())));
                }
            }
        }
    }
}
