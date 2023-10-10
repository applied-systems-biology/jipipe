package org.hkijena.jipipe.extensions.ijweka.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.ijweka.datatypes.WekaModelData;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;
import trainableSegmentation.WekaSegmentation;

@JIPipeDocumentation(name = "Import Weka model", description = "Imports a Trainable Weka Segmentation model from a *.model/*.arff file")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(value = FileData.class, slotName = "Model file", description = "The model file in *.model format", autoCreate = true)
@JIPipeInputSlot(value = FileData.class, slotName = "Data file", description = "The data file in *.arff format", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = WekaModelData.class, slotName = "Output", description = "The model", autoCreate = true)
public class ImportWekaModelFromFileAlgorithm extends JIPipeIteratingAlgorithm {

    private boolean processing3D;

    public ImportWekaModelFromFileAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportWekaModelFromFileAlgorithm(ImportWekaModelFromFileAlgorithm other) {
        super(other);
        this.processing3D = other.processing3D;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        FileData modelFileData = dataBatch.getInputData("Model file", FileData.class, progressInfo);
        FileData dataFileData = dataBatch.getInputData("Data file", FileData.class, progressInfo);
        try (IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(progressInfo)) {
            WekaSegmentation segmentation = new WekaSegmentation(processing3D);
            if (dataFileData != null) {
                segmentation.loadTrainingData(dataFileData.getPath());
            }
            segmentation.loadClassifier(modelFileData.getPath());
            WekaModelData modelData = new WekaModelData(segmentation);
            dataBatch.addOutputData(getFirstOutputSlot(), modelData, progressInfo);
        }

    }

    @JIPipeDocumentation(name = "Is processing 3D data", description = "If enabled, indicates that the model is a 3D model. <strong>Please ensure to set the correct value; otherwise " +
            "the Weka model will fail to import or behave in unexpected ways</strong>")
    @JIPipeParameter(value = "is-processing-3d", important = true)
    public boolean isProcessing3D() {
        return processing3D;
    }

    @JIPipeParameter("is-processing-3d")
    public void setProcessing3D(boolean processing3D) {
        this.processing3D = processing3D;
    }
}
