package org.hkijena.jipipe.extensions.ijfilaments.nodes.process;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;

@JIPipeDocumentation(name = "Simplify filaments", description = "Removes all intermediate vertices (degree 2) and thus produces a graph that contains only the endpoints and junctions.")
@JIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Process")
@JIPipeInputSlot(value = Filaments3DData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = Filaments3DData.class, slotName = "Output", autoCreate = true)
public class SimplifyFilamentsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    public SimplifyFilamentsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SimplifyFilamentsAlgorithm(SimplifyFilamentsAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData inputData = iterationStep.getInputData(getFirstInputSlot(), Filaments3DData.class, progressInfo);
        Filaments3DData outputData = new Filaments3DData(inputData);
        outputData.simplify();
        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

}
