package org.hkijena.jipipe.extensions.multiparameters.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

@JIPipeDocumentation(name = "Convert parameters to table", description = "Converts parameter data into da table")
@JIPipeInputSlot(slotName = "Input", value = ParametersData.class, autoCreate = true)
@JIPipeOutputSlot(slotName = "Output", value = ResultsTableData.class, autoCreate = true)
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Convert")
public class ConvertParametersToTableAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public ConvertParametersToTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ConvertParametersToTableAlgorithm(ConvertParametersToTableAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        dataBatch.addOutputData(getFirstOutputSlot(), dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo), progressInfo);
    }
}
