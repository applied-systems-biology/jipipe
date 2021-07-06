package org.hkijena.jipipe.extensions.forms.datatypes;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.parameters.JIPipeManualParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.forms.utils.SingleAnnotationIOSettings;
import org.hkijena.jipipe.extensions.parameters.primitives.FilePathParameterSettings;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.PathEditor;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.Component;
import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

@JIPipeDocumentation(name = "Path input form", description = "A form element that allows the user to input a path to a file or folder")
public class PathFormData extends ParameterFormData {

    private Path value = Paths.get("");
    private StringQueryExpression validationExpression = new StringQueryExpression("value != \"\"");
    private SingleAnnotationIOSettings annotationIOSettings = new SingleAnnotationIOSettings();
    private PathEditor.PathMode pathType = PathEditor.PathMode.FilesAndDirectories;
    private PathEditor.IOMode ioMode = PathEditor.IOMode.Open;
    private StringList extensions = new StringList();

    public PathFormData() {
        annotationIOSettings.getEventBus().register(this);
    }

    public PathFormData(PathFormData other) {
        super(other);
        this.value = other.value;
        this.validationExpression = new StringQueryExpression(other.validationExpression);
        this.annotationIOSettings = new SingleAnnotationIOSettings(other.annotationIOSettings);
        this.pathType = other.pathType;
        this.ioMode = other.ioMode;
        this.extensions = new StringList(other.extensions);
        annotationIOSettings.getEventBus().register(this);
    }

    @JIPipeDocumentation(name = "Initial value", description = "The initial string value")
    @JIPipeParameter("initial-value")
    @FilePathParameterSettings(pathMode = PathEditor.PathMode.FilesAndDirectories, ioMode = PathEditor.IOMode.Open)
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
    public PathEditor.PathMode getPathType() {
        return pathType;
    }

    @JIPipeParameter("path-type")
    public void setPathType(PathEditor.PathMode pathType) {
        this.pathType = pathType;
    }

    @JIPipeDocumentation(name = "Path I/O mode", description = "Decides if the user sees an open or save dialog.")
    @JIPipeParameter("io-mode")
    public PathEditor.IOMode getIoMode() {
        return ioMode;
    }

    @JIPipeParameter("io-mode")
    public void setIoMode(PathEditor.IOMode ioMode) {
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
                .addAnnotation(new FilePathParameterSettings() {
                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return FilePathParameterSettings.class;
                    }

                    @Override
                    public PathEditor.IOMode ioMode() {
                        return ioMode;
                    }

                    @Override
                    public PathEditor.PathMode pathMode() {
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
        return JIPipe.getParameterTypes().createEditorFor(workbench, access);
    }

    @Override
    public JIPipeData duplicate() {
        return new PathFormData(this);
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        if (!validationExpression.test(StringUtils.nullToEmpty(value))) {
            report.reportIsInvalid("Invalid value!",
                    String.format("The provided value '%s' does not comply to the test '%s'", value, validationExpression.getExpression()),
                    "Please correct your input",
                    this);
        }
    }

    @Override
    public String toString() {
        return String.format("Path form [name=%s, value=%s]", getName(), value);
    }

    @Override
    public void loadData(JIPipeMergingDataBatch dataBatch) {
        if (annotationIOSettings.getInputAnnotation().isEnabled()) {
            JIPipeAnnotation annotation =
                    dataBatch.getGlobalAnnotations().getOrDefault(annotationIOSettings.getInputAnnotation().getContent(),
                            null);
            if (annotation != null) {
                value = Paths.get(annotation.getValue());
            }
        }
    }

    @Override
    public void writeData(JIPipeMergingDataBatch dataBatch) {
        if (annotationIOSettings.getOutputAnnotation().isEnabled()) {
            annotationIOSettings.getAnnotationMergeStrategy().mergeInto(dataBatch.getGlobalAnnotations(),
                    Collections.singletonList(annotationIOSettings.getOutputAnnotation().createAnnotation(StringUtils.nullToEmpty(value))));
        }
    }

    public static PathFormData importFrom(Path rowStorage) {
        return FormData.importFrom(rowStorage, PathFormData.class);
    }
}
