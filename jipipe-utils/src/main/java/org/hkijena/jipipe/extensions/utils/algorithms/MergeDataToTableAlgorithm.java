package org.hkijena.jipipe.extensions.utils.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;

@JIPipeDocumentation(name = "Merge into tables", description = "Creates data batches from the incoming data and merges them into data table data. " +
        "Such tables might be needed for some nodes that process lists of data.")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeDataTable.class, slotName = "Tables", autoCreate = true)
public class MergeDataToTableAlgorithm extends JIPipeMergingAlgorithm {
    public MergeDataToTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MergeDataToTableAlgorithm(MergeDataToTableAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        JIPipeDataTable dataTable = new JIPipeDataTable(JIPipeData.class);
        for (int row : dataBatch.getInputSlotRows().get(getFirstInputSlot())) {
            dataTable.addData(getFirstInputSlot().getVirtualData(row),
                    getFirstInputSlot().getTextAnnotations(row),
                    JIPipeTextAnnotationMergeMode.OverwriteExisting,
                    getFirstInputSlot().getDataAnnotations(row),
                    JIPipeDataAnnotationMergeMode.OverwriteExisting);
        }
        dataBatch.addOutputData(getFirstOutputSlot(), dataTable, progressInfo);
    }
}
