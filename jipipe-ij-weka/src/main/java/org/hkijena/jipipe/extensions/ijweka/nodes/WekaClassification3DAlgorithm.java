package org.hkijena.jipipe.extensions.ijweka.nodes;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijweka.datatypes.WekaModelData;
import org.hkijena.jipipe.extensions.ijweka.parameters.collections.WekaTiling3DSettings;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.ImagePlus3DData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;
import trainableSegmentation.WekaSegmentation;

@JIPipeDocumentation(name = "Weka classifier 3D", description = "Classifies a 3D image with a Weka model. To obtain ROI from the generated labels, utilize the 'Labels to ROI' node.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Weka")
@JIPipeInputSlot(value = ImagePlus3DData.class, slotName = "Image", description = "Image on which the classification should be applied", autoCreate = true)
@JIPipeInputSlot(value = WekaModelData.class, slotName = "Model", description = "The model", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Classified image", description = "The classified image", autoCreate = true)
public class WekaClassification3DAlgorithm extends JIPipeIteratingAlgorithm {

    private WekaTiling3DSettings tilingSettings = new WekaTiling3DSettings();

    private OptionalIntegerParameter numThreads = new OptionalIntegerParameter(false, 0);
    private boolean outputProbabilityMaps = false;

    public WekaClassification3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(tilingSettings);
    }

    public WekaClassification3DAlgorithm(WekaClassification3DAlgorithm other) {
        super(other);
        this.tilingSettings = new WekaTiling3DSettings(other.tilingSettings);
        registerSubParameter(tilingSettings);
        this.numThreads = new OptionalIntegerParameter(other.numThreads);
        this.outputProbabilityMaps = other.outputProbabilityMaps;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData image = dataBatch.getInputData("Image", ImagePlus3DData.class, progressInfo);
        WekaModelData modelData = dataBatch.getInputData("Model", WekaModelData.class, progressInfo);
        WekaSegmentation segmentation = modelData.getSegmentation();

        ImagePlus wholeImage = image.getDuplicateImage();
        ImagePlus classified;

        if (tilingSettings.isApplyTiling()) {
            int tilesX = (int) Math.ceil(1.0 * image.getWidth() / tilingSettings.getTileSizeX());
            int tilesY = (int) Math.ceil(1.0 * image.getHeight() / tilingSettings.getTileSizeY());
            int tilesZ = (int) Math.ceil(1.0 * image.getNSlices() / tilingSettings.getTileSizeZ());
            try (IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(progressInfo.resolve("Weka"))) {
                classified = segmentation.applyClassifier(wholeImage, new int[]{tilesX, tilesY, tilesZ}, numThreads.getContentOrDefault(0), outputProbabilityMaps);
            }
        } else {
            try (IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(progressInfo.resolve("Weka"))) {
                classified = segmentation.applyClassifier(wholeImage, numThreads.getContentOrDefault(0), outputProbabilityMaps);
            }
        }

        dataBatch.addOutputData("Classified image", new ImagePlusData(classified), progressInfo);
    }

    @JIPipeDocumentation(name = "Generate tiles", description = "The following settings allow the generation of tiles to save memory.")
    @JIPipeParameter("tiling-parameters")
    public WekaTiling3DSettings getTilingParameters() {
        return tilingSettings;
    }

    @JIPipeDocumentation(name = "Override number of threads", description = "If enabled, set the number of threads to be utilized. Set to zero for automated assignment of threads.")
    @JIPipeParameter("num-threads")
    public OptionalIntegerParameter getNumThreads() {
        return numThreads;
    }

    @JIPipeParameter("num-threads")
    public void setNumThreads(OptionalIntegerParameter numThreads) {
        this.numThreads = numThreads;
    }

    @JIPipeDocumentation(name = "Output probability maps", description = "If enabled, output probability maps instead of class labels.")
    @JIPipeParameter("output-probability-maps")
    public boolean isOutputProbabilityMaps() {
        return outputProbabilityMaps;
    }

    @JIPipeParameter("output-probability-maps")
    public void setOutputProbabilityMaps(boolean outputProbabilityMaps) {
        this.outputProbabilityMaps = outputProbabilityMaps;
    }

}
