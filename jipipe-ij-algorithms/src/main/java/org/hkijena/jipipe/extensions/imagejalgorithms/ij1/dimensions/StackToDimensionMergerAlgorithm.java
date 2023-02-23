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
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Merge stacks into dimension", description = "Merges multiple stacks that miss a dimension into the target dimension. " +
        "Requires that all incoming stacks have the same size and that there is only one slice in the created dimension. " +
        "The type of the output image is determined by the first input slot. " +
        "This node has similar functionality to the 'Combine stacks' node.")
@JIPipeNode(menuPath = "Dimensions", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nHyperstacks")
public class StackToDimensionMergerAlgorithm extends JIPipeMergingAlgorithm {

    private HyperstackDimension createdDimension = HyperstackDimension.Channel;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public StackToDimensionMergerAlgorithm(JIPipeNodeInfo info) {
        super(info);
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

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

        List<ImagePlus> inputImages = new ArrayList<>();
        for (ImagePlusData data : dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo)) {
            inputImages.add(data.getImage());
        }
        inputImages = ImageJUtils.convertToConsensusBitDepthIfNeeded(inputImages);

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
                List<ImagePlus> finalInputImages = inputImages;
                ImageJUtils.forEachIndexedZCTSlice(inputImages.get(i), (ip, index) -> {
                    int targetStackIndex = ImageJUtils.zeroSliceIndexToOneStackIndex(index.getC(), z, index.getT(), numC, finalInputImages.size(), numT);
                    stack.setProcessor(ip, targetStackIndex);
                }, slotProgress);
            }
            ImagePlus result = new ImagePlus(createdDimension.toString(), stack);
            result.setDimensions(numC, inputImages.size(), numT);
            if (!inputImages.isEmpty())
                result.copyScale(inputImages.get(0));
            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
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
                List<ImagePlus> finalInputImages1 = inputImages;
                ImageJUtils.forEachIndexedZCTSlice(inputImages.get(i), (ip, index) -> {
                    int targetStackIndex = ImageJUtils.zeroSliceIndexToOneStackIndex(c, index.getZ(), index.getT(), finalInputImages1.size(), numZ, numT);
                    System.out.println(finalI + "# " + index + " -> " + targetStackIndex + " in " + (finalInputImages1.size() * numZ * numT));
                    stack.setProcessor(ip, targetStackIndex);
                }, slotProgress);
            }
            ImagePlus result = new ImagePlus(createdDimension.toString(), stack);
            result.setDimensions(inputImages.size(), numZ, numT);
            if (!inputImages.isEmpty())
                result.copyScale(inputImages.get(0));
            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
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
                List<ImagePlus> finalInputImages2 = inputImages;
                ImageJUtils.forEachIndexedZCTSlice(inputImages.get(i), (ip, index) -> {
                    int targetStackIndex = ImageJUtils.zeroSliceIndexToOneStackIndex(index.getC(), index.getZ(), t, numC, numZ, finalInputImages2.size());
                    stack.setProcessor(ip, targetStackIndex);
                }, slotProgress);
            }
            ImagePlus result = new ImagePlus(createdDimension.toString(), stack);
            result.setDimensions(numC, numZ, inputImages.size());
            if (!inputImages.isEmpty())
                result.copyScale(inputImages.get(0));
            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
        }
    }
}
