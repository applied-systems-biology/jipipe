package org.hkijena.jipipe.extensions.utils.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeSimpleIteratingAlgorithm;

@JIPipeDocumentation(name = "Duplicate data", description = "Creates a duplicate of the input data. Useful for debugging purposes.")
@JIPipeInputSlot(slotName = "Input", value = JIPipeData.class, autoCreate = true)
@JIPipeOutputSlot(slotName = "Output", value = JIPipeData.class, autoCreate = true)
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
public class DuplicateDataAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    public DuplicateDataAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public DuplicateDataAlgorithm(DuplicateDataAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        JIPipeData duplicate = dataBatch.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo).duplicate(progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), duplicate, progressInfo);
    }
}
