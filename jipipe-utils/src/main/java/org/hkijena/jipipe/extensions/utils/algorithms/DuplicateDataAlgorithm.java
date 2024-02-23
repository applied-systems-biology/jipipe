package org.hkijena.jipipe.extensions.utils.algorithms;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;

@SetJIPipeDocumentation(name = "Duplicate data", description = "Creates a duplicate of the input data. Useful for debugging purposes.")
@AddJIPipeInputSlot(slotName = "Input", value = JIPipeData.class, create = true)
@AddJIPipeOutputSlot(slotName = "Output", value = JIPipeData.class, create = true)
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
public class DuplicateDataAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    public DuplicateDataAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public DuplicateDataAlgorithm(DuplicateDataAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        JIPipeData duplicate = iterationStep.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo).duplicate(progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), duplicate, progressInfo);
    }
}
