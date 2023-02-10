package org.hkijena.jipipe.extensions.ijfilaments.nodes.measure;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.FilamentsData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

@JIPipeDocumentation(name = "Measure filament vertices", description = "Stores all available information about the vertices into a table")
@JIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Measure")
@JIPipeInputSlot(value = FilamentsData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class MeasureVerticesAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public MeasureVerticesAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MeasureVerticesAlgorithm(JIPipeSimpleIteratingAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        FilamentsData inputData = dataBatch.getInputData(getFirstInputSlot(), FilamentsData.class, progressInfo);
        ResultsTableData outputData = inputData.measureVertices();
        dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }
}
