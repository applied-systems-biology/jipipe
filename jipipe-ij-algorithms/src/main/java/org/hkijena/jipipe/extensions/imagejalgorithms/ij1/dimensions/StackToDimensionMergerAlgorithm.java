/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.dimensions;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.ImageJDataTypesExtension;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale16UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.util.ArrayList;
import java.util.List;

import static org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm.ITERATING_ALGORITHM_DESCRIPTION;

/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Merge stacks into dimension", description = "Merges multiple stacks that miss a dimension into the target dimension. " +
        "Requires that all incoming stacks have the same size and that there is only one slice in the created dimension. " +
        "The type of the output image is determined by the first input slot. " +
        "This node has similar functionality to the 'Combine stacks' node." +
        "\n\n" + ITERATING_ALGORITHM_DESCRIPTION)
@JIPipeNode(menuPath = "Dimensions", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class StackToDimensionMergerAlgorithm extends JIPipeIteratingAlgorithm {

    private HyperstackDimension createdDimension = HyperstackDimension.Channel;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public StackToDimensionMergerAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .restrictInputTo(ImageJDataTypesExtension.IMAGE_TYPES_DIMENSIONLESS)
                .addOutputSlot("Output", ImagePlusData.class, "*")
                .sealOutput()
                .build());
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public StackToDimensionMergerAlgorithm(StackToDimensionMergerAlgorithm other) {
        super(other);
        this.createdDimension = other.createdDimension;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        if (getEffectiveInputSlotCount() == 0)
            return;
        // We need to identify a proper type, so the bit depths are equal
        Class<? extends JIPipeData> targetType = getFirstInputSlot().getAcceptedDataType();
        if (targetType == ImagePlusData.class) {
            // Identify a proper type
            ImagePlus image = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
            switch (image.getBitDepth()) {
                case 8:
                    targetType = ImagePlusGreyscale8UData.class;
                    break;
                case 16:
                    targetType = ImagePlusGreyscale16UData.class;
                    break;
                case 24:
                    targetType = ImagePlusColorRGBData.class;
                    break;
                default:
                    targetType = ImagePlusGreyscale32FData.class;
                    break;
            }
        }
        List<ImagePlus> inputImages = new ArrayList<>();
        for (JIPipeDataSlot slot : getEffectiveInputSlots()) {
            ImagePlus img = ((ImagePlusData) dataBatch.getInputData(slot, targetType, progressInfo)).getImage();
            inputImages.add(img);
        }
        if (inputImages.isEmpty())
            return;
        if (inputImages.size() == 1) {
            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(inputImages.get(0)), progressInfo);
            return;
        }
        if (!ImageJUtils.imagesHaveSameSize(inputImages)) {
            throw new UserFriendlyRuntimeException("Images do not have the same size!",
                    "Images do not have the same size!",
                    getName(),
                    "All images in a batch must have exactly the same size.",
                    "Please check if the input is correct.");
        }
        final int width = inputImages.get(0).getWidth();
        final int height = inputImages.get(0).getHeight();
        final int numZ = inputImages.get(0).getNSlices();
        final int numC = inputImages.get(0).getNChannels();
        final int numT = inputImages.get(0).getNFrames();

        if (createdDimension == HyperstackDimension.Depth) {
            if (numZ > 1) {
                throw new UserFriendlyRuntimeException("Images must have no Z dimension!",
                        "Z dimension already exists!",
                        getName(),
                        "To create a new Z dimension based on the incoming stacks, they cannot already have a Z dimension.",
                        "Remove the Z dimension or check if your input is correct.");
            }
            ImageStack stack = new ImageStack(width, height, inputImages.size() * numC * numT);
            for (int i = 0; i < inputImages.size(); i++) {
                JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Slot", i, inputImages.size());
                final int z = i;
                ImageJUtils.forEachIndexedZCTSlice(inputImages.get(i), (ip, index) -> {
                    int targetStackIndex = ImageJUtils.zeroSliceIndexToOneStackIndex(index.getC(), z, index.getT(), numC, inputImages.size(), numT);
                    stack.setProcessor(ip, targetStackIndex);
                }, slotProgress);
            }
            ImagePlus result = new ImagePlus(createdDimension.toString(), stack);
            result.setDimensions(numC, inputImages.size(), numT);
            if(!inputImages.isEmpty())
                result.copyScale(inputImages.get(0));
            dataBatch.addOutputData(getFirstOutputSlot(), JIPipe.createData(targetType, result), progressInfo);
        } else if (createdDimension == HyperstackDimension.Channel) {
            if (numC > 1) {
                throw new UserFriendlyRuntimeException("Images must have no channel dimension!",
                        "Channel dimension already exists!",
                        getName(),
                        "To create a new channel dimension based on the incoming stacks, they cannot already have a channel dimension.",
                        "Remove the channel dimension or check if your input is correct.");
            }
            ImageStack stack = new ImageStack(width, height, inputImages.size() * numZ * numT);
            for (int i = 0; i < inputImages.size(); i++) {
                JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Slot", i, inputImages.size());
                final int c = i;
                int finalI = i;
                ImageJUtils.forEachIndexedZCTSlice(inputImages.get(i), (ip, index) -> {
                    int targetStackIndex = ImageJUtils.zeroSliceIndexToOneStackIndex(c, index.getZ(), index.getT(), inputImages.size(), numZ, numT);
                    System.out.println(finalI + "# " + index + " -> " + targetStackIndex + " in " + (inputImages.size() * numZ * numT));
                    stack.setProcessor(ip, targetStackIndex);
                }, slotProgress);
            }
            ImagePlus result = new ImagePlus(createdDimension.toString(), stack);
            result.setDimensions(inputImages.size(), numZ, numT);
            if(!inputImages.isEmpty())
                result.copyScale(inputImages.get(0));
            dataBatch.addOutputData(getFirstOutputSlot(), JIPipe.createData(targetType, result), progressInfo);
        } else if (createdDimension == HyperstackDimension.Frame) {
            if (numT > 1) {
                throw new UserFriendlyRuntimeException("Images must have no time dimension!",
                        "Time dimension already exists!",
                        getName(),
                        "To create a new channel dimension based on the incoming stacks, they cannot already have a time dimension.",
                        "Remove the time dimension or check if your input is correct.");
            }
            ImageStack stack = new ImageStack(width, height, inputImages.size() * numZ * numC);
            for (int i = 0; i < inputImages.size(); i++) {
                JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Slot", i, inputImages.size());
                final int t = i;
                ImageJUtils.forEachIndexedZCTSlice(inputImages.get(i), (ip, index) -> {
                    int targetStackIndex = ImageJUtils.zeroSliceIndexToOneStackIndex(index.getC(), index.getZ(), t, numC, numZ, inputImages.size());
                    stack.setProcessor(ip, targetStackIndex);
                }, slotProgress);
            }
            ImagePlus result = new ImagePlus(createdDimension.toString(), stack);
            result.setDimensions(numC, numZ, inputImages.size());
            if(!inputImages.isEmpty())
                result.copyScale(inputImages.get(0));
            dataBatch.addOutputData(getFirstOutputSlot(), JIPipe.createData(targetType, result), progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Created dimension", description = "The dimension that is created by merging the " +
            "incoming stacks.")
    @JIPipeParameter("created-dimension")
    public HyperstackDimension getCreatedDimension() {
        return createdDimension;
    }

    @JIPipeParameter("created-dimension")
    public void setCreatedDimension(HyperstackDimension createdDimension) {
        this.createdDimension = createdDimension;
    }

    @JIPipeDocumentation(name = "3 stack merge", description = "Loads example parameters that merge three stacks.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/channelmixer.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/channelmixer.png")
    public void setTo3ChannelExample(JIPipeWorkbench parent) {
        if (UIUtils.confirmResetParameters(parent, "Load example")) {
            JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
            slotConfiguration.clearInputSlots(true);
            for (int i = 0; i < 3; i++) {
                slotConfiguration.addSlot("C" + (i + 1), new JIPipeDataSlotInfo(ImagePlusData.class, JIPipeSlotType.Input), true);
            }
        }
    }

    @JIPipeDocumentation(name = "2 stack merge", description = "Loads example parameters that merge two stacks.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/channelmixer.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/channelmixer.png")
    public void setTo2ChannelExample(JIPipeWorkbench parent) {
        if (UIUtils.confirmResetParameters(parent, "Load example")) {
            JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
            slotConfiguration.clearInputSlots(true);
            for (int i = 0; i < 2; i++) {
                slotConfiguration.addSlot("C" + (i + 1), new JIPipeDataSlotInfo(ImagePlusData.class, JIPipeSlotType.Input), true);
            }
        }
    }
}
