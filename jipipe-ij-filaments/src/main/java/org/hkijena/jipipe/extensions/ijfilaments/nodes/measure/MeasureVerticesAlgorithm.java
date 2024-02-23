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

@SetJIPipeDocumentation(name = "Measure filament vertices", description = "Stores all available information about the vertices into a table")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Measure")
@AddJIPipeInputSlot(value = Filaments3DData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
public class MeasureVerticesAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public MeasureVerticesAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MeasureVerticesAlgorithm(JIPipeSimpleIteratingAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData inputData = iterationStep.getInputData(getFirstInputSlot(), Filaments3DData.class, progressInfo);
        ResultsTableData outputData = inputData.measureVertices();
        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }
}
