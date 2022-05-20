package org.hkijena.jipipe.extensions.ijweka.nodes;

import ij.ImagePlus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijweka.datatypes.WekaModelData;
import org.hkijena.jipipe.extensions.ijweka.parameters.collections.WekaTiling2DSettings;
import org.hkijena.jipipe.extensions.ijweka.parameters.collections.WekaTiling3DSettings;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.TileImage2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.UnTileImage2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.ImagePlus3DData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerParameter;
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

        ImagePlus wholeImage = image.getImage();
        ImagePlus classified;

        if(tilingSettings.isApplyTiling()) {
            int tilesX = (int) Math.ceil(1.0 * image.getWidth() / tilingSettings.getTileSizeX());
            int tilesY = (int) Math.ceil(1.0 * image.getHeight() / tilingSettings.getTileSizeY());
            int tilesZ = (int) Math.ceil(1.0 * image.getNSlices() / tilingSettings.getTileSizeZ());
            classified = segmentation.applyClassifier(wholeImage, new int[] { tilesX, tilesY, tilesZ }, numThreads.getContentOrDefault(0), outputProbabilityMaps);
        }
        else {
            classified = segmentation.applyClassifier(wholeImage, numThreads.getContentOrDefault(0), outputProbabilityMaps);
        }

        dataBatch.addOutputData("Classified image", new ImagePlusData(classified), progressInfo);
    }

    @JIPipeDocumentation(name = "Generate tiles", description = "The following settings allow the generation of tiles to save memory.")
    @JIPipeParameter("tiling-parameters")
    public WekaTiling3DSettings getTilingParameters() {
        return tilingSettings;
    }

}
