package org.hkijena.jipipe.extensions.ijweka.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.ijweka.datatypes.WekaModelData;
import trainableSegmentation.WekaSegmentation;

@JIPipeDocumentation(name = "Import Weka model", description = "Imports a Trainable Weka Segmentation model from a *.model/*.arff file")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(value = FileData.class, slotName = "Model file", description = "The model file in *.model format", autoCreate = true)
@JIPipeInputSlot(value = FileData.class, slotName = "Data file", description = "The data file in *.arff format", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = WekaModelData.class, slotName = "Output", description = "The model", autoCreate = true)
public class ImportWekaModelFromFileAlgorithm extends JIPipeIteratingAlgorithm {

    public ImportWekaModelFromFileAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportWekaModelFromFileAlgorithm(ImportWekaModelFromFileAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        FileData modelFileData = dataBatch.getInputData("Model file", FileData.class, progressInfo);
        FileData dataFileData = dataBatch.getInputData("Data file", FileData.class, progressInfo);
        WekaSegmentation segmentation = new WekaSegmentation();
        if(dataFileData != null) {
            segmentation.loadTrainingData(dataFileData.getPath());
        }
        segmentation.loadClassifier(modelFileData.getPath());
        WekaModelData modelData = new WekaModelData(segmentation);
        dataBatch.addOutputData(getFirstOutputSlot(), modelData, progressInfo);
    }
}
