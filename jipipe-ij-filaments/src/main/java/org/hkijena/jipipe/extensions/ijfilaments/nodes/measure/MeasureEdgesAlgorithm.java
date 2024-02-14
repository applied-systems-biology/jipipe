package org.hkijena.jipipe.extensions.ijfilaments.nodes.measure;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

@JIPipeDocumentation(name = "Measure filament edges", description = "Stores all available information about the edges and involved vertices into a table")
@JIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Measure")
@JIPipeInputSlot(value = Filaments3DData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
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
