package org.hkijena.jipipe.extensions.utils.algorithms.iterationsteps;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeIOSlotConfiguration;
import org.hkijena.jipipe.api.nodes.AddJIPipeNodeAlias;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Check iteration steps (multiple data per slot)", description = "Pass multiple inputs through this node to check if iteration steps are correctly created and filter out incomplete steps. " +
        "This node is designed for creating iteration steps where multiple data items can be assigned to an iteration step input slot.")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Iteration steps")
@AddJIPipeNodeAlias(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Filter", aliasName = "Limit to iteration steps (multiple data per slot)")
@AddJIPipeNodeAlias(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Filter", aliasName = "Filter multiple data by annotation (multiple data per slot)")
public class MultiIterationStepCheckerAlgorithm extends JIPipeMergingAlgorithm {
    private boolean keepOriginalAnnotations = true;
    private JIPipeExpressionParameter filter = new JIPipeExpressionParameter();
    private OptionalAnnotationNameParameter iterationStepIndexAnnotation = new OptionalAnnotationNameParameter("Iteration step", false);
    public MultiIterationStepCheckerAlgorithm(JIPipeNodeInfo info) {
        super(info, new JIPipeIOSlotConfiguration());
    }

    public MultiIterationStepCheckerAlgorithm(MultiIterationStepCheckerAlgorithm other) {
        super(other);
        this.keepOriginalAnnotations = other.keepOriginalAnnotations;
        this.iterationStepIndexAnnotation = new OptionalAnnotationNameParameter(other.iterationStepIndexAnnotation);
        this.filter = new JIPipeExpressionParameter(other.filter);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        {
            JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
            variables.putAnnotations(iterationStep.getMergedTextAnnotations());
            variables.set("iteration_step_index", iterationContext.getCurrentIterationStepIndex());
            variables.set("num_iteration_steps", iterationContext.getNumIterationSteps());
            if(!filter.test(variables)) {
                progressInfo.log("Iteration step filtered out. Skipping.");
                return;
            }
        }
        for (JIPipeInputDataSlot inputSlot : getInputSlots()) {
            for (int row : iterationStep.getInputRows(inputSlot)) {
                JIPipeData inputData = inputSlot.getData(row, JIPipeData.class, progressInfo);
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
    }

    @SetJIPipeDocumentation(name = "Keep original annotations", description = "If enabled, keep the original annotations of the input data without merging them")
    @JIPipeParameter("keep-original-annotations")
    public boolean isKeepOriginalAnnotations() {
        return keepOriginalAnnotations;
    }

    @JIPipeParameter("keep-original-annotations")
    public void setKeepOriginalAnnotations(boolean keepOriginalAnnotations) {
        this.keepOriginalAnnotations = keepOriginalAnnotations;
    }

    @SetJIPipeDocumentation(name = "Annotate with iteration step index", description = "If enabled, annotate each output with the annotation step index")
    @JIPipeParameter("iteration-step-index-annotation")
    public OptionalAnnotationNameParameter getIterationStepIndexAnnotation() {
        return iterationStepIndexAnnotation;
    }

    @JIPipeParameter("iteration-step-index-annotation")
    public void setIterationStepIndexAnnotation(OptionalAnnotationNameParameter iterationStepIndexAnnotation) {
        this.iterationStepIndexAnnotation = iterationStepIndexAnnotation;
    }

    @SetJIPipeDocumentation(name = "Filter", description = "Allows to filter data batches")
    @JIPipeParameter(value = "filter", important = true)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "Current iteration step index", key = "iteration_step_index", description = "The index of the current iteration step")
    @JIPipeExpressionParameterVariable(name = "Number of iteration steps", key = "num_iteration_steps", description = "The number of iteration steps that are processed")
    public JIPipeExpressionParameter getFilter() {
        return filter;
    }

    @JIPipeParameter("filter")
    public void setFilter(JIPipeExpressionParameter filter) {
        this.filter = filter;
    }
}
