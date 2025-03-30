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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.color;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.RGBStackMerge;
import ij.process.ImageProcessor;
import ij.process.LUT;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.parameters.library.graph.InputSlotMapParameterCollection;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.Vector2dParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.Vector2iParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.VectorParameterSettings;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;

import java.awt.*;
import java.util.*;
import java.util.List;

@SetJIPipeDocumentation(name = "Merge channels (composite)", description = "Creates a composite image by merging channels of the inputs. " +
        "Behaves similar to the ImageJ channel merger with composite setting, but with custom colors. " +
        "Please note that the channels will be merged in order of the input slots.")
@ConfigureJIPipeNode(menuPath = "Colors", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input")
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nColor", aliasName = "Merge Channels... (composite)")
public class MergeChannelsCompositeAlgorithm extends JIPipeIteratingAlgorithm {

    private final InputSlotMapParameterCollection channelColorAssignment;
    private boolean autoCalibrate = false;
    private ImageJCalibrationMode calibrationMode = ImageJCalibrationMode.AutomaticImageJ;
    private Vector2dParameter customRange = new Vector2dParameter();

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public MergeChannelsCompositeAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().restrictInputTo(ImagePlusGreyscaleData.class)
                .addOutputSlot("Output", "", ImagePlusData.class)
                .sealOutput()
                .build());
        channelColorAssignment = new InputSlotMapParameterCollection(Color.class, this, this::getNewChannelColor, false);
        channelColorAssignment.updateSlots();
        registerSubParameter(channelColorAssignment);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public MergeChannelsCompositeAlgorithm(MergeChannelsCompositeAlgorithm other) {
        super(other);
        channelColorAssignment = new InputSlotMapParameterCollection(Color.class, this, this::getNewChannelColor, false);
        other.channelColorAssignment.copyTo(channelColorAssignment);
        registerSubParameter(channelColorAssignment);

        this.calibrationMode = other.calibrationMode;
        this.autoCalibrate = other.autoCalibrate;
        this.customRange = new Vector2dParameter(other.customRange);
    }

    private Color getNewChannelColor(JIPipeDataSlotInfo info) {
        return Color.WHITE;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        List<ImagePlus> images = new ArrayList<>();
        for (JIPipeInputDataSlot inputSlot : getDataInputSlots()) {
            ImagePlus image = iterationStep.getInputData(inputSlot, ImagePlusGreyscaleData.class, progressInfo).getImage();
            if(image.getNChannels() > 1) {
                throw new IllegalArgumentException("All images should have exactly one channel");
            }
            images.add(image);
        }

        Map<ImageSliceIndex, ImageProcessor> sliceMap = new HashMap<>();
        for (int c = 0; c < images.size(); c++) {
            ImagePlus image = images.get(c);
            int finalC = c;
            ImageJIterationUtils.forEachIndexedZCTSlice(image, (ip, index) -> {
                sliceMap.put(new ImageSliceIndex(finalC, index.getZ(), index.getT()), ip);
            }, progressInfo.resolve("Image", c, images.size()));
        }

        ImagePlus imp2 = ImageJUtils.mergeMappedSlices(sliceMap);
        if(!imp2.isComposite()) {
            imp2 = new CompositeImage(imp2);
        }

        List<JIPipeInputDataSlot> dataInputSlots = getDataInputSlots();
        for (int c = 0; c < dataInputSlots.size(); c++) {
            JIPipeInputDataSlot inputSlot = dataInputSlots.get(c);
            Color color = channelColorAssignment.get(inputSlot.getName()).get(Color.class);
            LUT lut = ImageJUtils.createGradientLUT(Color.BLACK,color);
            ImageJUtils.setLut(imp2, lut, Collections.singleton(c));
        }

        if(autoCalibrate) {
            for (int c = 0; c < imp2.getNChannels(); c++) {
                ImageJUtils.calibrate(imp2, calibrationMode, customRange.getX(), customRange.getY(), Collections.singleton(c));
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(imp2), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Channel colors", description = "Assigns a color to the specified input slot")
    @JIPipeParameter("channel-color-assignments")
    public InputSlotMapParameterCollection getChannelColorAssignment() {
        return channelColorAssignment;
    }

    @SetJIPipeDocumentation(name = "Adjust displayed contrast", description = "If enabled, apply contrast enhancement")
    @JIPipeParameter("auto-calibrate")
    public boolean isAutoCalibrate() {
        return autoCalibrate;
    }

    @JIPipeParameter("auto-calibrate")
    public void setAutoCalibrate(boolean autoCalibrate) {
        this.autoCalibrate = autoCalibrate;
    }

    @SetJIPipeDocumentation(name = "Calibration mode", description = "Calibration mode that should be applied")
    @JIPipeParameter("calibration-mode")
    public ImageJCalibrationMode getCalibrationMode() {
        return calibrationMode;
    }

    @JIPipeParameter("calibration-mode")
    public void setCalibrationMode(ImageJCalibrationMode calibrationMode) {
        this.calibrationMode = calibrationMode;
    }

    @SetJIPipeDocumentation(name = "Calibration custom range", description = "If the calibration is set to custom, use the specified range")
    @JIPipeParameter("calibration-custom-range")
    @VectorParameterSettings(xLabel = "Min", yLabel = "Max")
    public Vector2dParameter getCustomRange() {
        return customRange;
    }

    @JIPipeParameter("calibration-custom-range")
    public void setCustomRange(Vector2dParameter customRange) {
        this.customRange = customRange;
    }
}
