package org.hkijena.jipipe.extensions.ijfilaments.nodes.merge;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;

@SetJIPipeDocumentation(name = "Merge filaments", description = "Merges multiple filament graphs into one")
@DefineJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Split")
@AddJIPipeInputSlot(value = Filaments3DData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = Filaments3DData.class, slotName = "Output", create = true)
public class MergeFilamentsAlgorithm extends JIPipeMergingAlgorithm {

    public MergeFilamentsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MergeFilamentsAlgorithm(MergeFilamentsAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData outputData = new Filaments3DData();
        for (Filaments3DData data : iterationStep.getInputData(getFirstInputSlot(), Filaments3DData.class, progressInfo)) {
            outputData.mergeWith(data);
        }
        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

}
