package org.hkijena.jipipe.extensions.multiparameters.nodes;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;

@SetJIPipeDocumentation(name = "Merge parameters", description = "Merges multiple parameter sets. To always multiply all incoming parameters, set the data batch grouping method to 'Multiply'. " +
        "Parameters with the same unique key are overwritten according to the input slot order.")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Parameters")
@AddJIPipeInputSlot(value = ParametersData.class, slotName = "Input 1", create = true)
@AddJIPipeInputSlot(value = ParametersData.class, slotName = "Input 2", create = true)
@AddJIPipeOutputSlot(value = ParametersData.class, slotName = "Output", create = true)
public class MergeParametersAlgorithm extends JIPipeIteratingAlgorithm {
    public MergeParametersAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addFromAnnotations(MergeParametersAlgorithm.class)
                .sealOutput()
                .build());
    }

    public MergeParametersAlgorithm(MergeParametersAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ParametersData outputData = new ParametersData();
        for (JIPipeDataSlot inputSlot : getInputSlots()) {
            ParametersData inputData = iterationStep.getInputData(inputSlot, ParametersData.class, progressInfo);
            outputData.getParameterData().putAll(inputData.getParameterData());
        }
        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }
}
