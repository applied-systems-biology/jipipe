package org.hkijena.jipipe.extensions.cellpose.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.cellpose.CellPosePretrainedModel;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleMaskData;
import org.hkijena.jipipe.extensions.python.OptionalPythonEnvironment;

@JIPipeDocumentation(name = "Cellpose training", description = "Trains a model with Cellpose. You start from an existing model or train from scratch.")
@JIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Images", autoCreate = true)
@JIPipeInputSlot(value = ImagePlus3DGreyscaleMaskData.class, slotName = "Masks", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Deep learning")
public class CellPoseTrainingAlgorithm extends JIPipeMergingAlgorithm {

    private boolean enableGPU = true;
    private CellPosePretrainedModel pretrainedModel = CellPosePretrainedModel.Cytoplasm;
    private int numEpochs = 500;
    private double learningRate = 0.2;
    private int batchSize = 8;
    private boolean useResidualConnections = true;
    private boolean useStyleVector = true;
    private boolean concatenateDownsampledLayers = false;
    private boolean enable3DSegmentation = true;
    private boolean cleanUpAfterwards = true;
    private double diameter = 30;
    private OptionalPythonEnvironment overrideEnvironment = new OptionalPythonEnvironment();

    public CellPoseTrainingAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public CellPoseTrainingAlgorithm(CellPoseTrainingAlgorithm other) {
        super(other);
    }

    @JIPipeDocumentation(name = "Learning rate")
    @JIPipeParameter("learning-rate")
    public double getLearningRate() {
        return learningRate;
    }

    @JIPipeParameter("learning-rate")
    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }

    @JIPipeDocumentation(name = "Batch size")
    @JIPipeParameter("batch-size")
    public int getBatchSize() {
        return batchSize;
    }

    @JIPipeParameter("batch-size")
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    @JIPipeDocumentation(name = "Use residual connections")
    @JIPipeParameter("use-residual-connections")
    public boolean isUseResidualConnections() {
        return useResidualConnections;
    }

    @JIPipeParameter("use-residual-connections")
    public void setUseResidualConnections(boolean useResidualConnections) {
        this.useResidualConnections = useResidualConnections;
    }

    @JIPipeDocumentation(name = "Use style vector")
    @JIPipeParameter("use-style-vector")
    public boolean isUseStyleVector() {
        return useStyleVector;
    }

    @JIPipeParameter("use-style-vector")
    public void setUseStyleVector(boolean useStyleVector) {
        this.useStyleVector = useStyleVector;
    }

    @JIPipeDocumentation(name = "Concatenate downsampled layers",
            description = "Concatenate downsampled layers with upsampled layers (off by default which means they are added)")
    @JIPipeParameter("concatenate-downsampled-layers")
    public boolean isConcatenateDownsampledLayers() {
        return concatenateDownsampledLayers;
    }

    @JIPipeParameter("concatenate-downsampled-layers")
    public void setConcatenateDownsampledLayers(boolean concatenateDownsampledLayers) {
        this.concatenateDownsampledLayers = concatenateDownsampledLayers;
    }

    @JIPipeDocumentation(name = "Diameter", description = "The cell diameter. Depending on the model, you can choose following values: " +
            "<ul>" +
            "<li><b>Cytoplasm</b>: You need to rescale all your images that structures have a diameter of about 30 pixels.</li>" +
            "<li><b>Nuclei</b>: You need to rescale all your images that structures have a diameter of about 17 pixels.</li>" +
            "<li><b>Custom</b>: You need to rescale all your images that structures have a diameter appropriate for the model.</li>" +
            "<li><b>None</b>: This will train from scratch. You can freely set the diameter. You also can set the diameter to 0 to disable scaling.</li>" +
            "</ul>")
    @JIPipeParameter("diameter")
    public double getDiameter() {
        return diameter;
    }

    @JIPipeParameter("diameter")
    public void setDiameter(double diameter) {
        this.diameter = diameter;
    }

    @JIPipeDocumentation(name = "Clean up data after processing", description = "If enabled, data is deleted from temporary directories after " +
            "the processing was finished. Disable this to make it possible to debug your scripts. The directories are accessible via the logs (Tools &gt; Logs).")
    @JIPipeParameter("cleanup-afterwards")
    public boolean isCleanUpAfterwards() {
        return cleanUpAfterwards;
    }

    @JIPipeParameter("cleanup-afterwards")
    public void setCleanUpAfterwards(boolean cleanUpAfterwards) {
        this.cleanUpAfterwards = cleanUpAfterwards;
    }

    @JIPipeDocumentation(name = "Override Python environment", description = "If enabled, a different Python environment is used for this Node. Otherwise " +
            "the one in the Project > Application settings > Extensions > Cellpose is used.")
    @JIPipeParameter("override-environment")
    public OptionalPythonEnvironment getOverrideEnvironment() {
        return overrideEnvironment;
    }

    @JIPipeParameter("override-environment")
    public void setOverrideEnvironment(OptionalPythonEnvironment overrideEnvironment) {
        this.overrideEnvironment = overrideEnvironment;
    }

    @JIPipeDocumentation(name = "Enable 3D segmentation", description = "If enabled, Cellpose will train in 3D. " +
            "Otherwise, JIPipe will prepare the data by splitting 3D data into planes.")
    @JIPipeParameter("enable-3d-segmentation")
    public boolean isEnable3DSegmentation() {
        return enable3DSegmentation;
    }

    @JIPipeParameter("enable-3d-segmentation")
    public void setEnable3DSegmentation(boolean enable3DSegmentation) {
        this.enable3DSegmentation = enable3DSegmentation;
    }

    @JIPipeDocumentation(name = "With GPU", description = "Utilize a GPU if available. Please note that you need to setup Cellpose " +
            "to allow usage of your GPU. Also ensure that enough memory is available.")
    @JIPipeParameter("enable-gpu")
    public boolean isEnableGPU() {
        return enableGPU;
    }

    @JIPipeParameter("enable-gpu")
    public void setEnableGPU(boolean enableGPU) {
        this.enableGPU = enableGPU;
    }

    @JIPipeDocumentation(name = "Model", description = "The pretrained model that should be used. You can either choose one of the models " +
            "provided by Cellpose, a custom model, or train from scratch. The pre-trained model has influence on the diameter and how the input images should be prepared:" +
            "<ul>" +
            "<li><b>Cytoplasm</b>: You need to rescale all your images that structures have a diameter of about 30 pixels.</li>" +
            "<li><b>Nuclei</b>: You need to rescale all your images that structures have a diameter of about 17 pixels.</li>" +
            "<li><b>Custom</b>: You need to rescale all your images that structures have a diameter appropriate for the model. Don't forget to set the diameter value.</li>" +
            "<li><b>None</b>: This will train from scratch. You can freely set the diameter. You also can set the diameter to 0 to disable scaling.</li>" +
            "</ul>")
    @JIPipeParameter("pretrained-model")
    public CellPosePretrainedModel getPretrainedModel() {
        return pretrainedModel;
    }

    @JIPipeParameter("pretrained-model")
    public void setPretrainedModel(CellPosePretrainedModel pretrainedModel) {
        this.pretrainedModel = pretrainedModel;

        // Update diameter
        switch (pretrainedModel) {
            case Cytoplasm:
                if(diameter != 30) {
                    JIPipeParameterCollection.setParameter(this, "diameter",  30.0);
                }
                break;
            case Nucleus:
                if(diameter != 17) {
                    JIPipeParameterCollection.setParameter(this, "diameter",  17.0);
                }
                break;
        }
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

    }

    @JIPipeDocumentation(name = "Epochs", description = "Number of epochs that should be trained.")
    @JIPipeParameter("epochs")
    public int getNumEpochs() {
        return numEpochs;
    }

    @JIPipeParameter("epochs")
    public void setNumEpochs(int numEpochs) {
        this.numEpochs = numEpochs;
    }
}
