/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.ijweka.nodes;

import ij.ImagePlus;
import ij.ImageStack;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.ijweka.datatypes.WekaModelData;
import org.hkijena.jipipe.plugins.ijweka.parameters.collections.WekaTiling2DSettings;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.transform.TileImage2DAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.transform.UnTileImage2DAlgorithm;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;
import trainableSegmentation.WekaSegmentation;

@SetJIPipeDocumentation(name = "Weka classifier 2D", description = "Classifies an image with a Weka model. If higher-dimensional data is provided, the classification is applied per slice. To obtain ROI from the generated labels, utilize the 'Labels to ROI' node.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Weka")
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", description = "Image on which the classification should be applied", create = true)
@AddJIPipeInputSlot(value = WekaModelData.class, slotName = "Model", description = "The model", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "Classified image", description = "The classified image", create = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus image = iterationStep.getInputData("Image", ImagePlusData.class, progressInfo).getDuplicateImage();
        WekaModelData modelData = iterationStep.getInputData("Model", WekaModelData.class, progressInfo);
        WekaSegmentation segmentation = modelData.getSegmentation();

        TileImage2DAlgorithm tileImage2DAlgorithm = JIPipe.createNode(TileImage2DAlgorithm.class);
        tileImage2DAlgorithm.setOverlapX(tilingSettings.getOverlapX());
        tileImage2DAlgorithm.setOverlapY(tilingSettings.getOverlapY());
        tileImage2DAlgorithm.setTileSizeX(tilingSettings.getTileSizeX());
        tileImage2DAlgorithm.setTileSizeY(tilingSettings.getTileSizeY());
        tileImage2DAlgorithm.setBorderMode(tilingSettings.getBorderMode());
        tileImage2DAlgorithm.setTileInsetXAnnotation(new OptionalTextAnnotationNameParameter("inset_x", true));
        tileImage2DAlgorithm.setTileInsetYAnnotation(new OptionalTextAnnotationNameParameter("inset_y", true));
        tileImage2DAlgorithm.setTileRealXAnnotation(new OptionalTextAnnotationNameParameter("real_x", true));
        tileImage2DAlgorithm.setTileRealYAnnotation(new OptionalTextAnnotationNameParameter("real_y", true));
        tileImage2DAlgorithm.setImageWidthAnnotation(new OptionalTextAnnotationNameParameter("img_width", true));
        tileImage2DAlgorithm.setImageHeightAnnotation(new OptionalTextAnnotationNameParameter("img_height", true));

        UnTileImage2DAlgorithm unTileImage2DAlgorithm = JIPipe.createNode(UnTileImage2DAlgorithm.class);
        unTileImage2DAlgorithm.setTileInsetXAnnotation(new OptionalTextAnnotationNameParameter("inset_x", true));
        unTileImage2DAlgorithm.setTileInsetYAnnotation(new OptionalTextAnnotationNameParameter("inset_y", true));
        unTileImage2DAlgorithm.setTileRealXAnnotation(new OptionalTextAnnotationNameParameter("real_x", true));
        unTileImage2DAlgorithm.setTileRealYAnnotation(new OptionalTextAnnotationNameParameter("real_y", true));
        unTileImage2DAlgorithm.setImageWidthAnnotation(new OptionalTextAnnotationNameParameter("img_width", true));
        unTileImage2DAlgorithm.setImageHeightAnnotation(new OptionalTextAnnotationNameParameter("img_height", true));

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
                        tileImage2DAlgorithm.clearSlotData(false, progressInfo);
                        tileImage2DAlgorithm.getFirstInputSlot().addData(new ImagePlusData(wholeSlice), progressInfo);
                        tileImage2DAlgorithm.run(runContext, progressInfo.resolve("Generate tiles"));

                        // Classify tiles
                        JIPipeDataTable tileTable = tileImage2DAlgorithm.getFirstOutputSlot();
                        for (int i = 0; i < tileTable.getRowCount(); i++) {
                            JIPipeProgressInfo tileProgress = progressInfo.resolveAndLog("Classify tiles", i, tileTable.getRowCount());
                            ImagePlus tileSlice = tileTable.getData(i, ImagePlusData.class, tileProgress).getImage();
                            ImagePlus classifiedTileSlice = segmentation.applyClassifier(tileSlice, numThreads.getContentOrDefault(0), outputProbabilityMaps);
                            tileTable.setData(i, new ImagePlusData(classifiedTileSlice));
                        }

                        // Merge tiles
                        unTileImage2DAlgorithm.clearSlotData(false, progressInfo);
                        unTileImage2DAlgorithm.getFirstInputSlot().addDataFromTable(tileTable, progressInfo);
                        unTileImage2DAlgorithm.run(runContext, progressInfo.resolve("Merge tiles"));

                        classified = unTileImage2DAlgorithm.getFirstOutputSlot().getData(0, ImagePlusData.class, progressInfo).getImage();

                        // Cleanup
                        tileImage2DAlgorithm.clearSlotData(false, progressInfo);
                        unTileImage2DAlgorithm.clearSlotData(false, progressInfo);
                    }
                } else {
                    progressInfo.log("Classifying whole image " + wholeSlice);
                    classified = segmentation.applyClassifier(wholeSlice, numThreads.getContentOrDefault(0), outputProbabilityMaps);
                }
                stack.setProcessor(classified.getProcessor(), index.zeroSliceIndexToOneStackIndex(image));
            }, progressInfo);
        }

        iterationStep.addOutputData("Classified image", new ImagePlusData(new ImagePlus("Classified", stack)), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Generate tiles", description = "The following settings allow the generation of tiles to save memory.")
    @JIPipeParameter("tiling-parameters")
    public WekaTiling2DSettings getTilingSettings() {
        return tilingSettings;
    }

    @SetJIPipeDocumentation(name = "Override number of threads", description = "If enabled, set the number of threads to be utilized. Set to zero for automated assignment of threads.")
    @JIPipeParameter("num-threads")
    public OptionalIntegerParameter getNumThreads() {
        return numThreads;
    }

    @JIPipeParameter("num-threads")
    public void setNumThreads(OptionalIntegerParameter numThreads) {
        this.numThreads = numThreads;
    }

    @SetJIPipeDocumentation(name = "Output probability maps", description = "If enabled, output probability maps instead of class labels.")
    @JIPipeParameter("output-probability-maps")
    public boolean isOutputProbabilityMaps() {
        return outputProbabilityMaps;
    }

    @JIPipeParameter("output-probability-maps")
    public void setOutputProbabilityMaps(boolean outputProbabilityMaps) {
        this.outputProbabilityMaps = outputProbabilityMaps;
    }
}
