package org.hkijena.jipipe.extensions.annotation.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalStringParameter;

@JIPipeDocumentation(name = "Extract data annotations", description = "Extracts a data annotation.")
@JIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class)
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", autoCreate = true)
public class ExtractDataAnnotation extends JIPipeSimpleIteratingAlgorithm {

    private StringQueryExpression annotationNameQuery = new StringQueryExpression();
    private boolean ignoreMissingAnnotations = true;
    private boolean keepOtherDataAnnotations = true;
    private boolean keepCurrentAnnotation = false;
    private OptionalStringParameter annotateWithCurrentData = new OptionalStringParameter("Data", true);

    public ExtractDataAnnotation(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractDataAnnotation(ExtractDataAnnotation other) {
        super(other);
        other.annotationNameQuery = new StringQueryExpression(other.annotationNameQuery);
        this.keepCurrentAnnotation = other.keepCurrentAnnotation;
        this.ignoreMissingAnnotations = other.ignoreMissingAnnotations;
        this.keepOtherDataAnnotations = other.keepOtherDataAnnotations;
        this.annotateWithCurrentData = new OptionalStringParameter(other.annotateWithCurrentData);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        String targetedAnnotationName = annotationNameQuery.queryFirst(iterationStep.getMergedDataAnnotations().keySet(), new ExpressionVariables());
        if (targetedAnnotationName == null) {
            if (ignoreMissingAnnotations)
                return;
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(this),
                    "Could not find data annotation matching '" + annotationNameQuery.getExpression() + "'",
                    "The node tried to find a data annotation that matches the expression '" + annotationNameQuery.getExpression() + "', but none did match. Following were available: " +
                            String.join(", ", iterationStep.getMergedTextAnnotations().keySet()),
                    "Check if the expression is correct or enable 'Ignore missing annotations'"));
        }
        JIPipeDataAnnotation dataAnnotation = iterationStep.getMergedDataAnnotation(targetedAnnotationName);
        if (!keepOtherDataAnnotations) {
            iterationStep.getMergedTextAnnotations().clear();
            iterationStep.addMergedDataAnnotation(dataAnnotation, JIPipeDataAnnotationMergeMode.OverwriteExisting);
        }
        if (!keepCurrentAnnotation) {
            iterationStep.removeMergedDataAnnotation(targetedAnnotationName);
        }
        if (annotateWithCurrentData.isEnabled()) {
            iterationStep.addMergedDataAnnotation(new JIPipeDataAnnotation(annotateWithCurrentData.getContent(), iterationStep.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo)),
                    JIPipeDataAnnotationMergeMode.OverwriteExisting);
        }
        iterationStep.addOutputData(getFirstOutputSlot(), dataAnnotation.getData(JIPipeData.class, progressInfo), progressInfo);
    }

    @JIPipeDocumentation(name = "Extracted data annotation", description = "Determines which annotation is extracted. If multiple match, the first matching annotation column is used. ")
    @JIPipeParameter("annotation-name")
    public StringQueryExpression getAnnotationNameQuery() {
        return annotationNameQuery;
    }

    @JIPipeParameter("annotation-name")
    public void setAnnotationNameQuery(StringQueryExpression annotationNameQuery) {
        this.annotationNameQuery = annotationNameQuery;
    }

    @JIPipeDocumentation(name = "Ignore missing data annotations", description = "If enabled, data rows with a missing data annotation are ignored.")
    @JIPipeParameter("ignore-missing-annotations")
    public boolean isIgnoreMissingAnnotations() {
        return ignoreMissingAnnotations;
    }

    @JIPipeParameter("ignore-missing-annotations")
    public void setIgnoreMissingAnnotations(boolean ignoreMissingAnnotations) {
        this.ignoreMissingAnnotations = ignoreMissingAnnotations;
    }

    @JIPipeDocumentation(name = "Keep other data annotations", description = "If enabled, existing data annotations that is not the targeted annotation are kept.")
    @JIPipeParameter("keep-other-data-annotations")
    public boolean isKeepOtherDataAnnotations() {
        return keepOtherDataAnnotations;
    }

    @JIPipeParameter("keep-other-data-annotations")
    public void setKeepOtherDataAnnotations(boolean keepOtherDataAnnotations) {
        this.keepOtherDataAnnotations = keepOtherDataAnnotations;
    }

    @JIPipeDocumentation(name = "Annotate with current data", description = "If enabled, the current (main) data is converted into an annotation of the specified name")
    @JIPipeParameter("annotate-with-current-data")
    public OptionalStringParameter getAnnotateWithCurrentData() {
        return annotateWithCurrentData;
    }

    @JIPipeParameter("annotate-with-current-data")
    public void setAnnotateWithCurrentData(OptionalStringParameter annotateWithCurrentData) {
        this.annotateWithCurrentData = annotateWithCurrentData;
    }

    @JIPipeDocumentation(name = "Keep current data annotation", description = "If enabled, the targeted data annotation is kept. Otherwise it is removed.")
    @JIPipeParameter("keep-current-annotation")
    public boolean isKeepCurrentAnnotation() {
        return keepCurrentAnnotation;
    }

    @JIPipeParameter("keep-current-annotation")
    public void setKeepCurrentAnnotation(boolean keepCurrentAnnotation) {
        this.keepCurrentAnnotation = keepCurrentAnnotation;
    }
}
