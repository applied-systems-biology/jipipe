package org.hkijena.jipipe.extensions.annotation.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataAnnotationMergeStrategy;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalStringParameter;

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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        String targetedAnnotationName = annotationNameQuery.queryFirst(dataBatch.getGlobalDataAnnotations().keySet(), new ExpressionVariables());
        if (targetedAnnotationName == null) {
            if (ignoreMissingAnnotations)
                return;
            throw new UserFriendlyRuntimeException("Could not find data annotation matching '" + annotationNameQuery.getExpression() + "'",
                    "Could not find data annotation!",
                    getDisplayName(),
                    "The node tried to find a data annotation that matches the expression '" + annotationNameQuery.getExpression() + "', but none did match. Following were available: " +
                            String.join(", ", dataBatch.getGlobalAnnotations().keySet()),
                    "Check if the expression is correct or enable 'Ignore missing annotations'");
        }
        JIPipeDataAnnotation dataAnnotation = dataBatch.getGlobalDataAnnotation(targetedAnnotationName);
        if (!keepOtherDataAnnotations) {
            dataBatch.getGlobalAnnotations().clear();
            dataBatch.addGlobalDataAnnotation(dataAnnotation, JIPipeDataAnnotationMergeStrategy.OverwriteExisting);
        }
        if (!keepCurrentAnnotation) {
            dataBatch.removeGlobalDataAnnotation(targetedAnnotationName);
        }
        if (annotateWithCurrentData.isEnabled()) {
            dataBatch.addGlobalDataAnnotation(new JIPipeDataAnnotation(annotateWithCurrentData.getContent(), dataBatch.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo)),
                    JIPipeDataAnnotationMergeStrategy.OverwriteExisting);
        }
        dataBatch.addOutputData(getFirstOutputSlot(), dataAnnotation.getData(JIPipeData.class, progressInfo), progressInfo);
    }

    @JIPipeDocumentation(name = "Extracted data annotation", description = "Determines which annotation is extracted. If multiple match, the first matching annotation column is used. " + StringQueryExpression.DOCUMENTATION_DESCRIPTION)
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
