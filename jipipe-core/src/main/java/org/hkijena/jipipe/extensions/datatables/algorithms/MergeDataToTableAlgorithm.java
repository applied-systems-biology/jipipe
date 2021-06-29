package org.hkijena.jipipe.extensions.datatables.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDataTableData;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;

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
                    JIPipeAnnotationMergeStrategy.OverwriteExisting,
                    getFirstInputSlot().getDataAnnotations(row),
                    JIPipeDataAnnotationMergeStrategy.OverwriteExisting);
        }
        dataBatch.addOutputData(getFirstOutputSlot(), new JIPipeDataTableData(slot), progressInfo);
    }
}
