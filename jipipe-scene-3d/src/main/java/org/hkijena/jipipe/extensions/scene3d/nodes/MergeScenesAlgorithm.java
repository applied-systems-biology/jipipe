package org.hkijena.jipipe.extensions.scene3d.nodes;


import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.extensions.scene3d.datatypes.Scene3DData;

@JIPipeDocumentation(name = "Merge 3D scenes", description = "Merges the input scenes into one")
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "3D Scenes")
@JIPipeInputSlot(value = Scene3DData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = Scene3DData.class, slotName = "Output", autoCreate = true)
public class MergeScenesAlgorithm extends JIPipeMergingAlgorithm {

    public MergeScenesAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MergeScenesAlgorithm(MergeScenesAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        Scene3DData outputData = new Scene3DData();
        for (Scene3DData nodes : iterationStep.getInputData(getFirstInputSlot(), Scene3DData.class, progressInfo)) {
            outputData.addAll(nodes);
        }
        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }
}
