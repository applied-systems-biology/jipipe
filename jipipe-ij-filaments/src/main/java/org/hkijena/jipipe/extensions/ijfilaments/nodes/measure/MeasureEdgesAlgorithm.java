package org.hkijena.jipipe.extensions.ijfilaments.nodes.measure;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

@SetJIPipeDocumentation(name = "Measure filament edges", description = "Stores all available information about the edges and involved vertices into a table")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Measure")
@AddJIPipeInputSlot(value = Filaments3DData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
public class MeasureEdgesAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public MeasureEdgesAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MeasureEdgesAlgorithm(JIPipeSimpleIteratingAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData inputData = iterationStep.getInputData(getFirstInputSlot(), Filaments3DData.class, progressInfo);
        ResultsTableData outputData = inputData.measureEdges();
        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }
}
