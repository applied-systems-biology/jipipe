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

package org.hkijena.jipipe.plugins.annotation.algorithms;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalStringParameter;

@SetJIPipeDocumentation(name = "Extract data annotations", description = "Extracts a data annotation.")
@ConfigureJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class)
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, name = "Output", create = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        String targetedAnnotationName = annotationNameQuery.queryFirst(iterationStep.getMergedDataAnnotations().keySet(), new JIPipeExpressionVariablesMap());
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

    @SetJIPipeDocumentation(name = "Extracted data annotation", description = "Determines which annotation is extracted. If multiple match, the first matching annotation column is used. ")
    @JIPipeParameter("annotation-name")
    public StringQueryExpression getAnnotationNameQuery() {
        return annotationNameQuery;
    }

    @JIPipeParameter("annotation-name")
    public void setAnnotationNameQuery(StringQueryExpression annotationNameQuery) {
        this.annotationNameQuery = annotationNameQuery;
    }

    @SetJIPipeDocumentation(name = "Ignore missing data annotations", description = "If enabled, data rows with a missing data annotation are ignored.")
    @JIPipeParameter("ignore-missing-annotations")
    public boolean isIgnoreMissingAnnotations() {
        return ignoreMissingAnnotations;
    }

    @JIPipeParameter("ignore-missing-annotations")
    public void setIgnoreMissingAnnotations(boolean ignoreMissingAnnotations) {
        this.ignoreMissingAnnotations = ignoreMissingAnnotations;
    }

    @SetJIPipeDocumentation(name = "Keep other data annotations", description = "If enabled, existing data annotations that is not the targeted annotation are kept.")
    @JIPipeParameter("keep-other-data-annotations")
    public boolean isKeepOtherDataAnnotations() {
        return keepOtherDataAnnotations;
    }

    @JIPipeParameter("keep-other-data-annotations")
    public void setKeepOtherDataAnnotations(boolean keepOtherDataAnnotations) {
        this.keepOtherDataAnnotations = keepOtherDataAnnotations;
    }

    @SetJIPipeDocumentation(name = "Annotate with current data", description = "If enabled, the current (main) data is converted into an annotation of the specified name")
    @JIPipeParameter("annotate-with-current-data")
    public OptionalStringParameter getAnnotateWithCurrentData() {
        return annotateWithCurrentData;
    }

    @JIPipeParameter("annotate-with-current-data")
    public void setAnnotateWithCurrentData(OptionalStringParameter annotateWithCurrentData) {
        this.annotateWithCurrentData = annotateWithCurrentData;
    }

    @SetJIPipeDocumentation(name = "Keep current data annotation", description = "If enabled, the targeted data annotation is kept. Otherwise it is removed.")
    @JIPipeParameter("keep-current-annotation")
    public boolean isKeepCurrentAnnotation() {
        return keepCurrentAnnotation;
    }

    @JIPipeParameter("keep-current-annotation")
    public void setKeepCurrentAnnotation(boolean keepCurrentAnnotation) {
        this.keepCurrentAnnotation = keepCurrentAnnotation;
    }
}
