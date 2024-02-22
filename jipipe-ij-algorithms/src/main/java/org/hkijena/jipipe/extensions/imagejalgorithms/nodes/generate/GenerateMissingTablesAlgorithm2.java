package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.generate;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMissingDataGeneratorAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

@SetJIPipeDocumentation(name = "Generate missing tables", description = "Generates placeholder or empty tables for data that are not paired " +
        "with a matching table in the same data batch. ")
@DefineJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Data", create = true)
@AddJIPipeInputSlot(value = ResultsTableData.class, slotName = "Table", create = true, optional = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Table", create = true)
public class GenerateMissingTablesAlgorithm2 extends JIPipeMissingDataGeneratorAlgorithm {

    private ResultsTableData placeholderTable = new ResultsTableData();

    public GenerateMissingTablesAlgorithm2(JIPipeNodeInfo info) {
        super(info);
    }

    public GenerateMissingTablesAlgorithm2(GenerateMissingTablesAlgorithm2 other) {
        super(other);
        this.placeholderTable = new ResultsTableData(other.placeholderTable);
    }

    @SetJIPipeDocumentation(name = "Placeholder", description = "Table that is generated for missing values.")
    @JIPipeParameter("placeholder")
    public ResultsTableData getPlaceholderTable() {
        return placeholderTable;
    }

    @JIPipeParameter("placeholder")
    public void setPlaceholderTable(ResultsTableData placeholderTable) {
        this.placeholderTable = placeholderTable;
    }

    @Override
    protected void runGenerator(JIPipeMultiIterationStep iterationStep, JIPipeInputDataSlot inputSlot, JIPipeOutputDataSlot outputSlot, JIPipeProgressInfo progressInfo) {
        iterationStep.addOutputData(outputSlot, new ResultsTableData(placeholderTable), progressInfo);
    }
}
