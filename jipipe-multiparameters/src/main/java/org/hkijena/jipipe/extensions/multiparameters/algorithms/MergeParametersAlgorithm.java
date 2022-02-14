package org.hkijena.jipipe.extensions.multiparameters.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;

@JIPipeDocumentation(name = "Merge parameters", description = "Merges multiple parameter sets. To always multiply all incoming parameters, set the data batch grouping method to 'Multiply'. " +
        "Parameters with the same unique key are overwritten according to the input slot order.")
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Parameters")
@JIPipeInputSlot(value = ParametersData.class, slotName = "Input 1", autoCreate = true)
@JIPipeInputSlot(value = ParametersData.class, slotName = "Input 2", autoCreate = true)
@JIPipeOutputSlot(value = ParametersData.class, slotName = "Output", autoCreate = true)
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ParametersData outputData = new ParametersData();
        for (JIPipeDataSlot inputSlot : getInputSlots()) {
            ParametersData inputData = dataBatch.getInputData(inputSlot, ParametersData.class, progressInfo);
            outputData.getParameterData().putAll(inputData.getParameterData());
        }
        dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }
}
