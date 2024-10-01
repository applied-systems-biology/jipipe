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

package org.hkijena.jipipe.plugins.utils.algorithms.iterationsteps;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeNodeAlias;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeIOSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Check iteration steps (one data per slot)",
        description = "Pass multiple inputs through this node to check if iteration steps are correctly created and filter out incomplete steps. " +
                "This node is designed for creating iteration steps where one data item is assigned to each iteration step input slot.")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Iteration steps")
@AddJIPipeNodeAlias(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Filter", aliasName = "Limit to iteration steps (one data per slot)")
@AddJIPipeNodeAlias(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Filter", aliasName = "Filter multiple data by annotation (one data per slot)")
public class SingleIterationStepCheckerAlgorithm extends JIPipeIteratingAlgorithm {
    private boolean keepOriginalAnnotations = true;
    private OptionalTextAnnotationNameParameter iterationStepIndexAnnotation = new OptionalTextAnnotationNameParameter("Iteration step", false);
    private JIPipeExpressionParameter filter = new JIPipeExpressionParameter();
    private boolean enableFilter = true;

    public SingleIterationStepCheckerAlgorithm(JIPipeNodeInfo info) {
        super(info, new JIPipeIOSlotConfiguration());
    }

    public SingleIterationStepCheckerAlgorithm(SingleIterationStepCheckerAlgorithm other) {
        super(other);
        this.keepOriginalAnnotations = other.keepOriginalAnnotations;
        this.iterationStepIndexAnnotation = new OptionalTextAnnotationNameParameter(other.iterationStepIndexAnnotation);
        this.filter = new JIPipeExpressionParameter(other.filter);
        this.enableFilter = other.enableFilter;
    }

    @SetJIPipeDocumentation(name = "Enable filter", description = "Determines if the filter is enabled")
    @JIPipeParameter(value = "enable-filter", important = true)
    public boolean isEnableFilter() {
        return enableFilter;
    }

    @JIPipeParameter("enable-filter")
    public void setEnableFilter(boolean enableFilter) {
        this.enableFilter = enableFilter;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        if(enableFilter) {
            JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
            variables.putAnnotations(iterationStep.getMergedTextAnnotations());
            getDefaultCustomExpressionVariables().writeToVariables(variables);
            variables.set("iteration_step_index", iterationContext.getCurrentIterationStepIndex());
            variables.set("num_iteration_steps", iterationContext.getNumIterationSteps());
            if (!filter.test(variables)) {
                progressInfo.log("Iteration step filtered out. Skipping.");
                return;
            }
        }
        for (JIPipeInputDataSlot inputSlot : getInputSlots()) {
            JIPipeData inputData = iterationStep.getInputData(inputSlot, JIPipeData.class, progressInfo);
            int row = iterationStep.getInputRow(inputSlot);
            List<JIPipeTextAnnotation> annotationList;
            if (keepOriginalAnnotations) {
                annotationList = new ArrayList<>(inputSlot.getTextAnnotations(row));
            } else {
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
    public OptionalTextAnnotationNameParameter getIterationStepIndexAnnotation() {
        return iterationStepIndexAnnotation;
    }

    @JIPipeParameter("iteration-step-index-annotation")
    public void setIterationStepIndexAnnotation(OptionalTextAnnotationNameParameter iterationStepIndexAnnotation) {
        this.iterationStepIndexAnnotation = iterationStepIndexAnnotation;
    }

    @SetJIPipeDocumentation(name = "Filter", description = "Allows to filter data batches")
    @JIPipeParameter(value = "filter", important = true)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(name = "Current iteration step index", key = "iteration_step_index", description = "The index of the current iteration step")
    @AddJIPipeExpressionParameterVariable(name = "Number of iteration steps", key = "num_iteration_steps", description = "The number of iteration steps that are processed")
    public JIPipeExpressionParameter getFilter() {
        return filter;
    }

    @JIPipeParameter("filter")
    public void setFilter(JIPipeExpressionParameter filter) {
        this.filter = filter;
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }
}
