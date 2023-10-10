package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.metadata;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.HashMap;
import java.util.Map;

@JIPipeDocumentation(name = "Set image metadata from table", description = "Sets the image metadata (property map) from a table. Only the first row is utilized.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Metadata")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", autoCreate = true)
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Metadata", description = "Table of image metadata (one row)", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
public class SetImageMetadataFromTableAlgorithm extends JIPipeIteratingAlgorithm {

    private boolean clearBeforeWrite = false;

    public SetImageMetadataFromTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SetImageMetadataFromTableAlgorithm(SetImageMetadataFromTableAlgorithm other) {
        super(other);
        this.clearBeforeWrite = other.clearBeforeWrite;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus imagePlus = dataBatch.getInputData("Image", ImagePlusData.class, progressInfo).getDuplicateImage();
        ResultsTableData metadata = dataBatch.getInputData("Metadata", ResultsTableData.class, progressInfo);

        if (metadata.getRowCount() > 0) {
            Map<String, String> properties = clearBeforeWrite ? new HashMap<>() : ImageJUtils.getImageProperties(imagePlus);
            for (String columnName : metadata.getColumnNames()) {
                properties.put(columnName, metadata.getValueAsString(0, columnName));
            }
            ImageJUtils.setImageProperties(imagePlus, properties);
        }

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(imagePlus), progressInfo);
    }

    @JIPipeDocumentation(name = "Clear properties before write", description = "If enabled, all existing ROI properties are deleted before writing the new properties")
    @JIPipeParameter("clear-before-write")
    public boolean isClearBeforeWrite() {
        return clearBeforeWrite;
    }

    @JIPipeParameter("clear-before-write")
    public void setClearBeforeWrite(boolean clearBeforeWrite) {
        this.clearBeforeWrite = clearBeforeWrite;
    }
}
