package org.hkijena.jipipe.extensions.ijweka.nodes;

import ij.ImagePlus;
import ij.ImageStack;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijweka.datatypes.WekaModelData;
import org.hkijena.jipipe.extensions.ijweka.parameters.collections.WekaTiling2DSettings;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.TileImage2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.UnTileImage2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;
import trainableSegmentation.WekaSegmentation;

@JIPipeDocumentation(name = "Weka classifier 2D", description = "Classifies an image with a Weka model. If higher-dimensional data is provided, the classification is applied per slice. To obtain ROI from the generated labels, utilize the 'Labels to ROI' node.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Weka")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", description = "Image on which the classification should be applied", autoCreate = true)
@JIPipeInputSlot(value = WekaModelData.class, slotName = "Model", description = "The model", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Classified image", description = "The classified image", autoCreate = true)
public class WekaClassification2DAlgorithm extends JIPipeIteratingAlgorithm {

    private final WekaTiling2DSettings tilingSettings;
    private OptionalIntegerParameter numThreads = new OptionalIntegerParameter(false, 0);
    private boolean outputProbabilityMaps = false;

    public WekaClassification2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.tilingSettings = new WekaTiling2DSettings();
        registerSubParameter(tilingSettings);
    }

    public WekaClassification2DAlgorithm(WekaClassification2DAlgorithm other) {
        super(other);
        this.tilingSettings = new WekaTiling2DSettings(other.tilingSettings);
        registerSubParameter(tilingSettings);
        this.numThreads = new OptionalIntegerParameter(other.numThreads);
        this.outputProbabilityMaps = other.outputProbabilityMaps;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus image = dataBatch.getInputData("Image", ImagePlusData.class, progressInfo).getDuplicateImage();
        WekaModelData modelData = dataBatch.getInputData("Model", WekaModelData.class, progressInfo);
        WekaSegmentation segmentation = modelData.getSegmentation();

        TileImage2DAlgorithm tileImage2DAlgorithm = JIPipe.createNode(TileImage2DAlgorithm.class);
        tileImage2DAlgorithm.setOverlapX(tilingSettings.getOverlapX());
        tileImage2DAlgorithm.setOverlapY(tilingSettings.getOverlapY());
        tileImage2DAlgorithm.setTileSizeX(tilingSettings.getTileSizeX());
        tileImage2DAlgorithm.setTileSizeY(tilingSettings.getTileSizeY());
        tileImage2DAlgorithm.setBorderMode(tilingSettings.getBorderMode());
        tileImage2DAlgorithm.setTileInsetXAnnotation(new OptionalAnnotationNameParameter("inset_x", true));
        tileImage2DAlgorithm.setTileInsetYAnnotation(new OptionalAnnotationNameParameter("inset_y", true));
        tileImage2DAlgorithm.setTileRealXAnnotation(new OptionalAnnotationNameParameter("real_x", true));
        tileImage2DAlgorithm.setTileRealYAnnotation(new OptionalAnnotationNameParameter("real_y", true));
        tileImage2DAlgorithm.setImageWidthAnnotation(new OptionalAnnotationNameParameter("img_width", true));
        tileImage2DAlgorithm.setImageHeightAnnotation(new OptionalAnnotationNameParameter("img_height", true));

        UnTileImage2DAlgorithm unTileImage2DAlgorithm = JIPipe.createNode(UnTileImage2DAlgorithm.class);
        unTileImage2DAlgorithm.setTileInsetXAnnotation(new OptionalAnnotationNameParameter("inset_x", true));
        unTileImage2DAlgorithm.setTileInsetYAnnotation(new OptionalAnnotationNameParameter("inset_y", true));
        unTileImage2DAlgorithm.setTileRealXAnnotation(new OptionalAnnotationNameParameter("real_x", true));
        unTileImage2DAlgorithm.setTileRealYAnnotation(new OptionalAnnotationNameParameter("real_y", true));
        unTileImage2DAlgorithm.setImageWidthAnnotation(new OptionalAnnotationNameParameter("img_width", true));
        unTileImage2DAlgorithm.setImageHeightAnnotation(new OptionalAnnotationNameParameter("img_height", true));

        ImageStack stack = new ImageStack(image.getWidth(), image.getHeight(), image.getNSlices() * image.getNChannels() * image.getNChannels());
        try (IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(progressInfo.resolve("Weka"))) {
            ImageJUtils.forEachIndexedZCTSlice(image, (ip, index) -> {
                ImagePlus wholeSlice = new ImagePlus(image.getTitle() + " " + index, ip);
                ImagePlus classified;
                if (tilingSettings.isApplyTiling()) {
                    if (tilingSettings.isUseWekaNativeTiling()) {
                        int tilesX = (int) Math.ceil(1.0 * image.getWidth() / tilingSettings.getTileSizeX());
                        int tilesY = (int) Math.ceil(1.0 * image.getHeight() / tilingSettings.getTileSizeY());
                        progressInfo.log("Classifying with tiling (via Weka's native tiling)");
                        classified = segmentation.applyClassifier(wholeSlice, new int[]{tilesX, tilesY}, numThreads.getContentOrDefault(0), outputProbabilityMaps);
                    } else {
                        progressInfo.log("Generating tiles for " + wholeSlice);

                        // Generate tiles
                        tileImage2DAlgorithm.clearSlotData();
                        tileImage2DAlgorithm.getFirstInputSlot().addData(new ImagePlusData(wholeSlice), progressInfo);
                        tileImage2DAlgorithm.run(progressInfo.resolve("Generate tiles"));

                        // Classify tiles
                        JIPipeDataTable tileTable = tileImage2DAlgorithm.getFirstOutputSlot();
                        for (int i = 0; i < tileTable.getRowCount(); i++) {
                            JIPipeProgressInfo tileProgress = progressInfo.resolveAndLog("Classify tiles", i, tileTable.getRowCount());
                            ImagePlus tileSlice = tileTable.getData(i, ImagePlusData.class, tileProgress).getImage();
                            ImagePlus classifiedTileSlice = segmentation.applyClassifier(tileSlice, numThreads.getContentOrDefault(0), outputProbabilityMaps);
                            tileTable.setData(i, new ImagePlusData(classifiedTileSlice));
                        }

                        // Merge tiles
                        unTileImage2DAlgorithm.clearSlotData();
                        unTileImage2DAlgorithm.getFirstInputSlot().addFromTable(tileTable, progressInfo);
                        unTileImage2DAlgorithm.run(progressInfo.resolve("Merge tiles"));

                        classified = unTileImage2DAlgorithm.getFirstOutputSlot().getData(0, ImagePlusData.class, progressInfo).getImage();

                        // Cleanup
                        tileImage2DAlgorithm.clearSlotData();
                        unTileImage2DAlgorithm.clearSlotData();
                    }
                } else {
                    progressInfo.log("Classifying whole image " + wholeSlice);
                    classified = segmentation.applyClassifier(wholeSlice, numThreads.getContentOrDefault(0), outputProbabilityMaps);
                }
                stack.setProcessor(classified.getProcessor(), index.zeroSliceIndexToOneStackIndex(image));
            }, progressInfo);
        }

        dataBatch.addOutputData("Classified image", new ImagePlusData(new ImagePlus("Classified", stack)), progressInfo);
    }

    @JIPipeDocumentation(name = "Generate tiles", description = "The following settings allow the generation of tiles to save memory.")
    @JIPipeParameter("tiling-parameters")
    public WekaTiling2DSettings getTilingSettings() {
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
