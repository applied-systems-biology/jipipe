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

package org.hkijena.jipipe.extensions.annotation.algorithms;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
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
import org.hkijena.jipipe.extensions.parameters.api.functions.StringPatternExtractionFunction;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;

/**
 * Generates annotations from filenames
 */
@SetJIPipeDocumentation(name = "Extract & replace annotations", description = "Algorithm that allows you to extract parts of an annotation and either " +
        "replace the existing annotation or put the results into a new one. If you require more flexibility, please use 'Set/Edit annotations', which provide customizable mathematical expressions for generating or editing annotations.")
@ConfigureJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", create = true)
public class ExtractAndReplaceAnnotation extends JIPipeSimpleIteratingAlgorithm {

    private StringPatternExtractionFunction.List functions = new StringPatternExtractionFunction.List();
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    /**
     * New instance
     *
     * @param info Algorithm info
     */
    public ExtractAndReplaceAnnotation(JIPipeNodeInfo info) {
        super(info);
        functions.addNewInstance();
    }

    /**
     * Copies the algorithm
     *
     * @param other Original algorithm
     */
    public ExtractAndReplaceAnnotation(ExtractAndReplaceAnnotation other) {
        super(other);
        this.functions = new StringPatternExtractionFunction.List(other.functions);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        for (StringPatternExtractionFunction function : functions) {
            JIPipeTextAnnotation inputAnnotation = iterationStep.getMergedTextAnnotation(function.getInput());
            if (inputAnnotation == null)
                continue;
            String newValue = function.getParameter().apply(inputAnnotation.getValue());
            if (newValue == null)
                continue;
            iterationStep.addMergedTextAnnotation(new JIPipeTextAnnotation(function.getOutput(), newValue), annotationMergeStrategy);
        }
        iterationStep.addOutputData(getFirstOutputSlot(), iterationStep.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Functions", description = "The functions that allow you to extract and replace annotation values. " +
            "To extract values, you can split the incoming string into multiple components and then select the n-th component " +
            "or select the one that matches RegEx. Alternatively you can define a RegEx string that contains a matching group (brackets). " +
            "This matching group will then be picked.")
    @JIPipeParameter("functions")
    @StringParameterSettings(monospace = true)
    public StringPatternExtractionFunction.List getFunctions() {
        return functions;
    }

    @JIPipeParameter("functions")
    public void setFunctions(StringPatternExtractionFunction.List functions) {
        this.functions = functions;
    }


    @SetJIPipeDocumentation(name = "Merge same annotation values", description = "Determines which strategy is applied if an annotation already exists.")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }
}
