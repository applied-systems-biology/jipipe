package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.generate;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMissingDataGeneratorAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

@JIPipeDocumentation(name = "Generate missing ROI lists", description = "Generates empty ROI lists for data that are not paired " +
        "with a matching ROI in the same data batch. ")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true)
public class GenerateMissingRoiListsAlgorithm extends JIPipeMissingDataGeneratorAlgorithm {
    public GenerateMissingRoiListsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public GenerateMissingRoiListsAlgorithm(GenerateMissingRoiListsAlgorithm other) {
        super(other);
    }

    @Override
    protected void runGenerator(JIPipeMergingDataBatch dataBatch, JIPipeInputDataSlot inputSlot, JIPipeOutputDataSlot outputSlot, JIPipeProgressInfo progressInfo) {
        dataBatch.addOutputData(outputSlot, new ROIListData(), progressInfo);
    }
}
