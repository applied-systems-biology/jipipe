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

package org.hkijena.jipipe.plugins.forms.datatypes;

import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.parameters.JIPipeReflectionParameterAccess;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.forms.utils.SingleAnnotationIOSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalStringParameter;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.util.Collections;
import java.util.Locale;

@SetJIPipeDocumentation(name = "Boolean input form", description = "A form element that allows the user to input a boolean (true/false) value")
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

    @SetJIPipeDocumentation(name = "Initial value", description = "The initial value")
    @JIPipeParameter("initial-value")
    public boolean getValue() {
        return value;
    }

    @JIPipeParameter("initial-value")
    public void setValue(boolean value) {
        this.value = value;
    }

    @SetJIPipeDocumentation(name = "Form element I/O", description = "Use following settings to determine how to extract initial values " +
            "from annotations and where to store the user-defined value.")
    @JIPipeParameter("form:io")
    public SingleAnnotationIOSettings getAnnotationIOSettings() {
        return annotationIOSettings;
    }

    @SetJIPipeDocumentation(name = "Value for 'TRUE'", description = "The annotation value that is written if the form value is TRUE")
    @JIPipeParameter("true-string")
    public OptionalStringParameter getTrueString() {
        return trueString;
    }

    @JIPipeParameter("true-string")
    public void setTrueString(OptionalStringParameter trueString) {
        this.trueString = trueString;
    }

    @SetJIPipeDocumentation(name = "Value for 'FALSE'", description = "The annotation value that is written if the form value is FALSE")
    @JIPipeParameter("false-string")
    public OptionalStringParameter getFalseString() {
        return falseString;
    }

    @JIPipeParameter("false-string")
    public void setFalseString(OptionalStringParameter falseString) {
        this.falseString = falseString;
    }

    @Override
    public Component getEditor(JIPipeDesktopWorkbench workbench) {
        JIPipeParameterTree tree = new JIPipeParameterTree(this);
        JIPipeReflectionParameterAccess access = (JIPipeReflectionParameterAccess) tree.getParameters().get("initial-value");
        access.setDocumentation(new JIPipeDocumentation(getName(), getDescription().getBody()));
        return JIPipe.getParameterTypes().createEditorInstance(access, workbench, new JIPipeParameterTree(access), null);
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new BooleanFormData(this);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
    }

    @Override
    public String toString() {
        return String.format("Boolean form [name=%s, value=%s]", getName(), value);
    }

    @Override
    public void loadData(JIPipeMultiIterationStep iterationStep) {
        if (annotationIOSettings.getInputAnnotation().isEnabled()) {
            JIPipeTextAnnotation annotation =
                    iterationStep.getMergedTextAnnotations().getOrDefault(annotationIOSettings.getInputAnnotation().getContent(),
                            null);
            if (annotation != null) {
                String value = StringUtils.nullToEmpty(annotation.getValue());
                if (trueString.isEnabled() && value.equals(trueString.getContent()))
                    this.value = true;
                else if (falseString.isEnabled() && value.equals(falseString.getContent()))
                    this.value = false;
                else if (value.toLowerCase(Locale.ROOT).startsWith("t"))
                    this.value = true;
                else if (value.toLowerCase(Locale.ROOT).startsWith("f"))
                    this.value = false;
                else if (NumberUtils.isCreatable(annotation.getValue())) {
                    if (NumberUtils.createInteger(annotation.getValue()) > 0)
                        this.value = true;
                }
            }
        }
    }

    @Override
    public void writeData(JIPipeMultiIterationStep iterationStep) {
        if (annotationIOSettings.getOutputAnnotation().isEnabled()) {
            if (value) {
                if (trueString.isEnabled()) {
                    annotationIOSettings.getAnnotationMergeStrategy().mergeInto(iterationStep.getMergedTextAnnotations(),
                            Collections.singletonList(annotationIOSettings.getOutputAnnotation().createAnnotation(trueString.getContent())));
                }
            } else {
                if (falseString.isEnabled()) {
                    annotationIOSettings.getAnnotationMergeStrategy().mergeInto(iterationStep.getMergedTextAnnotations(),
                            Collections.singletonList(annotationIOSettings.getOutputAnnotation().createAnnotation(falseString.getContent())));
                }
            }
        }
    }
}
