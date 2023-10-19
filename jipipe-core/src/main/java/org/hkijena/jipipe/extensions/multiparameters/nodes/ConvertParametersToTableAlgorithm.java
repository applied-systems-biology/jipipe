package org.hkijena.jipipe.extensions.multiparameters.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        iterationStep.addOutputData(getFirstOutputSlot(), iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo), progressInfo);
    }
}
