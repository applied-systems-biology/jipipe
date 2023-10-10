package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.metadata;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeSingleDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

@JIPipeDocumentation(name = "Extract image metadata as table", description = "Extracts the metadata (properties map) of the image and writes them into a table")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Metadata")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class ExtractImageMetadataAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    public ExtractImageMetadataAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractImageMetadataAlgorithm(ExtractImageMetadataAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus imagePlus = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        ResultsTableData table = new ResultsTableData();
        Map<String, Object> tableRow = new HashMap<>();
        if (imagePlus.getImageProperties() != null) {
            for (Map.Entry<Object, Object> entry : imagePlus.getImageProperties().entrySet()) {
                tableRow.put(StringUtils.nullToEmpty(entry.getKey()), entry.getValue());
            }
        }
        table.addRow(tableRow);
        dataBatch.addOutputData(getFirstOutputSlot(), table, progressInfo);
    }
}
