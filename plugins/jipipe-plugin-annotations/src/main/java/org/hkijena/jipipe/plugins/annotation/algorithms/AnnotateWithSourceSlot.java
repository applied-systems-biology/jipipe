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
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates annotations from filenames
 */
@SetJIPipeDocumentation(name = "Annotate with source slot", description = "Annotates the data with the name or custom label of the source slot. Please note " +
        "that this node cannot resolve multiple input slots, as, for optimization purposes, the information where data is coming from is deleted.")
@ConfigureJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "For all data")
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Data", create = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, name = "Annotated data", create = true)
public class AnnotateWithSourceSlot extends JIPipeSimpleIteratingAlgorithm {

    private String generatedAnnotation = "Source slot";
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    /**
     * New instance
     *
     * @param info Algorithm info
     */
    public AnnotateWithSourceSlot(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other Original algorithm
     */
    public AnnotateWithSourceSlot(AnnotateWithSourceSlot other) {
        super(other);
        this.generatedAnnotation = other.generatedAnnotation;
        this.annotationMergeStrategy = other.annotationMergeStrategy;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        if (!StringUtils.isNullOrEmpty(generatedAnnotation)) {
            JIPipeData inputData = iterationStep.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo);

            String annotationValue;
            Set<JIPipeDataSlot> sourceSlots = getParentGraph().getInputIncomingSourceSlots(getFirstInputSlot());
            if (sourceSlots.isEmpty()) {
                annotationValue = "";
            } else if (sourceSlots.size() == 1) {
                JIPipeDataSlot sourceSlot = sourceSlots.iterator().next();
                annotationValue = StringUtils.orElse(sourceSlot.getInfo().getCustomName(), sourceSlot.getName());
            } else {
                annotationValue = JsonUtils.toJsonString(sourceSlots.stream().map(sourceSlot ->
                        StringUtils.orElse(sourceSlot.getInfo().getCustomName(), sourceSlot.getName())).collect(Collectors.toList()));
            }

            iterationStep.addMergedTextAnnotation(new JIPipeTextAnnotation(generatedAnnotation, annotationValue), annotationMergeStrategy);
            iterationStep.addOutputData(getFirstOutputSlot(), inputData, progressInfo);
        }
    }

    /**
     * @return Generated annotation type
     */
    @SetJIPipeDocumentation(name = "Generated annotation", description = "Select which annotation type is generated for each data row")
    @JIPipeParameter("generated-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public String getGeneratedAnnotation() {
        return generatedAnnotation;
    }

    /**
     * Sets generated annotation type
     *
     * @param generatedAnnotation Annotation type
     */
    @JIPipeParameter("generated-annotation")
    public void setGeneratedAnnotation(String generatedAnnotation) {
        this.generatedAnnotation = generatedAnnotation;
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
