package org.hkijena.jipipe.plugins.ijfilaments.nodes.modify;


import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.plugins.parameters.library.quantities.Quantity;

@SetJIPipeDocumentation(name = "Flatten filaments to 2D", description = "Moves the Z of each filament vertex to zero and sets the Z voxel size to zero, which flattens 3D filaments into 2D filaments.")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = Filaments3DData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = Filaments3DData.class, name = "Output", create = true)
public class FlattenFilamentsAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public FlattenFilamentsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public FlattenFilamentsAlgorithm(FlattenFilamentsAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData filaments3DData = new Filaments3DData(iterationStep.getInputData(getFirstInputSlot(), Filaments3DData.class, progressInfo));
        for (FilamentVertex filamentVertex : filaments3DData.vertexSet()) {
            filamentVertex.getSpatialLocation().setZ(0);
            filamentVertex.setPhysicalVoxelSizeZ(new Quantity(0, "px"));
        }
        iterationStep.addOutputData(getFirstOutputSlot(), filaments3DData, progressInfo);
    }
}
