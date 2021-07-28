package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeMissingDataGeneratorAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

@JIPipeDocumentation(name = "Generate missing ROI lists", description = "Generates empty ROI lists for data that are not paired " +
        "with a matching ROI in the same data batch. " + JIPipeMissingDataGeneratorAlgorithm.GENERATOR_ALGORITHM_DESCRIPTION)
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
    protected void runGenerator(JIPipeMergingDataBatch dataBatch, JIPipeDataSlot inputSlot, JIPipeDataSlot outputSlot, JIPipeProgressInfo progressInfo) {
        dataBatch.addOutputData(outputSlot, new ROIListData(), progressInfo);
    }
}
