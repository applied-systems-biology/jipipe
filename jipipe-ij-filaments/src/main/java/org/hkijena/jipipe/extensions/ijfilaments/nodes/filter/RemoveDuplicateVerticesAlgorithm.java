package org.hkijena.jipipe.extensions.ijfilaments.nodes.filter;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.FilamentsData;

@JIPipeDocumentation(name = "Remove duplicate vertices", description = "Detects vertices with the same location and removes all duplicates. Edges are preserved. The metadata of deleted vertices will be removed.")
@JIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Process")
@JIPipeInputSlot(value = FilamentsData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = FilamentsData.class, slotName = "Output", autoCreate = true)
public class RemoveDuplicateVerticesAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    public RemoveDuplicateVerticesAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RemoveDuplicateVerticesAlgorithm(RemoveDuplicateVerticesAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        FilamentsData inputData = dataBatch.getInputData(getFirstInputSlot(), FilamentsData.class, progressInfo);
        FilamentsData outputData = new FilamentsData(inputData);
        outputData.removeDuplicateVertices();
        dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }
}
