package org.hkijena.jipipe.extensions.ijweka.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.ijweka.datatypes.WekaModelData;

@JIPipeDocumentation(name = "Import Weka model", description = "Imports a Trainable Weka Segmentation model from a *.model file")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(value = FileData.class, slotName = "Input", description = "The input file", autoCreate = true)
@JIPipeOutputSlot(value = WekaModelData.class, slotName = "Output", description = "The model", autoCreate = true)
public class ImportWekaModelFromFileAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    public ImportWekaModelFromFileAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportWekaModelFromFileAlgorithm(ImportWekaModelFromFileAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        FileData fileData = dataBatch.getInputData(getFirstInputSlot(), FileData.class, progressInfo);
        WekaModelData modelData = new WekaModelData(fileData.toPath());
        dataBatch.addOutputData(getFirstOutputSlot(), modelData, progressInfo);
    }
}
