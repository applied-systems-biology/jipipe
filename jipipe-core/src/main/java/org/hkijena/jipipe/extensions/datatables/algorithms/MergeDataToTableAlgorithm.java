package org.hkijena.jipipe.extensions.datatables.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;

@JIPipeDocumentation(name = "Merge into tables", description = "Creates data batches from the incoming data and merges them into data table data. " +
        "Such tables might be needed for some nodes that process lists of data.")
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeDataTableData.class, slotName = "Tables", autoCreate = true)
public class MergeDataToTableAlgorithm extends JIPipeMergingAlgorithm {
    public MergeDataToTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MergeDataToTableAlgorithm(MergeDataToTableAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        JIPipeDataSlot slot = new JIPipeDataSlot(new JIPipeDataSlotInfo(JIPipeData.class, JIPipeSlotType.Input, ""), null);
        for (Integer row : dataBatch.getInputSlotRows().get(getFirstInputSlot())) {
            slot.addData(getFirstInputSlot().getVirtualData(row),
                    getFirstInputSlot().getAnnotations(row),
                    JIPipeAnnotationMergeStrategy.OverwriteExisting);
        }
        dataBatch.addOutputData(getFirstOutputSlot(), new JIPipeDataTableData(slot), progressInfo);
    }
}