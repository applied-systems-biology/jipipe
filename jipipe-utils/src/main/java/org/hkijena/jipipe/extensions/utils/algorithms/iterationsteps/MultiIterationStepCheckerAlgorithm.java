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
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Check iteration steps (multiple data per slot)", description = "Pass multiple inputs through this node to check if iteration steps are correctly created and filter out incomplete steps. " +
        "This node is designed for creating iteration steps where multiple data items can be assigned to an iteration step input slot.")
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Iteration steps")
public class MultiIterationStepCheckerAlgorithm extends JIPipeMergingAlgorithm {
    private boolean keepOriginalAnnotations = true;

    private OptionalAnnotationNameParameter iterationStepIndexAnnotation = new OptionalAnnotationNameParameter("Iteration step", false);
    public MultiIterationStepCheckerAlgorithm(JIPipeNodeInfo info) {
        super(info, new JIPipeIOSlotConfiguration());
    }

    public MultiIterationStepCheckerAlgorithm(MultiIterationStepCheckerAlgorithm other) {
        super(other);
        this.keepOriginalAnnotations = other.keepOriginalAnnotations;
        this.iterationStepIndexAnnotation = new OptionalAnnotationNameParameter(other.iterationStepIndexAnnotation);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
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
}
