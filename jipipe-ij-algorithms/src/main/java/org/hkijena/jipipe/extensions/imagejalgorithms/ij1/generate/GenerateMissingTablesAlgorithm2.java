package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.generate;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

@JIPipeDocumentation(name = "Generate missing tables", description = "Generates placeholder or empty tables for data that are not paired " +
        "with a matching table in the same data batch. ")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Table", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Table", autoCreate = true)
public class GenerateMissingTablesAlgorithm2 extends JIPipeMissingDataGeneratorAlgorithm {

    private ResultsTableData placeholderTable = new ResultsTableData();

    public GenerateMissingTablesAlgorithm2(JIPipeNodeInfo info) {
        super(info);
    }

    public GenerateMissingTablesAlgorithm2(GenerateMissingTablesAlgorithm2 other) {
        super(other);
        this.placeholderTable = new ResultsTableData(other.placeholderTable);
    }

    @JIPipeDocumentation(name = "Placeholder", description = "Table that is generated for missing values.")
    @JIPipeParameter("placeholder")
    public ResultsTableData getPlaceholderTable() {
        return placeholderTable;
    }

    @JIPipeParameter("placeholder")
    public void setPlaceholderTable(ResultsTableData placeholderTable) {
        this.placeholderTable = placeholderTable;
    }

    @Override
    protected void runGenerator(JIPipeMergingDataBatch dataBatch, JIPipeDataSlot inputSlot, JIPipeDataSlot outputSlot, JIPipeProgressInfo progressInfo) {
        dataBatch.addOutputData(outputSlot, new ResultsTableData(placeholderTable), progressInfo);
    }
}
