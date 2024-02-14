package org.hkijena.jipipe.extensions.forms.datatypes;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeManualParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.forms.utils.SingleAnnotationIOSettings;
import org.hkijena.jipipe.extensions.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

@JIPipeDocumentation(name = "Path input form", description = "A form element that allows the user to input a path to a file or folder")
public class PathFormData extends ParameterFormData {

    private Path value = Paths.get("");
    private StringQueryExpression validationExpression = new StringQueryExpression("value != \"\"");
    private SingleAnnotationIOSettings annotationIOSettings = new SingleAnnotationIOSettings();
    private PathType pathType = PathType.FilesAndDirectories;
    private PathIOMode ioMode = PathIOMode.Open;
    private StringList extensions = new StringList();

    public PathFormData() {
    }

    public PathFormData(PathFormData other) {
        super(other);
        this.value = other.value;
        this.validationExpression = new StringQueryExpression(other.validationExpression);
        this.annotationIOSettings = new SingleAnnotationIOSettings(other.annotationIOSettings);
        this.pathType = other.pathType;
        this.ioMode = other.ioMode;
        this.extensions = new StringList(other.extensions);
    }

    public static PathFormData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return FormData.importData(storage, PathFormData.class, progressInfo);
    }

    @JIPipeDocumentation(name = "Initial value", description = "The initial string value")
    @JIPipeParameter("initial-value")
    @PathParameterSettings(pathMode = PathType.FilesAndDirectories, ioMode = PathIOMode.Open)
    public Path getValue() {
        return value;
    }

    @JIPipeParameter("initial-value")
    public void setValue(Path value) {
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

    @JIPipeDocumentation(name = "Path type", description = "Allows to restrict the path type.")
    @JIPipeParameter("path-type")
    public PathType getPathType() {
        return pathType;
    }

    @JIPipeParameter("path-type")
    public void setPathType(PathType pathType) {
        this.pathType = pathType;
    }

    @JIPipeDocumentation(name = "Path I/O mode", description = "Decides if the user sees an open or save dialog.")
    @JIPipeParameter("io-mode")
    public PathIOMode getIoMode() {
        return ioMode;
    }

    @JIPipeParameter("io-mode")
    public void setIoMode(PathIOMode ioMode) {
        this.ioMode = ioMode;
    }

    @JIPipeDocumentation(name = "Extensions", description = "Only for loading/saving files. List of extensions that are loaded/saved. " +
            "The extensions are provided without the leading dot, e.g. <code>jpg</code> or <code>tiff</code>.")
    @JIPipeParameter("extensions")
    public StringList getExtensions() {
        return extensions;
    }

    @JIPipeParameter("extensions")
    public void setExtensions(StringList extensions) {
        this.extensions = extensions;
    }

    @Override
    public Component getEditor(JIPipeWorkbench workbench) {
        JIPipeManualParameterAccess access = JIPipeManualParameterAccess.builder()
                .setGetter(this::getValue)
                .setSetter(this::setValue)
                .setFieldClass(Path.class)
                .setSource(this)
                .addAnnotation(new PathParameterSettings() {
                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return PathParameterSettings.class;
                    }

                    @Override
                    public PathIOMode ioMode() {
                        return ioMode;
                    }

                    @Override
                    public PathType pathMode() {
                        return pathType;
                    }

                    @Override
                    public String[] extensions() {
                        return extensions.toArray(new String[0]);
                    }

                    @Override
                    public FileChooserSettings.LastDirectoryKey key() {
                        return FileChooserSettings.LastDirectoryKey.Parameters;
                    }
                })
                .build();
        return JIPipe.getParameterTypes().createEditorFor(workbench, new JIPipeParameterTree(access), access);
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new PathFormData(this);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        if (!validationExpression.test(StringUtils.nullToEmpty(value))) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, reportContext, "Invalid value!",
                    String.format("The provided value '%s' does not comply to the test '%s'", value, validationExpression.getExpression()),
                    "Please correct your input"));
        }
    }

    @Override
    public String toString() {
        return String.format("Path form [name=%s, value=%s]", getName(), value);
    }

    @Override
    public void loadData(JIPipeMultiIterationStep iterationStep) {
        if (annotationIOSettings.getInputAnnotation().isEnabled()) {
            JIPipeTextAnnotation annotation =
                    iterationStep.getMergedTextAnnotations().getOrDefault(annotationIOSettings.getInputAnnotation().getContent(),
                            null);
            if (annotation != null) {
                value = Paths.get(annotation.getValue());
            }
        }
    }

    @Override
    public void writeData(JIPipeMultiIterationStep iterationStep) {
        if (annotationIOSettings.getOutputAnnotation().isEnabled()) {
            annotationIOSettings.getAnnotationMergeStrategy().mergeInto(iterationStep.getMergedTextAnnotations(),
                    Collections.singletonList(annotationIOSettings.getOutputAnnotation().createAnnotation(StringUtils.nullToEmpty(value))));
        }
    }
}
