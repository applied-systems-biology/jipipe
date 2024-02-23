package org.hkijena.jipipe.extensions.multiparameters.nodes;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

@SetJIPipeDocumentation(name = "Convert parameters to table", description = "Converts parameter data into da table")
@AddJIPipeInputSlot(slotName = "Input", value = ParametersData.class, create = true)
@AddJIPipeOutputSlot(slotName = "Output", value = ResultsTableData.class, create = true)
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Convert")
public class ConvertParametersToTableAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public ConvertParametersToTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ConvertParametersToTableAlgorithm(ConvertParametersToTableAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        iterationStep.addOutputData(getFirstOutputSlot(), iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo), progressInfo);
    }
}
