package org.hkijena.jipipe.extensions.utils.algorithms.datatable;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;

@SetJIPipeDocumentation(name = "Merge into data tables", description = "Creates data batches from the incoming data and merges them into data table data. " +
        "Such tables might be needed for some nodes that process lists of data.")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Data tables")
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Data", create = true)
@AddJIPipeOutputSlot(value = JIPipeDataTable.class, slotName = "Tables", create = true)
public class MergeDataToTableAlgorithm extends JIPipeMergingAlgorithm {
    public MergeDataToTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MergeDataToTableAlgorithm(MergeDataToTableAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        JIPipeDataTable dataTable = new JIPipeDataTable(JIPipeData.class);
        for (int row : iterationStep.getInputSlotRows().get(getFirstInputSlot())) {
            dataTable.addData(getFirstInputSlot().getDataItemStore(row),
                    getFirstInputSlot().getTextAnnotations(row),
                    JIPipeTextAnnotationMergeMode.OverwriteExisting,
                    getFirstInputSlot().getDataAnnotations(row),
                    JIPipeDataAnnotationMergeMode.OverwriteExisting,
                    getFirstInputSlot().getDataContext(row).branch(this),
                    progressInfo);
        }
        iterationStep.addOutputData(getFirstOutputSlot(), dataTable, progressInfo);
    }
}
