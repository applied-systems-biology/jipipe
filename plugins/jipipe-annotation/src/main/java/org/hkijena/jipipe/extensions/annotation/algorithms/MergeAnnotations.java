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
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeIOSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;

/**
 * Algorithm that merges the annotations of all inputs and outputs the data with the shared annotations
 */
@SetJIPipeDocumentation(name = "Merge annotations", description = "Merges the annotations of all incoming data and outputs the same data with those merged annotations.")
@ConfigureJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class)
public class MergeAnnotations extends JIPipeIteratingAlgorithm {
    /**
     * Creates a new instance
     *
     * @param info the info
     */
    public MergeAnnotations(JIPipeNodeInfo info) {
        super(info, new JIPipeIOSlotConfiguration());
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public MergeAnnotations(MergeAnnotations other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        for (JIPipeInputDataSlot inputSlot : getNonParameterInputSlots()) {
            JIPipeOutputDataSlot outputSlot = getOutputSlot(inputSlot.getName());
            iterationStep.addOutputData(outputSlot, iterationStep.getInputData(inputSlot, JIPipeData.class, progressInfo), progressInfo);
        }
    }

    @Override
    public boolean canPassThrough() {
        return true;
    }

    @Override
    protected void runPassThrough(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        for (JIPipeDataSlot inputSlot : getNonParameterInputSlots()) {
            JIPipeDataSlot outputSlot = getOutputSlot(inputSlot.getName());
            outputSlot.addDataFromSlot(inputSlot, progressInfo);
        }
    }
}
