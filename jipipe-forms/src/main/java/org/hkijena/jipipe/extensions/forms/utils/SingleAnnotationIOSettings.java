package org.hkijena.jipipe.extensions.forms.utils;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;

/**
 * Settings group for single annotation I/O
 */
public class SingleAnnotationIOSettings extends AbstractJIPipeParameterCollection {
    private OptionalTextAnnotationNameParameter outputAnnotation = new OptionalTextAnnotationNameParameter("Text", true);
    private OptionalTextAnnotationNameParameter inputAnnotation = new OptionalTextAnnotationNameParameter("", false);
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    public SingleAnnotationIOSettings() {
    }

    public SingleAnnotationIOSettings(SingleAnnotationIOSettings other) {
        this.outputAnnotation = new OptionalTextAnnotationNameParameter(other.outputAnnotation);
        this.inputAnnotation = new OptionalTextAnnotationNameParameter(other.inputAnnotation);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
    }

    @SetJIPipeDocumentation(name = "Output annotation", description = "Determines into which annotation the user input is written.")
    @JIPipeParameter("output-annotation")
    public OptionalTextAnnotationNameParameter getOutputAnnotation() {
        return outputAnnotation;
    }

    @JIPipeParameter("output-annotation")
    public void setOutputAnnotation(OptionalTextAnnotationNameParameter outputAnnotation) {
        this.outputAnnotation = outputAnnotation;
    }

    @SetJIPipeDocumentation(name = "Input annotation", description = "The annotation used to override the initial value. If the annotation does not exist, " +
            "the standard initial value is used.")
    @JIPipeParameter("input-annotation")
    public OptionalTextAnnotationNameParameter getInputAnnotation() {
        return inputAnnotation;
    }

    @JIPipeParameter("input-annotation")
    public void setInputAnnotation(OptionalTextAnnotationNameParameter inputAnnotation) {
        this.inputAnnotation = inputAnnotation;
    }

    @SetJIPipeDocumentation(name = "Merge output annotation", description = "Determines how the output annotation is merged with existing values.")
    @JIPipeParameter("merge-strategy")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }

}
