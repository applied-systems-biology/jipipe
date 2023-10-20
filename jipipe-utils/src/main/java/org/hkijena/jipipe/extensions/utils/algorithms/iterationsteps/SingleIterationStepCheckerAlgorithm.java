package org.hkijena.jipipe.extensions.utils.algorithms.iterationsteps;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIOSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeNodeAlias;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerRange;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Check iteration steps (one data per slot)",
        description = "Pass multiple inputs through this node to check if iteration steps are correctly created and filter out incomplete steps. " +
                "This node is designed for creating iteration steps where one data item is assigned to each iteration step input slot.")
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Iteration steps")
@JIPipeNodeAlias(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Filter", aliasName = "Limit to iteration steps (one data per slot)")
@JIPipeNodeAlias(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Filter", aliasName = "Filter multiple data by annotation (one data per slot)")
public class SingleIterationStepCheckerAlgorithm extends JIPipeIteratingAlgorithm {
    private boolean keepOriginalAnnotations = true;
    private OptionalAnnotationNameParameter iterationStepIndexAnnotation = new OptionalAnnotationNameParameter("Iteration step", false);
    private DefaultExpressionParameter filter = new DefaultExpressionParameter();
    public SingleIterationStepCheckerAlgorithm(JIPipeNodeInfo info) {
        super(info, new JIPipeIOSlotConfiguration());
    }

    public SingleIterationStepCheckerAlgorithm(SingleIterationStepCheckerAlgorithm other) {
        super(other);
        this.keepOriginalAnnotations = other.keepOriginalAnnotations;
        this.iterationStepIndexAnnotation = new OptionalAnnotationNameParameter(other.iterationStepIndexAnnotation);
        this.filter = new DefaultExpressionParameter(other.filter);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        {
            ExpressionVariables variables = new ExpressionVariables();
            variables.putAnnotations(iterationStep.getMergedTextAnnotations());
            variables.set("iteration_step_index", iterationContext.getCurrentIterationStepIndex());
            variables.set("num_iteration_steps", iterationContext.getNumIterationSteps());
            if(!filter.test(variables)) {
                progressInfo.log("Iteration step filtered out. Skipping.");
                return;
            }
        }
        for (JIPipeInputDataSlot inputSlot : getInputSlots()) {
            JIPipeData inputData = iterationStep.getInputData(inputSlot, JIPipeData.class, progressInfo);
            int row = iterationStep.getInputRow(inputSlot);
            List<JIPipeTextAnnotation> annotationList;
            if(keepOriginalAnnotations) {
                annotationList = new ArrayList<>(inputSlot.getTextAnnotations(row));
            }
            else {
                annotationList = new ArrayList<>(iterationStep.getMergedTextAnnotations().values());
            }
            iterationStepIndexAnnotation.addAnnotationIfEnabled(annotationList, String.valueOf(iterationContext.getCurrentIterationStepIndex()));

            getOutputSlot(inputSlot.getName()).addData(inputData,
                    annotationList,
                    JIPipeTextAnnotationMergeMode.Merge,
                    inputSlot.getDataAnnotations(row),
                    JIPipeDataAnnotationMergeMode.OverwriteExisting,
                    inputSlot.getDataContext(row).branch(this),
                    progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Keep original annotations", description = "If enabled, keep the original annotations of the input data without merging them")
    @JIPipeParameter("keep-original-annotations")
    public boolean isKeepOriginalAnnotations() {
        return keepOriginalAnnotations;
    }

    @JIPipeParameter("keep-original-annotations")
    public void setKeepOriginalAnnotations(boolean keepOriginalAnnotations) {
        this.keepOriginalAnnotations = keepOriginalAnnotations;
    }

    @JIPipeDocumentation(name = "Annotate with iteration step index", description = "If enabled, annotate each output with the annotation step index")
    @JIPipeParameter("iteration-step-index-annotation")
    public OptionalAnnotationNameParameter getIterationStepIndexAnnotation() {
        return iterationStepIndexAnnotation;
    }

    @JIPipeParameter("iteration-step-index-annotation")
    public void setIterationStepIndexAnnotation(OptionalAnnotationNameParameter iterationStepIndexAnnotation) {
        this.iterationStepIndexAnnotation = iterationStepIndexAnnotation;
    }

    @JIPipeDocumentation(name = "Filter", description = "Allows to filter data batches")
    @JIPipeParameter(value = "filter", important = true)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "Current iteration step index", key = "iteration_step_index", description = "The index of the current iteration step")
    @ExpressionParameterSettingsVariable(name = "Number of iteration steps", key = "num_iteration_steps", description = "The number of iteration steps that are processed")
    public DefaultExpressionParameter getFilter() {
        return filter;
    }

    @JIPipeParameter("filter")
    public void setFilter(DefaultExpressionParameter filter) {
        this.filter = filter;
    }
}