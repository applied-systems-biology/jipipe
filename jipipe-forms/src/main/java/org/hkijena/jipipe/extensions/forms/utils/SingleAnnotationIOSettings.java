package org.hkijena.jipipe.extensions.forms.utils;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;

/**
 * Settings group for single annotation I/O
 */
public class SingleAnnotationIOSettings implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();
    private OptionalAnnotationNameParameter outputAnnotation = new OptionalAnnotationNameParameter("Text", true);
    private OptionalAnnotationNameParameter inputAnnotation = new OptionalAnnotationNameParameter("", false);
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    public SingleAnnotationIOSettings() {
    }

    public SingleAnnotationIOSettings(SingleAnnotationIOSettings other) {
        this.outputAnnotation = new OptionalAnnotationNameParameter(other.outputAnnotation);
        this.inputAnnotation = new OptionalAnnotationNameParameter(other.inputAnnotation);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
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
    public JIPipeTextAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
