package org.hkijena.jipipe.extensions.ijfilaments.nodes.process;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;

@JIPipeDocumentation(name = "Remove duplicate vertices", description = "Detects vertices with the same location and removes all duplicates. Edges are preserved. The metadata of deleted vertices will be removed.")
@JIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Process")
@JIPipeInputSlot(value = Filaments3DData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = Filaments3DData.class, slotName = "Output", autoCreate = true)
public class RemoveDuplicateVerticesAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean onlySameComponent = true;

    public RemoveDuplicateVerticesAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RemoveDuplicateVerticesAlgorithm(RemoveDuplicateVerticesAlgorithm other) {
        super(other);
        this.onlySameComponent = other.onlySameComponent;
    }

    @JIPipeDocumentation(name = "Only merge if in same component", description = "If enabled, vertices will be only merged if they are in the same component.")
    @JIPipeParameter("only-same-component")
    public boolean isOnlySameComponent() {
        return onlySameComponent;
    }

    @JIPipeParameter("only-same-component")
    public void setOnlySameComponent(boolean onlySameComponent) {
        this.onlySameComponent = onlySameComponent;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Filaments3DData inputData = dataBatch.getInputData(getFirstInputSlot(), Filaments3DData.class, progressInfo);
        Filaments3DData outputData = new Filaments3DData(inputData);
        outputData.removeDuplicateVertices(onlySameComponent);
        dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }
}
