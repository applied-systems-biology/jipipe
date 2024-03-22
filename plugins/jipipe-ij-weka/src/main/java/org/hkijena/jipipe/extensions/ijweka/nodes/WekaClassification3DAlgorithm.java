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

package org.hkijena.jipipe.extensions.ijweka.nodes;

import ij.ImagePlus;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijweka.datatypes.WekaModelData;
import org.hkijena.jipipe.extensions.ijweka.parameters.collections.WekaTiling3DSettings;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.ImagePlus3DData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;
import trainableSegmentation.WekaSegmentation;

@SetJIPipeDocumentation(name = "Weka classifier 3D", description = "Classifies a 3D image with a Weka model. To obtain ROI from the generated labels, utilize the 'Labels to ROI' node.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Weka")
@AddJIPipeInputSlot(value = ImagePlus3DData.class, slotName = "Image", description = "Image on which the classification should be applied", create = true)
@AddJIPipeInputSlot(value = WekaModelData.class, slotName = "Model", description = "The model", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "Classified image", description = "The classified image", create = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData image = iterationStep.getInputData("Image", ImagePlus3DData.class, progressInfo);
        WekaModelData modelData = iterationStep.getInputData("Model", WekaModelData.class, progressInfo);
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

        iterationStep.addOutputData("Classified image", new ImagePlusData(classified), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Generate tiles", description = "The following settings allow the generation of tiles to save memory.")
    @JIPipeParameter("tiling-parameters")
    public WekaTiling3DSettings getTilingParameters() {
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
